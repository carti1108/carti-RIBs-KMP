package com.carti.ribs.samples.multi.todo.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 실제 브라우저(Karma ChromeHeadless)에서 다른 출처 API로 요청한다. 서버는 `karma.config.d/cross-origin-api.js`.
 * 브라우저가 CORS와 프리플라이트를 적용하는지, 공유 코드가 결과를 분류하는지 본다.
 * Node(jsNodeTest)에는 서버와 CORS가 없어 아무것도 하지 않고 끝난다.
 */
class CrossOriginApiBrowserTest {
    private val apiUrl: String? = karmaTodoApi()

    @Test
    fun postWithBearerTokenSendsPreflightThenRequest() = withApi("post") { base, log ->
        val client = TodoApiClient(BearerTokenTransport(KtorHttpTransport()) { "good-token" }, base)

        val created = client.createTodo("한글 할 일")

        assertEquals("한글 할 일", created.title)
        val (preflight, post) = log()
        assertEquals("OPTIONS", preflight.string("method"))
        assertEquals("POST", preflight.string("requestMethod"))
        val requested = preflight.string("requestHeaders").orEmpty().split(',').map { it.trim().lowercase() }
        assertTrue("authorization" in requested && "content-type" in requested, "preflight headers: $requested")
        assertEquals("POST", post.string("method"))
        assertEquals("Bearer good-token", post.string("authorization"))
        assertTrue(post.string("contentType").orEmpty().startsWith("application/json"))
        assertEquals("""{"title":"한글 할 일"}""", post.string("body"))
    }

    @Test
    fun missingTokenIsUnauthorizedWithProblemDetails() = withApi("unauthorized") { base, _ ->
        val client = TodoApiClient(BearerTokenTransport(KtorHttpTransport()) { null }, base)

        val error = assertFailsWith<TodoApiException.Unauthorized> { client.createTodo("우유") }

        assertEquals("토큰이 필요합니다", error.problem?.title)
        assertEquals(401, error.problem?.status)
    }

    @Test
    fun conflictAndOtherStatusCarryProblemDetails() = withApi("statuses") { base, _ ->
        val client = TodoApiClient(BearerTokenTransport(KtorHttpTransport()) { "good-token" }, base)

        val conflict = assertFailsWith<TodoApiException.Conflict> { client.createTodo("중복") }
        val unavailable = assertFailsWith<TodoApiException.BadStatus> { client.fetchTodos() }

        assertEquals("이미 있는 할 일", conflict.problem?.title)
        assertEquals(503, unavailable.status)
        assertEquals("점검 중", unavailable.problem?.title)
    }

    @Test
    fun patchWithBodyAndDeleteWithoutBody() = withApi("patch-delete") { base, log ->
        val transport = KtorHttpTransport()

        val patched = transport.send(
            HttpRequest("$base/todos/1", RequestMethod.PATCH, mapOf("Content-Type" to "application/json"), """{"done":true}"""),
        )
        val deleted = transport.send(HttpRequest("$base/todos/1", RequestMethod.DELETE))

        assertEquals(200, patched.statusCode)
        assertEquals(204, deleted.statusCode)
        val sent = log().filter { it.string("method") != "OPTIONS" }
        assertEquals(listOf("PATCH", "DELETE"), sent.map { it.string("method") })
        assertEquals("""{"done":true}""", sent[0].string("body"))
        assertEquals("", sent[1].string("body"))
        assertEquals(null, sent[1].string("contentType"))
    }

    @Test
    fun corsRejectionIsUnreachableAndRequestIsNotSent() = withApi("no-cors") { base, log ->
        val client = TodoApiClient(BearerTokenTransport(KtorHttpTransport()) { "good-token" }, "$base/no-cors")

        assertFailsWith<TodoApiException.Unreachable> { client.createTodo("우유") }

        // 프리플라이트가 거절돼 POST는 서버에 닿지 않는다.
        assertEquals(listOf("OPTIONS"), log().map { it.string("method") })
    }

    /** 테스트마다 서버 경로와 기록을 나눈다. 브라우저 밖에서는 아무것도 하지 않는다. */
    private fun withApi(
        id: String,
        body: suspend (base: String, log: suspend () -> List<JsonObject>) -> Unit,
    ) = runTest {
        val api = apiUrl ?: run {
            // 브라우저인데 주소가 없으면 서버 설정이 빠진 것이다. 조용히 통과하지 않는다.
            check(!isBrowser()) { "Karma did not pass client.todoApi (see karma.config.d/cross-origin-api.js)" }
            return@runTest
        }
        val logReader = KtorHttpTransport()
        body("$api/t/$id") {
            val response = logReader.send(HttpRequest("$api/_log/$id"))
            Json.parseToJsonElement(response.body).jsonArray.map { it.jsonObject }
        }
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.takeIf { it.isString }?.content
}

/** Karma 설정의 `client.todoApi`. Karma 밖이면 null. */
private fun karmaTodoApi(): String? =
    js("(typeof __karma__ !== 'undefined' && __karma__.config && __karma__.config.todoApi) || null")

private fun isBrowser(): Boolean = js("typeof window !== 'undefined'")
