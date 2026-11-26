@file:OptIn(ExperimentalJsExport::class, ExperimentalObjCName::class)

package com.carti.ribs.samples.multi.login

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import com.carti.ribs.Dependency
import com.carti.ribs.Presentable
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouting
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// 로그인 피처의 공개 계약. 부모 RIB과 플랫폼 뷰는 이 모듈만 안다. 구현은 login-impl에 있다.

/** 화면 상태 한 벌. */
@JsExport
data class LoginViewState(val errorMessage: String? = null)

/** 뷰 → 공유 코드: 사용자 입력. */
interface LoginPresentableListener {
    fun didTapLogin(rawName: String)
    fun didTapSignup()
}

/** 공유 코드 → 뷰. 메인 스레드에서만 불린다(ADR-0007). */
interface LoginPresentable : Presentable {
    var listener: LoginPresentableListener?
    // 계약마다 Obj-C 이름을 고정한다. 같은 `render(state:)`가 여러 계약에 있으면 Kotlin/Native가 Swift 인자 이름을
    // 선언 순서대로 `state_`, `state__`…로 바꿔 내보내서, RIB이 늘 때마다 Swift 쪽 이름이 밀린다.
    @ObjCName("renderLogin")
    fun render(state: LoginViewState)
}

interface LoginViewControllable : ViewControllable

/** 플랫폼 뷰 하나가 구현하는 묶음. */
interface LoginViewContract : LoginPresentable, LoginViewControllable

/** 부모가 제공해야 하는 의존성. */
interface LoginDependency : Dependency {
    fun makeLoginView(): LoginViewContract
}

/** 자식 → 부모: 사용자 의도. */
interface LoginListener {
    fun didLogin(userName: String)
    fun didRequestSignup()
}

/** RIBs-iOS의 `Buildable`. 부모는 구현 타입 대신 이 인터페이스로 자식을 만든다. */
interface LoginBuildable {
    fun build(listener: LoginListener): ViewableRouting
}
