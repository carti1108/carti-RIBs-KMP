package com.carti.ribs.samples.multi.todo.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** 401 뒤 토큰 갱신과 한 번의 재전송(ADR-0015). */
class BearerTokenRefreshTest {
    private val store = TokenStore("old")
    private val server = TokenCheckingServer(accepted = "new")

    @Test
    fun unauthorizedRefreshesOnceAndResendsWithNewToken() = runTest {
        val response = transport().send(REQUEST)

        assertEquals(200, response.statusCode)
        assertEquals(1, store.refreshCount)
        assertEquals(listOf<String?>("Bearer old", "Bearer new"), server.authorizations)
    }

    @Test
    fun resendsOnlyOnceEvenIfNewTokenIsRejected() = runTest {
        val strictServer = TokenCheckingServer(accepted = "nobody")

        val response = BearerTokenTransport(strictServer, store::current, store::refresh).send(REQUEST)

        assertEquals(401, response.statusCode)
        assertEquals(1, store.refreshCount)
        assertEquals(2, strictServer.authorizations.size)
    }

    @Test
    fun failedRefreshReturnsOriginalUnauthorized() = runTest {
        store.refreshResult = null

        val response = transport().send(REQUEST)

        assertEquals(401, response.statusCode)
        assertEquals(listOf<String?>("Bearer old"), server.authorizations)
    }

    @Test
    fun requestWithoutTokenIsNotRefreshed() = runTest {
        store.value = null

        val response = transport().send(REQUEST)

        assertEquals(401, response.statusCode)
        assertEquals(0, store.refreshCount)
    }

    @Test
    fun forbiddenIsNotRefreshed() = runTest {
        val forbidding = TokenCheckingServer(accepted = "new", rejectStatus = 403)

        val response = BearerTokenTransport(forbidding, store::current, store::refresh).send(REQUEST)

        assertEquals(403, response.statusCode)
        assertEquals(0, store.refreshCount)
    }

    @Test
    fun withoutRefreshUnauthorizedIsReturned() = runTest {
        val response = BearerTokenTransport(server, store::current).send(REQUEST)

        assertEquals(401, response.statusCode)
        assertEquals(1, server.authorizations.size)
    }

    @Test
    fun concurrentUnauthorizedResponsesShareOneRefresh() = runTest {
        val gate = CompletableDeferred<Unit>()
        store.beforeRefresh = { gate.await() }
        val transport = transport()

        val first = async { transport.send(REQUEST) }
        val second = async { transport.send(REQUEST) }
        testScheduler.runCurrent()
        gate.complete(Unit)

        assertEquals(listOf(200, 200), listOf(first.await().statusCode, second.await().statusCode))
        assertEquals(1, store.refreshCount)
    }

    @Test
    fun lateUnauthorizedForOldTokenResendsWithoutRefresh() = runTest {
        // 다른 요청이 이미 갱신해 저장소의 토큰이 바뀐 뒤에 옛 토큰의 401이 도착한 경우
        val slowServer = TokenCheckingServer(accepted = "new") { store.value = "new" }

        val response = BearerTokenTransport(slowServer, store::current, store::refresh).send(REQUEST)

        assertEquals(200, response.statusCode)
        assertEquals(0, store.refreshCount)
        assertEquals(listOf<String?>("Bearer old", "Bearer new"), slowServer.authorizations)
    }

    @Test
    fun concurrentRequestsShareFailedRefresh() = runTest {
        val gate = CompletableDeferred<Unit>()
        store.refreshResult = null
        store.beforeRefresh = { gate.await() }
        val transport = transport()

        val first = async { transport.send(REQUEST) }
        val second = async { transport.send(REQUEST) }
        testScheduler.runCurrent()
        gate.complete(Unit)

        assertEquals(listOf(401, 401), listOf(first.await().statusCode, second.await().statusCode))
        assertEquals(1, store.refreshCount)
    }

    private fun transport() = BearerTokenTransport(server, store::current, store::refresh)

    /** 플랫폼 토큰 저장소 흉내. 갱신하면 이후 [current]가 새 토큰을 돌려준다. */
    private class TokenStore(var value: String?) {
        var refreshResult: String? = "new"
        var refreshCount = 0
        var beforeRefresh: suspend () -> Unit = {}

        suspend fun current(): String? = value

        suspend fun refresh(): String? {
            refreshCount++
            beforeRefresh()
            refreshResult?.let { value = it }
            return refreshResult
        }
    }

    /** [accepted] 토큰이면 200, 아니면 [rejectStatus]. 401을 돌려주기 직전에 [onReject]를 부른다. */
    private class TokenCheckingServer(
        private val accepted: String,
        private val rejectStatus: Int = 401,
        private val onReject: () -> Unit = {},
    ) : HttpTransport {
        val authorizations = mutableListOf<String?>()

        override suspend fun send(request: HttpRequest): HttpResponse {
            val authorization = request.header(HttpRequest.AUTHORIZATION)
            authorizations += authorization
            if (authorization == "Bearer $accepted") return HttpResponse(200, "")
            onReject()
            return HttpResponse(rejectStatus, "")
        }
    }

    private companion object {
        val REQUEST = HttpRequest("https://api.example.test/todos")
    }
}
