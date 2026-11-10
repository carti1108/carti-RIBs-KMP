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
    view: RootViewControllable,
    interactor: RootInteractor,
    private val loggedOutBuilder: LoggedOutBuilder,
    private val loggedInBuilder: LoggedInBuilder,
) : LaunchRouter<RootViewControllable, RootInteractor>(view, interactor) {
    private var loggedOut: LoggedOutRouter? = null
    private var loggedIn: LoggedInRouter? = null

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
