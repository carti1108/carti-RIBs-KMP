package com.carti.ribs.samples.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.carti.ribs.ViewControllable
import com.carti.ribs.compose.ComposeRenderable
import com.carti.ribs.compose.ComposeScreenSlot
import com.carti.ribs.compose.RibSlot
import com.carti.ribs.samples.login.LoggedInViewContract
import com.carti.ribs.samples.login.LoggedInPresentableListener
import com.carti.ribs.samples.login.LoggedInViewState
import com.carti.ribs.samples.login.LoggedOutViewContract
import com.carti.ribs.samples.login.LoggedOutPresentableListener
import com.carti.ribs.samples.login.LoggedOutViewState
import com.carti.ribs.samples.login.AuthService
import com.carti.ribs.samples.login.RootDependency
import com.carti.ribs.samples.login.RootViewControllable
import com.carti.ribs.samples.login.Todo
import com.carti.ribs.samples.login.TodoDetailViewContract
import com.carti.ribs.samples.login.TodoDetailPresentableListener
import com.carti.ribs.samples.login.TodoListViewContract
import com.carti.ribs.samples.login.TodoListPresentableListener

/** 테스트와 화면이 같이 쓰는 태그. iOS 샘플의 accessibilityIdentifier와 이름을 맞췄다. */
object Tags {
    const val NAME_FIELD = "nameField"
    const val LOGIN_BUTTON = "loginButton"
    const val ERROR_LABEL = "errorLabel"
    const val LOADING_INDICATOR = "loadingIndicator"
    const val WELCOME_LABEL = "welcomeLabel"
    const val LOGOUT_BUTTON = "logoutButton"
    const val SESSION_LABEL = "sessionLabel"
    const val DETAIL_TITLE = "detailTitle"
    const val TOGGLE_DONE_BUTTON = "toggleDoneButton"
    const val BACK_BUTTON = "backButton"
    fun todoRow(id: Long): String = "todoRow-$id"
}

/** 앱 최상단 의존성. 공유 코드가 RIB마다 요청하는 뷰를 Compose 구현으로 만든다. */
class AppComponent(override val authService: AuthService) : RootDependency {
    override fun makeRootView(): RootViewControllable = RootScreen()
    override fun makeLoggedOutView(): LoggedOutViewContract = LoggedOutScreen()
    override fun makeLoggedInView(): LoggedInViewContract = LoggedInScreen()
    override fun makeTodoListView(): TodoListViewContract = TodoListScreen()
    override fun makeTodoDetailView(): TodoDetailViewContract = TodoDetailScreen()
}

/** 루트 RIB의 뷰. 자식 RIB 화면 하나를 보여 준다. */
class RootScreen : RootViewControllable, ComposeRenderable {
    private val slot = ComposeScreenSlot()

    override fun showScreen(screen: ViewControllable) = slot.show(screen)

    override fun hideScreen(screen: ViewControllable) = slot.hide(screen)

    @Composable
    override fun Content() = RibSlot(slot)
}

/**
 * 로그아웃 상태 RIB의 어댑터. 상태는 공유 코드가 `render`로 한 번에 넘기고, 입력은 `listener`로 돌려준다.
 * GC 환경이라 iOS처럼 listener를 약한 참조로 둘 필요가 없다.
 */
class LoggedOutScreen : LoggedOutViewContract, ComposeRenderable {
    override var listener: LoggedOutPresentableListener? = null
    private var state: LoggedOutViewState by mutableStateOf(LoggedOutViewState())

    override fun render(state: LoggedOutViewState) {
        this.state = state
    }

    @Composable
    override fun Content() =
        LoggedOutView(errorMessage = state.errorMessage, isLoading = state.isLoading) { listener?.didTapLogin(it) }
}

/** 로그인 상태 RIB의 어댑터. 자식 RIB 화면(목록·상세) 슬롯을 [LoggedInView]의 child로 넘긴다. */
class LoggedInScreen : LoggedInViewContract, ComposeRenderable {
    override var listener: LoggedInPresentableListener? = null
    private var state: LoggedInViewState by mutableStateOf(LoggedInViewState(userName = "", sessionSeconds = 0))
    private val slot = ComposeScreenSlot()

    override fun render(state: LoggedInViewState) {
        this.state = state
    }

    override fun showScreen(screen: ViewControllable) = slot.show(screen)

    override fun hideScreen(screen: ViewControllable) = slot.hide(screen)

    @Composable
    override fun Content() = LoggedInView(
        userName = state.userName,
        sessionSeconds = state.sessionSeconds,
        onLogout = { listener?.didTapLogout() },
    ) { RibSlot(slot) }
}

/** 할 일 목록 RIB의 어댑터. 공유 `Todo`를 [TodoRow]로 바꿔 넘긴다. */
class TodoListScreen : TodoListViewContract, ComposeRenderable {
    override var listener: TodoListPresentableListener? = null
    private var rows: List<TodoRow> by mutableStateOf(emptyList())

    override fun showTodos(todos: List<Todo>) {
        rows = todos.map { TodoRow(it.id, it.title, it.isDone) }
    }

    @Composable
    override fun Content() = TodoListView(rows) { listener?.didTapTodo(it) }
}

/** 할 일 상세 RIB의 어댑터. 화면 안 뒤로 버튼과 시스템 뒤로 가기가 같은 결과(상세 닫기)를 낸다. */
class TodoDetailScreen : TodoDetailViewContract, ComposeRenderable {
    override var listener: TodoDetailPresentableListener? = null
    private var title: String by mutableStateOf("")
    private var isDone: Boolean by mutableStateOf(false)

    override fun showTodo(title: String, isDone: Boolean) {
        this.title = title
        this.isDone = isDone
    }

    @Composable
    override fun Content() = TodoDetailView(
        title = title,
        isDone = isDone,
        onBack = { listener?.didTapBack() },
        onToggleDone = { listener?.didTapToggleDone() },
    )
}
