package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.Presenter
import com.carti.ribs.RibState
import com.carti.ribs.ViewRouter

interface LoggedOutDependency : Dependency {
    val viewFactory: LoginViewFactory
}

/** 자식 → 부모: 사용자 의도로 표현한다. 라우팅은 부모가 정한다. */
interface LoggedOutListener {
    fun didLogin(userName: String)
}

class LoggedOutBuilder(dependency: LoggedOutDependency) : Builder<LoggedOutDependency>(dependency) {
    fun build(listener: LoggedOutListener): LoggedOutRouter {
        val view = dependency.viewFactory.makeLoggedOutView()
        val interactor = LoggedOutInteractor(LoggedOutPresenter(view), listener)
        return LoggedOutRouter(view, interactor)
    }
}

/** 뷰 입력을 로그인 의도로 해석한다. 공백만 있는 이름은 오류로 보여 준다. */
class LoggedOutPresenter(private val view: LoggedOutViewControllable) : Presenter(), LoggedOutViewListener {
    var onLoginIntent: ((String) -> Unit)? = null

    fun bind() {
        view.listener = this
    }

    fun unbind() {
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
        presenter.bind()
    }

    override fun willResignActive() {
        presenter.unbind()
        presenter.onLoginIntent = null
    }
}

class LoggedOutRouter(view: LoggedOutViewControllable, interactor: LoggedOutInteractor) :
    ViewRouter<LoggedOutViewControllable, LoggedOutInteractor>(view, interactor)
