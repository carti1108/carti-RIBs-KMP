package com.carti.ribs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers

/** RIB 코루틴이 쓰는 디스패처 묶음. Uber `rib-coroutines`의 `RibDispatchersProvider`에 대응한다. */
@Suppress("PropertyName")
public interface RibDispatchersProvider {
    public val Main: CoroutineDispatcher

    /** 원본 `RibDispatchers.Main.immediate`. Interactor·Presenter 스코프가 이 디스패처를 쓴다. */
    public val MainImmediate: CoroutineDispatcher
    public val Default: CoroutineDispatcher
    public val Unconfined: CoroutineDispatcher
}

/** 기본값. `Dispatchers.Main`이 없는 환경(JVM 단위 테스트 등)에서는 [RibCoroutinesConfig]로 바꿔 끼운다. */
public object DefaultRibDispatchers : RibDispatchersProvider {
    override val Main: CoroutineDispatcher get() = Dispatchers.Main

    /** JS처럼 즉시 디스패치를 지원하지 않는 플랫폼에서는 [Main]으로 대신한다. */
    override val MainImmediate: CoroutineDispatcher
        get() = try {
            Dispatchers.Main.immediate
        } catch (_: UnsupportedOperationException) {
            Dispatchers.Main
        }

    override val Default: CoroutineDispatcher get() = Dispatchers.Default
    override val Unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}

/** 현재 설정된 디스패처에 접근하는 단축 경로. */
public object RibDispatchers : RibDispatchersProvider {
    override val Main: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.Main
    override val MainImmediate: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.MainImmediate
    override val Default: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.Default
    override val Unconfined: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.Unconfined
}

/** 코루틴 설정 주입 지점. Uber `RibCoroutinesConfig`를 따른다. 앱 시작 시 또는 테스트 준비 단계에서만 바꾼다. */
public object RibCoroutinesConfig {
    public var dispatchers: RibDispatchersProvider = DefaultRibDispatchers

    /** Interactor·Presenter 스코프에서 잡히지 않은 예외를 받는 핸들러. null이면 플랫폼 기본 처리. */
    public var exceptionHandler: CoroutineExceptionHandler? = null

    /** true면 생명주기가 끝난 뒤 `coroutineScope`에 접근할 때 예외 대신 이미 취소된 스코프를 돌려준다. */
    public var shouldCoroutineScopeFailSilentlyOnLifecycleEnded: Boolean = false

    public fun reset() {
        dispatchers = DefaultRibDispatchers
        exceptionHandler = null
        shouldCoroutineScopeFailSilentlyOnLifecycleEnded = false
    }
}
