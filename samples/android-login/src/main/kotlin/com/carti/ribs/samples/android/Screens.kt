package com.carti.ribs.samples.android

import com.carti.ribs.compose.ComposeRenderable
import com.carti.ribs.compose.ComposeScreenSlot
import com.carti.ribs.compose.RibSlot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.carti.ribs.samples.login.LoggedInPresentable
import com.carti.ribs.samples.login.LoggedInPresentableListener
import com.carti.ribs.samples.login.LoggedOutPresentable
import com.carti.ribs.samples.login.LoggedOutPresentableListener
import com.carti.ribs.samples.login.LoginViewFactory
import com.carti.ribs.samples.login.RootViewControllable

/** 테스트와 화면이 같이 쓰는 태그. iOS 샘플의 accessibilityIdentifier와 이름을 맞췄다. */
object Tags {
    const val NAME_FIELD = "nameField"
    const val LOGIN_BUTTON = "loginButton"
    const val ERROR_LABEL = "errorLabel"
    const val LOADING_INDICATOR = "loadingIndicator"
    const val WELCOME_LABEL = "welcomeLabel"
    const val LOGOUT_BUTTON = "logoutButton"
}

/** 공유 코드가 RIB마다 요청하는 뷰를 Compose 구현으로 만든다. */
class ComposeViewFactory : LoginViewFactory {
    override fun makeRootView(): RootViewControllable = RootScreen()
    override fun makeLoggedOutView(): LoggedOutPresentable = LoggedOutScreen()
    override fun makeLoggedInView(): LoggedInPresentable = LoggedInScreen()
}

/** 루트 RIB의 뷰. 자식 RIB 화면 하나를 보여 준다. */
class RootScreen : RootViewControllable, ComposeRenderable {
    private val slot = ComposeScreenSlot()

    override fun showScreen(screen: ViewControllable) = slot.show(screen)

    override fun hideScreen(screen: ViewControllable) = slot.hide(screen)

    @Composable
    override fun Content() = RibSlot(slot)
}

/**
 * 로그아웃 상태 RIB의 뷰. 상태는 공유 코드가 `showError`·`showLoading`으로 넘기고, 입력은 `listener`로 돌려준다.
 * GC 환경이라 iOS처럼 listener를 약한 참조로 둘 필요가 없다.
 */
class LoggedOutScreen : LoggedOutPresentable, ComposeRenderable {
    override var listener: LoggedOutPresentableListener? = null
    private var errorMessage: String? by mutableStateOf(null)
    private var isLoading: Boolean by mutableStateOf(false)

    override fun showError(message: String?) {
        errorMessage = message
    }

    override fun showLoading(isLoading: Boolean) {
        this.isLoading = isLoading
    }

    @Composable
    override fun Content() {
        var name by rememberSaveable { mutableStateOf("") }
        ScreenColumn {
            Text("로그인", style = MaterialTheme.typography.headlineLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름") },
                enabled = !isLoading,
                singleLine = true,
                modifier = Modifier.testTag(Tags.NAME_FIELD),
            )
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(Tags.ERROR_LABEL))
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.testTag(Tags.LOADING_INDICATOR))
            }
            Button(
                onClick = { listener?.didTapLogin(name) },
                enabled = !isLoading,
                modifier = Modifier.testTag(Tags.LOGIN_BUTTON),
            ) { Text("로그인") }
        }
    }
}

/** 로그인 상태 RIB의 뷰. */
class LoggedInScreen : LoggedInPresentable, ComposeRenderable {
    override var listener: LoggedInPresentableListener? = null
    private var userName: String by mutableStateOf("")

    override fun showUserName(name: String) {
        userName = name
    }

    @Composable
    override fun Content() {
        ScreenColumn {
            Text(
                "${userName}님, 환영합니다",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag(Tags.WELCOME_LABEL),
            )
            OutlinedButton(
                onClick = { listener?.didTapLogout() },
                modifier = Modifier.testTag(Tags.LOGOUT_BUTTON),
            ) { Text("로그아웃") }
        }
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
