package com.carti.ribs.samples.multi.todo.data

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException

/**
 * Ktor로 보내는 [HttpTransport](ADR-0013).
 * 기본 [HttpClient]는 타깃에 하나씩 넣은 엔진(OkHttp, Darwin, Js)을 쓴다. 테스트는 MockEngine을 넣는다.
 * 코루틴이 취소되면 Ktor가 엔진의 요청을 끊는다. 스레드 선택은 Ktor 엔진이 한다.
 */
class KtorHttpTransport(private val client: HttpClient = HttpClient()) : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse {
        try {
            val response = client.request(request.url) {
                method = request.method.toKtor()
                headers {
                    // Ktor는 Content-Type을 헤더가 아니라 본문으로만 받는다.
                    request.headers
                        .filterKeys { !it.equals(HttpRequest.CONTENT_TYPE, ignoreCase = true) }
                        .forEach { (name, value) -> append(name, value) }
                }
                request.body?.let { body ->
                    val contentType = request.header(HttpRequest.CONTENT_TYPE)?.let(ContentType::parse) ?: PLAIN_TEXT
                    // charset이 없으면 TextContent가 UTF-8로 쓴다.
                    setBody(TextContent(body, contentType))
                }
            }
            return HttpResponse(response.status.value, response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // 엔진마다 연결 실패 타입이 다르다. JVM은 IOException, JS 엔진은 Exception이 아닌 kotlin.Error("Fail to fetch")다.
            // 그래서 Throwable을 받고, 취소가 아니면 모두 응답 없음으로 본다.
            throw HttpTransportException("${request.method} ${request.url} 요청에 응답이 없다", e)
        }
    }

    private fun RequestMethod.toKtor(): HttpMethod = when (this) {
        RequestMethod.GET -> HttpMethod.Get
        RequestMethod.POST -> HttpMethod.Post
        RequestMethod.PATCH -> HttpMethod.Patch
        RequestMethod.DELETE -> HttpMethod.Delete
    }

    private companion object {
        val PLAIN_TEXT: ContentType = ContentType.Text.Plain
    }
}
