package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RIBs-iOS `RouterTests`를 옮기고 순서·상태 복원 테스트를 더했다. */
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
    fun load_emitsDidLoadOnce() = runTest(UnconfinedTestDispatcher()) {
        val router = node("a")
        val events = mutableListOf<RouterLifecycle>()
        backgroundScope.launch { router.lifecycle.collect { events += it } }

        router.load()
        router.load()

        assertEquals(listOf(RouterLifecycle.DID_LOAD), events)
        assertEquals(listOf("a.didLoad"), log)
    }

    @Test
    fun attachChild_activatesChildThenLoadsIt() {
        val parent = node("p")
        val child = MockRouting()

        parent.attachChild(child)

        assertEquals(1, parent.children.size)
        assertEquals(1, child.mockInteractable.activateCount)
        assertEquals(1, child.loadCount)
    }

    @Test
    fun attachChild_ordersActivationBeforeDidLoad() {
        val parent = node("p")

        parent.attachChild(node("c"))

        assertEquals(listOf("c.didBecomeActive", "c.didLoad"), log)
    }

    @Test
    fun attachChild_activatesSubtreeOfTheChild() {
        val parent = node("p")
        val child = node("c")
        val grandChild = MockRouting()
        child.attachChild(grandChild)
        grandChild.mockInteractable.deactivate()

        parent.attachChild(child)

        assertTrue(grandChild.mockInteractable.isActive)
        assertEquals(1, grandChild.loadCount)
    }

    @Test
    fun attachChild_sameInstanceTwice_fails() {
        val parent = node("p")
        val child = node("c")
        parent.attachChild(child)

        assertFailsWith<IllegalStateException> { parent.attachChild(child) }
    }

    @Test
    fun detachChild_deactivatesChildAndRemovesIt() {
        val parent = node("p")
        val child = MockRouting()
        parent.attachChild(child)

        parent.detachChild(child)

        assertTrue(parent.children.isEmpty())
        assertEquals(1, child.mockInteractable.deactivateCount)
    }

    @Test
    fun detachChild_deactivatesSubtreeInPreOrderAndKeepsIt() {
        val parent = node("p")
        val child = node("c")
        val grandChild = node("g")
        parent.attachChild(child)
        child.attachChild(grandChild)
        log.clear()

        parent.detachChild(child)

        assertEquals(listOf("c.willResignActive", "g.willResignActive"), log)
        assertEquals(1, child.children.size)
        assertFalse(grandChild.interactor.isActive)
    }

    @Test
    fun reattachChild_reactivatesKeptSubtreeWithoutReloading() {
        val parent = node("p")
        val child = node("c")
        parent.attachChild(child)
        child.attachChild(node("g"))
        parent.detachChild(child)
        log.clear()

        parent.attachChild(child)

        assertEquals(listOf("c.didBecomeActive", "g.didBecomeActive"), log)
    }

    @Test
    fun detachChild_notAttached_doesNothing() {
        val parent = node("p")

        parent.detachChild(MockRouting())

        assertTrue(parent.children.isEmpty())
    }

    @Test
    fun saveInstanceState_restoresInteractorStateByTag() {
        val parent = node("p")
        val child = node("c")
        parent.attachChild(child, "child")
        child.interactor.value = "kept"
        val saved = parent.saveInstanceState()

        val restoredParent = node("p")
        restoredParent.mount(saved)
        val restoredChild = node("c")
        restoredParent.attachChild(restoredChild, "child")

        assertEquals("kept", restoredChild.interactor.restoredValue)
    }

    @Test
    fun handleBackPress_delegatesToInteractor() {
        val router = node("a")
        router.interactor.consumesBack = true

        assertTrue(router.handleBackPress())
    }

    private fun node(name: String) = TestRouter(TestInteractor(name, log), name, log)

    private class TestInteractor(private val name: String, private val log: MutableList<String>) : Interactor() {
        var value: String? = null
        var restoredValue: String? = null
        var consumesBack = false

        override fun didBecomeActive() {
            log += "$name.didBecomeActive"
            restoredValue = savedInstanceState?.get(KEY_VALUE) as? String
        }

        override fun willResignActive() {
            log += "$name.willResignActive"
        }

        override fun onSaveInstanceState(outState: MutableMap<String, Any?>) {
            outState[KEY_VALUE] = value
        }

        override fun handleBackPress(): Boolean = consumesBack
    }

    private class TestRouter(
        interactor: TestInteractor,
        private val name: String,
        private val log: MutableList<String>,
    ) : Router<TestInteractor>(interactor) {
        override fun didLoad() {
            log += "$name.didLoad"
        }
    }

    /** 원본 테스트의 `RoutingMock`/`InteractableMock`처럼 호출 횟수만 센다. */
    private class MockRouting : Routing {
        val mockInteractable = MockInteractable()
        var loadCount = 0
        override val interactable: Interactable get() = mockInteractable
        override val children: List<Routing> = emptyList()
        override val lifecycle = kotlinx.coroutines.flow.emptyFlow<RouterLifecycle>()
        override fun load() {
            loadCount++
        }
        override fun attachChild(child: Routing) = Unit
        override fun detachChild(child: Routing) = Unit
    }

    private class MockInteractable : Interactable {
        var activateCount = 0
        var deactivateCount = 0
        private val state = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val isActive: Boolean get() = state.value
        override val isActiveStream = state
        override fun activate() {
            activateCount++
            state.value = true
        }
        override fun deactivate() {
            deactivateCount++
            state.value = false
        }
    }

    private companion object {
        const val KEY_VALUE = "value"
    }
}
