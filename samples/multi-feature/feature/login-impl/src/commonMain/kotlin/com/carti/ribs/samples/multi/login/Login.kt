package com.carti.ribs.samples.multi.login

import com.carti.ribs.Builder
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewableRouter
import com.carti.ribs.ViewableRouting
import com.carti.ribs.checkMainThread

class LoginBuilder(dependency: LoginDependency) : Builder<LoginDependency>(dependency), LoginBuildable {
    override fun build(listener: LoginListener): ViewableRouting {
        val view = dependency.makeLoginView()
        return LoginRouter(LoginInteractor(view, listener), view)
    }
}

/** 이름을 다듬어 비었으면 오류를, 아니면 로그인 의도를 부모에게 알린다. 라우팅은 부모가 정한다. */
class LoginInteractor(
    presenter: LoginPresentable,
    private val listener: LoginListener,
) : PresentableInteractor<LoginPresentable>(presenter), LoginPresentableListener {
    override fun didBecomeActive() {
        presenter.listener = this
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapLogin(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            render(LoginViewState(errorMessage = EMPTY_NAME_ERROR))
            return
        }
        render(LoginViewState())
        listener.didLogin(name)
    }

    override fun didTapSignup() {
        listener.didRequestSignup()
    }

    private fun render(state: LoginViewState) {
        checkMainThread("LoginPresentable.render")
        presenter.render(state)
    }

    companion object {
        const val EMPTY_NAME_ERROR: String = "이름을 입력하세요"
    }
}

class LoginRouter(interactor: LoginInteractor, view: LoginViewControllable) :
    ViewableRouter<LoginInteractor, LoginViewControllable>(interactor, view)
