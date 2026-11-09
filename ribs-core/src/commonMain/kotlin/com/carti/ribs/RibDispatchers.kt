package com.carti.ribs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** RIB 코루틴이 쓰는 디스패처 묶음. Uber `rib-coroutines`의 `RibDispatchersProvider`에 대응한다. */
@Suppress("PropertyName")
public interface RibDispatchersProvider {
    public val Main: CoroutineDispatcher
    public val Default: CoroutineDispatcher
    public val Unconfined: CoroutineDispatcher
}

/** 기본값. `Dispatchers.Main`이 없는 환경(JVM 단위 테스트 등)에서는 [RibCoroutinesConfig]로 바꿔 끼운다. */
public object DefaultRibDispatchers : RibDispatchersProvider {
    override val Main: CoroutineDispatcher get() = Dispatchers.Main
    override val Default: CoroutineDispatcher get() = Dispatchers.Default
    override val Unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}

/** 현재 설정된 디스패처에 접근하는 단축 경로. */
public object RibDispatchers : RibDispatchersProvider {
    override val Main: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.Main
    override val Default: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.Default
    override val Unconfined: CoroutineDispatcher get() = RibCoroutinesConfig.dispatchers.Unconfined
}

/** 디스패처 주입 지점. 앱 시작 시 또는 테스트 준비 단계에서만 바꾼다. */
public object RibCoroutinesConfig {
    public var dispatchers: RibDispatchersProvider = DefaultRibDispatchers

    public fun reset() {
        dispatchers = DefaultRibDispatchers
    }
}
