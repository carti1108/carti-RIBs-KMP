package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.Presentable
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouter
import com.carti.ribs.checkMainThread
import kotlinx.coroutines.launch

interface TodoListPresentableListener {
    fun didTapTodo(id: Long)
}

interface TodoListPresentable : Presentable {
    var listener: TodoListPresentableListener?
    fun showTodos(todos: List<Todo>)
}

/** Router → 뷰. 붙일 자식 화면이 없어 비어 있다. RIBs-iOS 템플릿의 `TodoListViewControllable`. */
interface TodoListViewControllable : ViewControllable

/** 플랫폼 뷰 하나가 구현하는 묶음. 팩토리가 돌려주고, Interactor는 [TodoListPresentable]로, Router는 [TodoListViewControllable]로만 본다. */
interface TodoListViewContract : TodoListPresentable, TodoListViewControllable

interface TodoListDependency : Dependency {
    val viewFactory: LoginViewFactory
    val todoStore: TodoStore
}

interface TodoListListener {
    fun didSelectTodo(id: Long)
}

class TodoListBuilder(dependency: TodoListDependency) : Builder<TodoListDependency>(dependency) {
    fun build(listener: TodoListListener): TodoListRouter {
        val view = dependency.viewFactory.makeTodoListView()
        return TodoListRouter(TodoListInteractor(view, dependency.todoStore, listener), view)
    }
}

/** 저장소의 목록을 구독해 뷰를 갱신한다. 구독은 활성 구간(메인 디스패처)에서만 돈다. */
class TodoListInteractor(
    presenter: TodoListPresentable,
    private val todoStore: TodoStore,
    private val listener: TodoListListener,
) : PresentableInteractor<TodoListPresentable>(presenter), TodoListPresentableListener {
    override fun didBecomeActive() {
        presenter.listener = this
        coroutineScope.launch {
            todoStore.todos.collect { todos ->
                checkMainThread("TodoListInteractor.showTodos")
                presenter.showTodos(todos)
            }
        }
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapTodo(id: Long) = listener.didSelectTodo(id)
}

class TodoListRouter(interactor: TodoListInteractor, view: TodoListViewControllable) :
    ViewableRouter<TodoListInteractor, TodoListViewControllable>(interactor, view)
