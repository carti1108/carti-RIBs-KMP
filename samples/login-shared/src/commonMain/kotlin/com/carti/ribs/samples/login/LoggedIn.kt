package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.RibState
import com.carti.ribs.ViewRouter

interface LoggedInDependency : Dependency {
    val viewFactory: LoginViewFactory
}

interface LoggedInListener {
    fun didRequestLogout()
}

class LoggedInBuilder(dependency: LoggedInDependency) : Builder<LoggedInDependency>(dependency) {
    fun build(listener: LoggedInListener, userName: String): LoggedInRouter {
        val view = dependency.viewFactory.makeLoggedInView()
        return LoggedInRouter(view, LoggedInInteractor(view, userName, listener))
    }
}

/** 화면 로직이 단순해 Presenter 없이 Interactor가 뷰 계약을 직접 다룬다(원본의 Presentable 패턴). */
class LoggedInInteractor(
    private val view: LoggedInViewControllable,
    private val userName: String,
    private val listener: LoggedInListener,
) : Interactor<Unit, LoggedInRouter>(Unit), LoggedInViewListener {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        view.listener = this
        view.showUserName(userName)
    }

    override fun willResignActive() {
        view.listener = null
    }

    override fun didTapLogout() {
        listener.didRequestLogout()
    }
}

class LoggedInRouter(view: LoggedInViewControllable, interactor: LoggedInInteractor) :
    ViewRouter<LoggedInViewControllable, LoggedInInteractor>(view, interactor)
