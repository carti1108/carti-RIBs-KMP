// 실제 Kotlin RIB 트리(app-root + 피처 4개)를 React로 그려 가입 → 할 일 → 설정 → 로그아웃을 지나간다
// 할 일 목록은 테스트 HTTP 서버로 실제 요청을 보내 가져온다(Ktor Js 엔진, ADR-0013)
import { createServer, type Server } from 'node:http'
import type { AddressInfo } from 'node:net'
import { transferableAbortController } from 'node:util'
import { StrictMode } from 'react'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, beforeEach, expect, test, vi } from 'vitest'
import { WebMultiFeatureApp } from 'multi-feature-kotlin'
import { App } from './App'

const TODOS = JSON.stringify([
  { id: 1, title: '우유 사기', done: false },
  { id: 2, title: '운동하기', done: false },
])

let server: Server
let baseUrl = ''
/** 남은 횟수만큼 503으로 답한다 */
let failuresLeft = 0
/** true면 401 Problem Details로 답한다 */
let unauthorized = false
let requestCount = 0

beforeAll(async () => {
  // jsdom 환경은 전역 AbortController를 jsdom 것으로 바꾸는데, Node의 fetch는 jsdom의 AbortSignal을 받지 않는다.
  // Ktor Js 엔진이 만드는 AbortController를 Node 것으로 돌려 놓는다. 실제 브라우저에서는 필요 없다.
  vi.stubGlobal('AbortController', transferableAbortController().constructor)
  server = createServer((request, response) => {
    requestCount += 1
    if (request.url !== '/api/todos') {
      response.writeHead(404).end()
    } else if (unauthorized) {
      response
        .writeHead(401, { 'Content-Type': 'application/problem+json' })
        .end(JSON.stringify({ title: '토큰 만료', status: 401 }))
    } else if (failuresLeft > 0) {
      failuresLeft -= 1
      response.writeHead(503).end()
    } else {
      response.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' }).end(TODOS)
    }
  })
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
  baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api`
})

afterAll(async () => {
  vi.unstubAllGlobals()
  await new Promise((resolve) => server.close(resolve))
})

beforeEach(() => {
  failuresLeft = 0
  unauthorized = false
  requestCount = 0
})

let running: WebMultiFeatureApp | null = null

function startApp(todoBaseUrl = baseUrl) {
  const app = new WebMultiFeatureApp(todoBaseUrl)
  running = app
  app.start()
  render(
    <StrictMode>
      <App rootSlot={app.rootSlot} />
    </StrictMode>,
  )
  return app
}

afterEach(() => {
  cleanup()
  running?.stop()
  running = null
})

test('빈 이름으로 로그인하면 오류를 보여 준다', () => {
  startApp()

  fireEvent.click(screen.getByRole('button', { name: '로그인' }))

  expect(screen.getByRole('alert').textContent).toBe('이름을 입력하세요')
})

test('가입 → 할 일 → 설정 → 닫기 → 로그아웃', async () => {
  startApp()

  fireEvent.click(screen.getByRole('button', { name: '가입하기' }))
  fireEvent.change(screen.getByLabelText('이름'), { target: { value: 'carti' } })
  fireEvent.click(screen.getByRole('button', { name: '가입' }))
  expect(screen.getByRole('heading').textContent).toBe('carti님의 할 일')

  // 목록은 샘플 서버에서 비동기로 불러온다
  const first = (await screen.findAllByRole('checkbox'))[0] as HTMLInputElement
  expect(screen.queryByRole('status')).toBeNull()
  fireEvent.click(first)
  expect((screen.getAllByRole('checkbox')[0] as HTMLInputElement).checked).toBe(true)

  fireEvent.click(screen.getByRole('button', { name: '설정' }))
  expect(screen.getByText('carti(으)로 로그인됨')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: '닫기' }))

  // 설정에 가려져 있던 동안에도 할 일 RIB은 붙어 있었으므로 체크가 남는다
  expect((screen.getAllByRole('checkbox')[0] as HTMLInputElement).checked).toBe(true)

  fireEvent.click(screen.getByRole('button', { name: '설정' }))
  fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
  expect(screen.getByRole('heading').textContent).toBe('로그인')
})

test('설정에서 뒤로 가기는 설정을 닫는다', async () => {
  const app = startApp()
  fireEvent.change(screen.getByLabelText('이름'), { target: { value: 'kim' } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))
  await screen.findAllByRole('checkbox')
  fireEvent.click(screen.getByRole('button', { name: '설정' }))

  let handled = false
  act(() => {
    handled = app.handleBack()
  })

  expect(handled).toBe(true)
  expect(screen.getByRole('heading').textContent).toBe('kim님의 할 일')
})

async function loginAs(name: string) {
  fireEvent.change(screen.getByLabelText('이름'), { target: { value: name } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))
}

test('서버가 503이면 오류를 보여 주고 다시 시도하면 목록을 불러온다', async () => {
  failuresLeft = 1
  startApp()
  await loginAs('kim')

  expect((await screen.findByRole('alert')).textContent).toBe('할 일 목록을 불러오지 못했습니다')
  expect(screen.queryAllByRole('checkbox')).toHaveLength(0)

  fireEvent.click(screen.getByRole('button', { name: '다시 시도' }))

  expect(await screen.findAllByRole('checkbox')).toHaveLength(2)
  expect(screen.queryByRole('alert')).toBeNull()
  expect(requestCount).toBe(2)
})

test('서버에 연결할 수 없으면 오류를 보여 준다', async () => {
  // 닫힌 포트: 열었다 바로 닫아 아무도 듣지 않는 주소를 만든다
  const closed = createServer()
  await new Promise<void>((resolve) => closed.listen(0, '127.0.0.1', resolve))
  const port = (closed.address() as AddressInfo).port
  await new Promise((resolve) => closed.close(resolve))

  startApp(`http://127.0.0.1:${port}/api`)
  await loginAs('kim')

  expect((await screen.findByRole('alert')).textContent).toBe('할 일 목록을 불러오지 못했습니다')
  expect(screen.getByRole('button', { name: '다시 시도' })).toBeTruthy()
})

