package com.carti.ribs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** Interactor의 활성 상태를 읽는 범위. RIBs-iOS `InteractorScope`에 대응한다. */
public interface InteractorScope {
    public val isActive: Boolean

    /** 활성 상태 스트림. 현재 값을 먼저 보내고 바뀔 때만 다시 보낸다(원본의 `distinctUntilChanged`). */
    public val isActiveStream: StateFlow<Boolean>
}

/** Router가 활성화·비활성화할 수 있는 Interactor. RIBs-iOS `Interactable`에 대응한다. */
public interface Interactable : InteractorScope {
    public fun activate()
    public fun deactivate()
}

/**
 * 비즈니스 로직을 담는 RIB 구성 요소. RIBs-iOS `Interactor`를 따른다.
 *
 * - [didBecomeActive]에서 시작한 작업은 [coroutineScope]에 띄운다. 원본의 `disposeOnDeactivate`에 해당하며,
 *   [willResignActive]가 끝난 뒤 취소된다.
 * - 이미 활성인데 [activate]하거나 비활성인데 [deactivate]하면 원본처럼 아무 일도 하지 않는다.
 * - 생명주기 메서드는 단일 스레드에서만 호출한다(ADR-0005).
 *
 * 원본에 없는 확장: 상태 복원([savedInstanceState], [onSaveInstanceState], ADR-0008)과 [handleBackPress].
 */
public open class Interactor : Interactable {

    private val activeState = MutableStateFlow(false)
    private val activeScope = LifecycleScope(this::class.simpleName ?: "Interactor")
    private var restoredState: RibState? = null

    final override val isActive: Boolean get() = activeState.value

    final override val isActiveStream: StateFlow<Boolean> = activeState.asStateFlow()

    /**
     * 활성 구간에 묶인 스코프. 활성 전에는 [LifecycleNotStartedException], 비활성 후에는
     * [LifecycleEndedException]을 던진다.
     */
    public val coroutineScope: CoroutineScope
        get() = activeScope.scope

    /** Router가 넘긴 복원 상태. 없으면 null. [didBecomeActive]에서 읽는다. */
    protected val savedInstanceState: RibState?
        get() = restoredState

    final override fun activate() {
        if (isActive) return
        activeScope.start()
        activeState.value = true
        didBecomeActive()
    }

    /** 활성화된 직후 호출된다. */
    protected open fun didBecomeActive() {}

    final override fun deactivate() {
        if (!isActive) return
        willResignActive()
        activeScope.end()
        activeState.value = false
    }

    /** 비활성화 직전에 호출된다. 이 시점까지 [coroutineScope]는 살아 있다. */
    protected open fun willResignActive() {}

    /** 상태 저장 요청. Router가 상태를 저장할 때 호출한다. */
    protected open fun onSaveInstanceState(outState: MutableMap<String, Any?>) {}

    /** 뒤로 가기를 처리했으면 true. 원본 iOS에는 없고 Android 어댑터가 쓴다. */
    public open fun handleBackPress(): Boolean = false

    internal fun restore(state: RibState?) {
        restoredState = state
    }

    internal fun saveInstanceState(): RibState {
        val outState = mutableMapOf<String, Any?>()
        onSaveInstanceState(outState)
        return RibState(outState.toMap())
    }
}

/** RIB 생명주기에 기대지 않는 뷰 쪽 Interactor 연결점. RIBs-iOS `PresentableInteractor`를 따른다. */
public open class PresentableInteractor<P>(
    /** 이 Interactor가 상태를 보내는 뷰. 보통 뷰가 `XxxPresentable`을 직접 구현한다. */
    public val presenter: P,
) : Interactor()

/** [scope]가 활성일 때만 값을 내보낸다. RIBs-iOS `confineTo(_:)`에 대응한다. */
public fun <T> Flow<T>.confineTo(scope: InteractorScope): Flow<T> =
    combine(scope.isActiveStream, this) { isActive, value -> isActive to value }
        .filter { (isActive, _) -> isActive }
        .map { (_, value) -> value }
