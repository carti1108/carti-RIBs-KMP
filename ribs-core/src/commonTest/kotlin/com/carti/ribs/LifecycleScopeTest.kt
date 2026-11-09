package com.carti.ribs

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleScopeTest {
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
    fun interactorScope_usesMainImmediateDispatcher() {
        val interactor = SampleInteractor().also { it.dispatchAttach(null) }

        assertSame(testDispatcher, interactor.coroutineScope.coroutineContext[kotlin.coroutines.ContinuationInterceptor])
    }

    @Test
    fun interactorScope_isNamedAfterClass() {
        val interactor = SampleInteractor().also { it.dispatchAttach(null) }

        assertEquals("SampleInteractor", interactor.coroutineScope.coroutineContext[CoroutineName]?.name)
    }

    @Test
    fun interactorScope_usesConfiguredExceptionHandler() {
        val caught = mutableListOf<Throwable>()
        RibCoroutinesConfig.exceptionHandler = CoroutineExceptionHandler { _, e -> caught += e }
        val interactor = SampleInteractor().also { it.dispatchAttach(null) }

        interactor.coroutineScope.launch { throw IllegalArgumentException("boom") }

        assertEquals(listOf("boom"), caught.map { it.message })
        assertTrue(interactor.isAttached())
    }

    @Test
    fun interactorScope_beforeAttach_throwsLifecycleNotStarted() {
        assertFailsWith<LifecycleNotStartedException> { SampleInteractor().coroutineScope }
    }

    @Test
    fun interactorScope_afterDetach_throwsLifecycleEnded() {
        val interactor = SampleInteractor()
        interactor.dispatchAttach(null)
        interactor.dispatchDetach()

        assertFailsWith<LifecycleEndedException> { interactor.coroutineScope }
    }

    @Test
    fun interactorScope_afterDetach_withFailSilently_returnsCancelledScope() {
        RibCoroutinesConfig.shouldCoroutineScopeFailSilentlyOnLifecycleEnded = true
        val interactor = SampleInteractor()
        interactor.dispatchAttach(null)
        interactor.dispatchDetach()

        val scope = interactor.coroutineScope

        assertTrue(scope.coroutineContext[Job]?.isCancelled == true)
    }

    @Test
    fun presenterScope_afterUnload_throwsLifecycleEnded() {
        val presenter = SamplePresenter()
        presenter.dispatchLoad()
        presenter.dispatchUnload()

        val error = assertFailsWith<LifecycleEndedException> { presenter.coroutineScope }
        assertIs<IllegalStateException>(error)
    }

    private class SampleInteractor : Interactor<Unit, Router<*>>(Unit)

    private class SamplePresenter : Presenter()
}
