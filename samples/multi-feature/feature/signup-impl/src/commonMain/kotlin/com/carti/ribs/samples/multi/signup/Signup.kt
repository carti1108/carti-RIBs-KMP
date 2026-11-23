package com.carti.ribs.samples.multi.signup

import com.carti.ribs.Builder
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewableRouter
import com.carti.ribs.ViewableRouting
import com.carti.ribs.checkMainThread

class SignupBuilder(dependency: SignupDependency) : Builder<SignupDependency>(dependency), SignupBuildable {
    override fun build(listener: SignupListener): ViewableRouting {
        val view = dependency.makeSignupView()
        return SignupRouter(SignupInteractor(view, listener), view)
    }
}

class SignupInteractor(
    presenter: SignupPresentable,
    private val listener: SignupListener,
) : PresentableInteractor<SignupPresentable>(presenter), SignupPresentableListener {
    override fun didBecomeActive() {
        presenter.listener = this
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapSignup(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            render(SignupViewState(errorMessage = EMPTY_NAME_ERROR))
            return
        }
        render(SignupViewState())
        listener.didSignUp(name)
    }

    override fun didTapCancel() {
        listener.didCancelSignup()
    }

    private fun render(state: SignupViewState) {
        checkMainThread("SignupPresentable.render")
        presenter.render(state)
    }

    companion object {
        const val EMPTY_NAME_ERROR: String = "이름을 입력하세요"
    }
}

class SignupRouter(interactor: SignupInteractor, view: SignupViewControllable) :
    ViewableRouter<SignupInteractor, SignupViewControllable>(interactor, view)
