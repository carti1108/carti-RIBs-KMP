package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** RIBs-iOS `InteractorTests`를 옮긴 테스트. */
@OptIn(ExperimentalCoroutinesApi::class)
class InteractorTest {

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun interactorIsInactiveByDefault() {
        val interactor = CountingInteractor()

        assertFalse(interactor.isActive)
        assertFalse(interactor.isActiveStream.value)
    }

    @Test
    fun isActive_whenActivated_isTrue() {
        val interactor = CountingInteractor()

        interactor.activate()

        assertTrue(interactor.isActive)
        assertTrue(interactor.isActiveStream.value)
    }

    @Test
    fun isActive_whenDeactivated_isFalse() {
        val interactor = CountingInteractor()
        interactor.activate()

        interactor.deactivate()

        assertFalse(interactor.isActive)
        assertFalse(interactor.isActiveStream.value)
    }

    @Test
    fun didBecomeActive_isNotCalledWhenAlreadyActive() {
        val interactor = CountingInteractor()
        interactor.activate()

        interactor.activate()

        assertEquals(1, interactor.didBecomeActiveCount)
    }

    @Test
    fun willResignActive_isNotCalledWhenAlreadyInactive() {
        val interactor = CountingInteractor()
        interactor.deactivate()
        interactor.activate()
        interactor.deactivate()

        interactor.deactivate()

        assertEquals(1, interactor.willResignActiveCount)
    }

    @Test
    fun coroutineScope_isCancelledAfterWillResignActive() {
        val log = mutableListOf<String>()
        val interactor = object : Interactor() {
            override fun didBecomeActive() {
                coroutineScope.launch {
                    try {
                        awaitCancellation()
                    } finally {
                        log += "cancelled"
                    }
                }
            }

            override fun willResignActive() {
                log += "willResignActive"
            }
        }
        interactor.activate()

        interactor.deactivate()

        assertEquals(listOf("willResignActive", "cancelled"), log)
    }

    @Test
    fun confineTo_emitsOnlyWhileInteractorIsActive() = runTest(UnconfinedTestDispatcher()) {
        val interactor = CountingInteractor()
        val source = MutableSharedFlow<Int>()
        val emitted = mutableListOf<Int>()
        backgroundScope.launch { source.confineTo(interactor).collect { emitted += it } }

        source.emit(1)
        assertEquals(emptyList(), emitted)

        interactor.activate()
        source.emit(2)

        // 원본(combineLatest)처럼 활성화 순간 마지막 값을 다시 내보낸다.
        assertEquals(listOf(1, 2), emitted)
    }

    @Test
    fun presentableInteractor_keepsPresenter() {
        val presenter = object : Presentable {}

        val interactor = PresentableInteractor(presenter)

        assertSame(presenter, interactor.presenter)
    }

    @Test
    fun handleBackPress_defaultsToFalse() {
        assertFalse(CountingInteractor().handleBackPress())
    }

    private class CountingInteractor : Interactor() {
        var didBecomeActiveCount = 0
        var willResignActiveCount = 0

        override fun didBecomeActive() {
            didBecomeActiveCount++
        }

        override fun willResignActive() {
            willResignActiveCount++
        }
    }
}