test('서버가 401이면 오류 없이 로그인 화면으로 돌아간다', async () => {
  unauthorized = true
  startApp()
  await loginAs('kim')

  expect(await screen.findByRole('heading', { name: '로그인' })).toBeTruthy()
  expect(screen.queryByRole('alert')).toBeNull()
  expect(requestCount).toBe(1)
})

// jsdom의 Blob에는 text()가 없다(실제 브라우저에는 있다). 테스트에서만 FileReader로 채운다
if (!('text' in Blob.prototype)) {
  Object.defineProperty(Blob.prototype, 'text', {
    configurable: true,
    value(this: Blob) {
      return new Promise<string>((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result as string)
        reader.onerror = () => reject(reader.error)
        reader.readAsText(this)
      })
    },
  })
}

/** jsdom에는 navigator.clipboard가 없다. 테스트마다 넣고 afterEach에서 지운다 */
function setClipboard(writeText: (text: string) => Promise<void>) {
  Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
}

afterEach(() => {
  Reflect.deleteProperty(navigator, 'clipboard')
})

async function openTodoList() {
  startApp()
  await loginAs('kim')
  await screen.findAllByRole('checkbox')
}

test('목록 복사는 클립보드에 제목을 쓰고 결과를 알린다', async () => {
  const writeText = vi.fn(() => Promise.resolve())
  setClipboard(writeText)
  await openTodoList()

  fireEvent.click(screen.getByRole('button', { name: '목록 복사' }))

  expect((await screen.findByRole('status')).textContent).toBe('목록을 복사했습니다')
  expect(writeText).toHaveBeenCalledWith('우유 사기\n운동하기')
})

test('클립보드 쓰기가 거절되면 실패를 알린다', async () => {
  setClipboard(() => Promise.reject(new Error('denied')))
  await openTodoList()

  fireEvent.click(screen.getByRole('button', { name: '목록 복사' }))

  expect((await screen.findByRole('status')).textContent).toBe('목록을 복사하지 못했습니다')
})

test('파일을 고르면 줄마다 할 일로 더한다', async () => {
  await openTodoList()
  const file = new File(['  빨래 \n\n청소\n'], '할일.txt', { type: 'text/plain' })

  fireEvent.change(screen.getByLabelText('파일에서 더하기'), { target: { files: [file] } })

  expect((await screen.findByRole('status')).textContent).toBe('할일.txt에서 2개를 더했습니다')
  expect(screen.getAllByRole('checkbox')).toHaveLength(4)
  expect(screen.getByText('청소')).toBeTruthy()
})

test('파일을 끌어다 놓아도 같은 동작으로 더한다', async () => {
  await openTodoList()
  const file = new File(['장보기'], '놓은.txt', { type: 'text/plain' })

  fireEvent.drop(screen.getByRole('main'), { dataTransfer: { files: [file] } })

  expect((await screen.findByRole('status')).textContent).toBe('놓은.txt에서 1개를 더했습니다')
  expect(screen.getByText('장보기')).toBeTruthy()
})

test('파일은 앞 64KB만 읽어 넘긴다', async () => {
  await openTodoList()
  const padding = ' '.repeat(70 * 1024)
  const file = new File([`앞줄\n${padding}\n뒷줄`], '큰.txt', { type: 'text/plain' })

  fireEvent.change(screen.getByLabelText('파일에서 더하기'), { target: { files: [file] } })

  expect((await screen.findByRole('status')).textContent).toBe('큰.txt에서 1개를 더했습니다')
  expect(screen.queryByText('뒷줄')).toBeNull()
})
