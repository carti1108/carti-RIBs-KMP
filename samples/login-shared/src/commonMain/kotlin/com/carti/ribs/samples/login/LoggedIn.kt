@file:OptIn(ExperimentalJsExport::class, ExperimentalObjCName::class)

package com.carti.ribs.samples.login

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoggedInPresentableListener {
    fun didTapLogout()
}

/** 머리말 상태 한 벌. 세션 시간은 Swift·JS에서 다루기 쉽게 Int로 둔다. 웹은 이 타입을 그대로 쓴다. */
@JsExport
data class LoggedInViewState(val userName: String, val sessionSeconds: Int)

/** Interactor → 뷰: 머리말(사용자 이름, 세션 시간)을 그린다(ADR-0006). */
interface LoggedInPresentable : Presentable {
    /** 활성 구간에만 설정된다. 플랫폼은 약한 참조로 둔다. */
    var listener: LoggedInPresentableListener?
    // 계약마다 Obj-C 이름을 고정한다. 같은 `render(state:)`가 여러 계약에 있으면 Kotlin/Native가 Swift 인자 이름을
    // 선언 순서대로 `state_`, `state__`…로 바꿔 내보내서, RIB이 늘 때마다 Swift 쪽 이름이 밀린다.
    @ObjCName("renderLoggedIn")
    fun render(state: LoggedInViewState)
}

/** Router → 뷰: 자식 화면을 붙이고 뗀다. RIBs-iOS 템플릿의 `LoggedInViewControllable`. */
interface LoggedInViewControllable : ViewControllable {
    fun showScreen(screen: ViewControllable)
    fun hideScreen(screen: ViewControllable)
}

/** 플랫폼 뷰 하나가 구현하는 묶음. Dependency가 돌려주고, Interactor는 [LoggedInPresentable]로, Router는 [LoggedInViewControllable]로만 본다. */
interface LoggedInViewContract : LoggedInPresentable, LoggedInViewControllable

/** 자식(목록·상세)의 뷰는 이 RIB이 만들 수 없으므로 부모에게서 받아 넘긴다. */
interface LoggedInDependency : Dependency {
    fun makeLoggedInView(): LoggedInViewContract
    fun makeTodoListView(): TodoListViewContract
    fun makeTodoDetailView(): TodoDetailViewContract
}

interface LoggedInListener {
    fun didRequestLogout()
}

/** 목록과 상세가 같은 [TodoStore]를 쓰도록 여기서 한 번만 만든다. */
class LoggedInComponent(dependency: LoggedInDependency) :
    Component<LoggedInDependency>(dependency), TodoListDependency, TodoDetailDependency {
    override fun makeTodoListView(): TodoListViewContract = dependency.makeTodoListView()
    override fun makeTodoDetailView(): TodoDetailViewContract = dependency.makeTodoDetailView()
    override val todoStore: TodoStore by shared { TodoStore() }
}

class LoggedInBuilder(dependency: LoggedInDependency) : Builder<LoggedInDependency>(dependency) {
    fun build(listener: LoggedInListener, userName: String): LoggedInRouter {
        val component = LoggedInComponent(dependency)
        val view = dependency.makeLoggedInView()
        val interactor = LoggedInInteractor(view, userName, component.todoStore, listener, SessionTimerWorker())
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
        render(LoggedInViewState(userName, sessionSeconds = 0))
        (savedInstanceState?.get(KEY_DONE_IDS) as? List<*>)?.filterIsInstance<Long>()?.let(todoStore::restoreDone)
        router?.routeToList()
        (savedInstanceState?.get(KEY_OPEN_DETAIL_ID) as? Long)?.let { router?.routeToDetail(it) }
        sessionTimer.start(this)
        coroutineScope.launch {
            sessionTimer.seconds.collect { render(LoggedInViewState(userName, it)) }
        }
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

    private fun render(state: LoggedInViewState) {
        checkMainThread("LoggedInPresentable.render")
        presenter.render(state)
    }

    private companion object {
        const val KEY_DONE_IDS = "doneIds"
        const val KEY_OPEN_DETAIL_ID = "openDetailId"
    }
}

class LoggedInRouter(
    interactor: LoggedInInteractor,
    view: LoggedInViewControllable,
    private val listBuilder: TodoListBuilder,
    private val detailBuilder: TodoDetailBuilder,
) : ViewableRouter<LoggedInInteractor, LoggedInViewControllable>(interactor, view) {
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
 * 로그인한 동안 1초마다 세션 시간을 [seconds]로 내보낸다. 시작할 때 0으로 돌아가고, Interactor가 비활성화되면 함께 멈춘다.
 * 뷰는 모른다. 화면 상태는 Interactor가 합쳐서 그린다.
 */
class SessionTimerWorker(private val tickMillis: Long = TICK_MILLIS) : Worker() {
    private val elapsed = MutableStateFlow(0)
    val seconds: StateFlow<Int> = elapsed.asStateFlow()

    override fun didStart(interactorScope: InteractorScope) {
        elapsed.value = 0
        coroutineScope.launch {
            while (true) {
                delay(tickMillis)
                elapsed.value++
            }
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
