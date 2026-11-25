package com.carti.ribs.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 로그아웃 화면의 순수 뷰(ADR-0011). 값과 콜백만 받고 공유 모듈 타입을 모른다.
 * 입력 중인 이름은 화면 지역 상태라 여기서 가진다.
 */
@Composable
fun LoggedOutView(errorMessage: String?, isLoading: Boolean, onLogin: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            onClick = { onLogin(name) },
            enabled = !isLoading,
            modifier = Modifier.testTag(Tags.LOGIN_BUTTON),
        ) { Text("로그인") }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoggedOutViewPreview() = LoggedOutView(errorMessage = null, isLoading = false, onLogin = {})

@Preview(showBackground = true)
@Composable
private fun LoggedOutViewErrorPreview() =
    LoggedOutView(errorMessage = "이름을 입력하세요", isLoading = false, onLogin = {})

@Preview(showBackground = true)
@Composable
private fun LoggedOutViewLoadingPreview() = LoggedOutView(errorMessage = null, isLoading = true, onLogin = {})
