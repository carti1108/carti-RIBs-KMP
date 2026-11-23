@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.samples.multi.signup

import com.carti.ribs.Dependency
import com.carti.ribs.Presentable
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouting
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// 가입 피처의 공개 계약. 구현은 signup-impl에 있다.

@JsExport
data class SignupViewState(val errorMessage: String? = null)

interface SignupPresentableListener {
    fun didTapSignup(rawName: String)
    fun didTapCancel()
}

interface SignupPresentable : Presentable {
    var listener: SignupPresentableListener?
    fun render(state: SignupViewState)
}

interface SignupViewControllable : ViewControllable

interface SignupViewContract : SignupPresentable, SignupViewControllable

interface SignupDependency : Dependency {
    fun makeSignupView(): SignupViewContract
}

interface SignupListener {
    fun didSignUp(userName: String)
    fun didCancelSignup()
}

interface SignupBuildable {
    fun build(listener: SignupListener): ViewableRouting
}
