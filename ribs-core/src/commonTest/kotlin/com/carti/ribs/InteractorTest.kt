package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InteractorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun dispatchAttach_callsDidBecomeActiveWithSavedState() {
        val interactor = RecordingInteractor()
        val state = RibState(mapOf("key" to "value"))

        interactor.dispatchAttach(state)

        assertTrue(interactor.isAttached())
        assertEquals(listOf("didBecomeActive"), interactor.calls)
        assertSame(state, interactor.receivedState)
    }

    @Test
    fun dispatchDetach_callsWillResignActiveAndReturnsPresenter() {
        val interactor = RecordingInteractor()
        interactor.dispatchAttach(null)

        val presenter = interactor.dispatchDetach()

        assertFalse(interactor.isAttached())
        assertEquals(listOf("didBecomeActive", "willResignActive"), interactor.calls)
        assertSame(interactor.presenter, presenter)
    }

    @Test
    fun dispatchAttach_whenAlreadyAttached_fails() {
        val interactor = RecordingInteractor()
        interactor.dispatchAttach(null)

        assertFailsWith<IllegalStateException> { interactor.dispatchAttach(null) }
    }

    @Test
    fun dispatchDetach_whenNotAttached_fails() {
        val interactor = RecordingInteractor()

        assertFailsWith<IllegalStateException> { interactor.dispatchDetach() }
    }

    @Test
    fun lifecycle_emitsActiveThenInactive() = runTest(testDispatcher) {
        val interactor = RecordingInteractor()
        val events = mutableListOf<InteractorEvent>()
        backgroundScope.launch { interactor.lifecycle.collect { events += it } }

        interactor.dispatchAttach(null)
        interactor.dispatchDetach()

        assertEquals(listOf(InteractorEvent.ACTIVE, InteractorEvent.INACTIVE), events)
    }

    @Test
    fun lifecycle_replaysLatestEventToLateSubscriber() = runTest(testDispatcher) {
        val interactor = RecordingInteractor()
        interactor.dispatchAttach(null)
        val events = mutableListOf<InteractorEvent>()

        backgroundScope.launch { interactor.lifecycle.collect { events += it } }

        assertEquals(listOf(InteractorEvent.ACTIVE), events)
    }

    @Test
    fun coroutineScope_isCancelledAfterWillResignActive() {
        val interactor = RecordingInteractor()
        interactor.dispatchAttach(null)
        val job = interactor.coroutineScope.launch { awaitCancellation() }

        interactor.dispatchDetach()

        assertTrue(interactor.jobWasActiveDuringWillResign == true)
        assertTrue(job.isCancelled)
    }

    @Test
    fun coroutineScope_whenNotAttached_fails() {
        val interactor = RecordingInteractor()

        assertFailsWith<IllegalStateException> { interactor.coroutineScope }
    }

    @Test
    fun coroutineScope_isRecreatedOnReattach() {
        val interactor = RecordingInteractor()
        interactor.dispatchAttach(null)
        val first = interactor.coroutineScope
        interactor.dispatchDetach()

        interactor.dispatchAttach(null)
        val second = interactor.coroutineScope

        assertNotSame(first, second)
        assertTrue(second.coroutineContext[Job]?.isActive == true)
    }

    @Test
    fun router_whenNotSet_fails() {
        val interactor = RecordingInteractor()

        assertFailsWith<IllegalStateException> { interactor.router }
    }

    @Test
    fun handleBackPress_defaultsToFalse() {
        assertFalse(RecordingInteractor().handleBackPress())
    }

    private class FakePresenter

    private class RecordingInteractor :
        Interactor<FakePresenter, Router<RecordingInteractor>>(FakePresenter()) {
        val calls = mutableListOf<String>()
        var receivedState: RibState? = null
        var jobWasActiveDuringWillResign: Boolean? = null

        override fun didBecomeActive(savedInstanceState: RibState?) {
            calls += "didBecomeActive"
            receivedState = savedInstanceState
        }

        override fun willResignActive() {
            calls += "willResignActive"
            jobWasActiveDuringWillResign = coroutineScope.coroutineContext[Job]?.isActive
        }
    }
}
