package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.RibState
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewRouter
import com.carti.ribs.checkMainThread
import kotlinx.coroutines.launch

interface TodoListPresentableListener {
    fun didTapTodo(id: Long)
}

interface TodoListPresentable : ViewControllable {
    var listener: TodoListPresentableListener?
    fun showTodos(todos: List<Todo>)
}

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
        return TodoListRouter(view, TodoListInteractor(view, dependency.todoStore, listener))
    }
}

/** 저장소의 목록을 구독해 뷰를 갱신한다. 구독은 활성 구간(메인 디스패처)에서만 돈다. */
class TodoListInteractor(
    presenter: TodoListPresentable,
    private val todoStore: TodoStore,
    private val listener: TodoListListener,
) : Interactor<TodoListPresentable, TodoListRouter>(presenter), TodoListPresentableListener {
    override fun didBecomeActive(savedInstanceState: RibState?) {
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

class TodoListRouter(view: TodoListPresentable, interactor: TodoListInteractor) :
    ViewRouter<TodoListPresentable, TodoListInteractor>(view, interactor)
