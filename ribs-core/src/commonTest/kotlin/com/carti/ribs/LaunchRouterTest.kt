package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchRouterTest {
    private val log = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun launch_attachesRootThenShowsView() {
        val root = rootRouter()

        root.launch(RecordingHost(log))

        assertEquals(listOf("root.didBecomeActive", "host.show(root)"), log)
        assertTrue(root.interactor.isAttached())
    }

    @Test
    fun launch_twice_fails() {
        val root = rootRouter()
        root.launch(RecordingHost(log))

        assertFailsWith<IllegalStateException> { root.launch(RecordingHost(log)) }
    }

    @Test
    fun shutdown_detachesWholeTreeThenHidesView() {
        val root = rootRouter()
        val child = TestViewRouter(FakeView("child"), TestInteractor("child", log))
        root.launch(RecordingHost(log))
        root.attachChild(child)
        log.clear()

        root.shutdown()

        assertEquals(
            listOf("root.willResignActive", "child.willResignActive", "host.hide(root)"),
            log,
        )
        assertFalse(child.interactor.isAttached())
    }

    @Test
    fun shutdown_beforeLaunchOrTwice_isNoOp() {
        val root = rootRouter()
        root.shutdown()
        root.launch(RecordingHost(log))
        root.shutdown()
        log.clear()

        root.shutdown()

        assertTrue(log.isEmpty())
    }

    @Test
    fun mount_embedsSubtreeIntoNonRibHost() {
        val router = TestViewRouter(FakeView("embedded"), TestInteractor("embedded", log))

        val mount = router.mount(RecordingHost(log))

        assertTrue(mount.isMounted)
        assertTrue(router.interactor.isAttached())
        assertEquals(listOf("embedded.didBecomeActive", "host.show(embedded)"), log)
    }

    @Test
    fun unmount_isIdempotent() {
        val router = TestViewRouter(FakeView("embedded"), TestInteractor("embedded", log))
        val mount = router.mount(RecordingHost(log))
        mount.unmount()
        log.clear()

        mount.unmount()

        assertFalse(mount.isMounted)
        assertTrue(log.isEmpty())
    }

    @Test
    fun mount_routerWithoutView_attachesOnly() {
        val router = PlainRouter(TestInteractor("plain", log))

        val mount = router.mount()
        mount.unmount()

        assertEquals(listOf("plain.didBecomeActive", "plain.willResignActive"), log)
    }

    @Test
    fun mount_alreadyAttachedRouter_fails() {
        val router = TestViewRouter(FakeView("x"), TestInteractor("x", log))
        router.mount(RecordingHost(log))

        assertFailsWith<IllegalStateException> { router.mount(RecordingHost(log)) }
    }

    private fun rootRouter() = TestLaunchRouter(FakeView("root"), TestInteractor("root", log))

    private class FakeView(val name: String) : ViewControllable

    private class RecordingHost(private val log: MutableList<String>) : ViewHost<FakeView> {
        override fun show(view: FakeView) {
            log += "host.show(${view.name})"
        }

        override fun hide(view: FakeView) {
            log += "host.hide(${view.name})"
        }
    }

    private class TestInteractor(
        private val name: String,
        private val log: MutableList<String>,
    ) : Interactor<Unit, Router<*>>(Unit) {
        override fun didBecomeActive(savedInstanceState: RibState?) {
            log += "$name.didBecomeActive"
        }

        override fun willResignActive() {
            log += "$name.willResignActive"
        }
    }

    private class TestLaunchRouter(view: FakeView, interactor: TestInteractor) :
        LaunchRouter<FakeView, TestInteractor>(view, interactor)

    private class TestViewRouter(view: FakeView, interactor: TestInteractor) :
        ViewRouter<FakeView, TestInteractor>(view, interactor)

    private class PlainRouter(interactor: TestInteractor) : Router<TestInteractor>(interactor)
}
