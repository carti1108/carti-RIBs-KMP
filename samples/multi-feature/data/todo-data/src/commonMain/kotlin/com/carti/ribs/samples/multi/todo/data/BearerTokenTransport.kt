package com.carti.ribs.samples.multi.todo.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 다른 [HttpTransport]를 감싸 `Authorization: Bearer <토큰>`을 붙인다.
 * 보낼 때마다 [token]을 불러 읽는다. null이면 헤더 없이 보낸다. 플랫폼 전송은 인증을 모른다.
 *
 * [refresh]가 있으면 토큰을 붙여 보낸 요청이 401을 받았을 때 한 번만 다시 보낸다(ADR-0015).
 * - 지금 [token]이 거절된 토큰과 다르면 갱신하지 않고 지금 토큰으로 다시 보낸다(다른 요청이 이미 갱신했다).
 * - 아니면 [refresh]를 부른다. 갱신은 한 번에 하나만 돌고, 동시에 401을 받은 요청은 그 결과를 기다린다.
 * - 갱신이 null이면 원래 401을 돌려준다. 같은 토큰으로 기다리던 요청도 다시 갱신하지 않는다.
 * - 다시 보낸 요청이 또 401이면 그대로 돌려준다.
 *
 * 토큰의 원본은 플랫폼 저장소 하나다. [refresh]는 새 토큰을 저장해 이후 [token]이 그 값을 돌려주게 해야 한다.
 */
class BearerTokenTransport(
    private val inner: HttpTransport,
    private val token: suspend () -> String?,
    private val refresh: (suspend () -> String?)?,
) : HttpTransport {
    constructor(inner: HttpTransport, token: suspend () -> String?) : this(inner, token, null)

    // 갱신을 하나로 묶는 데만 쓴다. 생명주기 코드가 아니라 여러 요청 코루틴이 함께 쓰는 데이터 계층 상태다.
    private val refreshMutex = Mutex()

    /** 갱신에 실패한 토큰. 같은 토큰으로 기다리던 요청이 다시 갱신하지 않게 한다. */
    private var refreshFailedFor: String? = null

    override suspend fun send(request: HttpRequest): HttpResponse {
        require(request.header(HttpRequest.AUTHORIZATION) == null) { "Request already has an Authorization header" }
        val sentToken = token() ?: return inner.send(request)
        val response = inner.send(request.withBearer(sentToken))
        if (response.statusCode != UNAUTHORIZED || refresh == null) return response
        val nextToken = nextTokenAfterRejecting(sentToken, refresh) ?: return response
        return inner.send(request.withBearer(nextToken))
    }

    private suspend fun nextTokenAfterRejecting(rejected: String, refresh: suspend () -> String?): String? =
        refreshMutex.withLock {
            val current = token()
            when {
                current != null && current != rejected -> current
                refreshFailedFor == rejected -> null
                else -> refresh().also { refreshFailedFor = if (it == null) rejected else null }
            }
        }

    private fun HttpRequest.withBearer(value: String): HttpRequest =
        copy(headers = headers + (HttpRequest.AUTHORIZATION to "Bearer $value"))

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
