package com.carti.ribs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Interactor 생명주기 이벤트. Uber `InteractorEvent`와 같다. */
public enum class InteractorEvent { ACTIVE, INACTIVE }

/** 제네릭 없이 Interactor를 가리키는 타입. Uber `InteractorType`에 대응한다. */
public interface InteractorType {
    public fun isAttached(): Boolean
    public fun handleBackPress(): Boolean
}

/**
 * 비즈니스 로직을 담는 RIB 구성 요소. Uber RIBs Android `Interactor<P, R>`를 따른다.
 *
 * - [didBecomeActive]에서 시작한 작업은 [coroutineScope]에 띄운다. 이 스코프는 [willResignActive]가 끝난 뒤 취소된다.
 * - 생명주기 메서드는 단일 스레드에서만 호출한다(ADR-0005).
 */
public abstract class Interactor<P, R : Router<*>>(public val presenter: P) : InteractorType {

    private val lifecycleEvents = MutableSharedFlow<InteractorEvent>(replay = 1, extraBufferCapacity = 1)

    /** 생명주기 이벤트 스트림. 마지막 이벤트를 새 구독자에게 다시 보낸다(원본의 BehaviorRelay). */
    public val lifecycle: SharedFlow<InteractorEvent> = lifecycleEvents.asSharedFlow()

    private var activeScope: CoroutineScope? = null
    private var attachedRouter: R? = null

    /** 이 Interactor의 Router. Router가 생성될 때 연결된다. */
    public val router: R
        get() = checkNotNull(attachedRouter) { "Router is not set for ${this::class.simpleName}" }

    /** 활성 구간에 묶인 스코프. Uber `rib-coroutines`의 `Interactor.coroutineScope`에 대응한다. */
    public val coroutineScope: CoroutineScope
        get() = checkNotNull(activeScope) { "${this::class.simpleName} is not attached" }

    final override fun isAttached(): Boolean = activeScope != null

    /** 활성화될 때 호출된다. */
    protected open fun didBecomeActive(savedInstanceState: RibState?) {}

    /** 비활성화 직전에 호출된다. 이 시점까지 [coroutineScope]는 살아 있다. */
    protected open fun willResignActive() {}

    /** 상태 저장 요청. Router가 상태를 저장할 때 호출한다. */
    protected open fun onSaveInstanceState(outState: MutableMap<String, Any?>) {}

    /** 뒤로 가기를 처리했으면 true. */
    override fun handleBackPress(): Boolean = false

    /**
     * Router 생성 시 Router가 자신을 연결한다. 제네릭은 런타임에 지워지므로 타입 일치는
     * `Router<I>`의 선언(`I`가 이 Interactor 타입)으로 보장된다.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun setRouterInternal(router: Router<*>) {
        check(attachedRouter == null) { "Router is already set for ${this::class.simpleName}" }
        attachedRouter = router as R
    }

    internal fun dispatchAttach(savedInstanceState: RibState?) {
        check(!isAttached()) { "${this::class.simpleName} is already attached" }
        activeScope = CoroutineScope(SupervisorJob() + RibDispatchers.Main)
        lifecycleEvents.tryEmit(InteractorEvent.ACTIVE)
        didBecomeActive(savedInstanceState)
    }

    internal fun dispatchDetach(): P {
        val scope = checkNotNull(activeScope) { "${this::class.simpleName} is not attached" }
        willResignActive()
        activeScope = null
        scope.cancel()
        lifecycleEvents.tryEmit(InteractorEvent.INACTIVE)
        return presenter
    }

    internal fun saveInstanceState(): RibState {
        val outState = mutableMapOf<String, Any?>()
        onSaveInstanceState(outState)
        return RibState(outState.toMap())
    }
}
