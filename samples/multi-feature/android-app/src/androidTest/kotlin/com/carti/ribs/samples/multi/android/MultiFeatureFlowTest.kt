package com.carti.ribs.samples.multi.android

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 피처 4개를 지나는 흐름. iOS `MultiFeatureFlowUITests`, 웹 `App.test.tsx`와 같은 시나리오다. */
@RunWith(AndroidJUnit4::class)
class MultiFeatureFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun signupThenTodoThenSettingsThenLogout() {
        compose.onNodeWithTag(Tags.SIGNUP_LINK).performClick()
        compose.onNodeWithTag(Tags.NAME_FIELD).performTextInput("carti")
        compose.onNodeWithTag(Tags.SIGNUP_BUTTON).performClick()

        compose.onNodeWithTag(Tags.WELCOME_LABEL).assertTextEquals("carti님의 할 일")
        compose.onNodeWithTag(Tags.todoRow(1)).onChildAt(0).performClick()

        compose.onNodeWithTag(Tags.SETTINGS_BUTTON).performClick()
        compose.onNodeWithTag(Tags.SETTINGS_USER_LABEL).assertTextEquals("carti(으)로 로그인됨")
        compose.onNodeWithTag(Tags.CLOSE_BUTTON).performClick()

        // 설정을 닫아도 할 일 RIB은 붙어 있었으므로 체크 상태가 남는다.
        compose.onNodeWithTag(Tags.todoRow(1)).onChildAt(0).assertIsOn()

        compose.onNodeWithTag(Tags.SETTINGS_BUTTON).performClick()
        compose.onNodeWithTag(Tags.LOGOUT_BUTTON).performClick()
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).assertExists()
    }

    @Test
    fun blankLoginShowsError() {
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).performClick()

        compose.onNodeWithTag(Tags.ERROR_LABEL).assertTextEquals("이름을 입력하세요")
    }
}
