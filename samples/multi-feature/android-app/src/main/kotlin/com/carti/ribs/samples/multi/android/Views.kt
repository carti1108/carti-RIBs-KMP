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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 순수 뷰(ADR-0011). 값과 콜백만 받고 피처 -api 모듈과 ribs-* 모듈을 import하지 않는다.
// 계약 구현은 Screens.kt의 어댑터가 맡는다.

/** 할 일 한 줄에 그릴 값. 피처 모듈의 항목 타입을 순수 뷰에 들이지 않으려고 둔다. */
data class TodoRow(val id: Int, val title: String, val isDone: Boolean)

@Composable
fun LoginView(errorMessage: String?, onLogin: (String) -> Unit, onSignup: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    ScreenColumn {
        Text("로그인", style = MaterialTheme.typography.headlineLarge)
        NameField(name) { name = it }
        ErrorText(errorMessage)
        Button(onClick = { onLogin(name) }, modifier = Modifier.testTag(Tags.LOGIN_BUTTON)) { Text("로그인") }
        TextButton(onClick = onSignup, modifier = Modifier.testTag(Tags.SIGNUP_LINK)) { Text("가입하기") }
    }
}

@Composable
fun SignupView(errorMessage: String?, onSignup: (String) -> Unit, onCancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    ScreenColumn {
        Text("가입", style = MaterialTheme.typography.headlineLarge)
        NameField(name) { name = it }
        ErrorText(errorMessage)
        Button(onClick = { onSignup(name) }, modifier = Modifier.testTag(Tags.SIGNUP_BUTTON)) { Text("가입") }
        TextButton(onClick = onCancel, modifier = Modifier.testTag(Tags.CANCEL_BUTTON)) { Text("취소") }
    }
}

@Composable
fun TodoView(userName: String, rows: List<TodoRow>, onToggle: (Int) -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${userName}님의 할 일",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(Tags.WELCOME_LABEL),
        )
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag(Tags.todoRow(row.id))) {
                Checkbox(checked = row.isDone, onCheckedChange = { onToggle(row.id) })
                Text(row.title)
            }
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.testTag(Tags.SETTINGS_BUTTON)) { Text("설정") }
    }
}

@Composable
fun SettingsView(userName: String, onLogout: () -> Unit, onClose: () -> Unit) {
    ScreenColumn {
        Text("설정", style = MaterialTheme.typography.headlineLarge)
        Text("${userName}(으)로 로그인됨", modifier = Modifier.testTag(Tags.SETTINGS_USER_LABEL))
        Button(onClick = onLogout, modifier = Modifier.testTag(Tags.LOGOUT_BUTTON)) { Text("로그아웃") }
        TextButton(onClick = onClose, modifier = Modifier.testTag(Tags.CLOSE_BUTTON)) { Text("닫기") }
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

@Preview(showBackground = true)
@Composable
private fun LoginViewPreview() = LoginView(errorMessage = "이름을 입력하세요", onLogin = {}, onSignup = {})

@Preview(showBackground = true)
@Composable
private fun SignupViewPreview() = SignupView(errorMessage = null, onSignup = {}, onCancel = {})

@Preview(showBackground = true)
@Composable
private fun TodoViewPreview() = TodoView(
    userName = "carti",
    rows = listOf(TodoRow(1, "장보기", isDone = false), TodoRow(2, "운동", isDone = true)),
    onToggle = {},
    onSettings = {},
)

@Preview(showBackground = true)
@Composable
private fun SettingsViewPreview() = SettingsView(userName = "carti", onLogout = {}, onClose = {})
