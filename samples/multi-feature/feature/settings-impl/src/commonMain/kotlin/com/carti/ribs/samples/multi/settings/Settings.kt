package com.carti.ribs.samples.multi.settings

import com.carti.ribs.Builder
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewableRouter
import com.carti.ribs.ViewableRouting
import com.carti.ribs.checkMainThread

class SettingsBuilder(dependency: SettingsDependency) : Builder<SettingsDependency>(dependency), SettingsBuildable {
    override fun build(listener: SettingsListener, userName: String): ViewableRouting {
        val view = dependency.makeSettingsView()
        return SettingsRouter(SettingsInteractor(view, listener, userName), view)
    }
}

class SettingsInteractor(
    presenter: SettingsPresentable,
    private val listener: SettingsListener,
    private val userName: String,
) : PresentableInteractor<SettingsPresentable>(presenter), SettingsPresentableListener {
    override fun didBecomeActive() {
        presenter.listener = this
        checkMainThread("SettingsPresentable.render")
        presenter.render(SettingsViewState(userName))
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapLogout() {
        listener.didRequestLogout()
    }

    override fun didTapClose() {
        listener.didCloseSettings()
    }
}

class SettingsRouter(interactor: SettingsInteractor, view: SettingsViewControllable) :
    ViewableRouter<SettingsInteractor, SettingsViewControllable>(interactor, view)
