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

    /** 플랫폼 입력의 결과 문구. [render]가 한 번 내보내면 지운다. */
    private var notice: String? = null

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

    override fun didCopyList(succeeded: Boolean) {
        notice = if (succeeded) COPIED else COPY_FAILED
        render()
    }

    /** 줄마다 앞뒤 공백을 지우고 빈 줄을 건너뛰어 할 일로 더한다. 한 번에 [MAX_IMPORTED_TODOS]개까지다. */
    override fun didReceiveFile(name: String, text: String) {
        val current = items
        if (current == null) {
            // 뒤이어 불러온 목록이 덮어쓰므로 불러오기 전에는 더하지 않는다.
            notice = NOT_LOADED
            render()
            return
        }
        val titles = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        val added = titles.take(MAX_IMPORTED_TODOS)
        val firstId = (current.maxOfOrNull { it.id } ?: 0) + 1
        items = current + added.mapIndexed { index, title -> TodoItem(firstId + index, title, isDone = false) }
        notice = importNotice(name, added.size, skipped = titles.size - added.size)
        render()
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
                notice = notice,
            ),
        )
        notice = null
    }

    companion object {
        const val LOAD_ERROR: String = "할 일 목록을 불러오지 못했습니다"
        const val COPIED: String = "목록을 복사했습니다"
        const val COPY_FAILED: String = "목록을 복사하지 못했습니다"
        const val NOT_LOADED: String = "목록을 불러온 뒤에 파일을 더하세요"

        /** 한 파일에서 한 번에 더하는 최대 개수. 뷰는 파일의 앞부분만 읽어 넘긴다. */
        const val MAX_IMPORTED_TODOS: Int = 100

        private fun importNotice(name: String, added: Int, skipped: Int): String = when {
            added == 0 -> "${name}에 더할 줄이 없습니다"
            skipped == 0 -> "${name}에서 ${added}개를 더했습니다"
            else -> "${name}에서 ${added}개를 더했습니다. ${skipped}줄은 한 번에 더할 수 있는 수를 넘어 건너뛰었습니다"
        }
    }
}

class TodoRouter(interactor: TodoInteractor, view: TodoViewControllable) :
    ViewableRouter<TodoInteractor, TodoViewControllable>(interactor, view)
