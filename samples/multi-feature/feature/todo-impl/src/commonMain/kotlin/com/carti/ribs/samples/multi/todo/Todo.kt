package com.carti.ribs.samples.multi.todo

import com.carti.ribs.Builder
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewableRouter
import com.carti.ribs.ViewableRouting
import com.carti.ribs.checkMainThread

class TodoBuilder(dependency: TodoDependency) : Builder<TodoDependency>(dependency), TodoBuildable {
    override fun build(listener: TodoListener, userName: String): ViewableRouting {
        val view = dependency.makeTodoView()
        return TodoRouter(TodoInteractor(view, listener, userName), view)
    }
}

/** 목록은 RIB이 붙어 있는 동안 유지된다. 설정 화면에 가려졌다 돌아와도 그대로다. */
class TodoInteractor(
    presenter: TodoPresentable,
    private val listener: TodoListener,
    private val userName: String,
) : PresentableInteractor<TodoPresentable>(presenter), TodoPresentableListener {
    private var items: List<TodoItem> = INITIAL_TITLES.mapIndexed { index, title ->
        TodoItem(id = index + 1, title = title, isDone = false)
    }

    override fun didBecomeActive() {
        presenter.listener = this
        render()
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didToggle(id: Int) {
        require(items.any { it.id == id }) { "Unknown todo id: $id" }
        items = items.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
        render()
    }

    override fun didTapSettings() {
        listener.didRequestSettings()
    }

    private fun render() {
        checkMainThread("TodoPresentable.render")
        presenter.render(TodoViewState(userName, items.toTypedArray()))
    }

    private companion object {
        val INITIAL_TITLES = listOf("우유 사기", "운동하기", "책 읽기")
    }
}

class TodoRouter(interactor: TodoInteractor, view: TodoViewControllable) :
    ViewableRouter<TodoInteractor, TodoViewControllable>(interactor, view)
