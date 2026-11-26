package com.carti.ribs.samples.multi.todo.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProblemDetailsTest {
    @Test
    fun readsStandardMembers() {
        val problem = ProblemDetails.parse(
            """{"type":"https://example.test/out-of-credit","title":"잔액 부족","status":403,
               "detail":"잔액이 30, 비용이 50","instance":"/account/12345/msgs/abc","balance":30}""",
        )

        assertEquals(
            ProblemDetails(
                type = "https://example.test/out-of-credit",
                title = "잔액 부족",
                status = 403,
                detail = "잔액이 30, 비용이 50",
                instance = "/account/12345/msgs/abc",
            ),
            problem,
        )
    }

    @Test
    fun missingTypeIsAboutBlank() {
        assertEquals(ProblemDetails.ABOUT_BLANK, ProblemDetails.parse("""{"title":"없음"}""")?.type)
    }

    @Test
    fun memberWithWrongTypeIsIgnoredAndOthersAreKept() {
        // RFC 9457 §3.1: 타입이 틀린 멤버는 무시한다.
        val problem = ProblemDetails.parse("""{"type":7,"title":["x"],"status":"409","detail":"충돌"}""")

        assertEquals(ProblemDetails(ProblemDetails.ABOUT_BLANK, null, null, "충돌", null), problem)
    }

    @Test
    fun notAJsonObjectIsNull() {
        assertNull(ProblemDetails.parse(""))
        assertNull(ProblemDetails.parse("<html>오류</html>"))
        assertNull(ProblemDetails.parse("""["배열"]"""))
    }
}
