package com.carti.ribs

import kotlinx.coroutines.CoroutineDispatcher

/** 모든 디스패처를 하나의 테스트 디스패처로 바꾼다. */
internal class TestRibDispatchers(dispatcher: CoroutineDispatcher) : RibDispatchersProvider {
    override val Main: CoroutineDispatcher = dispatcher
    override val MainImmediate: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}
