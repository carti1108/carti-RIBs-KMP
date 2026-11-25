@file:OptIn(ExperimentalJsExport::class, ExperimentalObjCName::class)

package com.carti.ribs.samples.multi.settings

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import com.carti.ribs.Dependency
import com.carti.ribs.Presentable
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouting
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// 설정 피처의 공개 계약. 구현은 settings-impl에 있다.

@JsExport
data class SettingsViewState(val userName: String)

interface SettingsPresentableListener {
    fun didTapLogout()
    fun didTapClose()
}

interface SettingsPresentable : Presentable {
    var listener: SettingsPresentableListener?
    // 계약마다 Obj-C 이름을 고정한다. 같은 `render(state:)`가 여러 계약에 있으면 Kotlin/Native가 Swift 인자 이름을
    // 선언 순서대로 `state_`, `state__`…로 바꿔 내보내서, RIB이 늘 때마다 Swift 쪽 이름이 밀린다.
    @ObjCName("renderSettings")
    fun render(state: SettingsViewState)
}

interface SettingsViewControllable : ViewControllable

interface SettingsViewContract : SettingsPresentable, SettingsViewControllable

interface SettingsDependency : Dependency {
    fun makeSettingsView(): SettingsViewContract
}

interface SettingsListener {
    fun didRequestLogout()
    fun didCloseSettings()
}

interface SettingsBuildable {
    fun build(listener: SettingsListener, userName: String): ViewableRouting
}
