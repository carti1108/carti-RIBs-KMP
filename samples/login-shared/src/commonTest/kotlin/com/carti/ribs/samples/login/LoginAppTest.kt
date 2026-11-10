package com.carti.ribs.samples.login

import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewHost
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LoginAppTest {
    private lateinit var factory: FakeViewFactory
    private lateinit var host: FakeHost
    private lateinit var app: LoginApp

    @BeforeTest
    fun setUp() {
        factory = FakeViewFactory()
        host = FakeHost()
        app = LoginApp(factory)
    }

    @AfterTest
    fun tearDown() {
        app.stop()
    }

    @Test
    fun startShowsRootWithLoggedOutScreen() {
        app.start(host)

        assertSame(factory.root, host.shown)
        assertSame(factory.loggedOut.last(), factory.root.screen)
    }

    @Test
    fun blankNameShowsErrorAndStaysLoggedOut() {
        app.start(host)

        factory.loggedOut.last().listener?.didTapLogin("   ")

        assertEquals(LoggedOutPresenter.EMPTY_NAME_ERROR, factory.loggedOut.last().error)
        assertSame(factory.loggedOut.last(), factory.root.screen)
    }

    @Test
    fun loginSwitchesToLoggedInWithTrimmedName() {
        app.start(host)

        factory.loggedOut.last().listener?.didTapLogin("  carti ")

        val loggedIn = factory.loggedIn.last()
        assertSame(loggedIn, factory.root.screen)
        assertEquals("carti", loggedIn.userName)
    }

    @Test
    fun logoutReturnsToFreshLoggedOutScreen() {
        app.start(host)
        factory.loggedOut.last().listener?.didTapLogin("carti")

        factory.loggedIn.last().listener?.didTapLogout()

        assertEquals(2, factory.loggedOut.size)
        assertSame(factory.loggedOut.last(), factory.root.screen)
        assertNull(factory.loggedIn.last().listener)
    }

    @Test
    fun stopHidesRootAndDetachesListeners() {
        app.start(host)

        app.stop()

        assertTrue(host.hidden)
        assertNull(factory.loggedOut.last().listener)
    }
}

private class FakeRootView : RootViewControllable {
    var screen: ViewControllable? = null
    override fun showScreen(screen: ViewControllable) { this.screen = screen }
    override fun hideScreen(screen: ViewControllable) { if (this.screen === screen) this.screen = null }
}

private class FakeLoggedOutView : LoggedOutPresentable {
    override var listener: LoggedOutPresentableListener? = null
    var error: String? = null
    override fun showError(message: String?) { error = message }
}

private class FakeLoggedInView : LoggedInPresentable {
    override var listener: LoggedInPresentableListener? = null
    var userName: String? = null
    override fun showUserName(name: String) { userName = name }
}

private class FakeViewFactory : LoginViewFactory {
    val root = FakeRootView()
    val loggedOut = mutableListOf<FakeLoggedOutView>()
    val loggedIn = mutableListOf<FakeLoggedInView>()
    override fun makeRootView(): RootViewControllable = root
    override fun makeLoggedOutView(): LoggedOutPresentable = FakeLoggedOutView().also { loggedOut += it }
    override fun makeLoggedInView(): LoggedInPresentable = FakeLoggedInView().also { loggedIn += it }
}

private class FakeHost : ViewHost<RootViewControllable> {
    var shown: RootViewControllable? = null
    var hidden = false
    override fun show(view: RootViewControllable) { shown = view }
    override fun hide(view: RootViewControllable) { hidden = true }
}
