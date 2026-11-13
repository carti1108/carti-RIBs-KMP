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
class RibThreadingTest {
    private var onMainThread = true

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
        RibThreadingConfig.reset()
    }

    @Test
    fun withoutChecker_lifecycleRunsOnAnyThread() {
        onMainThread = false
        val parent = node()

        parent.mount()
        parent.attachChild(node())

        assertTrue(parent.interactor.isActive)
    }

    @Test
    fun checkerOnMainThread_allowsLifecycle() {
        installChecker()
        val parent = node()
        val child = node()

        val mount = parent.mount()
        parent.attachChild(child)
        parent.detachChild(child)
        mount.unmount()

        assertFalse(parent.interactor.isActive)
    }

    @Test
    fun checkerOffMainThread_failsMountWithoutChangingState() {
        installChecker()
        onMainThread = false
        val router = node()

        val error = assertFailsWith<IllegalStateException> { router.mount() }

        assertEquals("Router.mount must be called on the main thread", error.message)
        assertFalse(router.interactor.isActive)
    }

    @Test
    fun checkerOffMainThread_failsChildAttachAndDetach() {
        installChecker()
        val parent = node()
        val child = node()
        val mount = parent.mount()
        parent.attachChild(child)
        onMainThread = false

        assertFailsWith<IllegalStateException> { parent.attachChild(node()) }
        assertFailsWith<IllegalStateException> { parent.detachChild(child) }
        assertFailsWith<IllegalStateException> { mount.unmount() }
        assertTrue(child.interactor.isActive)
    }

    @Test
    fun checkMainThread_reportsOperationName() {
        installChecker()
        onMainThread = false

        val error = assertFailsWith<IllegalStateException> { checkMainThread("LoggedOutView.showError") }

        assertEquals("LoggedOutView.showError must be called on the main thread", error.message)
    }

    private fun installChecker() {
        RibThreadingConfig.mainThreadChecker = MainThreadChecker { onMainThread }
    }

    private fun node(): Router<PlainInteractor> = PlainRouter(PlainInteractor())

    private class PlainInteractor : Interactor()
    private class PlainRouter(interactor: PlainInteractor) : Router<PlainInteractor>(interactor)
}
