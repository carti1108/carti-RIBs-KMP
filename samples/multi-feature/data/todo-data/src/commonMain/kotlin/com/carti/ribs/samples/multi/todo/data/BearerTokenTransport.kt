package com.carti.ribs.samples.multi.todo.data

/**
 * 다른 [HttpTransport]를 감싸 `Authorization: Bearer <토큰>`을 붙인다.
 * 보낼 때마다 [token]을 불러 읽는다. null이면 헤더 없이 보낸다. 플랫폼 전송은 인증을 모른다.
 */
class BearerTokenTransport(
    private val inner: HttpTransport,
    private val token: suspend () -> String?,
) : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse {
        require(request.header(HttpRequest.AUTHORIZATION) == null) { "Request already has an Authorization header" }
        val current = token() ?: return inner.send(request)
        return inner.send(request.copy(headers = request.headers + (HttpRequest.AUTHORIZATION to "Bearer $current")))
    }
}
