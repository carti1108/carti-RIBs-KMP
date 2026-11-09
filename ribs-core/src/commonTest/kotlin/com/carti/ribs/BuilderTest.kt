package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class BuilderTest {

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun shared_returnsSameInstanceWithinComponent() {
        val component = ParentComponent(EmptyComponent())

        assertSame(component.counter, component.counter)
    }

    @Test
    fun shared_isCreatedLazilyOnFirstAccess() {
        var created = 0
        val component = object : Component<EmptyDependency>(EmptyComponent()) {
            val value by shared { created++; Any() }
        }
        assertEquals(0, created)

        component.value
        component.value

        assertEquals(1, created)
    }

    @Test
    fun shared_isDistinctPerComponentInstance() {
        val first = ParentComponent(EmptyComponent())
        val second = ParentComponent(EmptyComponent())

        assertNotSame(first.counter, second.counter)
    }

    @Test
    fun childBuilder_receivesDependencyFromParentComponent() {
        val parentComponent = ParentComponent(EmptyComponent())

        val childRouter = ChildBuilder(parentComponent).build(listener = object : ChildListener {}, step = 3)

        assertSame(parentComponent.counter, childRouter.interactor.counter)
        assertEquals(3, childRouter.interactor.step)
    }

    // --- 부모 RIB: 자식에게 Counter를 제공한다 ---

    class Counter

    private class ParentComponent(dependency: EmptyDependency) :
        Component<EmptyDependency>(dependency), ChildDependency {
        override val counter: Counter by shared { Counter() }
    }

    // --- 자식 RIB ---

    private interface ChildDependency : Dependency {
        val counter: Counter
    }

    private interface ChildListener

    private class ChildInteractor(
        val counter: Counter,
        val step: Int,
        @Suppress("unused") val listener: ChildListener,
    ) : Interactor<Unit, ChildRouter>(Unit)

    private class ChildRouter(interactor: ChildInteractor) : Router<ChildInteractor>(interactor)

    private class ChildBuilder(dependency: ChildDependency) : Builder<ChildDependency>(dependency) {
        fun build(listener: ChildListener, step: Int): ChildRouter {
            val interactor = ChildInteractor(dependency.counter, step, listener)
            return ChildRouter(interactor)
        }
    }
}
