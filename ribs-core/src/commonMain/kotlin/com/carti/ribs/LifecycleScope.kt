package com.carti.ribs

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** 생명주기가 시작되기 전에 스코프에 접근했다. AutoDispose `LifecycleNotStartedException`에 대응한다. */
public class LifecycleNotStartedException(message: String) : IllegalStateException(message)

/** 생명주기가 끝난 뒤 스코프에 접근했다. AutoDispose `LifecycleEndedException`에 대응한다. */
public class LifecycleEndedException(message: String) : IllegalStateException(message)

/**
 * 활성 구간마다 새 [CoroutineScope]를 만들고 구간이 끝나면 취소한다. Interactor와 Presenter가 함께 쓴다.
 * 스코프 구성은 Uber `rib-coroutines`와 같다: SupervisorJob + Main.immediate + CoroutineName + 예외 핸들러.
 */
internal class LifecycleScope(private val ownerName: String) {
    private var current: CoroutineScope? = null
    private var hasEnded = false

    val isActive: Boolean get() = current != null

    val scope: CoroutineScope
        get() {
            current?.let { return it }
            if (!hasEnded) throw LifecycleNotStartedException("$ownerName is not active yet")
            if (RibCoroutinesConfig.shouldCoroutineScopeFailSilentlyOnLifecycleEnded) {
                return CoroutineScope(SupervisorJob() + CoroutineName(ownerName)).also { it.cancel() }
            }
            throw LifecycleEndedException("$ownerName is no longer active")
        }

    fun start() {
        check(current == null) { "$ownerName is already active" }
        current = CoroutineScope(
            SupervisorJob() +
                RibDispatchers.MainImmediate +
                CoroutineName(ownerName) +
                (RibCoroutinesConfig.exceptionHandler ?: EmptyCoroutineContext as CoroutineContext),
        )
    }

    /** 스코프를 떼어 내고 취소한다. 호출 측은 [end] 전에 정리 콜백을 부른다. */
    fun end() {
        val scope = checkNotNull(current) { "$ownerName is not active" }
        current = null
        hasEnded = true
        scope.cancel()
    }
}
