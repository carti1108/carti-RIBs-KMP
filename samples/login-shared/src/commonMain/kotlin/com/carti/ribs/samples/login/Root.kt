package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Component
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.LaunchRouter
import com.carti.ribs.RibState
import com.carti.ribs.ViewControllable

/** 루트 뷰 계약. 자식 RIB 화면 하나를 보여 준다. 플랫폼이 구현한다(ADR-0006). */
interface RootViewControllable : ViewControllable {
    fun showScreen(screen: ViewControllable)
    fun hideScreen(screen: ViewControllable)
}

interface RootDependency : Dependency {
    val viewFactory: LoginViewFactory
    val authService: AuthService
}

class RootComponent(dependency: RootDependency) :
    Component<RootDependency>(dependency), LoggedOutDependency, LoggedInDependency {
    override val viewFactory: LoginViewFactory get() = dependency.viewFactory
    override val authService: AuthService get() = dependency.authService
}

class RootBuilder(dependency: RootDependency) : Builder<RootDependency>(dependency) {
    fun build(): RootRouter {
        val component = RootComponent(dependency)
        return RootRouter(
            view = dependency.viewFactory.makeRootView(),
            interactor = RootInteractor(),
            loggedOutBuilder = LoggedOutBuilder(component),
            loggedInBuilder = LoggedInBuilder(component),
        )
    }
}

/** 저장 상태: 로그인한 사용자 이름. 되살릴 때 이름이 있으면 로그인 화면을 건너뛴다. */
class RootInteractor : Interactor<Unit, RootRouter>(Unit), LoggedOutListener, LoggedInListener {
    private var userName: String? = null

    override fun didBecomeActive(savedInstanceState: RibState?) {
        val restoredUser = savedInstanceState?.get(KEY_USER_NAME) as? String
        if (restoredUser != null) didLogin(restoredUser) else router.routeToLoggedOut()
    }

    override fun onSaveInstanceState(outState: MutableMap<String, Any?>) {
        outState[KEY_USER_NAME] = userName
    }

    override fun didLogin(userName: String) {
        this.userName = userName
        router.routeAwayFromLoggedOut()
        router.routeToLoggedIn(userName)
    }

    override fun didRequestLogout() {
        userName = null
        router.routeAwayFromLoggedIn()
        router.routeToLoggedOut()
    }

    private companion object {
        const val KEY_USER_NAME = "userName"
    }
}

class RootRouter(
    view: RootViewControllable,
    interactor: RootInteractor,
    private val loggedOutBuilder: LoggedOutBuilder,
    private val loggedInBuilder: LoggedInBuilder,
) : LaunchRouter<RootViewControllable, RootInteractor>(view, interactor) {
    private var loggedOut: LoggedOutRouter? = null
    private var loggedIn: LoggedInRouter? = null

    /** 자식에게 먼저 맡긴다. 아무도 처리하지 않으면 false라서 플랫폼 기본 동작을 따른다. */
    override fun handleBackPress(): Boolean = loggedIn?.handleBackPress() ?: false

    fun routeToLoggedOut() {
        if (loggedOut != null) return
        val child = loggedOutBuilder.build(interactor)
        attachChild(child)
        view.showScreen(child.view)
        loggedOut = child
    }

    fun routeAwayFromLoggedOut() {
        val child = loggedOut ?: return
        detachChild(child)
        view.hideScreen(child.view)
        loggedOut = null
    }

    fun routeToLoggedIn(userName: String) {
        if (loggedIn != null) return
        val child = loggedInBuilder.build(interactor, userName)
        attachChild(child)
        view.showScreen(child.view)
        loggedIn = child
    }

    fun routeAwayFromLoggedIn() {
        val child = loggedIn ?: return
        detachChild(child)
        view.hideScreen(child.view)
        loggedIn = null
    }
}
