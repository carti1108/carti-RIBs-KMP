package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.RibState
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewRouter

interface TodoDetailPresentableListener {
    fun didTapToggleDone()
    fun didTapBack()
}

interface TodoDetailPresentable : ViewControllable {
    var listener: TodoDetailPresentableListener?
    fun showTodo(title: String, isDone: Boolean)
}

interface TodoDetailDependency : Dependency {
    val viewFactory: LoginViewFactory
    val todoStore: TodoStore
}

/** 자식 → 부모: 상세를 닫아 달라는 의도. 닫는 일(라우팅)은 부모가 한다. */
interface TodoDetailListener {
    fun didRequestCloseDetail()
}

class TodoDetailBuilder(dependency: TodoDetailDependency) : Builder<TodoDetailDependency>(dependency) {
    /** [todoId]가 저장소에 없으면 null을 돌려준다(복원 시 사라진 항목 등). */
    fun build(listener: TodoDetailListener, todoId: Long): TodoDetailRouter? {
        if (dependency.todoStore.find(todoId) == null) return null
        val view = dependency.viewFactory.makeTodoDetailView()
        val interactor = TodoDetailInteractor(view, todoId, dependency.todoStore, listener)
        return TodoDetailRouter(view, interactor, todoId)
    }
}

/** 완료 표시를 바꾸면 저장소를 거쳐 목록에도 반영된다. */
class TodoDetailInteractor(
    presenter: TodoDetailPresentable,
    private val todoId: Long,
    private val todoStore: TodoStore,
    private val listener: TodoDetailListener,
) : Interactor<TodoDetailPresentable, TodoDetailRouter>(presenter), TodoDetailPresentableListener {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        presenter.listener = this
        render()
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapToggleDone() {
        todoStore.toggle(todoId)
        render()
    }

    override fun didTapBack() = listener.didRequestCloseDetail()

    private fun render() {
        val todo = checkNotNull(todoStore.find(todoId)) { "Todo $todoId disappeared while its detail was open" }
        presenter.showTodo(todo.title, todo.isDone)
    }
}

class TodoDetailRouter(view: TodoDetailPresentable, interactor: TodoDetailInteractor, val todoId: Long) :
    ViewRouter<TodoDetailPresentable, TodoDetailInteractor>(view, interactor)
