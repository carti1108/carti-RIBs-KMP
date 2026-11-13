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
    fun launch_showsViewThenActivatesThenLoads() {
        val root = rootRouter()

        root.launch(RecordingHost(log))

        assertEquals(listOf("host.show(root)", "root.didBecomeActive", "root.didLoad"), log)
        assertTrue(root.interactor.isActive)
    }

    @Test
    fun launch_twice_fails() {
        val root = rootRouter()
        root.launch(RecordingHost(log))

        assertFailsWith<IllegalStateException> { root.launch(RecordingHost(log)) }
    }

    @Test
    fun shutdown_deactivatesWholeTreeThenHidesView() {
        val root = rootRouter()
        val child = TestViewRouter(TestInteractor("child", log), FakeView("child"))
        root.launch(RecordingHost(log))
        root.attachChild(child)
        log.clear()

        root.shutdown()

        assertEquals(
            listOf("root.willResignActive", "child.willResignActive", "host.hide(root)"),
            log,
        )
        assertFalse(child.interactor.isActive)
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
        val router = TestViewRouter(TestInteractor("embedded", log), FakeView("embedded"))

        val mount = router.mount(RecordingHost(log))

        assertTrue(mount.isMounted)
        assertTrue(router.interactor.isActive)
        assertEquals(listOf("host.show(embedded)", "embedded.didBecomeActive", "embedded.didLoad"), log)
    }

    @Test
    fun unmount_isIdempotent() {
        val router = TestViewRouter(TestInteractor("embedded", log), FakeView("embedded"))
        val mount = router.mount(RecordingHost(log))
        mount.unmount()
        log.clear()

        mount.unmount()

        assertFalse(mount.isMounted)
        assertTrue(log.isEmpty())
    }

    @Test
    fun mount_routerWithoutView_activatesOnly() {
        val router = PlainRouter(TestInteractor("plain", log))

        val mount = router.mount()
        mount.unmount()

        assertEquals(listOf("plain.didBecomeActive", "plain.didLoad", "plain.willResignActive"), log)
    }

    @Test
    fun mount_alreadyActiveRouter_fails() {
        val router = TestViewRouter(TestInteractor("x", log), FakeView("x"))
        router.mount(RecordingHost(log))

        assertFailsWith<IllegalStateException> { router.mount(RecordingHost(log)) }
    }

    private fun rootRouter() = TestLaunchRouter(TestInteractor("root", log), FakeView("root"))

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
        val name: String,
        val log: MutableList<String>,
    ) : Interactor() {
        override fun didBecomeActive() {
            log += "$name.didBecomeActive"
        }

        override fun willResignActive() {
            log += "$name.willResignActive"
        }
    }

    private class TestLaunchRouter(interactor: TestInteractor, view: FakeView) :
        LaunchRouter<TestInteractor, FakeView>(interactor, view) {
        override fun didLoad() {
            interactor.log += "${interactor.name}.didLoad"
        }
    }

    private class TestViewRouter(interactor: TestInteractor, view: FakeView) :
        ViewableRouter<TestInteractor, FakeView>(interactor, view) {
        override fun didLoad() {
            interactor.log += "${interactor.name}.didLoad"
        }
    }

    private class PlainRouter(interactor: TestInteractor) : Router<TestInteractor>(interactor) {
        override fun didLoad() {
            interactor.log += "${interactor.name}.didLoad"
        }
    }
}
