package com.carti.ribs.samples.multi.todo.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BearerTokenTransportTest {
    private val inner = RecordingTransport()

    @Test
    fun addsAuthorizationHeaderAndKeepsOtherParts() = runTest {
        val request = HttpRequest(
            url = "https://api.example.test/todos",
            method = RequestMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = "{}",
        )

        BearerTokenTransport(inner) { "abc" }.send(request)

        assertEquals(
            request.copy(headers = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer abc")),
            inner.requests.single(),
        )
    }

    @Test
    fun sendsWithoutHeaderWhenThereIsNoToken() = runTest {
        BearerTokenTransport(inner) { null }.send(HttpRequest("https://api.example.test/todos"))

        assertEquals(HttpRequest("https://api.example.test/todos"), inner.requests.single())
    }

    @Test
    fun readsTokenOnEverySend() = runTest {
        var token = "first"
        val transport = BearerTokenTransport(inner) { token }

        transport.send(HttpRequest("https://api.example.test/a"))
        token = "second"
        transport.send(HttpRequest("https://api.example.test/b"))

        assertEquals(listOf("Bearer first", "Bearer second"), inner.requests.map { it.headers["Authorization"] })
    }

    @Test
    fun rejectsRequestThatAlreadyHasAuthorization() = runTest {
        val request = HttpRequest("https://api.example.test/todos", headers = mapOf("authorization" to "Basic x"))

        assertFailsWith<IllegalArgumentException> { BearerTokenTransport(inner) { "abc" }.send(request) }
    }

    private class RecordingTransport : HttpTransport {
        val requests = mutableListOf<HttpRequest>()

        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return HttpResponse(200, "")
        }
    }
}
