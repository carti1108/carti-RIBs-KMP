package com.carti.ribs.samples.multi.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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

class LoginScreen : LoginViewContract, ComposeRenderable {
    override var listener: LoginPresentableListener? = null
    private var state by mutableStateOf(LoginViewState())

    override fun render(state: LoginViewState) {
        this.state = state
    }

    @Composable
    override fun Content() {
        var name by rememberSaveable { mutableStateOf("") }
        ScreenColumn {
            Text("로그인", style = MaterialTheme.typography.headlineLarge)
            NameField(name) { name = it }
            ErrorText(state.errorMessage)
            Button(onClick = { listener?.didTapLogin(name) }, modifier = Modifier.testTag(Tags.LOGIN_BUTTON)) {
                Text("로그인")
            }
            TextButton(onClick = { listener?.didTapSignup() }, modifier = Modifier.testTag(Tags.SIGNUP_LINK)) {
                Text("가입하기")
            }
        }
    }
}

class SignupScreen : SignupViewContract, ComposeRenderable {
    override var listener: SignupPresentableListener? = null
    private var state by mutableStateOf(SignupViewState())

    override fun render(state: SignupViewState) {
        this.state = state
    }

    @Composable
    override fun Content() {
        var name by rememberSaveable { mutableStateOf("") }
        ScreenColumn {
            Text("가입", style = MaterialTheme.typography.headlineLarge)
            NameField(name) { name = it }
            ErrorText(state.errorMessage)
            Button(onClick = { listener?.didTapSignup(name) }, modifier = Modifier.testTag(Tags.SIGNUP_BUTTON)) {
                Text("가입")
            }
            TextButton(onClick = { listener?.didTapCancel() }, modifier = Modifier.testTag(Tags.CANCEL_BUTTON)) {
                Text("취소")
            }
        }
    }
}

class TodoScreen : TodoViewContract, ComposeRenderable {
    override var listener: TodoPresentableListener? = null
    private var state by mutableStateOf(TodoViewState(userName = "", items = emptyArray()))

    override fun render(state: TodoViewState) {
        this.state = state
    }

    @Composable
    override fun Content() {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${state.userName}님의 할 일",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(Tags.WELCOME_LABEL),
            )
            state.items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag(Tags.todoRow(item.id)),
                ) {
                    Checkbox(checked = item.isDone, onCheckedChange = { listener?.didToggle(item.id) })
                    Text(item.title)
                }
            }
            OutlinedButton(
                onClick = { listener?.didTapSettings() },
                modifier = Modifier.testTag(Tags.SETTINGS_BUTTON),
            ) { Text("설정") }
        }
    }
}

class SettingsScreen : SettingsViewContract, ComposeRenderable {
    override var listener: SettingsPresentableListener? = null
    private var state by mutableStateOf(SettingsViewState(userName = ""))

    override fun render(state: SettingsViewState) {
        this.state = state
    }

    @Composable
    override fun Content() {
        ScreenColumn {
            Text("설정", style = MaterialTheme.typography.headlineLarge)
            Text("${state.userName}(으)로 로그인됨", modifier = Modifier.testTag(Tags.SETTINGS_USER_LABEL))
            Button(onClick = { listener?.didTapLogout() }, modifier = Modifier.testTag(Tags.LOGOUT_BUTTON)) {
                Text("로그아웃")
            }
            TextButton(onClick = { listener?.didTapClose() }, modifier = Modifier.testTag(Tags.CLOSE_BUTTON)) {
                Text("닫기")
            }
        }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("이름") },
        singleLine = true,
        modifier = Modifier.testTag(Tags.NAME_FIELD),
    )
}

@Composable
private fun ErrorText(message: String?) {
    message?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(Tags.ERROR_LABEL))
    }
}

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
