@file:OptIn(ExperimentalJsExport::class, ExperimentalObjCName::class)

package com.carti.ribs.samples.multi.todo

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
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
    // 계약마다 Obj-C 이름을 고정한다. 같은 `render(state:)`가 여러 계약에 있으면 Kotlin/Native가 Swift 인자 이름을
    // 선언 순서대로 `state_`, `state__`…로 바꿔 내보내서, RIB이 늘 때마다 Swift 쪽 이름이 밀린다.
    @ObjCName("renderTodo")
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
