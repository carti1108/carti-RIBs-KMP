package com.carti.ribs.sample

import com.carti.ribs.Builder
import com.carti.ribs.Component
import com.carti.ribs.Dependency
import com.carti.ribs.EmptyDependency
import com.carti.ribs.Interactor
import com.carti.ribs.LaunchRouter
import com.carti.ribs.Presenter
import com.carti.ribs.RibCoroutineWorker
import com.carti.ribs.RibState
import com.carti.ribs.Router
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewRouter
import com.carti.ribs.bind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * 통합 테스트용 샘플 트리: Root → LoggedOut ⇄ LoggedIn
 * RIB마다 Dependency / Component / Builder / Interactor / Router 를 원본 규약대로 둔다.
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
            FakeView("root"),
            interactor,
            LoggedOutBuilder(component),
            LoggedInBuilder(component),
        )
    }
}

class RootInteractor : Interactor<Unit, RootRouter>(Unit), LoggedOutListener, LoggedInListener {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        router.routeToLoggedOut()
    }

    override fun didLogin(userName: String) {
        router.routeAwayFromLoggedOut()
        router.routeToLoggedIn(userName)
    }

    override fun didRequestLogout() {
        router.routeAwayFromLoggedIn()
        router.routeToLoggedOut()
    }
}

class RootRouter(
    view: FakeView,
    interactor: RootInteractor,
    private val loggedOutBuilder: LoggedOutBuilder,
    private val loggedInBuilder: LoggedInBuilder,
) : LaunchRouter<FakeView, RootInteractor>(view, interactor) {
    private var loggedOut: LoggedOutRouter? = null
    private var loggedIn: LoggedInRouter? = null

    fun routeToLoggedOut() {
        if (loggedOut != null) return
        loggedOut = loggedOutBuilder.build(interactor).also(::attachChild)
    }

    fun routeAwayFromLoggedOut() {
        loggedOut?.let(::detachChild)
        loggedOut = null
    }

    fun routeToLoggedIn(userName: String) {
        if (loggedIn != null) return
        loggedIn = loggedInBuilder.build(interactor, userName).also(::attachChild)
    }

    fun routeAwayFromLoggedIn() {
        loggedIn?.let(::detachChild)
        loggedIn = null
    }
}

// ---------- LoggedOut (뷰 + Presenter) ----------

interface LoggedOutDependency : Dependency {
    val sessionStore: SessionStore
}

/** 자식 → 부모: 사용자 의도로 표현한다. 라우팅은 부모가 정한다. */
interface LoggedOutListener {
    fun didLogin(userName: String)
}

class LoggedOutBuilder(dependency: LoggedOutDependency) : Builder<LoggedOutDependency>(dependency) {
    fun build(listener: LoggedOutListener): LoggedOutRouter {
        val presenter = LoggedOutPresenter()
        val interactor = LoggedOutInteractor(presenter, dependency.sessionStore, listener)
        return LoggedOutRouter(FakeView("loggedOut"), interactor)
    }
}

/** 뷰 입력을 해석해 Interactor로 넘긴다. 공백만 있는 이름은 거른다. */
class LoggedOutPresenter : Presenter() {
    var onLoginIntent: ((String) -> Unit)? = null
    var errorMessage: String? = null
        private set

    fun didTapLogin(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            errorMessage = "이름을 입력하세요"
            return
        }
        errorMessage = null
        onLoginIntent?.invoke(name)
    }
}

class LoggedOutInteractor(
    presenter: LoggedOutPresenter,
    val sessionStore: SessionStore,
    private val listener: LoggedOutListener,
) : Interactor<LoggedOutPresenter, LoggedOutRouter>(presenter) {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        presenter.onLoginIntent = { name ->
            sessionStore.signIn(name)
            listener.didLogin(name)
        }
    }

    override fun willResignActive() {
        presenter.onLoginIntent = null
    }
}

class LoggedOutRouter(view: FakeView, interactor: LoggedOutInteractor) :
    ViewRouter<FakeView, LoggedOutInteractor>(view, interactor)

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
class HeartbeatWorker : RibCoroutineWorker {
    var job: Job? = null
        private set
    var stopped = false
        private set

    override suspend fun onStart(scope: CoroutineScope) {
        stopped = false
        job = scope.launch { awaitCancellation() }
    }

    override fun onStop(cause: Throwable) {
        stopped = true
    }
}

class LoggedInInteractor(
    val userName: String,
    val sessionStore: SessionStore,
    private val listener: LoggedInListener,
    val heartbeat: HeartbeatWorker,
) : Interactor<Unit, LoggedInRouter>(Unit) {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        coroutineScope.bind(heartbeat)
    }

    fun logout() {
        sessionStore.signOut()
        listener.didRequestLogout()
    }
}

class LoggedInRouter(interactor: LoggedInInteractor) : Router<LoggedInInteractor>(interactor)
