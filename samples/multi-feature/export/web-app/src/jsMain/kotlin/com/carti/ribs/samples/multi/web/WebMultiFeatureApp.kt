@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.samples.multi.web

import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewHost
import com.carti.ribs.samples.multi.login.LoginPresentableListener
import com.carti.ribs.samples.multi.login.LoginViewContract
import com.carti.ribs.samples.multi.login.LoginViewState
import com.carti.ribs.samples.multi.root.MultiFeatureApp
import com.carti.ribs.samples.multi.root.RootDependency
import com.carti.ribs.samples.multi.root.RootViewControllable
import com.carti.ribs.samples.multi.settings.SettingsPresentableListener
import com.carti.ribs.samples.multi.settings.SettingsViewContract
import com.carti.ribs.samples.multi.settings.SettingsViewState
import com.carti.ribs.samples.multi.signup.SignupPresentableListener
import com.carti.ribs.samples.multi.signup.SignupViewContract
import com.carti.ribs.samples.multi.signup.SignupViewState
import com.carti.ribs.samples.multi.todo.TodoPresentableListener
import com.carti.ribs.samples.multi.todo.TodoViewContract
import com.carti.ribs.samples.multi.todo.TodoViewState
import com.carti.ribs.web.ScreenSlot
import com.carti.ribs.web.ViewStore
import com.carti.ribs.web.WebViewControllable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// 뷰 계약을 화면 상태 스냅숏(ViewStore)으로 바꾸는 웹 구현(ADR-0009).
// React는 store를 구독하고 입력은 내보낸 함수로 전달한다.

/**
 * React 앱의 진입점. 루트 RIB을 [rootSlot]에 붙인다.
 * StrictMode의 이중 마운트와 겹치지 않도록 컴포넌트 밖(main.tsx)에서 [start]·[stop]을 부른다.
 */
@JsExport
class WebMultiFeatureApp {
    val rootSlot: ScreenSlot = ScreenSlot()
    private val app = MultiFeatureApp(WebAppComponent())

    fun start() {
        app.start(
            object : ViewHost<RootViewControllable> {
                override fun show(view: RootViewControllable) = rootSlot.show(view as WebRootView)
                override fun hide(view: RootViewControllable) = rootSlot.hide(view as WebRootView)
            },
        )
    }

    fun handleBack(): Boolean = app.handleBackPress()

    fun stop() = app.stop()
}

private class WebAppComponent : RootDependency {
    override fun makeRootView(): RootViewControllable = WebRootView()
    override fun makeLoginView(): LoginViewContract = WebLoginView()
    override fun makeSignupView(): SignupViewContract = WebSignupView()
    override fun makeTodoView(): TodoViewContract = WebTodoView()
    override fun makeSettingsView(): SettingsViewContract = WebSettingsView()
}

private fun ViewControllable.asWeb(): WebViewControllable =
    this as? WebViewControllable ?: error("${this::class.simpleName} is not a WebViewControllable")

@JsExport
class WebRootView : RootViewControllable, WebViewControllable {
    override val screenKey: String = "root"
    val slot: ScreenSlot = ScreenSlot()

    @JsExport.Ignore
    override fun showScreen(screen: ViewControllable) = slot.show(screen.asWeb())

    @JsExport.Ignore
    override fun hideScreen(screen: ViewControllable) = slot.hide(screen.asWeb())
}

@JsExport
class WebLoginView : LoginViewContract, WebViewControllable {
    override val screenKey: String = "login"
    val store: ViewStore<LoginViewState> = ViewStore(LoginViewState())

    @JsExport.Ignore
    override var listener: LoginPresentableListener? = null

    fun login(rawName: String) {
        listener?.didTapLogin(rawName)
    }

    fun signup() {
        listener?.didTapSignup()
    }

    @JsExport.Ignore
    override fun render(state: LoginViewState) = store.set(state)
}

@JsExport
class WebSignupView : SignupViewContract, WebViewControllable {
    override val screenKey: String = "signup"
    val store: ViewStore<SignupViewState> = ViewStore(SignupViewState())

    @JsExport.Ignore
    override var listener: SignupPresentableListener? = null

    fun signup(rawName: String) {
        listener?.didTapSignup(rawName)
    }

    fun cancel() {
        listener?.didTapCancel()
    }

    @JsExport.Ignore
    override fun render(state: SignupViewState) = store.set(state)
}

@JsExport
class WebTodoView : TodoViewContract, WebViewControllable {
    override val screenKey: String = "todo"
    val store: ViewStore<TodoViewState> = ViewStore(TodoViewState(userName = "", items = emptyArray(), isLoading = false, errorMessage = null))

    @JsExport.Ignore
    override var listener: TodoPresentableListener? = null

    fun toggle(id: Int) {
        listener?.didToggle(id)
    }

    fun openSettings() {
        listener?.didTapSettings()
    }

    fun retry() {
        listener?.didTapRetry()
    }

    @JsExport.Ignore
    override fun render(state: TodoViewState) = store.set(state)
}

@JsExport
class WebSettingsView : SettingsViewContract, WebViewControllable {
    override val screenKey: String = "settings"
    val store: ViewStore<SettingsViewState> = ViewStore(SettingsViewState(userName = ""))

    @JsExport.Ignore
    override var listener: SettingsPresentableListener? = null

    fun logout() {
        listener?.didTapLogout()
    }

    fun close() {
        listener?.didTapClose()
    }

    @JsExport.Ignore
    override fun render(state: SettingsViewState) = store.set(state)
}
