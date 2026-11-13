// 뷰는 그리기와 함수 호출만 한다. 상태 변환과 화면 전환은 Kotlin RIB이 맡는다(ADR-0009)
import { useState } from 'react'
import { RibSlot, useViewStore, type ScreenRenderers } from 'ribs-react'
import type {
  ScreenSlot,
  WebLoggedInView,
  WebLoggedOutView,
  WebRootView,
  WebTodoDetailView,
  WebTodoListView,
} from 'web-login-kotlin'

function LoggedOutScreen({ view }: { view: WebLoggedOutView }) {
  const state = useViewStore(view.store)
  const [name, setName] = useState('')
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        view.login(name)
      }}
    >
      <label>
        이름
        <input value={name} onChange={(event) => setName(event.target.value)} disabled={state.isLoading} />
      </label>
      <button type="submit" disabled={state.isLoading}>
        {state.isLoading ? '로그인 중' : '로그인'}
      </button>
      {state.errorMessage != null && <p role="alert">{state.errorMessage}</p>}
    </form>
  )
}

function TodoListScreen({ view }: { view: WebTodoListView }) {
  const { todos } = useViewStore(view.store)
  return (
    <ul>
      {todos.map((todo) => (
        <li key={todo.id}>
          <button type="button" onClick={() => view.select(todo.id)}>
            {todo.title}
            {todo.isDone ? ' (완료)' : ''}
          </button>
        </li>
      ))}
    </ul>
  )
}

function TodoDetailScreen({ view }: { view: WebTodoDetailView }) {
  const state = useViewStore(view.store)
  return (
    <section>
      <h2>{state.title}</h2>
      <p>{state.isDone ? '완료' : '할 일'}</p>
      <button type="button" onClick={() => view.toggleDone()}>
        완료 전환
      </button>
      <button type="button" onClick={() => view.back()}>
        목록으로
      </button>
    </section>
  )
}

const childScreens: ScreenRenderers = {
  todoList: (view: WebTodoListView) => <TodoListScreen view={view} />,
  todoDetail: (view: WebTodoDetailView) => <TodoDetailScreen view={view} />,
}

function LoggedInScreen({ view }: { view: WebLoggedInView }) {
  const state = useViewStore(view.store)
  return (
    <main>
      <h1>{state.userName}님</h1>
      <p>접속 {state.sessionSeconds}초</p>
      <button type="button" onClick={() => view.logout()}>
        로그아웃
      </button>
      <RibSlot slot={view.slot} screens={childScreens} />
    </main>
  )
}

const rootScreens: ScreenRenderers = {
  loggedOut: (view: WebLoggedOutView) => <LoggedOutScreen view={view} />,
  loggedIn: (view: WebLoggedInView) => <LoggedInScreen view={view} />,
}

const appScreens: ScreenRenderers = {
  root: (view: WebRootView) => <RibSlot slot={view.slot} screens={rootScreens} />,
}

export function App({ rootSlot }: { rootSlot: ScreenSlot }) {
  return <RibSlot slot={rootSlot} screens={appScreens} />
}
