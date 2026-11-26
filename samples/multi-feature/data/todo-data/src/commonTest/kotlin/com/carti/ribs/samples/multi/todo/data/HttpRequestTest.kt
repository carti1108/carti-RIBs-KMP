package com.carti.ribs.samples.multi.todo.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpRequestTest {
    @Test
    fun defaultsToGetWithoutHeadersOrBody() {
        val request = HttpRequest("https://api.example.test/todos")

        assertEquals(RequestMethod.GET, request.method)
        assertEquals(emptyMap(), request.headers)
        assertEquals(null, request.body)
    }

    @Test
    fun rejectsLineBreakInHeaderNameOrValue() {
        assertFailsWith<IllegalArgumentException> { HttpRequest(URL, headers = mapOf("X-A\r\nX-B" to "1")) }
        assertFailsWith<IllegalArgumentException> { HttpRequest(URL, headers = mapOf("X-A" to "1\nX-B: 2")) }
    }

    @Test
    fun rejectsBlankOrColonHeaderName() {
        assertFailsWith<IllegalArgumentException> { HttpRequest(URL, headers = mapOf(" " to "1")) }
        assertFailsWith<IllegalArgumentException> { HttpRequest(URL, headers = mapOf("X:A" to "1")) }
    }

    @Test
    fun rejectsContentTypeWithoutBody() {
        assertFailsWith<IllegalArgumentException> {
            HttpRequest(URL, RequestMethod.POST, headers = mapOf("Content-Type" to "application/json"))
        }
    }

    @Test
    fun rejectsBodyOnGet() {
        assertFailsWith<IllegalArgumentException> { HttpRequest(URL, body = "{}") }
    }

    private companion object {
        const val URL = "https://api.example.test/todos"
    }
}
