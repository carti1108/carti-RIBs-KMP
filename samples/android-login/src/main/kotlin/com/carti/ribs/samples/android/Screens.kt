package com.carti.ribs.samples.android

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.carti.ribs.samples.login.LoggedInViewContract
import com.carti.ribs.samples.login.LoggedInPresentableListener
import com.carti.ribs.samples.login.LoggedOutViewContract
import com.carti.ribs.samples.login.LoggedOutPresentableListener
import com.carti.ribs.samples.login.LoginViewFactory
import com.carti.ribs.samples.login.RootViewControllable
import com.carti.ribs.samples.login.Todo
import com.carti.ribs.samples.login.TodoDetailViewContract
import com.carti.ribs.samples.login.TodoDetailPresentableListener
import com.carti.ribs.samples.login.TodoListViewContract
import com.carti.ribs.samples.login.TodoListPresentableListener

/** 테스트와 화면이 같이 쓰는 태그. iOS 샘플의 accessibilityIdentifier와 이름을 맞췄다. */
object Tags {
    const val NAME_FIELD = "nameField"
    const val LOGIN_BUTTON = "loginButton"
    const val ERROR_LABEL = "errorLabel"
    const val LOADING_INDICATOR = "loadingIndicator"
    const val WELCOME_LABEL = "welcomeLabel"
    const val LOGOUT_BUTTON = "logoutButton"
    const val SESSION_LABEL = "sessionLabel"
    const val DETAIL_TITLE = "detailTitle"
    const val TOGGLE_DONE_BUTTON = "toggleDoneButton"
    const val BACK_BUTTON = "backButton"
    fun todoRow(id: Long): String = "todoRow-$id"
}

/** 공유 코드가 RIB마다 요청하는 뷰를 Compose 구현으로 만든다. */
class ComposeViewFactory : LoginViewFactory {
    override fun makeRootView(): RootViewControllable = RootScreen()
    override fun makeLoggedOutView(): LoggedOutViewContract = LoggedOutScreen()
    override fun makeLoggedInView(): LoggedInViewContract = LoggedInScreen()
    override fun makeTodoListView(): TodoListViewContract = TodoListScreen()
    override fun makeTodoDetailView(): TodoDetailViewContract = TodoDetailScreen()
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
class LoggedOutScreen : LoggedOutViewContract, ComposeRenderable {
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

/** 로그인 상태 RIB의 뷰. 머리말과 자식 RIB 화면(목록·상세) 하나를 그린다. */
class LoggedInScreen : LoggedInViewContract, ComposeRenderable {
    override var listener: LoggedInPresentableListener? = null
    private var userName: String by mutableStateOf("")
    private var sessionSeconds: Long by mutableLongStateOf(0)
    private val slot = ComposeScreenSlot()

    override fun showUserName(name: String) {
        userName = name
    }

    override fun showSessionSeconds(seconds: Long) {
        sessionSeconds = seconds
    }

    override fun showScreen(screen: ViewControllable) = slot.show(screen)

    override fun hideScreen(screen: ViewControllable) = slot.hide(screen)

    @Composable
    override fun Content() {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${userName}님, 환영합니다",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(Tags.WELCOME_LABEL),
            )
            Text("로그인 ${sessionSeconds}초", modifier = Modifier.testTag(Tags.SESSION_LABEL))
            OutlinedButton(
                onClick = { listener?.didTapLogout() },
                modifier = Modifier.testTag(Tags.LOGOUT_BUTTON),
            ) { Text("로그아웃") }
            RibSlot(slot)
        }
    }
}

/** 할 일 목록 RIB의 뷰. */
class TodoListScreen : TodoListViewContract, ComposeRenderable {
    override var listener: TodoListPresentableListener? = null
    private var todos: List<Todo> by mutableStateOf(emptyList())

    override fun showTodos(todos: List<Todo>) {
        this.todos = todos
    }

    @Composable
    override fun Content() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            todos.forEach { todo ->
                TextButton(
                    onClick = { listener?.didTapTodo(todo.id) },
                    modifier = Modifier.testTag(Tags.todoRow(todo.id)),
                ) { Text((if (todo.isDone) "✓ " else "") + todo.title) }
            }
        }
    }
}

/** 할 일 상세 RIB의 뷰. 화면 안 뒤로 버튼과 시스템 뒤로 가기가 같은 결과(상세 닫기)를 낸다. */
class TodoDetailScreen : TodoDetailViewContract, ComposeRenderable {
    override var listener: TodoDetailPresentableListener? = null
    private var title: String by mutableStateOf("")
    private var isDone: Boolean by mutableStateOf(false)

    override fun showTodo(title: String, isDone: Boolean) {
        this.title = title
        this.isDone = isDone
    }

    @Composable
    override fun Content() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { listener?.didTapBack() }, modifier = Modifier.testTag(Tags.BACK_BUTTON)) {
                Text("← 목록")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag(Tags.DETAIL_TITLE))
            Button(
                onClick = { listener?.didTapToggleDone() },
                modifier = Modifier.testTag(Tags.TOGGLE_DONE_BUTTON),
            ) { Text(if (isDone) "완료 취소" else "완료로 표시") }
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
