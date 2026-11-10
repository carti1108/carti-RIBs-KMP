package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Interactor
import com.carti.ribs.RibState
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewPresenter
import com.carti.ribs.ViewRouter
import com.carti.ribs.checkMainThread
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoggedOutPresentableListener {
    fun didTapLogin(rawName: String)
}

/** 공유 코드 → 뷰: 플랫폼이 구현하는 뷰 계약(ADR-0006). 메인 스레드에서만 불린다(ADR-0007). */
interface LoggedOutPresentable : ViewControllable {
    /** 활성 구간에만 설정된다. 비활성이면 null이라 입력이 무시된다. 플랫폼은 약한 참조로 둔다. */
    var listener: LoggedOutPresentableListener?
    fun showError(message: String?)
    fun showLoading(isLoading: Boolean)
}

interface LoggedOutDependency : Dependency {
    val viewFactory: LoginViewFactory
    val authService: AuthService
}

/** 자식 → 부모: 사용자 의도로 표현한다. 라우팅은 부모가 정한다. */
interface LoggedOutListener {
    fun didLogin(userName: String)
}

class LoggedOutBuilder(dependency: LoggedOutDependency) : Builder<LoggedOutDependency>(dependency) {
    fun build(listener: LoggedOutListener): LoggedOutRouter {
        val presenter = LoggedOutPresenter(dependency.viewFactory.makeLoggedOutView())
        val interactor = LoggedOutInteractor(presenter, dependency.authService, listener)
        return LoggedOutRouter(presenter.view, interactor)
    }
}

/**
 * 뷰 입력을 로그인 의도로 해석하고, Interactor가 정한 상태를 뷰에 그린다.
 * 뷰 호출은 코루틴에서 올 수 있으므로 [checkMainThread]로 메인 스레드인지 확인한다(ADR-0007).
 */
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
            showError(EMPTY_NAME_ERROR)
            return
        }
        showError(null)
        onLoginIntent?.invoke(name)
    }

    fun showLoading(isLoading: Boolean) {
        checkMainThread("LoggedOutPresenter.showLoading")
        view.showLoading(isLoading)
    }

    fun showError(message: String?) {
        checkMainThread("LoggedOutPresenter.showError")
        view.showError(message)
    }

    companion object {
        const val EMPTY_NAME_ERROR: String = "이름을 입력하세요"
    }
}

/** 로그인 요청을 활성 구간 스코프에서 실행한다. detach되면 요청이 취소된다. */
class LoggedOutInteractor(
    presenter: LoggedOutPresenter,
    private val authService: AuthService,
    private val listener: LoggedOutListener,
) : Interactor<LoggedOutPresenter, LoggedOutRouter>(presenter) {
    private var loginJob: Job? = null

    override fun didBecomeActive(savedInstanceState: RibState?) {
        presenter.onLoginIntent = ::login
    }

    override fun willResignActive() {
        presenter.onLoginIntent = null
        loginJob = null
    }

    private fun login(userName: String) {
        if (loginJob?.isActive == true) return
        // 활성 구간 스코프는 메인 디스패처라 결과도 메인 스레드에서 받는다.
        loginJob = coroutineScope.launch {
            presenter.showLoading(true)
            val result = authService.signIn(userName)
            presenter.showLoading(false)
            when (result) {
                is AuthResult.Success -> listener.didLogin(result.userName)
                is AuthResult.Failure -> presenter.showError(result.message)
            }
        }
    }
}

class LoggedOutRouter(view: LoggedOutPresentable, interactor: LoggedOutInteractor) :
    ViewRouter<LoggedOutPresentable, LoggedOutInteractor>(view, interactor)
