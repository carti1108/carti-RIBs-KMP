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

/** 파일에서 이만큼만 읽어 Kotlin으로 넘긴다. 큰 파일을 통째로 넘기지 않는다(ADR-0016) */
const MAX_FILE_BYTES = 64 * 1024

function TodoScreen({ view }: { view: WebTodoView }) {
  const state = useViewStore(view.store)

  // 클립보드와 파일은 뷰가 브라우저 API로 다루고 결과 하나만 Kotlin에 넘긴다(ADR-0016)
  const copyList = () => {
    const text = state.items.map((item) => item.title).join('\n')
    // 보안 컨텍스트가 아니거나 권한을 거절하면 clipboard가 없거나 writeText가 거절된다
    if (navigator.clipboard === undefined) {
      view.copied(false)
      return
    }
    navigator.clipboard.writeText(text).then(
      () => view.copied(true),
      () => view.copied(false),
    )
  }
  const receiveFile = (file: File | undefined) => {
    if (file === undefined) return
    // 앞부분만 읽는다. 바이트 경계에서 잘린 마지막 글자는 대체 문자가 될 수 있다
    void file
      .slice(0, MAX_FILE_BYTES)
      .text()
      .then((text) => view.fileReceived(file.name, text))
  }

  return (
    <main
      onDragOver={(event) => event.preventDefault()}
      onDrop={(event) => {
        event.preventDefault()
        receiveFile(event.dataTransfer.files[0])
      }}
    >
      <h1>{state.userName}님의 할 일</h1>
      {state.isLoading && <p role="status">불러오는 중</p>}
      {state.notice != null && <p role="status">{state.notice}</p>}
      {state.errorMessage != null && (
        <>
          <p role="alert">{state.errorMessage}</p>
          <button type="button" onClick={() => view.retry()}>
            다시 시도
          </button>
        </>
      )}
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
      <button type="button" onClick={copyList}>
        목록 복사
      </button>
      <label>
        파일에서 더하기
        <input
          type="file"
          accept=".txt,text/plain"
          onChange={(event) => {
            receiveFile(event.target.files?.[0])
            // 같은 파일을 다시 골라도 change가 나도록 비운다
            event.target.value = ''
          }}
        />
      </label>
      <p>텍스트 파일을 화면에 끌어다 놓아도 줄마다 할 일로 더합니다</p>
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
