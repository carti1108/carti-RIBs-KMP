package com.carti.ribs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RibStateTest {
    @Test
    fun encodeThenDecode_restoresEveryAllowedType() {
        val state = RibState(
            mapOf(
                "text" to "할 일 \"1\"\n\\경로\t끝",
                "count" to 42L,
                "ratio" to 0.5,
                "whole" to 2.0,
                "flag" to true,
                "nothing" to null,
                "list" to listOf("a", 1L, false, null, listOf(2.5)),
                "child" to RibState(mapOf("id" to 7L)),
            ),
        )

        val restored = RibState.decodeFromString(state.encodeToString())

        assertEquals(state, restored)
        assertEquals(2.0, restored["whole"])
        assertEquals(42L, restored["count"])
    }

    @Test
    fun emptyState_roundTrips() {
        assertEquals(RibState(), RibState.decodeFromString(RibState().encodeToString()))
    }

    @Test
    fun equality_comparesValues() {
        assertEquals(RibState(mapOf("a" to 1L)), RibState(mapOf("a" to 1L)))
        assertNotEquals(RibState(mapOf("a" to 1L)), RibState(mapOf("a" to 2L)))
    }

    @Test
    fun constructor_rejectsTypesThatCannotSurviveProcessDeath() {
        val error = assertFailsWith<IllegalArgumentException> { RibState(mapOf("obj" to Any())) }
        assertEquals(
            "RibState value for 'obj' has unsupported type Any. Use String, Long, Double, Boolean, List or RibState",
            error.message,
        )
        assertFailsWith<IllegalArgumentException> { RibState(mapOf("nan" to Double.NaN)) }
        assertFailsWith<IllegalArgumentException> { RibState(mapOf("nested" to listOf(Any()))) }
    }

    @Test
    fun constructor_rejectsIntWhereRuntimeCanTellItApart() {
        // JS는 Int와 Double이 같은 숫자 타입이라 구분할 수 없다(ADR-0008). JVM·Native에서만 거부를 확인한다.
        if (isJsNumberModel()) return

        assertFailsWith<IllegalArgumentException> { RibState(mapOf("id" to 1)) }
        assertFailsWith<IllegalArgumentException> { RibState(mapOf("ids" to listOf(1))) }
    }

    private fun isJsNumberModel(): Boolean = (1 as Any) is Double

    @Test
    fun decode_rejectsMalformedInput() {
        assertFailsWith<IllegalArgumentException> { RibState.decodeFromString("") }
        assertFailsWith<IllegalArgumentException> { RibState.decodeFromString("[1]") }
        assertFailsWith<IllegalArgumentException> { RibState.decodeFromString("{\"a\":") }
        assertFailsWith<IllegalArgumentException> { RibState.decodeFromString("{\"a\":1} extra") }
    }
}
