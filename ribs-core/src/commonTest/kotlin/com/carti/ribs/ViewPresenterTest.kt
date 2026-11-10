package com.carti.ribs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewPresenterTest {
    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun viewPresenter_exposesItsView() {
        val view = object : ViewControllable {}

        val presenter = object : ViewPresenter<ViewControllable>(view) {}

        assertSame(view, presenter.view)
    }

    @Test
    fun emptyPresenter_loadsWithInteractor() {
        val presenter = EmptyPresenter()
        val router = EmptyRouter(EmptyInteractor(presenter))

        router.dispatchAttach(null)

        assertTrue(presenter.coroutineScope.isActive)
    }

    private class EmptyInteractor(presenter: EmptyPresenter) : Interactor<EmptyPresenter, Router<*>>(presenter)
    private class EmptyRouter(interactor: EmptyInteractor) : Router<EmptyInteractor>(interactor)
}
