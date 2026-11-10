package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.RibState
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewPresenter
import com.carti.ribs.ViewRouter

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoggedOutPresentableListener {
    fun didTapLogin(rawName: String)
}

/** 공유 코드 → 뷰: 플랫폼이 구현하는 뷰 계약(ADR-0006). */
interface LoggedOutPresentable : ViewControllable {
    /** 활성 구간에만 설정된다. 비활성이면 null이라 입력이 무시된다. 플랫폼은 약한 참조로 둔다. */
    var listener: LoggedOutPresentableListener?
    fun showError(message: String?)
}

interface LoggedOutDependency : Dependency {
    val viewFactory: LoginViewFactory
}

/** 자식 → 부모: 사용자 의도로 표현한다. 라우팅은 부모가 정한다. */
interface LoggedOutListener {
    fun didLogin(userName: String)
}

class LoggedOutBuilder(dependency: LoggedOutDependency) : Builder<LoggedOutDependency>(dependency) {
    fun build(listener: LoggedOutListener): LoggedOutRouter {
        val presenter = LoggedOutPresenter(dependency.viewFactory.makeLoggedOutView())
        return LoggedOutRouter(presenter.view, LoggedOutInteractor(presenter, listener))
    }
}

/** 뷰 입력을 로그인 의도로 해석한다. 공백만 있는 이름은 오류로 보여 준다. */
class LoggedOutPresenter(view: LoggedOutPresentable) :
    ViewPresenter<LoggedOutPresentable>(view), LoggedOutPresentableListener {
    var onLoginIntent: ((String) -> Unit)? = null

    override fun didLoad() {
        view.listener = this
    }

    override fun willUnload() {
        view.listener = null
    }

    override fun didTapLogin(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            view.showError(EMPTY_NAME_ERROR)
            return
        }
        view.showError(null)
        onLoginIntent?.invoke(name)
    }

    companion object {
        const val EMPTY_NAME_ERROR: String = "이름을 입력하세요"
    }
}

class LoggedOutInteractor(
    presenter: LoggedOutPresenter,
    private val listener: LoggedOutListener,
) : Interactor<LoggedOutPresenter, LoggedOutRouter>(presenter) {
    override fun didBecomeActive(savedInstanceState: RibState?) {
        presenter.onLoginIntent = listener::didLogin
    }

    override fun willResignActive() {
        presenter.onLoginIntent = null
    }
}

class LoggedOutRouter(view: LoggedOutPresentable, interactor: LoggedOutInteractor) :
    ViewRouter<LoggedOutPresentable, LoggedOutInteractor>(view, interactor)
