package com.carti.ribs.samples.multi.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carti.ribs.samples.multi.todo.data.BearerTokenTransport
import com.carti.ribs.samples.multi.todo.data.HttpRequest
import com.carti.ribs.samples.multi.todo.data.KtorHttpTransport
import com.carti.ribs.samples.multi.todo.data.RequestMethod
import com.carti.ribs.samples.multi.todo.data.TodoApiClient
import com.carti.ribs.samples.multi.todo.data.TodoApiException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.util.Collections
import kotlin.concurrent.thread

/**
 * 기기에서 Ktor OkHttp 엔진으로 기기 안 테스트 서버에 쓰기 요청을 보낸다(ADR-0014).
 * 메서드·헤더·UTF-8 본문이 그대로 나가고, 401 Problem Details가 공유 코드에서 분류되는지 본다.
 */
@RunWith(AndroidJUnit4::class)
class KtorHttpTransportDeviceTest {
    private val server = ServerSocket(0)
    private val baseUrl = "http://127.0.0.1:${server.localPort}"
    private val seen = Collections.synchronizedList(mutableListOf<SeenRequest>())

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun postSendsBearerTokenAndUtf8JsonBody() = runBlocking {
        serve(201, """{"id":3,"title":"한글","done":false}""")
        val client = TodoApiClient(BearerTokenTransport(KtorHttpTransport()) { "device-token" }, baseUrl)

        val created = client.createTodo("한글")

        assertEquals("한글", created.title)
        val request = seen.single()
        assertEquals("POST /todos", request.line)
        assertEquals("Bearer device-token", request.headers["authorization"])
        assertTrue(request.headers["content-type"].orEmpty().startsWith("application/json"))
        assertEquals("""{"title":"한글"}""", request.body)
    }

    @Test
    fun patchSendsBodyAndDeleteSendsNone() = runBlocking {
        serve(204, "")
        val transport = KtorHttpTransport()

        transport.send(
            HttpRequest("$baseUrl/todos/1", RequestMethod.PATCH, mapOf("Content-Type" to "application/json"), """{"done":true}"""),
        )
        transport.send(HttpRequest("$baseUrl/todos/1", RequestMethod.DELETE))

        assertEquals(listOf("PATCH /todos/1", "DELETE /todos/1"), seen.map { it.line })
        assertEquals("""{"done":true}""", seen[0].body)
        assertEquals("", seen[1].body)
        assertNull(seen[1].headers["content-type"])
    }

    @Test
    fun unauthorizedProblemDetailsIsClassified() = runBlocking {
        serve(401, """{"title":"토큰 만료","status":401}""", contentType = "application/problem+json")
        val client = TodoApiClient(KtorHttpTransport(), baseUrl)

        val error = runCatching { client.fetchTodos() }.exceptionOrNull()

        assertTrue("got $error", error is TodoApiException.Unauthorized)
        assertEquals("토큰 만료", (error as TodoApiException.Unauthorized).problem?.title)
    }

    private class SeenRequest(val line: String, val headers: Map<String, String>, val body: String)

    /** 요청마다 요청 줄·헤더·본문을 기록하고 [status]·[body]로 답한다. */
    private fun serve(status: Int, body: String, contentType: String = "application/json") {
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: return@thread
                socket.use {
                    val input = it.getInputStream()
                    val line = readLine(input).substringBeforeLast(' ')
                    val headers = generateSequence { readLine(input).takeIf(String::isNotEmpty) }
                        .associate { header -> header.substringBefore(':').lowercase() to header.substringAfter(':').trim() }
                    val length = headers["content-length"]?.toInt() ?: 0
                    val received = ByteArray(length).also { bytes -> var read = 0; while (read < length) read += input.read(bytes, read, length - read) }
                    seen += SeenRequest(line, headers, received.decodeToString())
                    val bytes = body.encodeToByteArray()
                    val head = "HTTP/1.1 $status X\r\nContent-Type: $contentType; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                    it.getOutputStream().apply {
                        write(head.encodeToByteArray())
                        write(bytes)
                        flush()
                    }
                }
            }
        }
    }

    /** CRLF로 끝나는 한 줄을 바이트로 읽는다. 본문을 읽기 전에 버퍼링 리더가 앞서 읽지 않게 한다. */
    private fun readLine(input: java.io.InputStream): String {
        val bytes = java.io.ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1 || b == '\n'.code) break
            if (b != '\r'.code) bytes.write(b)
        }
        return bytes.toString(Charsets.UTF_8.name())
    }
}
