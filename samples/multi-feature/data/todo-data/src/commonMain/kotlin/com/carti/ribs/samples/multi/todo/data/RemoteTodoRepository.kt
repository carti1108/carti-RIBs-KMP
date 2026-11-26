package com.carti.ribs.samples.multi.todo.data

import com.carti.ribs.samples.multi.todo.TodoItem
import com.carti.ribs.samples.multi.todo.TodoRepository
import com.carti.ribs.samples.multi.todo.TodoRepositoryException

/**
 * [TodoApiClient]로 [TodoRepository]를 구현한다.
 * API 실패는 피처가 다르게 다루는 것만 남긴다. 401은 [TodoRepositoryException.Unauthorized], 나머지는
 * [TodoRepositoryException.Unavailable]이다(ADR-0015).
 * 취소(`CancellationException`)는 잡지 않는다.
 */
class RemoteTodoRepository(private val client: TodoApiClient) : TodoRepository {
    constructor(transport: HttpTransport, baseUrl: String) : this(TodoApiClient(transport, baseUrl))

    override suspend fun fetchTodos(): List<TodoItem> = try {
        client.fetchTodos()
    } catch (e: TodoApiException.Unauthorized) {
        throw TodoRepositoryException.Unauthorized("할 일 목록 요청이 인증 거절됐다: ${e.message}", e)
    } catch (e: TodoApiException) {
        throw TodoRepositoryException.Unavailable("할 일 목록을 가져오지 못했다: ${e.message}", e)
    }
}
