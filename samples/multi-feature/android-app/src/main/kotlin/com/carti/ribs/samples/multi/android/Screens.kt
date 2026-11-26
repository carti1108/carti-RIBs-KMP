package com.carti.ribs.samples.multi.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.carti.ribs.ViewControllable
import com.carti.ribs.compose.ComposeRenderable
import com.carti.ribs.compose.ComposeScreenSlot
import com.carti.ribs.compose.RibSlot
import com.carti.ribs.samples.multi.login.LoginPresentableListener
import com.carti.ribs.samples.multi.login.LoginViewContract
import com.carti.ribs.samples.multi.login.LoginViewState
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

// 피처마다 화면 하나. 피처를 Android 모듈로 더 나누려면 이 파일의 화면을 feature/<이름>-ui-android로 옮기면 된다.

/** 테스트와 화면이 같이 쓰는 태그. iOS accessibilityIdentifier와 이름을 맞췄다. */
object Tags {
    const val NAME_FIELD = "nameField"
    const val LOGIN_BUTTON = "loginButton"
    const val SIGNUP_LINK = "signupLink"
    const val SIGNUP_BUTTON = "signupButton"
    const val CANCEL_BUTTON = "cancelButton"
    const val ERROR_LABEL = "errorLabel"
    const val WELCOME_LABEL = "welcomeLabel"
    const val SETTINGS_BUTTON = "settingsButton"
    const val SETTINGS_USER_LABEL = "settingsUserLabel"
    const val CLOSE_BUTTON = "closeButton"
    const val LOGOUT_BUTTON = "logoutButton"
    const val LOADING_INDICATOR = "loadingIndicator"
    const val RETRY_BUTTON = "retryButton"
    fun todoRow(id: Int): String = "todoRow-$id"
}

/** 앱 최상단 의존성. 피처마다 선언한 뷰 생성 함수를 Compose 화면으로 채운다. */
class AppComponent : RootDependency {
    override fun makeRootView(): RootViewControllable = RootScreen()
    override fun makeLoginView(): LoginViewContract = LoginScreen()
    override fun makeSignupView(): SignupViewContract = SignupScreen()
    override fun makeTodoView(): TodoViewContract = TodoScreen()
    override fun makeSettingsView(): SettingsViewContract = SettingsScreen()
}

class RootScreen : RootViewControllable, ComposeRenderable {
    private val slot = ComposeScreenSlot()

    override fun showScreen(screen: ViewControllable) = slot.show(screen)

    override fun hideScreen(screen: ViewControllable) = slot.hide(screen)

    @Composable
    override fun Content() = RibSlot(slot)
}

// 어댑터(ADR-0011). 피처 계약을 구현하고 상태를 Views.kt의 순수 뷰 인자로 풀어 넘긴다.

class LoginScreen : LoginViewContract, ComposeRenderable {
    override var listener: LoginPresentableListener? = null
    private var state by mutableStateOf(LoginViewState())

    override fun render(state: LoginViewState) {
        this.state = state
    }

    @Composable
    override fun Content() = LoginView(
        errorMessage = state.errorMessage,
        onLogin = { listener?.didTapLogin(it) },
        onSignup = { listener?.didTapSignup() },
    )
}

class SignupScreen : SignupViewContract, ComposeRenderable {
    override var listener: SignupPresentableListener? = null
    private var state by mutableStateOf(SignupViewState())

    override fun render(state: SignupViewState) {
        this.state = state
    }

    @Composable
    override fun Content() = SignupView(
        errorMessage = state.errorMessage,
        onSignup = { listener?.didTapSignup(it) },
        onCancel = { listener?.didTapCancel() },
    )
}

class TodoScreen : TodoViewContract, ComposeRenderable {
    override var listener: TodoPresentableListener? = null
    private var state by mutableStateOf(TodoViewState(userName = "", items = emptyArray(), isLoading = false, errorMessage = null, notice = null))

    override fun render(state: TodoViewState) {
        this.state = state
    }

    @Composable
    override fun Content() = TodoView(
        userName = state.userName,
        rows = state.items.map { TodoRow(it.id, it.title, it.isDone) },
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onToggle = { listener?.didToggle(it) },
        onRetry = { listener?.didTapRetry() },
        onSettings = { listener?.didTapSettings() },
    )
}

class SettingsScreen : SettingsViewContract, ComposeRenderable {
    override var listener: SettingsPresentableListener? = null
    private var state by mutableStateOf(SettingsViewState(userName = ""))

    override fun render(state: SettingsViewState) {
        this.state = state
    }

    @Composable
    override fun Content() = SettingsView(
        userName = state.userName,
        onLogout = { listener?.didTapLogout() },
        onClose = { listener?.didTapClose() },
    )
}
