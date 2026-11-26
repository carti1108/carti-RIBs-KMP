package com.carti.ribs.samples.multi.todo.data

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 기본 생성자가 고른 실제 엔진(JVM은 OkHttp)으로 로컬 서버에 요청한다.
 * 엔진마다 동작이 달라 commonTest가 아니라 여기서 확인한다. JS 엔진은 웹 Vitest가 확인한다.
 */
class KtorHttpTransportJvmTest {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val baseUrl get() = "http://127.0.0.1:${server.address.port}"

    @AfterTest
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun sendsRealRequestAndReturnsNonSuccessResponse() = runBlocking {
        server.createContext("/todos") { exchange ->
            val body = "점검 중".encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
            exchange.sendResponseHeaders(503, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        val response = KtorHttpTransport().send(HttpRequest("$baseUrl/todos"))

        assertEquals(HttpResponse(503, "점검 중"), response)
    }

    @Test
    fun createTodoSendsBearerTokenAndUtf8JsonBody() = runBlocking {
        val seen = recordRequests(201, """{"id":5,"title":"한글 제목","done":false}""")
        val client = TodoApiClient(BearerTokenTransport(KtorHttpTransport()) { "token-1" }, baseUrl)

        val created = client.createTodo("한글 제목")

        assertEquals("한글 제목", created.title)
        val request = seen.single()
        assertEquals("POST", request.method)
        assertEquals("Bearer token-1", request.authorization)
        assertTrue(request.contentType.orEmpty().startsWith("application/json"), "got ${request.contentType}")
        assertEquals("""{"title":"한글 제목"}""", request.body)
    }

    @Test
    fun patchSendsBodyAndDeleteSendsNone() = runBlocking {
        val seen = recordRequests(204, "")
        val transport = KtorHttpTransport()

        transport.send(
            HttpRequest(
                "$baseUrl/todos/1",
                RequestMethod.PATCH,
                mapOf("Content-Type" to "application/json"),
                """{"done":true}""",
            ),
        )
        transport.send(HttpRequest("$baseUrl/todos/1", RequestMethod.DELETE))

        assertEquals(listOf("PATCH", "DELETE"), seen.map { it.method })
        assertEquals("""{"done":true}""", seen[0].body)
        assertEquals("", seen[1].body)
        assertEquals(null, seen[1].contentType)
    }

    @Test
    fun refusedConnectionIsTransportFailure() = runBlocking {
        val closedPort = ServerSocket(0).use { it.localPort }

        assertFailsWith<HttpTransportException> {
            KtorHttpTransport().send(HttpRequest("http://127.0.0.1:$closedPort/todos"))
        }
        Unit
    }

    @Test
    fun cancellingCallerClosesConnection() = runBlocking {
        val requestArrived = CountDownLatch(1)
        val connectionClosed = CountDownLatch(1)
        server.createContext("/todos") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            requestArrived.countDown()
            // 클라이언트가 연결을 끊을 때까지 조금씩 쓴다. 끊기면 쓰기가 실패한다.
            try {
                exchange.responseBody.use { out ->
                    repeat(WRITE_ATTEMPTS) {
                        out.write('['.code)
                        out.flush()
                        Thread.sleep(WRITE_INTERVAL_MS)
                    }
                }
            } catch (e: java.io.IOException) {
                connectionClosed.countDown()
            }
        }
        server.start()
        var thrown: Throwable? = null

        val job = launch(Dispatchers.Default) {
            try {
                KtorHttpTransport().send(HttpRequest("$baseUrl/todos"))
            } catch (e: Throwable) {
                thrown = e
                throw e
            }
        }
        assertTrue(requestArrived.await(WAIT_SECONDS, TimeUnit.SECONDS))
        job.cancelAndJoin()

        assertTrue(thrown is CancellationException, "got $thrown")
        assertTrue(connectionClosed.await(WAIT_SECONDS, TimeUnit.SECONDS), "서버가 연결 끊김을 보지 못했다")
    }

    private class SeenRequest(
        val method: String,
        val authorization: String?,
        val contentType: String?,
        val body: String,
    )

    /** /todos 아래로 오는 요청을 기록하고 [status]·[body]로 답한다. */
    private fun recordRequests(status: Int, body: String): List<SeenRequest> {
        val seen = java.util.Collections.synchronizedList(mutableListOf<SeenRequest>())
        server.createContext("/todos") { exchange ->
            seen += SeenRequest(
                method = exchange.requestMethod,
                authorization = exchange.requestHeaders.getFirst("Authorization"),
                contentType = exchange.requestHeaders.getFirst("Content-Type"),
                body = exchange.requestBody.readBytes().decodeToString(),
            )
            val bytes = body.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) } else exchange.close()
        }
        server.start()
        return seen
    }

    private companion object {
        const val WAIT_SECONDS = 5L
        const val WRITE_ATTEMPTS = 100
        const val WRITE_INTERVAL_MS = 50L
    }
}
