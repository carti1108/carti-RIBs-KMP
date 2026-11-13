package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RIBs-iOS `WorkerTests`를 옮긴 테스트. */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkerTest {

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun didStart_onceOnly_boundToInteractor() {
        val interactor = Interactor()
        val worker = CountingWorker()
        assertCounts(worker, started = false, didStart = 0, didStop = 0)

        worker.start(interactor)
        assertCounts(worker, started = true, didStart = 0, didStop = 0)

        interactor.activate()
        assertCounts(worker, started = true, didStart = 1, didStop = 0)

        interactor.deactivate()
        assertCounts(worker, started = true, didStart = 1, didStop = 1)

        worker.start(interactor)
        assertCounts(worker, started = true, didStart = 1, didStop = 1)

        interactor.activate()
        assertCounts(worker, started = true, didStart = 2, didStop = 1)

        worker.stop()
        assertCounts(worker, started = false, didStart = 2, didStop = 2)

        worker.stop()
        assertCounts(worker, started = false, didStart = 2, didStop = 2)
    }

    @Test
    fun isStartedStream_followsStartAndStop() {
        val worker = CountingWorker()
        assertFalse(worker.isStartedStream.value)

        worker.start(Interactor())
        assertTrue(worker.isStartedStream.value)

        worker.stop()
        assertFalse(worker.isStartedStream.value)
    }

    @Test
    fun coroutineScope_isCancelledWhenInteractorDeactivates() {
        val interactor = Interactor().also { it.activate() }
        var cancelled = false
        val worker = object : Worker() {
            override fun didStart(interactorScope: InteractorScope) {
                coroutineScope.launch {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
            }
        }
        worker.start(interactor)

        interactor.deactivate()

        assertTrue(cancelled)
    }

    @Test
    fun stop_detachesFromInteractor() {
        val interactor = Interactor()
        val worker = CountingWorker()
        worker.start(interactor)
        worker.stop()

        interactor.activate()

        assertEquals(0, worker.didStartCount)
    }

    private fun assertCounts(worker: CountingWorker, started: Boolean, didStart: Int, didStop: Int) {
        assertEquals(started, worker.isStarted)
        assertEquals(didStart, worker.didStartCount)
        assertEquals(didStop, worker.didStopCount)
    }

    private class CountingWorker : Worker() {
        var didStartCount = 0
        var didStopCount = 0

        override fun didStart(interactorScope: InteractorScope) {
            didStartCount++
        }

        override fun didStop() {
            didStopCount++
        }
    }
}
