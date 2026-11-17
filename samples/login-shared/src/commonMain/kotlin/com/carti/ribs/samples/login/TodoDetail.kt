package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.Presentable
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouter

interface TodoDetailPresentableListener {
    fun didTapToggleDone()
    fun didTapBack()
}

interface TodoDetailPresentable : Presentable {
    var listener: TodoDetailPresentableListener?
    fun showTodo(title: String, isDone: Boolean)
}

/** Router → 뷰. 붙일 자식 화면이 없어 비어 있다. RIBs-iOS 템플릿의 `TodoDetailViewControllable`. */
interface TodoDetailViewControllable : ViewControllable

/** 플랫폼 뷰 하나가 구현하는 묶음. Dependency가 돌려주고, Interactor는 [TodoDetailPresentable]로, Router는 [TodoDetailViewControllable]로만 본다. */
interface TodoDetailViewContract : TodoDetailPresentable, TodoDetailViewControllable

interface TodoDetailDependency : Dependency {
    val todoStore: TodoStore
    fun makeTodoDetailView(): TodoDetailViewContract
}

/** 자식 → 부모: 상세를 닫아 달라는 의도. 닫는 일(라우팅)은 부모가 한다. */
interface TodoDetailListener {
    fun didRequestCloseDetail()
}

class TodoDetailBuilder(dependency: TodoDetailDependency) : Builder<TodoDetailDependency>(dependency) {
    /** [todoId]가 저장소에 없으면 null을 돌려준다(복원 시 사라진 항목 등). */
    fun build(listener: TodoDetailListener, todoId: Long): TodoDetailRouter? {
        if (dependency.todoStore.find(todoId) == null) return null
        val view = dependency.makeTodoDetailView()
        val interactor = TodoDetailInteractor(view, todoId, dependency.todoStore, listener)
        return TodoDetailRouter(interactor, view, todoId)
    }
}

/** 완료 표시를 바꾸면 저장소를 거쳐 목록에도 반영된다. */
class TodoDetailInteractor(
    presenter: TodoDetailPresentable,
    private val todoId: Long,
    private val todoStore: TodoStore,
    private val listener: TodoDetailListener,
) : PresentableInteractor<TodoDetailPresentable>(presenter), TodoDetailPresentableListener {
    override fun didBecomeActive() {
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

class TodoDetailRouter(interactor: TodoDetailInteractor, view: TodoDetailViewControllable, val todoId: Long) :
    ViewableRouter<TodoDetailInteractor, TodoDetailViewControllable>(interactor, view)
