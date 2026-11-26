@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.samples.multi.todo

import com.carti.ribs.Dependency
import com.carti.ribs.Presentable
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouting
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// 할 일 피처의 공개 계약. 구현은 todo-impl에 있다.
// JS로 내보내는 타입이라 Long 대신 Int, List 대신 Array를 쓴다(ADR-0009).

@JsExport
data class TodoItem(val id: Int, val title: String, val isDone: Boolean)

@JsExport
class TodoViewState(val userName: String, val items: Array<TodoItem>)

interface TodoPresentableListener {
    fun didToggle(id: Int)
    fun didTapSettings()
}

interface TodoPresentable : Presentable {
    var listener: TodoPresentableListener?
    fun render(state: TodoViewState)
}

interface TodoViewControllable : ViewControllable

interface TodoViewContract : TodoPresentable, TodoViewControllable

/**
 * 할 일 목록 저장소. 피처는 이 인터페이스만 알고 구현(todo-data)은 모른다(ADR-0012).
 * 구현은 조립 모듈(app-root)이 넣는다.
 */
interface TodoRepository {
    /**
     * 목록을 가져온다. 호출한 코루틴이 취소되면 `CancellationException`이 그대로 올라온다.
     * 그 밖의 실패는 [TodoRepositoryException]으로 던진다.
     */
    suspend fun fetchTodos(): List<TodoItem>
}

/** 피처가 다르게 다루는 실패만 나눈다. 원인은 [cause]에 남긴다. */
sealed class TodoRepositoryException(message: String, cause: Throwable?) : Exception(message, cause) {
    /** 지금은 가져올 수 없다. 다시 시도할 수 있다. */
    class Unavailable(message: String, cause: Throwable? = null) : TodoRepositoryException(message, cause)
}

interface TodoDependency : Dependency {
    fun makeTodoView(): TodoViewContract
}

interface TodoListener {
    fun didRequestSettings()
}

interface TodoBuildable {
    fun build(listener: TodoListener, userName: String): ViewableRouting
}
