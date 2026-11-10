package com.carti.ribs

/**
 * Android `Bundle`을 대신하는 저장 상태. 읽기 전용 값 묶음이다.
 *
 * 프로세스가 다시 시작돼도 살아남도록 값은 `String`, `Long`, `Double`(유한값), `Boolean`, `null`,
 * 이 타입들의 `List`, 중첩 [RibState]만 허용한다. 플랫폼은 [encodeToString]으로 얻은 문자열 하나만
 * 저장하고, 되살릴 때 [decodeFromString]으로 읽는다(ADR-0008).
 */
public class RibState(values: Map<String, Any?> = emptyMap()) {
    private val values: Map<String, Any?> = values.toMap()

    init {
        this.values.forEach { (key, value) -> requireSupported(key, value) }
    }

    public operator fun get(key: String): Any? = values[key]
    public fun contains(key: String): Boolean = key in values
    public val keys: Set<String> get() = values.keys

    /** 플랫폼 저장소(Android `Bundle` 등)에 넣을 문자열로 바꾼다. */
    public fun encodeToString(): String = RibStateCodec.encode(values)

    override fun equals(other: Any?): Boolean = other is RibState && other.values == values
    override fun hashCode(): Int = values.hashCode()
    override fun toString(): String = "RibState(${encodeToString()})"

    public companion object {
        /** [encodeToString]이 만든 문자열을 되살린다. 형식이 맞지 않으면 [IllegalArgumentException]으로 실패한다. */
        public fun decodeFromString(encoded: String): RibState = RibState(RibStateCodec.decode(encoded))

        private fun requireSupported(key: String, value: Any?) {
            require(isSupported(value)) {
                "RibState value for '$key' has unsupported type ${value?.let { it::class.simpleName }}. " +
                    "Use String, Long, Double, Boolean, List or RibState"
            }
        }

        private fun isSupported(value: Any?): Boolean = when (value) {
            null, is String, is Long, is Boolean, is RibState -> true
            is Double -> value.isFinite()
            is List<*> -> value.all(::isSupported)
            else -> false
        }
    }
}
