package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PresenterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val log = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun dispatchLoad_setsLoadedThenEmitsLoadedThenCallsDidLoad() = runTest(testDispatcher) {
        val presenter = RecordingPresenter(log)
        backgroundScope.launch { presenter.lifecycleFlow.collect { log += "event.$it" } }

        presenter.dispatchLoad()

        assertEquals(listOf("event.LOADED", "didLoad(loaded=true)"), log)
    }

    @Test
    fun dispatchUnload_callsWillUnloadThenEmitsUnloaded() = runTest(testDispatcher) {
        val presenter = RecordingPresenter(log)
        presenter.dispatchLoad()
        backgroundScope.launch { presenter.lifecycleFlow.collect { log += "event.$it" } }
        log.clear()

        presenter.dispatchUnload()

        assertEquals(listOf("willUnload(loaded=false)", "event.UNLOADED"), log)
    }

    @Test
    fun coroutineScope_isCancelledOnUnload() {
        val presenter = RecordingPresenter(log)
        presenter.dispatchLoad()
        val job = presenter.coroutineScope.launch { awaitCancellation() }

        presenter.dispatchUnload()

        assertTrue(job.isCancelled)
    }

    @Test
    fun coroutineScope_whenNotLoaded_fails() {
        assertFailsWith<IllegalStateException> { RecordingPresenter(log).coroutineScope }
    }

    @Test
    fun interactor_loadsPresenterBetweenActiveEventAndDidBecomeActive() = runTest(testDispatcher) {
        val presenter = RecordingPresenter(log)
        val interactor = PresentingInteractor(presenter, log)
        backgroundScope.launch { interactor.lifecycle.collect { log += "interactor.$it" } }

        interactor.dispatchAttach(null)

        assertEquals(
            listOf("interactor.ACTIVE", "didLoad(loaded=true)", "interactor.didBecomeActive"),
            log,
        )
    }

    @Test
    fun interactor_unloadsPresenterBeforeWillResignActive() = runTest(testDispatcher) {
        val presenter = RecordingPresenter(log)
        val interactor = PresentingInteractor(presenter, log)
        interactor.dispatchAttach(null)
        backgroundScope.launch { interactor.lifecycle.collect { log += "interactor.$it" } }
        log.clear()

        interactor.dispatchDetach()

        assertEquals(
            listOf("willUnload(loaded=false)", "interactor.willResignActive", "interactor.INACTIVE"),
            log,
        )
    }

    @Test
    fun viewRouter_exposesViewAndActsAsRouter() {
        val view = FakeView()
        val interactor = PresentingInteractor(RecordingPresenter(log), log)
        val router = FakeViewRouter(view, interactor)

        router.dispatchAttach(null)

        assertSame(view, router.view)
        assertSame(router, interactor.router)
        assertTrue(interactor.isAttached())
        assertFalse(view.hasLogic)
    }

    private class RecordingPresenter(private val log: MutableList<String>) : Presenter() {
        override fun didLoad() {
            log += "didLoad(loaded=$isLoaded)"
        }

        override fun willUnload() {
            log += "willUnload(loaded=$isLoaded)"
        }
    }

    private class PresentingInteractor(
        presenter: RecordingPresenter,
        private val log: MutableList<String>,
    ) : Interactor<RecordingPresenter, Router<*>>(presenter) {
        override fun didBecomeActive(savedInstanceState: RibState?) {
            log += "interactor.didBecomeActive"
        }

        override fun willResignActive() {
            log += "interactor.willResignActive"
        }
    }

    private class FakeView : ViewControllable {
        val hasLogic = false
    }

    private class FakeViewRouter(view: FakeView, interactor: PresentingInteractor) :
        ViewRouter<FakeView, PresentingInteractor>(view, interactor)
}
