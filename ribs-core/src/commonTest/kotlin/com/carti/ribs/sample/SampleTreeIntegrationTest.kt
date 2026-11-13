package com.carti.ribs.sample

import com.carti.ribs.EmptyComponent
import com.carti.ribs.RibCoroutinesConfig
import com.carti.ribs.Router
import com.carti.ribs.TestRibDispatchers
import com.carti.ribs.ViewHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SampleTreeIntegrationTest {
    private val host = RecordingHost()

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = TestRibDispatchers(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun launch_showsRootAndRoutesToLoggedOut() {
        val root = RootBuilder(EmptyComponent()).build()

        root.launch(host)

        assertEquals(listOf("show(root)"), host.log)
        assertIs<LoggedOutRouter>(root.children.single())
    }

    @Test
    fun loginIntent_replacesLoggedOutWithLoggedIn() {
        val root = RootBuilder(EmptyComponent()).build().also { it.launch(host) }
        val loggedOut = root.childOf<LoggedOutRouter>()

        loggedOut.viewController.didTapLogin("  carti  ")

        val loggedIn = root.childOf<LoggedInRouter>()
        assertEquals(1, root.children.size)
        assertEquals("carti", loggedIn.interactor.userName)
        assertFalse(loggedOut.interactor.isActive)
        assertTrue(loggedIn.interactor.heartbeat.job?.isActive == true)
    }

    @Test
    fun blankName_isRejectedByPresenterWithoutRouting() {
        val root = RootBuilder(EmptyComponent()).build().also { it.launch(host) }
        val presenter = root.childOf<LoggedOutRouter>().viewController

        presenter.didTapLogin("   ")

        assertEquals("이름을 입력하세요", presenter.errorMessage)
        assertIs<LoggedOutRouter>(root.children.single())
    }

    @Test
    fun childrenShareSessionStoreFromRootComponent() {
        val root = RootBuilder(EmptyComponent()).build().also { it.launch(host) }
        val loggedOut = root.childOf<LoggedOutRouter>()
        loggedOut.viewController.didTapLogin("carti")
        val loggedIn = root.childOf<LoggedInRouter>()

        assertSame(loggedOut.interactor.sessionStore, loggedIn.interactor.sessionStore)
        assertEquals("carti", loggedIn.interactor.sessionStore.user.value)

        loggedIn.interactor.logout()

        assertNull(loggedIn.interactor.sessionStore.user.value)
    }

    @Test
    fun logout_stopsWorkerAndReturnsToLoggedOut() {
        val root = RootBuilder(EmptyComponent()).build().also { it.launch(host) }
        root.childOf<LoggedOutRouter>().viewController.didTapLogin("carti")
        val loggedIn = root.childOf<LoggedInRouter>()
        val heartbeat = loggedIn.interactor.heartbeat

        loggedIn.interactor.logout()

        assertTrue(heartbeat.stopped)
        assertTrue(heartbeat.job?.isCancelled == true)
        assertFalse(loggedIn.interactor.isActive)
        assertIs<LoggedOutRouter>(root.children.single())
    }

    @Test
    fun shutdown_detachesWholeTreeAndHidesRoot() {
        val root = RootBuilder(EmptyComponent()).build().also { it.launch(host) }
        root.childOf<LoggedOutRouter>().viewController.didTapLogin("carti")
        val loggedIn = root.childOf<LoggedInRouter>()

        root.shutdown()

        assertFalse(root.interactor.isActive)
        assertFalse(loggedIn.interactor.isActive)
        assertTrue(loggedIn.interactor.heartbeat.stopped)
        assertTrue(root.children.isEmpty())
        assertEquals(listOf("show(root)", "hide(root)"), host.log)
    }

    @Test
    fun relaunchAfterShutdown_startsFreshTree() {
        val root = RootBuilder(EmptyComponent()).build()
        root.launch(host)
        root.childOf<LoggedOutRouter>().viewController.didTapLogin("carti")
        root.shutdown()

        root.launch(host)

        assertIs<LoggedOutRouter>(root.children.single())
    }

    private inline fun <reified T : Router<*>> RootRouter.childOf(): T =
        children.filterIsInstance<T>().single()

    private class RecordingHost : ViewHost<FakeView> {
        val log = mutableListOf<String>()
        override fun show(view: FakeView) {
            log += "show(${view.name})"
        }

        override fun hide(view: FakeView) {
            log += "hide(${view.name})"
        }
    }
}
