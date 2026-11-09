package com.carti.ribs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RibCoroutineWorkerTest {

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun bind_startsWorkerAndCompletesHandleAfterOnStartReturns() {
        val interactor = attachedInteractor()
        val worker = RecordingWorker()

        val handle = interactor.coroutineScope.bind(worker)

        assertTrue(worker.started)
        assertTrue(handle.isCompleted)
    }

    @Test
    fun interactorDetach_stopsWorkerAndCancelsItsChildren() {
        val interactor = attachedInteractor()
        val worker = RecordingWorker()
        interactor.coroutineScope.bind(worker)

        interactor.dispatchDetach()

        assertIs<CancellationException>(worker.stopCause)
        assertTrue(worker.childJob?.isCancelled == true)
    }

    @Test
    fun unbind_stopsWorkerWhileInteractorStaysActive() {
        val interactor = attachedInteractor()
        val worker = RecordingWorker()
        val handle = interactor.coroutineScope.bind(worker)

        handle.unbind()

        assertIs<CancellationException>(worker.stopCause)
        assertTrue(worker.childJob?.isCancelled == true)
        assertTrue(interactor.isAttached())
    }

    @Test
    fun onStartFailure_completesHandleExceptionallyAndCallsOnStop() {
        val interactor = attachedInteractor()
        val error = IllegalStateException("boom")
        var stopCause: Throwable? = null
        val worker = object : RibCoroutineWorker {
            override suspend fun onStart(scope: CoroutineScope) = throw error
            override fun onStop(cause: Throwable) {
                stopCause = cause
            }
        }

        val handle = interactor.coroutineScope.bind(worker)

        assertTrue(handle.isCancelled)
        // JVM 디버그 모드는 스택 추적 복원을 위해 예외를 복사하므로 인스턴스가 아니라 타입·메시지로 비교한다.
        assertIs<IllegalStateException>(stopCause)
        assertEquals(error.message, stopCause?.message)
        assertTrue(interactor.isAttached())
    }

    @Test
    fun bindMany_startsEveryWorker() {
        val interactor = attachedInteractor()
        val workers = listOf(RecordingWorker(), RecordingWorker())

        interactor.coroutineScope.bind(workers)

        assertEquals(listOf(true, true), workers.map { it.started })
    }

    @Test
    fun onStop_isNotCalledBeforeUnbind() {
        val interactor = attachedInteractor()
        val worker = RecordingWorker()

        interactor.coroutineScope.bind(worker)

        assertFalse(worker.stopCause != null)
    }

    private fun attachedInteractor() = PlainInteractor().also { it.dispatchAttach(null) }

    private class PlainInteractor : Interactor<Unit, Router<*>>(Unit)

    private class RecordingWorker : RibCoroutineWorker {
        var started = false
        var stopCause: Throwable? = null
        var childJob: Job? = null

        override suspend fun onStart(scope: CoroutineScope) {
            started = true
            childJob = scope.launch { awaitCancellation() }
        }

        override fun onStop(cause: Throwable) {
            stopCause = cause
        }
    }
}
