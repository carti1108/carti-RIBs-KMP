package com.carti.ribs

/**
 * Android `Bundle`을 대신하는 저장 상태. 읽기 전용 값 묶음이다.
 *
 * 직렬화 방식과 플랫폼 상태 복원 연결은 아직 정하지 않았다(docs/architecture.md "원본과 다른 점").
 */
public class RibState(private val values: Map<String, Any?> = emptyMap()) {
    public operator fun get(key: String): Any? = values[key]
    public fun contains(key: String): Boolean = key in values
    public val keys: Set<String> get() = values.keys
}
