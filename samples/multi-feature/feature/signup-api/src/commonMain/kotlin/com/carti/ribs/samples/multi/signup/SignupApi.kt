@file:OptIn(ExperimentalJsExport::class, ExperimentalObjCName::class)

package com.carti.ribs.samples.multi.signup

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
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
    // 계약마다 Obj-C 이름을 고정한다. 같은 `render(state:)`가 여러 계약에 있으면 Kotlin/Native가 Swift 인자 이름을
    // 선언 순서대로 `state_`, `state__`…로 바꿔 내보내서, RIB이 늘 때마다 Swift 쪽 이름이 밀린다.
    @ObjCName("renderSignup")
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
