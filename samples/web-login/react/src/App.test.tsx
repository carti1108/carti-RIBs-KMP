// 실제 Kotlin RIB 트리(login-shared)를 React로 그려 로그인 → 목록 → 상세 → 로그아웃을 지나간다
import { StrictMode } from 'react'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { WebLoginApp } from 'web-login-kotlin'
import { App } from './App'

let running: WebLoginApp | null = null

function startApp() {
  const app = new WebLoginApp(0)
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

test('빈 이름이면 오류를 보여 주고 로그인 화면에 남는다', async () => {
  startApp()

  fireEvent.click(screen.getByRole('button', { name: '로그인' }))

  expect(await screen.findByRole('alert')).toHaveProperty('textContent', '이름을 입력하세요')
})

test('로그인부터 상세 전환, 로그아웃까지 Kotlin RIB 흐름대로 화면이 바뀐다', async () => {
  startApp()

  fireEvent.change(screen.getByLabelText('이름'), { target: { value: '  carti ' } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))

  expect(await screen.findByRole('heading', { name: 'carti님' })).toBeDefined()
  fireEvent.click(await screen.findByRole('button', { name: '우유 사기' }))

  expect(await screen.findByRole('heading', { name: '우유 사기' })).toBeDefined()
  fireEvent.click(screen.getByRole('button', { name: '완료 전환' }))
  expect(await screen.findByText('완료')).toBeDefined()

  fireEvent.click(screen.getByRole('button', { name: '목록으로' }))
  expect(await screen.findByRole('button', { name: '우유 사기 (완료)' })).toBeDefined()

  fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
  expect(await screen.findByRole('button', { name: '로그인' })).toBeDefined()
})

test('StrictMode로 그려도 RIB은 한 번만 붙고, stop하면 화면이 비워진다', async () => {
  const app = startApp()
  expect(screen.getAllByRole('button', { name: '로그인' })).toHaveLength(1)

  act(() => app.stop())
  running = null

  expect(screen.queryByRole('button', { name: '로그인' })).toBeNull()
})
