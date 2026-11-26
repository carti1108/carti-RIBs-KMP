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

/**
 * [isLoading] 동안에는 [items]가 비어 있다. 불러오기에 실패하면 [errorMessage]가 있다.
 * [notice]는 복사·파일 같은 플랫폼 입력의 결과 문구다. 다음 동작으로 다시 그릴 때 지워진다.
 */
@JsExport
class TodoViewState(
    val userName: String,
    val items: Array<TodoItem>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val notice: String?,
)

/**
 * 뷰가 코어로 보내는 동작. 클립보드·파일 같은 플랫폼 입력은 뷰가 플랫폼 API로 처리하고 결과 하나만 넘긴다(ADR-0016).
 */
interface TodoPresentableListener {
    fun didToggle(id: Int)
    fun didTapSettings()
    fun didTapRetry()

    /** 뷰가 목록 제목들을 클립보드에 썼다. 권한 거절 등으로 실패하면 [succeeded]가 false다. */
    fun didCopyList(succeeded: Boolean)

    /** 파일을 받았다(선택이든 끌어다 놓기든). [text]는 뷰가 읽은 텍스트로, 뷰가 앞부분만 읽어 크기를 제한한다. */
    fun didReceiveFile(name: String, text: String)
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

    /** 인증이 거절됐다(토큰 만료 등). 다시 시도해도 같다. 피처는 부모에게 다시 로그인을 요청한다. */
    class Unauthorized(message: String, cause: Throwable? = null) : TodoRepositoryException(message, cause)
}

/** 플랫폼이 구현하는 부분. 뷰만 만든다. */
interface TodoViewFactory {
    fun makeTodoView(): TodoViewContract
}

interface TodoDependency : Dependency, TodoViewFactory {
    val todoRepository: TodoRepository
}

interface TodoListener {
    fun didRequestSettings()

    /** 저장소가 인증 거절을 알렸다. 다시 로그인해야 한다. */
    fun didRequireSignIn()
}

interface TodoBuildable {
    fun build(listener: TodoListener, userName: String): ViewableRouting
}
