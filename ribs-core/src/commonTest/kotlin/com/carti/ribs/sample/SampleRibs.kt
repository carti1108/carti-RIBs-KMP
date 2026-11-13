package com.carti.ribs.sample

import com.carti.ribs.Builder
import com.carti.ribs.Component
import com.carti.ribs.Dependency
import com.carti.ribs.EmptyDependency
import com.carti.ribs.Interactable
import com.carti.ribs.Interactor
import com.carti.ribs.InteractorScope
import com.carti.ribs.LaunchRouter
import com.carti.ribs.Presentable
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.Router
import com.carti.ribs.Routing
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouter
import com.carti.ribs.Worker
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * 통합 테스트용 샘플 트리: Root → LoggedOut ⇄ LoggedIn
 * RIB마다 Dependency / Component / Builder / Interactor / Router 를 RIBs-iOS 템플릿 규약대로 둔다.
 */

// ---------- 공용 서비스 ----------

/** Root에서 만들어 자식 둘이 같이 쓰는 세션 저장소. */
class SessionStore {
    private val _user = MutableStateFlow<String?>(null)
    val user: StateFlow<String?> = _user
    fun signIn(name: String) { _user.value = name }
    fun signOut() { _user.value = null }
}

class FakeView(val name: String) : ViewControllable

// ---------- Root ----------

class RootComponent(dependency: EmptyDependency) :
    Component<EmptyDependency>(dependency), LoggedOutDependency, LoggedInDependency {
    override val sessionStore: SessionStore by shared { SessionStore() }
}

class RootBuilder(dependency: EmptyDependency) : Builder<EmptyDependency>(dependency) {
    fun build(): RootRouter {
        val component = RootComponent(dependency)
        val interactor = RootInteractor()
        return RootRouter(
            interactor,
            FakeView("root"),
            LoggedOutBuilder(component),
            LoggedInBuilder(component),
        )
    }
}

interface RootRouting : Routing {
    fun routeToLoggedOut()
    fun routeAwayFromLoggedOut()
    fun routeToLoggedIn(userName: String)
    fun routeAwayFromLoggedIn()
}

interface RootInteractable : Interactable, LoggedOutListener, LoggedInListener {
    var router: RootRouting?
}

class RootInteractor : Interactor(), RootInteractable {
    override var router: RootRouting? = null

    override fun didBecomeActive() {
        router?.routeToLoggedOut()
    }

    override fun didLogin(userName: String) {
        router?.routeAwayFromLoggedOut()
        router?.routeToLoggedIn(userName)
    }

    override fun didRequestLogout() {
        router?.routeAwayFromLoggedIn()
        router?.routeToLoggedOut()
    }
}

class RootRouter(
    interactor: RootInteractable,
    view: FakeView,
    private val loggedOutBuilder: LoggedOutBuilder,
    private val loggedInBuilder: LoggedInBuilder,
) : LaunchRouter<RootInteractable, FakeView>(interactor, view), RootRouting {
    private var loggedOut: LoggedOutRouter? = null
    private var loggedIn: LoggedInRouter? = null

    init {
        interactor.router = this
    }

    override fun routeToLoggedOut() {
        if (loggedOut != null) return
        loggedOut = loggedOutBuilder.build(interactor).also(::attachChild)
    }

    override fun routeAwayFromLoggedOut() {
        loggedOut?.let(::detachChild)
        loggedOut = null
    }

    override fun routeToLoggedIn(userName: String) {
        if (loggedIn != null) return
        loggedIn = loggedInBuilder.build(interactor, userName).also(::attachChild)
    }

    override fun routeAwayFromLoggedIn() {
        loggedIn?.let(::detachChild)
        loggedIn = null
    }
}

// ---------- LoggedOut (뷰가 Presentable을 구현) ----------

interface LoggedOutDependency : Dependency {
    val sessionStore: SessionStore
}

/** 자식 → 부모: 사용자 의도로 표현한다. 라우팅은 부모가 정한다. */
interface LoggedOutListener {
    fun didLogin(userName: String)
}

class LoggedOutBuilder(dependency: LoggedOutDependency) : Builder<LoggedOutDependency>(dependency) {
    fun build(listener: LoggedOutListener): LoggedOutRouter {
        val view = LoggedOutView()
        val interactor = LoggedOutInteractor(view, dependency.sessionStore, listener)
        return LoggedOutRouter(interactor, view)
    }
}

interface LoggedOutPresentableListener {
    fun login(name: String)
}

interface LoggedOutPresentable : Presentable {
    var listener: LoggedOutPresentableListener?
}

/** 뷰 입력을 해석해 리스너로 넘긴다. 공백만 있는 이름은 거른다. */
class LoggedOutView : LoggedOutPresentable, ViewControllable {
    override var listener: LoggedOutPresentableListener? = null
    var errorMessage: String? = null
        private set

    fun didTapLogin(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            errorMessage = "이름을 입력하세요"
            return
        }
        errorMessage = null
        listener?.login(name)
    }
}

class LoggedOutInteractor(
    presenter: LoggedOutPresentable,
    val sessionStore: SessionStore,
    private val listener: LoggedOutListener,
) : PresentableInteractor<LoggedOutPresentable>(presenter), LoggedOutPresentableListener {
    init {
        presenter.listener = this
    }

    override fun login(name: String) {
        sessionStore.signIn(name)
        listener.didLogin(name)
    }
}

class LoggedOutRouter(interactor: LoggedOutInteractor, view: LoggedOutView) :
    ViewableRouter<LoggedOutInteractor, LoggedOutView>(interactor, view)

// ---------- LoggedIn (뷰 없음 + Worker) ----------

interface LoggedInDependency : Dependency {
    val sessionStore: SessionStore
}

interface LoggedInListener {
    fun didRequestLogout()
}

class LoggedInBuilder(dependency: LoggedInDependency) : Builder<LoggedInDependency>(dependency) {
    fun build(listener: LoggedInListener, userName: String): LoggedInRouter {
        val interactor = LoggedInInteractor(userName, dependency.sessionStore, listener, HeartbeatWorker())
        return LoggedInRouter(interactor)
    }
}

/** 로그인 동안 돌아가는 작업. Interactor가 비활성화되면 멈춘다. */
class HeartbeatWorker : Worker() {
    var job: Job? = null
        private set
    var stopped = false
        private set

    override fun didStart(interactorScope: InteractorScope) {
        stopped = false
        job = coroutineScope.launch { awaitCancellation() }
    }

    override fun didStop() {
        stopped = true
    }
}

class LoggedInInteractor(
    val userName: String,
    val sessionStore: SessionStore,
    private val listener: LoggedInListener,
    val heartbeat: HeartbeatWorker,
) : Interactor() {
    override fun didBecomeActive() {
        heartbeat.start(this)
    }

    fun logout() {
        sessionStore.signOut()
        listener.didRequestLogout()
    }
}

class LoggedInRouter(interactor: LoggedInInteractor) : Router<LoggedInInteractor>(interactor)
