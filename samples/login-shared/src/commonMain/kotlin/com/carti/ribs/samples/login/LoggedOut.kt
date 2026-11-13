package com.carti.ribs.samples.login

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Presentable
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouter
import com.carti.ribs.checkMainThread
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoggedOutPresentableListener {
    fun didTapLogin(rawName: String)
}

/** 공유 코드 → 뷰: 플랫폼이 구현하는 뷰 계약(ADR-0006). 메인 스레드에서만 불린다(ADR-0007). */
interface LoggedOutPresentable : Presentable {
    /** 활성 구간에만 설정된다. 비활성이면 null이라 입력이 무시된다. 플랫폼은 약한 참조로 둔다. */
    var listener: LoggedOutPresentableListener?
    fun showError(message: String?)
    fun showLoading(isLoading: Boolean)
}

/** Router → 뷰. 붙일 자식 화면이 없어 비어 있다. RIBs-iOS 템플릿의 `LoggedOutViewControllable`. */
interface LoggedOutViewControllable : ViewControllable

/** 플랫폼 뷰 하나가 구현하는 묶음. 팩토리가 돌려주고, Interactor는 [LoggedOutPresentable]로, Router는 [LoggedOutViewControllable]로만 본다. */
interface LoggedOutViewContract : LoggedOutPresentable, LoggedOutViewControllable

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
        val view = dependency.viewFactory.makeLoggedOutView()
        val interactor = LoggedOutInteractor(view, dependency.authService, listener)
        return LoggedOutRouter(interactor, view)
    }
}

/**
 * 뷰 입력을 로그인 의도로 해석하고, 로그인 요청을 활성 구간 스코프에서 실행한다. 비활성화되면 요청이 취소된다.
 * 뷰 호출은 코루틴에서 올 수 있으므로 [checkMainThread]로 메인 스레드인지 확인한다(ADR-0007).
 */
class LoggedOutInteractor(
    presenter: LoggedOutPresentable,
    private val authService: AuthService,
    private val listener: LoggedOutListener,
) : PresentableInteractor<LoggedOutPresentable>(presenter), LoggedOutPresentableListener {
    private var loginJob: Job? = null

    override fun didBecomeActive() {
        presenter.listener = this
    }

    override fun willResignActive() {
        presenter.listener = null
        loginJob = null
    }

    override fun didTapLogin(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            showError(EMPTY_NAME_ERROR)
            return
        }
        showError(null)
        login(name)
    }

    private fun login(userName: String) {
        if (loginJob?.isActive == true) return
        // 활성 구간 스코프는 메인 디스패처라 결과도 메인 스레드에서 받는다.
        loginJob = coroutineScope.launch {
            showLoading(true)
            val result = authService.signIn(userName)
            showLoading(false)
            when (result) {
                is AuthResult.Success -> listener.didLogin(result.userName)
                is AuthResult.Failure -> showError(result.message)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        checkMainThread("LoggedOutPresentable.showLoading")
        presenter.showLoading(isLoading)
    }

    private fun showError(message: String?) {
        checkMainThread("LoggedOutPresentable.showError")
        presenter.showError(message)
    }

    companion object {
        const val EMPTY_NAME_ERROR: String = "이름을 입력하세요"
    }
}

class LoggedOutRouter(interactor: LoggedOutInteractor, view: LoggedOutViewControllable) :
    ViewableRouter<LoggedOutInteractor, LoggedOutViewControllable>(interactor, view)
