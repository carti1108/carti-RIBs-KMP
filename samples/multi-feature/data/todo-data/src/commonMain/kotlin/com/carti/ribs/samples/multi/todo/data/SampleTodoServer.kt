package com.carti.ribs.samples.multi.todo.data

/**
 * 샘플 앱용 가짜 서버. 네트워크 없이 [HttpTransport]를 구현해 JSON 본문을 돌려준다.
 * 실제 플랫폼 전송 구현이 생기면 그것으로 바꿔 끼운다.
 */
class SampleTodoServer : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse =
        if (request.url == "$BASE_URL/todos") HttpResponse(200, TODOS_JSON) else HttpResponse(404, "")

    companion object {
        const val BASE_URL: String = "https://sample.invalid"

        private const val TODOS_JSON = """[
            {"id":1,"title":"우유 사기","done":false},
            {"id":2,"title":"운동하기","done":false},
            {"id":3,"title":"책 읽기","done":false}
        ]"""
    }
}
