package com.carti.ribs.samples.multi.android

import android.content.Intent
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 기기 안 테스트 서버로 실제 HTTP 요청을 보낸다(Ktor OkHttp 엔진).
 * 첫 요청은 503이라 오류와 다시 시도가 보이고, 다시 시도하면 서버의 목록을 그린다.
 */
@RunWith(AndroidJUnit4::class)
class RemoteTodoFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val server = ServerSocket(0)
    private val requests = AtomicInteger()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        thread(isDaemon = true) { serve() }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TODO_BASE_URL, "http://127.0.0.1:${server.localPort}/api")
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        scenario?.close()
        server.close()
    }

    @Test
    fun serverFailureShowsRetryThenServerListAppears() {
        compose.onNodeWithTag(Tags.NAME_FIELD).performTextInput("carti")
        compose.onNodeWithTag(Tags.LOGIN_BUTTON).performClick()

        waitForTag(Tags.RETRY_BUTTON)
        compose.onNodeWithTag(Tags.ERROR_LABEL).assertTextEquals("할 일 목록을 불러오지 못했습니다")
        compose.onNodeWithTag(Tags.RETRY_BUTTON).performClick()

        waitForTag(Tags.todoRow(7))
        compose.onNodeWithTag(Tags.todoRow(7)).assertExists()
        assertEquals(2, requests.get())
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    /** 요청 줄과 헤더를 읽고 버린 뒤 첫 요청은 503, 그다음부터는 목록 JSON을 돌려준다. */
    private fun serve() {
        while (!server.isClosed) {
            val socket = runCatching { server.accept() }.getOrNull() ?: return
            socket.use {
                val reader = it.getInputStream().bufferedReader()
                while (reader.readLine().orEmpty().isNotEmpty()) Unit
                val first = requests.incrementAndGet() == 1
                val status = if (first) "503 Service Unavailable" else "200 OK"
                val body = (if (first) "" else """[{"id":7,"title":"서버에서 온 할 일","done":false}]""").encodeToByteArray()
                val head = "HTTP/1.1 $status\r\nContent-Type: application/json; charset=utf-8\r\n" +
                    "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                it.getOutputStream().apply {
                    write(head.encodeToByteArray())
                    write(body)
                    flush()
                }
            }
        }
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
