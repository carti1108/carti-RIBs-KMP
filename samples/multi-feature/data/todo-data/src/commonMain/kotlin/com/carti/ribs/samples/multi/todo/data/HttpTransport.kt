package com.carti.ribs.samples.multi.todo.data

/** 보낼 요청. 지금은 GET만 쓴다. 메서드·헤더·본문은 쓰기 요청을 다룰 때 더한다. */
data class HttpRequest(val url: String)

/** 받은 응답. 상태 코드가 무엇이든 응답을 받았으면 이것으로 돌려준다. 본문은 UTF-8 문자열이다. */
data class HttpResponse(val statusCode: Int, val body: String)

/** 응답을 받지 못했다(연결 실패, 시간 초과 등). 취소는 이것이 아니라 `CancellationException`이다. */
class HttpTransportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 요청을 보내고 응답을 받는다. 플랫폼이 구현하는 것은 이것 하나다.
 * 상태 코드 해석과 본문 해석은 호출하는 쪽이 한다.
 */
interface HttpTransport {
    /** 응답을 받지 못하면 [HttpTransportException]을 던진다. 호출한 코루틴이 취소되면 요청도 멈춘다. */
    suspend fun send(request: HttpRequest): HttpResponse
}
