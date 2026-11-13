package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Component
import com.carti.ribs.Dependency
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.Presentable
import com.carti.ribs.InteractorScope
import com.carti.ribs.Worker
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouter
import com.carti.ribs.checkMainThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoggedInPresentableListener {
    fun didTapLogout()
}

/** 공유 코드 → 뷰: 머리말(사용자 이름, 세션 시간)과 자식 RIB 화면 하나를 담는 영역(ADR-0006). */
interface LoggedInPresentable : Presentable, ViewControllable {
    /** 활성 구간에만 설정된다. 플랫폼은 약한 참조로 둔다. */
    var listener: LoggedInPresentableListener?
    fun showUserName(name: String)
    fun showSessionSeconds(seconds: Long)
    fun showScreen(screen: ViewControllable)
    fun hideScreen(screen: ViewControllable)
}

interface LoggedInDependency : Dependency {
    val viewFactory: LoginViewFactory
}

interface LoggedInListener {
    fun didRequestLogout()
}

/** 목록과 상세가 같은 [TodoStore]를 쓰도록 여기서 한 번만 만든다. */
class LoggedInComponent(dependency: LoggedInDependency) :
    Component<LoggedInDependency>(dependency), TodoListDependency, TodoDetailDependency {
    override val viewFactory: LoginViewFactory get() = dependency.viewFactory
    override val todoStore: TodoStore by shared { TodoStore() }
}

class LoggedInBuilder(dependency: LoggedInDependency) : Builder<LoggedInDependency>(dependency) {
    fun build(listener: LoggedInListener, userName: String): LoggedInRouter {
        val component = LoggedInComponent(dependency)
        val view = dependency.viewFactory.makeLoggedInView()
        val interactor = LoggedInInteractor(view, userName, component.todoStore, listener, SessionTimerWorker(view))
        return LoggedInRouter(interactor, view, TodoListBuilder(component), TodoDetailBuilder(component))
    }
}

/**
 * 목록을 기본 화면으로 두고 상세를 위에 쌓는다. 뒤로 가기는 열린 상세를 닫고, 닫을 것이 없으면 false를 돌려준다.
 * 저장 상태: 완료한 할 일과 열려 있던 상세 id.
 */
class LoggedInInteractor(
    presenter: LoggedInPresentable,
    private val userName: String,
    private val todoStore: TodoStore,
    private val listener: LoggedInListener,
    private val sessionTimer: SessionTimerWorker,
) : PresentableInteractor<LoggedInPresentable>(presenter), LoggedInPresentableListener,
    TodoListListener, TodoDetailListener {

    /** Router가 만들어질 때 자신을 넣는다(RIBs-iOS 템플릿과 같음). */
    var router: LoggedInRouter? = null

    override fun didBecomeActive() {
        presenter.listener = this
        presenter.showUserName(userName)
        (savedInstanceState?.get(KEY_DONE_IDS) as? List<*>)?.filterIsInstance<Long>()?.let(todoStore::restoreDone)
        router?.routeToList()
        (savedInstanceState?.get(KEY_OPEN_DETAIL_ID) as? Long)?.let { router?.routeToDetail(it) }
        sessionTimer.start(this)
    }

    override fun willResignActive() {
        presenter.listener = null
        sessionTimer.stop()
    }

    override fun onSaveInstanceState(outState: MutableMap<String, Any?>) {
        outState[KEY_DONE_IDS] = todoStore.doneIds
        outState[KEY_OPEN_DETAIL_ID] = router?.openDetailId
    }

    override fun handleBackPress(): Boolean {
        val router = router ?: return false
        if (router.openDetailId == null) return false
        router.routeAwayFromDetail()
        return true
    }

    override fun didTapLogout() = listener.didRequestLogout()

    override fun didSelectTodo(id: Long) {
        router?.routeToDetail(id)
    }

    override fun didRequestCloseDetail() {
        router?.routeAwayFromDetail()
    }

    private companion object {
        const val KEY_DONE_IDS = "doneIds"
        const val KEY_OPEN_DETAIL_ID = "openDetailId"
    }
}

class LoggedInRouter(
    interactor: LoggedInInteractor,
    view: LoggedInPresentable,
    private val listBuilder: TodoListBuilder,
    private val detailBuilder: TodoDetailBuilder,
) : ViewableRouter<LoggedInInteractor, LoggedInPresentable>(interactor, view) {
    private var list: TodoListRouter? = null
    private var detail: TodoDetailRouter? = null

    init {
        interactor.router = this
    }

    val openDetailId: Long? get() = detail?.todoId

    fun routeToList() {
        if (list != null) return
        val child = listBuilder.build(interactor)
        attachChild(child)
        viewController.showScreen(child.viewController)
        list = child
    }

    fun routeToDetail(todoId: Long) {
        if (detail != null) return
        val child = detailBuilder.build(interactor, todoId) ?: return
        attachChild(child)
        viewController.showScreen(child.viewController)
        detail = child
    }

    fun routeAwayFromDetail() {
        val child = detail ?: return
        detachChild(child)
        viewController.hideScreen(child.viewController)
        detail = null
        list?.let { viewController.showScreen(it.viewController) }
    }
}

/**
 * 로그인한 동안 1초마다 세션 시간을 뷰에 알린다. Interactor가 비활성화되면 함께 멈춘다.
 * Worker의 스코프는 메인 디스패처라 뷰를 바로 부른다(ADR-0007).
 */
class SessionTimerWorker(
    private val presenter: LoggedInPresentable,
    private val tickMillis: Long = TICK_MILLIS,
) : Worker() {
    override fun didStart(interactorScope: InteractorScope) {
        coroutineScope.launch {
            var seconds = 0L
            while (true) {
                checkMainThread("SessionTimerWorker.showSessionSeconds")
                presenter.showSessionSeconds(seconds)
                delay(tickMillis)
                seconds++
            }
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
