package com.carti.ribs.samples.multi.todo.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorHttpTransportTest {
    @Test
    fun returnsStatusAndBodyWhateverTheStatus() = runTest {
        var method: HttpMethod? = null
        var url: String? = null
        val transport = transport { request ->
            method = request.method
            url = request.url.toString()
            respond("잠시 후", HttpStatusCode.ServiceUnavailable, TEXT_UTF8)
        }

        val response = transport.send(HttpRequest("https://api.example.test/todos"))

        assertEquals(HttpResponse(503, "잠시 후"), response)
        assertEquals(HttpMethod.Get, method)
        assertEquals("https://api.example.test/todos", url)
    }

    @Test
    fun decodesBodyAsUtf8() = runTest {
        val transport = transport { respond("""[{"title":"우유"}]""", HttpStatusCode.OK, TEXT_UTF8) }

        assertEquals("""[{"title":"우유"}]""", transport.send(HttpRequest(URL)).body)
    }

    @Test
    fun engineFailureBecomesTransportException() = runTest {
        val transport = transport { throw IllegalStateException("연결 거부") }

        val error = assertFailsWith<HttpTransportException> { transport.send(HttpRequest(URL)) }

        // JVM에서는 코루틴 스택 복원이 예외를 복사하므로 같은 객체가 아니라 타입과 메시지를 본다.
        assertEquals("연결 거부", assertIs<IllegalStateException>(error.cause).message)
    }

    @Test
    fun cancellationStopsRequestAndIsNotATransportFailure() = runTest {
        val started = CompletableDeferred<Unit>()
        val engineCancelled = CompletableDeferred<Unit>()
        val transport = transport {
            started.complete(Unit)
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                engineCancelled.complete(Unit)
                throw e
            }
        }
        var thrown: Throwable? = null

        val job = launch {
            try {
                transport.send(HttpRequest(URL))
            } catch (e: Throwable) {
                thrown = e
                throw e
            }
        }
        started.await()
        job.cancelAndJoin()

        // MockEngine은 자기 디스패처에서 돌아 호출자보다 늦게 취소를 받을 수 있다. 실제 시간으로 기다린다.
        withContext(Dispatchers.Default) { withTimeout(WAIT_MS) { engineCancelled.await() } }
        assertTrue(thrown is CancellationException, "got $thrown")
        assertNull((thrown as? HttpTransportException))
    }

    private fun transport(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        KtorHttpTransport(HttpClient(MockEngine(handler)))

    private companion object {
        const val URL = "https://api.example.test/todos"
        const val WAIT_MS = 5_000L
        val TEXT_UTF8 = headersOf("Content-Type", "text/plain; charset=utf-8")
    }
}
