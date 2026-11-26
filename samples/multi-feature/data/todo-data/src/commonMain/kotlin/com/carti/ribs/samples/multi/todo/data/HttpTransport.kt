package com.carti.ribs.samples.multi.todo.data

/** 보낼 수 있는 메서드. 이 데이터 계층이 쓰는 것만 둔다. */
enum class RequestMethod { GET, POST, PATCH, DELETE }

/**
 * 보낼 요청. 본문은 UTF-8 텍스트(JSON)만 싣는다.
 * 본문이 있으면 `Content-Type` 헤더로 형식을 알린다. 없으면 전송이 `text/plain; charset=utf-8`로 보낸다.
 */
data class HttpRequest(
    val url: String,
    val method: RequestMethod = RequestMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
) {
    init {
        headers.forEach { (name, value) ->
            require(name.isNotBlank() && name.none { it == ':' || it.isWhitespace() }) { "Invalid header name: '$name'" }
            require(value.none { it == '\r' || it == '\n' }) { "Header value of '$name' contains a line break" }
        }
        require(body == null || method != RequestMethod.GET) { "GET request cannot have a body" }
        require(body != null || header(CONTENT_TYPE) == null) { "Content-Type without a body" }
    }

    /** 이름의 대소문자를 가리지 않고 헤더 값을 찾는다. */
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    companion object {
        const val CONTENT_TYPE: String = "Content-Type"
        const val AUTHORIZATION: String = "Authorization"
    }
}

/** 받은 응답. 상태 코드가 무엇이든 응답을 받았으면 이것으로 돌려준다. 본문은 UTF-8 문자열이다. */
data class HttpResponse(val statusCode: Int, val body: String)

/** 응답을 받지 못했다(연결 실패, 시간 초과, 브라우저의 CORS 거부 등). 취소는 이것이 아니라 `CancellationException`이다. */
class HttpTransportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 요청을 보내고 응답을 받는다. 메서드·헤더·본문을 받은 그대로 보낸다.
 * 상태 코드 해석, 본문 해석, 인증 헤더는 호출하는 쪽이 한다.
 */
interface HttpTransport {
    /** 응답을 받지 못하면 [HttpTransportException]을 던진다. 호출한 코루틴이 취소되면 요청도 멈춘다. */
    suspend fun send(request: HttpRequest): HttpResponse
}
