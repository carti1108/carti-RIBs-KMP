package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Component
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.LaunchRouter
import com.carti.ribs.ViewControllable

/** 루트 뷰 계약. 자식 RIB 화면 하나를 보여 준다. 플랫폼이 구현한다(ADR-0006). */
interface RootViewControllable : ViewControllable {
    fun showScreen(screen: ViewControllable)
    fun hideScreen(screen: ViewControllable)
}

/**
 * 앱 최상단 의존성. 플랫폼이 구현한다(RIBs-iOS의 `AppComponent` 자리).
 * 공유 코드는 플랫폼 뷰를 만들 수 없으므로 자손 RIB의 뷰 생성도 여기까지 올라온다.
 * 각 RIB은 자기 `XxxDependency`에 선언된 것만 보므로, 기능을 모듈로 나눠도 이 인터페이스만 앱 모듈에 남는다.
 */
interface RootDependency : Dependency {
    val authService: AuthService
    fun makeRootView(): RootViewControllable
    fun makeLoggedOutView(): LoggedOutViewContract
    fun makeLoggedInView(): LoggedInViewContract
    fun makeTodoListView(): TodoListViewContract
    fun makeTodoDetailView(): TodoDetailViewContract
}

class RootComponent(dependency: RootDependency) :
    Component<RootDependency>(dependency), LoggedOutDependency, LoggedInDependency {
    override val authService: AuthService get() = dependency.authService
    override fun makeLoggedOutView(): LoggedOutViewContract = dependency.makeLoggedOutView()
    override fun makeLoggedInView(): LoggedInViewContract = dependency.makeLoggedInView()
    override fun makeTodoListView(): TodoListViewContract = dependency.makeTodoListView()
    override fun makeTodoDetailView(): TodoDetailViewContract = dependency.makeTodoDetailView()
}

class RootBuilder(dependency: RootDependency) : Builder<RootDependency>(dependency) {
    fun build(): RootRouter {
        val component = RootComponent(dependency)
        return RootRouter(
            interactor = RootInteractor(),
            view = dependency.makeRootView(),
            loggedOutBuilder = LoggedOutBuilder(component),
            loggedInBuilder = LoggedInBuilder(component),
        )
    }
}

/** 저장 상태: 로그인한 사용자 이름. 되살릴 때 이름이 있으면 로그인 화면을 건너뛴다. */
class RootInteractor : Interactor(), LoggedOutListener, LoggedInListener {
    /** Router가 만들어질 때 자신을 넣는다(RIBs-iOS 템플릿과 같음). */
    var router: RootRouter? = null
    private var userName: String? = null

    override fun didBecomeActive() {
        val restoredUser = savedInstanceState?.get(KEY_USER_NAME) as? String
        if (restoredUser != null) didLogin(restoredUser) else router?.routeToLoggedOut()
    }

    override fun onSaveInstanceState(outState: MutableMap<String, Any?>) {
        outState[KEY_USER_NAME] = userName
    }

    override fun didLogin(userName: String) {
        this.userName = userName
        router?.routeAwayFromLoggedOut()
        router?.routeToLoggedIn(userName)
    }

    override fun didRequestLogout() {
        userName = null
        router?.routeAwayFromLoggedIn()
        router?.routeToLoggedOut()
    }

    private companion object {
        const val KEY_USER_NAME = "userName"
    }
}

class RootRouter(
    interactor: RootInteractor,
    view: RootViewControllable,
    private val loggedOutBuilder: LoggedOutBuilder,
    private val loggedInBuilder: LoggedInBuilder,
) : LaunchRouter<RootInteractor, RootViewControllable>(interactor, view) {
    private var loggedOut: LoggedOutRouter? = null
    private var loggedIn: LoggedInRouter? = null

    init {
        interactor.router = this
    }

    /** 자식에게 먼저 맡긴다. 아무도 처리하지 않으면 false라서 플랫폼 기본 동작을 따른다. */
    override fun handleBackPress(): Boolean = loggedIn?.handleBackPress() ?: false

    fun routeToLoggedOut() {
        if (loggedOut != null) return
        val child = loggedOutBuilder.build(interactor)
        attachChild(child)
        viewController.showScreen(child.viewController)
        loggedOut = child
    }

    fun routeAwayFromLoggedOut() {
        val child = loggedOut ?: return
        detachChild(child)
        viewController.hideScreen(child.viewController)
        loggedOut = null
    }

    fun routeToLoggedIn(userName: String) {
        if (loggedIn != null) return
        val child = loggedInBuilder.build(interactor, userName)
        attachChild(child)
        viewController.showScreen(child.viewController)
        loggedIn = child
    }

    fun routeAwayFromLoggedIn() {
        val child = loggedIn ?: return
        detachChild(child)
        viewController.hideScreen(child.viewController)
        loggedIn = null
    }
}
