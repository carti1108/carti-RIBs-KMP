package com.carti.ribs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Presenter 생명주기 이벤트. Uber `PresenterEvent`와 같다. */
public enum class PresenterEvent { LOADED, UNLOADED }

/**
 * 도메인 상태를 ViewModel로 바꾸고, 뷰 입력을 사용자 의도로 해석하는 구성 요소.
 * Uber RIBs Android `Presenter`를 따른다. Interactor가 attach/detach될 때 함께 로드/언로드된다.
 *
 * 원본과 다른 점: 중복 로드와 로드 전 언로드는 실패한다(활성 스코프가 겹치지 않게 하기 위해).
 */
public abstract class Presenter {

    private val lifecycleEvents = MutableSharedFlow<PresenterEvent>(replay = 1, extraBufferCapacity = 1)

    /** 생명주기 이벤트 스트림. 마지막 이벤트를 새 구독자에게 다시 보낸다. */
    public open val lifecycleFlow: SharedFlow<PresenterEvent> = lifecycleEvents.asSharedFlow()

    private val loadedScope = LifecycleScope(this::class.simpleName ?: "Presenter")

    protected var isLoaded: Boolean = false
        private set

    /**
     * 로드 구간에 묶인 스코프. 언로드되면 취소된다.
     * 로드 전에는 [LifecycleNotStartedException], 언로드 후에는 [LifecycleEndedException]을 던진다.
     */
    public val coroutineScope: CoroutineScope
        get() = loadedScope.scope

    /** 로드된 뒤 호출된다. */
    protected open fun didLoad() {}

    /** 언로드 직전에 호출된다. 이 시점까지 [coroutineScope]는 살아 있다. */
    protected open fun willUnload() {}

    public fun dispatchLoad() {
        check(!isLoaded) { "${this::class.simpleName} is already loaded" }
        isLoaded = true
        loadedScope.start()
        lifecycleEvents.tryEmit(PresenterEvent.LOADED)
        didLoad()
    }

    public fun dispatchUnload() {
        check(isLoaded) { "${this::class.simpleName} is not loaded" }
        isLoaded = false
        willUnload()
        loadedScope.end()
        lifecycleEvents.tryEmit(PresenterEvent.UNLOADED)
    }
}
