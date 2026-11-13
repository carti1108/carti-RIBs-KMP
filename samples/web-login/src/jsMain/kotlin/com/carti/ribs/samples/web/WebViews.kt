@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.samples.web

import com.carti.ribs.ViewControllable
import com.carti.ribs.samples.login.LoggedInViewContract
import com.carti.ribs.samples.login.LoggedInPresentableListener
import com.carti.ribs.samples.login.LoggedOutViewContract
import com.carti.ribs.samples.login.LoggedOutPresentableListener
import com.carti.ribs.samples.login.RootViewControllable
import com.carti.ribs.samples.login.Todo
import com.carti.ribs.samples.login.TodoDetailViewContract
import com.carti.ribs.samples.login.TodoDetailPresentableListener
import com.carti.ribs.samples.login.TodoListViewContract
import com.carti.ribs.samples.login.TodoListPresentableListener
import com.carti.ribs.web.ScreenSlot
import com.carti.ribs.web.ViewStore
import com.carti.ribs.web.WebViewControllable

// Presenter의 명령형 호출을 화면 상태 스냅숏으로 바꾸는 웹 뷰 구현(ADR-0009).
// React는 store를 구독하고, 입력은 내보낸 함수(login, logout 등)로 전달한다

private fun ViewControllable.asWeb(): WebViewControllable =
    this as? WebViewControllable ?: error("${this::class.simpleName} is not a WebViewControllable")

@JsExport
class WebRootView : RootViewControllable, WebViewControllable {
    override val screenKey: String = "root"
    val slot: ScreenSlot = ScreenSlot()

    @JsExport.Ignore
    override fun showScreen(screen: ViewControllable) = slot.show(screen.asWeb())

    @JsExport.Ignore
    override fun hideScreen(screen: ViewControllable) = slot.hide(screen.asWeb())
}

@JsExport
data class LoggedOutState(val isLoading: Boolean, val errorMessage: String?)

@JsExport
class WebLoggedOutView : LoggedOutViewContract, WebViewControllable {
    override val screenKey: String = "loggedOut"
    val store: ViewStore<LoggedOutState> = ViewStore(LoggedOutState(isLoading = false, errorMessage = null))

    @JsExport.Ignore
    override var listener: LoggedOutPresentableListener? = null

    fun login(rawName: String) {
        listener?.didTapLogin(rawName)
    }

    @JsExport.Ignore
    override fun showError(message: String?) = store.update { it.copy(errorMessage = message) }

    @JsExport.Ignore
    override fun showLoading(isLoading: Boolean) = store.update { it.copy(isLoading = isLoading) }
}

@JsExport
data class LoggedInState(val userName: String, val sessionSeconds: Int)

@JsExport
class WebLoggedInView : LoggedInViewContract, WebViewControllable {
    override val screenKey: String = "loggedIn"
    val store: ViewStore<LoggedInState> = ViewStore(LoggedInState(userName = "", sessionSeconds = 0))
    val slot: ScreenSlot = ScreenSlot()

    @JsExport.Ignore
    override var listener: LoggedInPresentableListener? = null

    fun logout() {
        listener?.didTapLogout()
    }

    @JsExport.Ignore
    override fun showUserName(name: String) = store.update { it.copy(userName = name) }

    // JS로 내보낼 때 Long은 bigint가 되므로 화면 상태에는 Int로 담는다
    @JsExport.Ignore
    override fun showSessionSeconds(seconds: Long) = store.update { it.copy(sessionSeconds = seconds.toInt()) }

    @JsExport.Ignore
    override fun showScreen(screen: ViewControllable) = slot.show(screen.asWeb())

    @JsExport.Ignore
    override fun hideScreen(screen: ViewControllable) = slot.hide(screen.asWeb())
}

@JsExport
data class TodoItem(val id: Int, val title: String, val isDone: Boolean)

@JsExport
class TodoListState(val todos: Array<TodoItem>) {
    // Array는 내용 비교를 하지 않으므로 직접 정의해 같은 목록이면 다시 그리지 않게 한다
    override fun equals(other: Any?): Boolean = other is TodoListState && other.todos.contentEquals(todos)
    override fun hashCode(): Int = todos.contentHashCode()
}

@JsExport
class WebTodoListView : TodoListViewContract, WebViewControllable {
    override val screenKey: String = "todoList"
    val store: ViewStore<TodoListState> = ViewStore(TodoListState(emptyArray()))

    @JsExport.Ignore
    override var listener: TodoListPresentableListener? = null

    fun select(id: Int) {
        listener?.didTapTodo(id.toLong())
    }

    @JsExport.Ignore
    override fun showTodos(todos: List<Todo>) =
        store.set(TodoListState(todos.map { TodoItem(it.id.toInt(), it.title, it.isDone) }.toTypedArray()))
}

@JsExport
data class TodoDetailState(val title: String, val isDone: Boolean)

@JsExport
class WebTodoDetailView : TodoDetailViewContract, WebViewControllable {
    override val screenKey: String = "todoDetail"
    val store: ViewStore<TodoDetailState> = ViewStore(TodoDetailState(title = "", isDone = false))

    @JsExport.Ignore
    override var listener: TodoDetailPresentableListener? = null

    fun toggleDone() {
        listener?.didTapToggleDone()
    }

    fun back() {
        listener?.didTapBack()
    }

    @JsExport.Ignore
    override fun showTodo(title: String, isDone: Boolean) = store.set(TodoDetailState(title, isDone))
}
