// 실제 Kotlin RIB 트리(app-root + 피처 4개)를 React로 그려 가입 → 할 일 → 설정 → 로그아웃을 지나간다
import { StrictMode } from 'react'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { WebMultiFeatureApp } from 'multi-feature-kotlin'
import { App } from './App'

let running: WebMultiFeatureApp | null = null

function startApp() {
  const app = new WebMultiFeatureApp()
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
