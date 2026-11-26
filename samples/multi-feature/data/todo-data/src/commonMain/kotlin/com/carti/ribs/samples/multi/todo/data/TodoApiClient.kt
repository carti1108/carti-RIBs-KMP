package com.carti.ribs.samples.multi.todo.data

import com.carti.ribs.samples.multi.todo.TodoItem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** [TodoApiClient]의 실패. 상태 코드별로 나누고 오류 본문의 [ProblemDetails]를 담는다. */
sealed class TodoApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** 응답을 받지 못했다. 브라우저의 CORS 거부도 여기다(페이지 코드는 원인을 볼 수 없다). */
    class Unreachable(cause: HttpTransportException) : TodoApiException("응답이 없다", cause)

    /** 401. 토큰이 없거나 만료됐다. */
    class Unauthorized(val problem: ProblemDetails?) : TodoApiException("HTTP 401 ${problem?.title.orEmpty()}")

    /** 409. 서버 상태와 충돌한다. */
    class Conflict(val problem: ProblemDetails?) : TodoApiException("HTTP 409 ${problem?.title.orEmpty()}")

    /** 그 밖의 2xx가 아닌 상태. */
    class BadStatus(val status: Int, val problem: ProblemDetails?) :
        TodoApiException("HTTP $status ${problem?.title.orEmpty()}")

    /** 2xx인데 본문을 읽을 수 없다. */
    class InvalidResponse(cause: Throwable) : TodoApiException("응답 본문을 읽을 수 없다", cause)
}

/**
 * 할 일 API. 요청을 만들고, 상태 코드를 분류하고, 본문을 [TodoItem]으로 바꾼다.
 * 취소(`CancellationException`)는 감싸지 않는다.
 */
class TodoApiClient(
    private val transport: HttpTransport,
    private val baseUrl: String,
) {
    /** `GET /todos` */
    suspend fun fetchTodos(): List<TodoItem> =
        decode<List<TodoDto>>(send(HttpRequest("$baseUrl/todos"))).map(TodoDto::toItem)

    /** `POST /todos` `{"title": ...}` → 만든 항목 */
    suspend fun createTodo(title: String): TodoItem {
        val request = HttpRequest(
            url = "$baseUrl/todos",
            method = RequestMethod.POST,
            headers = mapOf(HttpRequest.CONTENT_TYPE to JSON_CONTENT_TYPE),
            body = json.encodeToString(CreateTodoBody(title)),
        )
        return decode<TodoDto>(send(request)).toItem()
    }

    private suspend fun send(request: HttpRequest): HttpResponse {
        val response = try {
            transport.send(request)
        } catch (e: HttpTransportException) {
            throw TodoApiException.Unreachable(e)
        }
        if (response.statusCode in SUCCESS) return response
        val problem = ProblemDetails.parse(response.body)
        throw when (response.statusCode) {
            UNAUTHORIZED -> TodoApiException.Unauthorized(problem)
            CONFLICT -> TodoApiException.Conflict(problem)
            else -> TodoApiException.BadStatus(response.statusCode, problem)
        }
    }

    private inline fun <reified T> decode(response: HttpResponse): T = try {
        json.decodeFromString<T>(response.body)
    } catch (e: SerializationException) {
        throw TodoApiException.InvalidResponse(e)
    } catch (e: IllegalArgumentException) {
        throw TodoApiException.InvalidResponse(e)
    }

    private companion object {
        val SUCCESS = 200..299
        const val UNAUTHORIZED = 401
        const val CONFLICT = 409
        const val JSON_CONTENT_TYPE = "application/json"
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** 서버 응답의 항목 모양. 피처의 [TodoItem]과 따로 둬 서버 필드 이름이 피처로 새지 않게 한다. */
@Serializable
internal class TodoDto(val id: Int, val title: String, val done: Boolean) {
    fun toItem(): TodoItem = TodoItem(id = id, title = title, isDone = done)
}

@Serializable
internal class CreateTodoBody(val title: String)
