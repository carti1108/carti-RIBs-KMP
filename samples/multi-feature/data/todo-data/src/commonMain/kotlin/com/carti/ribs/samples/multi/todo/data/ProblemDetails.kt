package com.carti.ribs.samples.multi.todo.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** RFC 9457 Problem Details의 표준 멤버. 확장 멤버는 읽지 않는다. */
data class ProblemDetails(
    val type: String,
    val title: String?,
    val status: Int?,
    val detail: String?,
    val instance: String?,
) {
    companion object {
        const val ABOUT_BLANK: String = "about:blank"

        /**
         * 오류 본문을 읽는다. JSON 객체가 아니면(빈 본문, HTML 오류 페이지 등) null이다.
         * 타입이 틀린 멤버는 그 멤버만 버린다(RFC 9457 §3.1). `type`이 없으면 [ABOUT_BLANK]다.
         */
        fun parse(body: String): ProblemDetails? {
            val json = try {
                Json.parseToJsonElement(body) as? JsonObject
            } catch (e: SerializationException) {
                null
            } ?: return null
            return ProblemDetails(
                type = json.string("type") ?: ABOUT_BLANK,
                title = json.string("title"),
                status = (json["status"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull,
                detail = json.string("detail"),
                instance = json.string("instance"),
            )
        }

        private fun JsonObject.string(name: String): String? =
            (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
