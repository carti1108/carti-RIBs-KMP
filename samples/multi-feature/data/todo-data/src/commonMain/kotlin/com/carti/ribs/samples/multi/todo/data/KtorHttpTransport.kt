package com.carti.ribs.samples.multi.todo.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

/**
 * Ktor로 보내는 [HttpTransport](ADR-0013).
 * 기본 [HttpClient]는 타깃에 하나씩 넣은 엔진(OkHttp, Darwin, Js)을 쓴다. 테스트는 MockEngine을 넣는다.
 * 코루틴이 취소되면 Ktor가 엔진의 요청을 끊는다. 스레드 선택은 Ktor 엔진이 한다.
 */
class KtorHttpTransport(private val client: HttpClient = HttpClient()) : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse {
        try {
            val response = client.get(request.url)
            return HttpResponse(response.status.value, response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // 엔진마다 연결 실패 타입이 다르다. JVM은 IOException, JS 엔진은 Exception이 아닌 kotlin.Error("Fail to fetch")다.
            // 그래서 Throwable을 받고, 취소가 아니면 모두 응답 없음으로 본다.
            throw HttpTransportException("${request.url} 요청에 응답이 없다", e)
        }
    }
}
