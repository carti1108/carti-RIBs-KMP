package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RouterTest {
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
    fun constructor_linksRouterToInteractor() {
        val router = node("a")

        assertSame(router, router.interactor.router)
    }

    @Test
    fun dispatchAttach_callsDidLoadThenWillAttachThenActivatesInteractor() {
        val router = node("a")

        router.dispatchAttach(null)

        assertEquals(listOf("a.didLoad", "a.willAttach", "a.didBecomeActive"), log)
        assertTrue(router.interactor.isAttached())
    }

    @Test
    fun dispatchAttach_secondTime_doesNotCallDidLoadAgain() {
        val router = node("a")
        router.dispatchAttach(null)
        router.dispatchDetach()
        log.clear()

        router.dispatchAttach(null)

        assertEquals(listOf("a.willAttach", "a.didBecomeActive"), log)
    }

    @Test
    fun attachChild_addsChildAndActivatesIt() {
        val parent = attachedNode("p")
        val child = node("c")

        parent.attachChild(child)

        assertEquals(listOf<Router<*>>(child), parent.getChildren())
        assertTrue(child.interactor.isAttached())
    }

    @Test
    fun attachChild_sameInstanceTwice_fails() {
        val parent = attachedNode("p")
        val child = node("c")
        parent.attachChild(child)

        assertFailsWith<IllegalStateException> { parent.attachChild(child) }
    }

    @Test
    fun attachChild_defaultTagIsClassName_customTagIsKept() {
        val parent = attachedNode("p")
        val defaultTagged = node("c1")
        val customTagged = node("c2")

        parent.attachChild(defaultTagged)
        parent.attachChild(customTagged, "custom")

        assertEquals(TestRouter::class.simpleName, defaultTagged.tag)
        assertEquals("custom", customTagged.tag)
    }

    @Test
    fun detachChild_deactivatesInteractorThenCallsWillDetach() {
        val parent = attachedNode("p")
        val child = node("c")
        parent.attachChild(child)
        log.clear()

        parent.detachChild(child)

        assertEquals(listOf("c.willResignActive", "c.willDetach"), log)
        assertTrue(parent.getChildren().isEmpty())
        assertFalse(child.interactor.isAttached())
    }

    @Test
    fun detachChild_unknownChild_isNoOp() {
        val parent = attachedNode("p")
        val stranger = node("s")
        log.clear()

        parent.detachChild(stranger)

        assertTrue(log.isEmpty())
    }

    @Test
    fun dispatchDetach_detachesSelfFirstThenWholeSubtree() {
        val root = attachedNode("r")
        val child = node("c")
        val grandChild = node("g")
        root.attachChild(child)
        child.attachChild(grandChild)
        log.clear()

        root.dispatchDetach()

        assertEquals(
            listOf(
                "r.willResignActive", "r.willDetach",
                "c.willResignActive", "c.willDetach",
                "g.willResignActive", "g.willDetach",
            ),
            log,
        )
        assertTrue(root.getChildren().isEmpty())
        assertTrue(child.getChildren().isEmpty())
        assertFalse(grandChild.interactor.isAttached())
    }

    @Test
    fun savedState_isRestoredToChildByTag() {
        val parent = attachedNode("p")
        val child = node("c")
        parent.attachChild(child, "child")
        child.interactor.valueToSave = "hello"
        val saved = parent.saveInstanceStateInternal()
        parent.dispatchDetach()

        val restoredParent = node("p2")
        val restoredChild = node("c2")
        restoredParent.dispatchAttach(saved)
        restoredParent.attachChild(restoredChild, "child")

        assertEquals("hello", restoredChild.interactor.restoredValue)
    }

    @Test
    fun handleBackPress_delegatesToInteractor() {
        val router = attachedNode("a")
        router.interactor.consumesBack = true

        assertTrue(router.handleBackPress())
    }

    private fun node(name: String) = TestRouter(name, TestInteractor(name, log), log)

    private fun attachedNode(name: String) = node(name).also {
        it.dispatchAttach(null)
        log.clear()
    }

    private class TestInteractor(
        private val name: String,
        private val log: MutableList<String>,
    ) : Interactor<Unit, TestRouter>(Unit) {
        var consumesBack = false
        var valueToSave: String? = null
        var restoredValue: Any? = null

        override fun didBecomeActive(savedInstanceState: RibState?) {
            log += "$name.didBecomeActive"
            restoredValue = savedInstanceState?.get(KEY_VALUE)
        }

        override fun willResignActive() {
            log += "$name.willResignActive"
        }

        override fun onSaveInstanceState(outState: MutableMap<String, Any?>) {
            outState[KEY_VALUE] = valueToSave
        }

        override fun handleBackPress(): Boolean = consumesBack
    }

    private class TestRouter(
        private val name: String,
        interactor: TestInteractor,
        private val log: MutableList<String>,
    ) : Router<TestInteractor>(interactor) {
        override fun didLoad() {
            log += "$name.didLoad"
        }

        override fun willAttach() {
            log += "$name.willAttach"
        }

        override fun willDetach() {
            log += "$name.willDetach"
        }
    }

    private companion object {
        const val KEY_VALUE = "value"
    }
}
