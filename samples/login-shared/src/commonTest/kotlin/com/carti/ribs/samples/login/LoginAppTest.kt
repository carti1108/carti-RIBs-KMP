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
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.time.Duration.Companion.seconds

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
    fun blankNameShowsErrorWithoutCallingService() = appTest {
        app.start(host)

        loggedOut().listener?.didTapLogin("   ")
        runCurrent()

        assertEquals(LoggedOutPresenter.EMPTY_NAME_ERROR, loggedOut().error)
        assertEquals(emptyList(), auth.requests)
    }

    @Test
    fun loginShowsLoadingUntilServiceResponds() = appTest {
        app.start(host)

        loggedOut().listener?.didTapLogin("  carti ")
        runCurrent()

        assertEquals(listOf("carti"), auth.requests)
        assertTrue(loggedOut().isLoading)
        assertSame(loggedOut(), factory.root.screen)
    }

    @Test
    fun successfulLoginSwitchesToLoggedIn() = appTest {
        app.start(host)
        login("carti")

        auth.respond(AuthResult.Success("carti"))
        runCurrent()

        assertSame(factory.loggedIn.last(), factory.root.screen)
        assertEquals("carti", factory.loggedIn.last().userName)
    }

    @Test
    fun failedLoginShowsErrorAndStopsLoading() = appTest {
        app.start(host)
        login("carti")

        auth.respond(AuthResult.Failure("서버 오류"))
        runCurrent()

        assertEquals("서버 오류", loggedOut().error)
        assertFalse(loggedOut().isLoading)
        assertSame(loggedOut(), factory.root.screen)
    }

    @Test
    fun tapWhileLoadingIsIgnored() = appTest {
        app.start(host)
        login("carti")

        loggedOut().listener?.didTapLogin("other")
        runCurrent()

        assertEquals(listOf("carti"), auth.requests)
    }

    @Test
    fun stopWhileLoadingCancelsRequest() = appTest {
        app.start(host)
        login("carti")

        app.stop()
        runCurrent()

        assertTrue(auth.wasCancelled)
        assertTrue(host.hidden)
    }

    @Test
    fun logoutReturnsToFreshLoggedOutScreen() = appTest {
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

    @Test
    fun loggedInShowsTodoListByDefault() = appTest {
        startLoggedIn()

        assertSame(factory.todoLists.last(), factory.loggedIn.last().screen)
        assertEquals(TodoStore.SAMPLE_TODOS, factory.todoLists.last().todos)
    }

    @Test
    fun selectingTodoPushesDetailAndBackPopsIt() = appTest {
        startLoggedIn()

        factory.todoLists.last().listener?.didTapTodo(2)
        assertSame(factory.todoDetails.last(), factory.loggedIn.last().screen)
        assertEquals("RIB 문서 읽기", factory.todoDetails.last().title)

        assertTrue(app.handleBackPress())
        assertSame(factory.todoLists.last(), factory.loggedIn.last().screen)
        assertNull(factory.todoDetails.last().listener)

        assertFalse(app.handleBackPress())
    }

    @Test
    fun backOnLoggedOutScreenIsNotHandled() {
        app.start(host)

        assertFalse(app.handleBackPress())
    }

    @Test
    fun togglingDoneInDetailUpdatesListThroughSharedStore() = appTest {
        startLoggedIn()
        factory.todoLists.last().listener?.didTapTodo(1)

        factory.todoDetails.last().listener?.didTapToggleDone()
        factory.todoDetails.last().listener?.didTapBack()
        runCurrent()

        assertEquals(true, factory.todoDetails.last().isDone)
        assertEquals(listOf(1L), factory.todoLists.last().todos.filter { it.isDone }.map { it.id })
        assertSame(factory.todoLists.last(), factory.loggedIn.last().screen)
    }

    @Test
    fun sessionTimerTicksWhileLoggedInAndStopsAfterLogout() = appTest {
        startLoggedIn()
        val view = factory.loggedIn.last()
        assertEquals(0L, view.sessionSeconds)

        advanceTimeBy(2_500)
        assertEquals(2L, view.sessionSeconds)

        view.listener?.didTapLogout()
        advanceTimeBy(5_000)
        assertEquals(2L, view.sessionSeconds)
    }

    @Test
    fun savedStateRestoresUserOpenDetailAndDoneTodos() = appTest {
        startLoggedIn()
        factory.todoLists.last().listener?.didTapTodo(3)
        factory.todoDetails.last().listener?.didTapToggleDone()
        val saved = app.saveState()
        app.stop()

        val restoredFactory = FakeViewFactory()
        val restored = LoginApp(restoredFactory, FakeAuthService())
        restored.start(FakeHost(), saved)
        runCurrent()

        assertTrue(restoredFactory.loggedOut.isEmpty())
        assertEquals("carti", restoredFactory.loggedIn.last().userName)
        assertSame(restoredFactory.todoDetails.last(), restoredFactory.loggedIn.last().screen)
        assertEquals("운동하기", restoredFactory.todoDetails.last().title)
        assertEquals(true, restoredFactory.todoDetails.last().isDone)
        restored.stop()
    }

    /**
     * 로그인하면 세션 타이머가 끝없이 돌기 때문에, runTest가 끝나기 전에 트리를 내려 코루틴을 정리한다.
     * 타임아웃은 그래도 남는 코루틴 때문에 테스트가 멈추지 않게 하는 안전장치다.
     */
    private fun appTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher, timeout = TEST_TIMEOUT) {
        try {
            block()
        } finally {
            app.stop()
        }
    }

    private fun TestScope.startLoggedIn() {
        app.start(host)
        login("carti")
        auth.respond(AuthResult.Success("carti"))
        runCurrent()
    }

    private fun TestScope.login(name: String) {
        loggedOut().listener?.didTapLogin(name)
        runCurrent()
    }

    private fun loggedOut(): FakeLoggedOutView = factory.loggedOut.last()
}

private val TEST_TIMEOUT = 10.seconds

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
    var sessionSeconds: Long? = null
    var screen: ViewControllable? = null
    override fun showUserName(name: String) { userName = name }
    override fun showSessionSeconds(seconds: Long) { sessionSeconds = seconds }
    override fun showScreen(screen: ViewControllable) { this.screen = screen }
    override fun hideScreen(screen: ViewControllable) { if (this.screen === screen) this.screen = null }
}

private class FakeTodoListView : TodoListPresentable {
    override var listener: TodoListPresentableListener? = null
    var todos: List<Todo> = emptyList()
    override fun showTodos(todos: List<Todo>) { this.todos = todos }
}

private class FakeTodoDetailView : TodoDetailPresentable {
    override var listener: TodoDetailPresentableListener? = null
    var title: String? = null
    var isDone: Boolean? = null
    override fun showTodo(title: String, isDone: Boolean) { this.title = title; this.isDone = isDone }
}

private class FakeViewFactory : LoginViewFactory {
    val root = FakeRootView()
    val loggedOut = mutableListOf<FakeLoggedOutView>()
    val loggedIn = mutableListOf<FakeLoggedInView>()
    val todoLists = mutableListOf<FakeTodoListView>()
    val todoDetails = mutableListOf<FakeTodoDetailView>()
    override fun makeTodoListView(): TodoListPresentable = FakeTodoListView().also { todoLists += it }
    override fun makeTodoDetailView(): TodoDetailPresentable = FakeTodoDetailView().also { todoDetails += it }
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
