package com.carti.ribs.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 로그인 상태 화면의 순수 뷰(ADR-0011). 자식 RIB 화면은 [child]로 받아 머리말 아래에 그린다.
 * 슬롯 연결은 어댑터가 맡는다.
 */
@Composable
fun LoggedInView(userName: String, sessionSeconds: Int, onLogout: () -> Unit, child: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${userName}님, 환영합니다",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(Tags.WELCOME_LABEL),
        )
        Text("로그인 ${sessionSeconds}초", modifier = Modifier.testTag(Tags.SESSION_LABEL))
        OutlinedButton(onClick = onLogout, modifier = Modifier.testTag(Tags.LOGOUT_BUTTON)) { Text("로그아웃") }
        child()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoggedInViewPreview() =
    LoggedInView(userName = "carti", sessionSeconds = 42, onLogout = {}) { Text("자식 화면 자리") }
