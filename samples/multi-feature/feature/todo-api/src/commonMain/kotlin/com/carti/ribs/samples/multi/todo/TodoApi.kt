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

interface TodoDependency : Dependency {
    fun makeTodoView(): TodoViewContract
}

interface TodoListener {
    fun didRequestSettings()
}

interface TodoBuildable {
    fun build(listener: TodoListener, userName: String): ViewableRouting
}
