// 뷰는 그리기와 함수 호출만 한다. 화면 전환은 Kotlin RIB이 맡는다(ADR-0009)
// 피처마다 화면 컴포넌트 하나. 피처가 늘면 src/features/<이름>/으로 나눈다.
import { useState } from 'react'
import { RibSlot, useViewStore, type ScreenRenderers } from 'ribs-react'
import type {
  ScreenSlot,
  WebLoginView,
  WebRootView,
  WebSettingsView,
  WebSignupView,
  WebTodoView,
} from 'multi-feature-kotlin'

function NameForm({
  title,
  submitLabel,
  errorMessage,
  onSubmit,
  children,
}: {
  title: string
  submitLabel: string
  errorMessage: string | null | undefined
  onSubmit: (name: string) => void
  children?: React.ReactNode
}) {
  const [name, setName] = useState('')
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit(name)
      }}
    >
      <h1>{title}</h1>
      <label>
        이름
        <input value={name} onChange={(event) => setName(event.target.value)} />
      </label>
      <button type="submit">{submitLabel}</button>
      {children}
      {errorMessage != null && <p role="alert">{errorMessage}</p>}
    </form>
  )
}

function LoginScreen({ view }: { view: WebLoginView }) {
  const state = useViewStore(view.store)
  return (
    <NameForm title="로그인" submitLabel="로그인" errorMessage={state.errorMessage} onSubmit={(name) => view.login(name)}>
      <button type="button" onClick={() => view.signup()}>
        가입하기
      </button>
    </NameForm>
  )
}

function SignupScreen({ view }: { view: WebSignupView }) {
  const state = useViewStore(view.store)
  return (
    <NameForm title="가입" submitLabel="가입" errorMessage={state.errorMessage} onSubmit={(name) => view.signup(name)}>
      <button type="button" onClick={() => view.cancel()}>
        취소
      </button>
    </NameForm>
  )
}

function TodoScreen({ view }: { view: WebTodoView }) {
  const state = useViewStore(view.store)
  return (
    <main>
      <h1>{state.userName}님의 할 일</h1>
      <ul>
        {state.items.map((item) => (
          <li key={item.id}>
            <label>
              <input type="checkbox" checked={item.isDone} onChange={() => view.toggle(item.id)} />
              {item.title}
            </label>
          </li>
        ))}
      </ul>
      <button type="button" onClick={() => view.openSettings()}>
        설정
      </button>
    </main>
  )
}

function SettingsScreen({ view }: { view: WebSettingsView }) {
  const state = useViewStore(view.store)
  return (
    <main>
      <h1>설정</h1>
      <p>{state.userName}(으)로 로그인됨</p>
      <button type="button" onClick={() => view.logout()}>
        로그아웃
      </button>
      <button type="button" onClick={() => view.close()}>
        닫기
      </button>
    </main>
  )
}

const rootScreens: ScreenRenderers = {
  login: (view: WebLoginView) => <LoginScreen view={view} />,
  signup: (view: WebSignupView) => <SignupScreen view={view} />,
  todo: (view: WebTodoView) => <TodoScreen view={view} />,
  settings: (view: WebSettingsView) => <SettingsScreen view={view} />,
}

const appScreens: ScreenRenderers = {
  root: (view: WebRootView) => <RibSlot slot={view.slot} screens={rootScreens} />,
}

export function App({ rootSlot }: { rootSlot: ScreenSlot }) {
  return <RibSlot slot={rootSlot} screens={appScreens} />
}
