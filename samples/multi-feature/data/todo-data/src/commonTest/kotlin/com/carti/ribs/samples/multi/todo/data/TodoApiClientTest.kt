package com.carti.ribs.samples.multi.todo.data

import com.carti.ribs.samples.multi.todo.TodoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class TodoApiClientTest {
    @Test
    fun fetchTodosSendsGetAndDecodesList() = runTest {
        val transport = FakeTransport(HttpResponse(200, """[{"id":1,"title":"우유","done":true,"extra":0}]"""))

        val todos = TodoApiClient(transport, BASE_URL).fetchTodos()

        assertEquals(listOf(TodoItem(1, "우유", true)), todos)
        assertEquals(HttpRequest("$BASE_URL/todos"), transport.requests.single())
    }

    @Test
    fun createTodoPostsJsonAndDecodesCreatedItem() = runTest {
        val transport = FakeTransport(HttpResponse(201, """{"id":9,"title":"새 \"할 일\"","done":false}"""))

        val created = TodoApiClient(transport, BASE_URL).createTodo("새 \"할 일\"")

        assertEquals(TodoItem(9, "새 \"할 일\"", false), created)
        val request = transport.requests.single()
        assertEquals(RequestMethod.POST, request.method)
        assertEquals("$BASE_URL/todos", request.url)
        assertEquals(mapOf("Content-Type" to "application/json"), request.headers)
        assertEquals("""{"title":"새 \"할 일\""}""", request.body)
    }

    @Test
    fun unauthorizedCarriesProblemDetails() = runTest {
        val body = """{"type":"https://example.test/expired","title":"토큰 만료","status":401}"""
        val client = TodoApiClient(FakeTransport(HttpResponse(401, body)), BASE_URL)

        val error = assertFailsWith<TodoApiException.Unauthorized> { client.fetchTodos() }

        assertEquals(ProblemDetails("https://example.test/expired", "토큰 만료", 401, null, null), error.problem)
    }

    @Test
    fun conflictCarriesProblemDetails() = runTest {
        val client = TodoApiClient(FakeTransport(HttpResponse(409, """{"title":"이미 있음"}""")), BASE_URL)

        val error = assertFailsWith<TodoApiException.Conflict> { client.createTodo("우유") }

        assertEquals("이미 있음", error.problem?.title)
        assertEquals(ProblemDetails.ABOUT_BLANK, error.problem?.type)
    }

    @Test
    fun otherStatusIsBadStatusWithoutProblemWhenBodyIsNotJsonObject() = runTest {
        val client = TodoApiClient(FakeTransport(HttpResponse(502, "<html>Bad Gateway</html>")), BASE_URL)

        val error = assertFailsWith<TodoApiException.BadStatus> { client.fetchTodos() }

        assertEquals(502, error.status)
        assertNull(error.problem)
    }

    @Test
    fun transportFailureIsUnreachable() = runTest {
        val client = TodoApiClient(FakeTransport(failure = HttpTransportException("연결 실패")), BASE_URL)

        val error = assertFailsWith<TodoApiException.Unreachable> { client.fetchTodos() }

        assertIs<HttpTransportException>(error.cause)
    }

    @Test
    fun unreadableSuccessBodyIsInvalidResponse() = runTest {
        val client = TodoApiClient(FakeTransport(HttpResponse(201, """{"id":"x"}""")), BASE_URL)

        assertFailsWith<TodoApiException.InvalidResponse> { client.createTodo("우유") }
    }

    @Test
    fun cancellationIsNotWrapped() = runTest {
        val client = TodoApiClient(FakeTransport(failure = CancellationException("취소")), BASE_URL)

        assertFailsWith<CancellationException> { client.fetchTodos() }
    }

    private class FakeTransport(
        private val response: HttpResponse? = null,
        private val failure: Throwable? = null,
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()

        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            failure?.let { throw it }
            return checkNotNull(response) { "응답이나 실패 중 하나를 줘야 한다" }
        }
    }

    private companion object {
        const val BASE_URL = "https://api.example.test"
    }
}
