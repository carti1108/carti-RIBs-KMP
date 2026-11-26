package com.carti.ribs.samples.multi.todo.data

import com.carti.ribs.samples.multi.todo.TodoItem
import com.carti.ribs.samples.multi.todo.TodoRepositoryException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoteTodoRepositoryTest {
    @Test
    fun decodesTodosFromResponseBody() = runTest {
        val transport = FakeTransport {
            HttpResponse(200, """[{"id":1,"title":"우유 사기","done":false,"extra":"무시"},{"id":2,"title":"운동","done":true}]""")
        }

        val todos = RemoteTodoRepository(transport, BASE_URL).fetchTodos()

        assertEquals(listOf(TodoItem(1, "우유 사기", false), TodoItem(2, "운동", true)), todos)
        assertEquals(listOf(HttpRequest("$BASE_URL/todos")), transport.requests)
    }

    @Test
    fun nonSuccessStatusIsUnavailable() = runTest {
        val repository = RemoteTodoRepository(FakeTransport { HttpResponse(503, "") }, BASE_URL)

        val error = assertFailsWith<TodoRepositoryException.Unavailable> { repository.fetchTodos() }

        assertTrue("503" in error.message.orEmpty())
    }

    @Test
    fun malformedBodyIsUnavailable() = runTest {
        val repository = RemoteTodoRepository(FakeTransport { HttpResponse(200, """{"id":1}""") }, BASE_URL)

        val error = assertFailsWith<TodoRepositoryException.Unavailable> { repository.fetchTodos() }

        assertIs<IllegalArgumentException>(error.cause)
    }

    @Test
    fun transportFailureIsUnavailable() = runTest {
        val failure = HttpTransportException("연결 실패")
        val repository = RemoteTodoRepository(FakeTransport { throw failure }, BASE_URL)

        val error = assertFailsWith<TodoRepositoryException.Unavailable> { repository.fetchTodos() }

        assertEquals(failure, error.cause)
    }

    @Test
    fun cancellationIsNotConvertedToRepositoryError() = runTest {
        val repository = RemoteTodoRepository(FakeTransport { throw CancellationException("취소") }, BASE_URL)

        assertFailsWith<CancellationException> { repository.fetchTodos() }
    }

    @Test
    fun sampleServerServesTodoList() = runTest {
        val todos = RemoteTodoRepository(SampleTodoServer(), SampleTodoServer.BASE_URL).fetchTodos()

        assertEquals(listOf("우유 사기", "운동하기", "책 읽기"), todos.map { it.title })
        assertTrue(todos.none { it.isDone })
    }

    @Test
    fun sampleServerAnswersNotFoundForUnknownUrl() = runTest {
        val response = SampleTodoServer().send(HttpRequest("${SampleTodoServer.BASE_URL}/unknown"))

        assertEquals(404, response.statusCode)
    }

    private class FakeTransport(private val respond: () -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()

        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return respond()
        }
    }

    private companion object {
        const val BASE_URL = "https://api.example.test"
    }
}
