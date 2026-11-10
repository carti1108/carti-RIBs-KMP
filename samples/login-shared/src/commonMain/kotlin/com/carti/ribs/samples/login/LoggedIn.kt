package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.RibState
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewRouter

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoggedInPresentableListener {
    fun didTapLogout()
}

/** 공유 코드 → 뷰: 플랫폼이 구현하는 뷰 계약(ADR-0006). */
interface LoggedInPresentable : ViewControllable {
    /** 활성 구간에만 설정된다. 플랫폼은 약한 참조로 둔다. */
    var listener: LoggedInPresentableListener?
    fun showUserName(name: String)
}

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

/**
 * 화면 로직이 단순해 별도 Presenter 없이 뷰 계약을 presenter 자리에 받는다
 * (원본의 `Interactor<P, R>`에서 P가 뷰가 구현하는 인터페이스인 형태, RIBs-iOS의 `PresentableInteractor`).
 */
class LoggedInInteractor(
    presenter: LoggedInPresentable,
    private val userName: String,
    private val listener: LoggedInListener,
) : Interactor<LoggedInPresentable, LoggedInRouter>(presenter), LoggedInPresentableListener {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        presenter.listener = this
        presenter.showUserName(userName)
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapLogout() {
        listener.didRequestLogout()
    }
}

class LoggedInRouter(view: LoggedInPresentable, interactor: LoggedInInteractor) :
    ViewRouter<LoggedInPresentable, LoggedInInteractor>(view, interactor)
