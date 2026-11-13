package com.carti.ribs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** RIBs-iOS `Working`. */
public interface Working {
    public val isStarted: Boolean
    public val isStartedStream: StateFlow<Boolean>
    public fun start(interactorScope: InteractorScope)
    public fun stop()
}

/**
 * Interactor 활성 구간에 맞춰 돌아가는 작업 단위. RIBs-iOS `Worker`를 따른다.
 *
 * [start] 뒤에는 Interactor가 활성일 때만 [didStart]가, 비활성이 되거나 [stop]하면 [didStop]이 불린다.
 * [didStart]에서 시작한 작업은 [coroutineScope]에 띄운다. 원본의 `disposeOnStop`에 해당한다.
 *
 * 원본과 다른 점: Interactor 활성 스트림은 [RibDispatchers.MainImmediate]에서 구독한다. `deinit`이 없으므로
 * [stop]이 구독도 끊는다(원본은 소멸 때 끊는다). 약한 참조 대신 이 구독이 끝나면 Interactor 참조를 놓는다.
 */
public open class Worker : Working {

    private val startedState = MutableStateFlow(false)
    private val workScope = LifecycleScope(this::class.simpleName ?: "Worker")
    private var binding: Job? = null

    final override val isStarted: Boolean get() = startedState.value

    final override val isStartedStream: StateFlow<Boolean> = startedState.asStateFlow()

    /** 작업 구간에 묶인 스코프. [didStart]부터 [didStop] 직후까지 살아 있다. */
    public val coroutineScope: CoroutineScope
        get() = workScope.scope

    final override fun start(interactorScope: InteractorScope) {
        if (isStarted) return
        stop()
        startedState.value = true
        bind(interactorScope)
    }

    /** Interactor가 활성인 동안 작업을 시작할 때 호출된다. */
    protected open fun didStart(interactorScope: InteractorScope) {}

    final override fun stop() {
        binding?.cancel()
        binding = null
        if (!isStarted) return
        startedState.value = false
        executeStop()
    }

    /** 작업이 멈춘 뒤 호출된다. 이 시점에 [coroutineScope]는 이미 취소되었다. */
    protected open fun didStop() {}

    private fun bind(interactorScope: InteractorScope) {
        binding = CoroutineScope(SupervisorJob() + RibDispatchers.MainImmediate)
            .launch(start = CoroutineStart.UNDISPATCHED) {
                interactorScope.isActiveStream.collect { isInteractorActive ->
                    if (isInteractorActive) {
                        if (isStarted) executeStart(interactorScope)
                    } else {
                        executeStop()
                    }
                }
            }
    }

    private fun executeStart(interactorScope: InteractorScope) {
        if (workScope.isActive) return
        workScope.start()
        didStart(interactorScope)
    }

    private fun executeStop() {
        if (!workScope.isActive) return
        workScope.end()
        didStop()
    }
}
