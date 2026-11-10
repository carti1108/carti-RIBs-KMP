package com.carti.ribs.samples.android

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 공유 코드(login-shared)의 RIB 트리가 Compose 화면을 실제로 전환하는지 확인한다. iOS `LoginFlowUITests`와 같은 흐름이다. */
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_LOGIN_DELAY_MILLIS, LOGIN_DELAY_MILLIS)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun close() {
        scenario.close()
    }

    @Test
    fun blankNameShowsError() {
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).performClick()

        compose.onNodeWithTag(Tags.ERROR_LABEL).assertTextEquals("이름을 입력하세요")
    }

    @Test
    fun loginThenLogoutReturnsToEmptyLoggedOutScreen() {
        compose.onNodeWithTag(Tags.NAME_FIELD).performTextInput("  carti ")
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).performClick()

        waitForTag(Tags.LOADING_INDICATOR)
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).assertIsNotEnabled()

        waitForTag(Tags.WELCOME_LABEL)
        compose.onNodeWithTag(Tags.WELCOME_LABEL).assertTextEquals("carti님, 환영합니다")

        compose.onNodeWithTag(Tags.LOGOUT_BUTTON).performClick()

        waitForTag(Tags.NAME_FIELD)
        compose.onNodeWithTag(Tags.NAME_FIELD).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag(Tags.ERROR_LABEL).assertDoesNotExist()
    }

    @Test
    fun failedLoginShowsServerErrorAndAllowsRetry() {
        compose.onNodeWithTag(Tags.NAME_FIELD).performTextInput("fail")
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).performClick()

        waitForTag(Tags.ERROR_LABEL)
        compose.onNodeWithTag(Tags.ERROR_LABEL).assertTextEquals("로그인에 실패했습니다")
        compose.onNodeWithTag(Tags.LOADING_INDICATOR).assertDoesNotExist()
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).assertIsEnabled()
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(TIMEOUT_MILLIS) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val LOGIN_DELAY_MILLIS = 1_000L
        const val TIMEOUT_MILLIS = 5_000L
    }
}
