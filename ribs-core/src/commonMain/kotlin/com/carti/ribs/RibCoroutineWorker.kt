package com.carti.ribs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Interactor(또는 Presenter) 활성 구간에 묶여 돌아가는 작업 단위.
 * Uber RIBs Android `RibCoroutineWorker`를 따른다. 원본의 Rx 기반 `Worker`/`WorkerBinder`는
 * 원본에서도 deprecated라 옮기지 않았다.
 *
 * 보통 `didBecomeActive`에서 `coroutineScope.bind(worker)`로 붙인다. Interactor가 비활성화되면 함께 멈춘다.
 */
public fun interface RibCoroutineWorker {
    /** 작업을 시작한다. 오래 걸리는 일은 [scope]에 자식 코루틴으로 띄운다. */
    public suspend fun onStart(scope: CoroutineScope)

    /** 작업이 멈출 때 호출된다. [cause]는 취소 또는 [onStart]에서 난 오류다. */
    public fun onStop(cause: Throwable) {}
}

/** [bind]의 결과. [onStart]가 끝나면 완료되는 Job이며, [unbind]로 Worker를 직접 멈출 수 있다. */
public interface BindWorkerHandle : Job {
    /** Worker를 멈춘다. 반환된 Job은 Worker가 완전히 멈추면 완료된다. */
    public fun unbind(): Job
}

/**
 * 이 스코프가 살아 있는 동안 [worker]를 돌린다. 스코프가 취소되거나 [BindWorkerHandle.unbind]를 부르면
 * [RibCoroutineWorker.onStop]이 호출된다.
 *
 * [context]의 기본값은 [RibDispatchers.Default]다. 원본처럼 [RibDispatchers.Unconfined]이면 즉시 시작한다.
 */
public fun CoroutineScope.bind(
    worker: RibCoroutineWorker,
    context: CoroutineContext = RibDispatchers.Default,
): BindWorkerHandle {
    val bindJob = CompletableDeferred<Unit>()
    val start = if (context[ContinuationInterceptor] == RibDispatchers.Unconfined) {
        CoroutineStart.UNDISPATCHED
    } else {
        CoroutineStart.DEFAULT
    }
    val unbindJob = launch(context, start) {
        try {
            supervisorScope {
                worker.onStart(this)
                bindJob.complete(Unit)
                awaitCancellation()
            }
        } catch (cancellation: CancellationException) {
            bindJob.cancel(cancellation)
            worker.onStop(cancellation)
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") failure: Throwable) {
            // 실패는 핸들을 통해 알린다. 바인딩한 스코프(Interactor)까지 무너뜨리지 않는다.
            bindJob.completeExceptionally(failure)
            worker.onStop(failure)
        }
    }
    bindJob.invokeOnCompletion { cause ->
        if (cause is CancellationException) unbindJob.cancel(cause)
    }
    return BindWorkerHandleImpl(bindJob, unbindJob)
}

/** 여러 Worker를 같은 방식으로 묶는다. */
public fun CoroutineScope.bind(
    workers: Iterable<RibCoroutineWorker>,
    context: CoroutineContext = RibDispatchers.Default,
) {
    workers.forEach { bind(it, context) }
}

private class BindWorkerHandleImpl(
    private val bindJob: Job,
    private val unbindJob: Job,
) : BindWorkerHandle, Job by bindJob {
    override fun unbind(): Job {
        unbindJob.cancel(CancellationException("Worker was manually unbound."))
        return unbindJob
    }
}
