package com.carti.ribs.samples.login

import com.carti.ribs.RibCoroutinesConfig
import com.carti.ribs.RibDispatchersProvider
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginAppTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var factory: FakeViewFactory
    private lateinit var host: FakeHost
    private lateinit var auth: FakeAuthService
    private lateinit var app: LoginApp

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = SingleDispatcher(dispatcher)
        factory = FakeViewFactory()
        host = FakeHost()
        auth = FakeAuthService()
        app = LoginApp(factory, auth)
    }

    @AfterTest
    fun tearDown() {
        app.stop()
        RibCoroutinesConfig.reset()
    }

    @Test
    fun startShowsRootWithLoggedOutScreen() {
        app.start(host)

        assertSame(factory.root, host.shown)
        assertSame(loggedOut(), factory.root.screen)
    }

    @Test
    fun blankNameShowsErrorWithoutCallingService() = runTest(dispatcher) {
        app.start(host)

        loggedOut().listener?.didTapLogin("   ")
        runCurrent()

        assertEquals(LoggedOutPresenter.EMPTY_NAME_ERROR, loggedOut().error)
        assertEquals(emptyList(), auth.requests)
    }

    @Test
    fun loginShowsLoadingUntilServiceResponds() = runTest(dispatcher) {
        app.start(host)

        loggedOut().listener?.didTapLogin("  carti ")
        runCurrent()

        assertEquals(listOf("carti"), auth.requests)
        assertTrue(loggedOut().isLoading)
        assertSame(loggedOut(), factory.root.screen)
    }

    @Test
    fun successfulLoginSwitchesToLoggedIn() = runTest(dispatcher) {
        app.start(host)
        login("carti")

        auth.respond(AuthResult.Success("carti"))
        runCurrent()

        assertSame(factory.loggedIn.last(), factory.root.screen)
        assertEquals("carti", factory.loggedIn.last().userName)
    }

    @Test
    fun failedLoginShowsErrorAndStopsLoading() = runTest(dispatcher) {
        app.start(host)
        login("carti")

        auth.respond(AuthResult.Failure("서버 오류"))
        runCurrent()

        assertEquals("서버 오류", loggedOut().error)
        assertFalse(loggedOut().isLoading)
        assertSame(loggedOut(), factory.root.screen)
    }

    @Test
    fun tapWhileLoadingIsIgnored() = runTest(dispatcher) {
        app.start(host)
        login("carti")

        loggedOut().listener?.didTapLogin("other")
        runCurrent()

        assertEquals(listOf("carti"), auth.requests)
    }

    @Test
    fun stopWhileLoadingCancelsRequest() = runTest(dispatcher) {
        app.start(host)
        login("carti")

        app.stop()
        runCurrent()

        assertTrue(auth.wasCancelled)
        assertTrue(host.hidden)
    }

    @Test
    fun logoutReturnsToFreshLoggedOutScreen() = runTest(dispatcher) {
        app.start(host)
        login("carti")
        auth.respond(AuthResult.Success("carti"))
        runCurrent()

        factory.loggedIn.last().listener?.didTapLogout()

        assertEquals(2, factory.loggedOut.size)
        assertSame(loggedOut(), factory.root.screen)
        assertFalse(loggedOut().isLoading)
        assertNull(factory.loggedIn.last().listener)
    }

    private fun TestScope.login(name: String) {
        loggedOut().listener?.didTapLogin(name)
        runCurrent()
    }

    private fun loggedOut(): FakeLoggedOutView = factory.loggedOut.last()
}

private class SingleDispatcher(dispatcher: CoroutineDispatcher) : RibDispatchersProvider {
    override val Main: CoroutineDispatcher = dispatcher
    override val MainImmediate: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}

/** 응답 시점을 테스트가 정하는 인증 서비스. */
private class FakeAuthService : AuthService {
    val requests = mutableListOf<String>()
    var wasCancelled = false
        private set
    private var pending = CompletableDeferred<AuthResult>()

    override suspend fun signIn(userName: String): AuthResult {
        requests += userName
        pending = CompletableDeferred()
        try {
            return pending.await()
        } catch (e: CancellationException) {
            wasCancelled = true
            throw e
        }
    }

    fun respond(result: AuthResult) {
        pending.complete(result)
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
    var isLoading = false
    override fun showError(message: String?) { error = message }
    override fun showLoading(isLoading: Boolean) { this.isLoading = isLoading }
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
