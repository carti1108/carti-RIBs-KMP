package com.carti.ribs.samples.login

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Todo(val id: Long, val title: String, val isDone: Boolean)

/**
 * 로그인한 동안 목록과 상세가 함께 쓰는 할 일 저장소. LoggedIn Component가 `shared`로 하나만 만든다.
 * 상태는 메인 스레드에서만 바꾼다(ADR-0005).
 */
class TodoStore(initial: List<Todo> = SAMPLE_TODOS) {
    private val state = MutableStateFlow(initial)
    val todos: StateFlow<List<Todo>> = state.asStateFlow()

    fun find(id: Long): Todo? = state.value.firstOrNull { it.id == id }

    fun toggle(id: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
    }

    val doneIds: List<Long> get() = state.value.filter { it.isDone }.map { it.id }

    fun restoreDone(ids: Collection<Long>) {
        state.value = state.value.map { it.copy(isDone = it.id in ids) }
    }

    companion object {
        val SAMPLE_TODOS: List<Todo> = listOf(
            Todo(1, "우유 사기", isDone = false),
            Todo(2, "RIB 문서 읽기", isDone = false),
            Todo(3, "운동하기", isDone = false),
        )
    }
}
