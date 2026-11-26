package com.carti.ribs.samples.multi.todo.data

import com.carti.ribs.samples.multi.todo.TodoItem
import com.carti.ribs.samples.multi.todo.TodoRepository
import com.carti.ribs.samples.multi.todo.TodoRepositoryException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `GET {baseUrl}/todos`로 목록을 가져와 [TodoItem]으로 바꾼다.
 * 응답을 못 받음, 2xx가 아닌 상태, 읽을 수 없는 본문은 모두 [TodoRepositoryException.Unavailable]이 된다.
 * 취소(`CancellationException`)는 잡지 않는다.
 */
class RemoteTodoRepository(
    private val transport: HttpTransport,
    private val baseUrl: String,
) : TodoRepository {
    override suspend fun fetchTodos(): List<TodoItem> {
        val response = try {
            transport.send(HttpRequest("$baseUrl/todos"))
        } catch (e: HttpTransportException) {
            throw TodoRepositoryException.Unavailable("할 일 목록 요청에 응답이 없다", e)
        }
        if (response.statusCode !in SUCCESS) {
            throw TodoRepositoryException.Unavailable("할 일 목록 요청이 HTTP ${response.statusCode}로 실패했다")
        }
        val dtos = try {
            json.decodeFromString<List<TodoDto>>(response.body)
        } catch (e: IllegalArgumentException) {
            // SerializationException도 IllegalArgumentException이다.
            throw TodoRepositoryException.Unavailable("할 일 목록 응답을 읽을 수 없다", e)
        }
        return dtos.map { TodoItem(id = it.id, title = it.title, isDone = it.done) }
    }

    private companion object {
        val SUCCESS = 200..299
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** 서버 응답의 항목 모양. 피처의 [TodoItem]과 따로 둬 서버 필드 이름이 피처로 새지 않게 한다. */
@Serializable
internal class TodoDto(val id: Int, val title: String, val done: Boolean)
