@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.samples.multi.settings

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
