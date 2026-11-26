package com.carti.ribs.samples.multi.todo

import com.carti.ribs.Builder
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewableRouter
import com.carti.ribs.ViewableRouting
import com.carti.ribs.checkMainThread
import kotlinx.coroutines.launch

class TodoBuilder(dependency: TodoDependency) : Builder<TodoDependency>(dependency), TodoBuildable {
    override fun build(listener: TodoListener, userName: String): ViewableRouting {
        val view = dependency.makeTodoView()
        return TodoRouter(TodoInteractor(view, dependency.todoRepository, listener, userName), view)
    }
}

/**
 * 활성화될 때 저장소에서 목록을 불러온다. 요청은 활성 구간에 묶여 있어 RIB이 떨어지면 취소된다.
 * 불러온 목록은 RIB이 붙어 있는 동안 유지된다. 설정 화면에 가려졌다 돌아와도 다시 불러오지 않는다.
 */
class TodoInteractor(
    presenter: TodoPresentable,
    private val repository: TodoRepository,
    private val listener: TodoListener,
    private val userName: String,
) : PresentableInteractor<TodoPresentable>(presenter), TodoPresentableListener {
    /** null이면 아직 불러오지 못했다. */
    private var items: List<TodoItem>? = null
    private var isLoading = false
    private var errorMessage: String? = null

    override fun didBecomeActive() {
        presenter.listener = this
        if (items == null) load() else render()
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didToggle(id: Int) {
        val current = items
        require(current != null && current.any { it.id == id }) { "Unknown todo id: $id" }
        items = current.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
        render()
    }

    override fun didTapSettings() {
        listener.didRequestSettings()
    }

    /** 이미 불러오는 중이면 요청을 하나 더 보내지 않는다. */
    override fun didTapRetry() {
        if (isLoading) return
        load()
    }

    private fun load() {
        isLoading = true
        errorMessage = null
        render()
        coroutineScope.launch {
            // 취소(CancellationException)는 잡지 않는다. 비활성화되면 그대로 끝난다.
            val loaded = try {
                repository.fetchTodos()
            } catch (e: TodoRepositoryException.Unavailable) {
                null
            } catch (e: TodoRepositoryException.Unauthorized) {
                // 다시 시도해도 같으므로 오류 문구 없이 로딩만 끄고, 다시 로그인할지는 부모가 정한다.
                isLoading = false
                render()
                listener.didRequireSignIn()
                return@launch
            }
            isLoading = false
            if (loaded == null) errorMessage = LOAD_ERROR else items = loaded
            render()
        }
    }

    private fun render() {
        checkMainThread("TodoPresentable.render")
        presenter.render(
            TodoViewState(
                userName = userName,
                items = items.orEmpty().toTypedArray(),
                isLoading = isLoading,
                errorMessage = errorMessage,
            ),
        )
    }

    companion object {
        const val LOAD_ERROR: String = "할 일 목록을 불러오지 못했습니다"
    }
}

class TodoRouter(interactor: TodoInteractor, view: TodoViewControllable) :
    ViewableRouter<TodoInteractor, TodoViewControllable>(interactor, view)
