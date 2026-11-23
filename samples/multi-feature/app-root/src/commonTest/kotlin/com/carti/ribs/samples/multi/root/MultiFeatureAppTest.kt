package com.carti.ribs.samples.multi.root

import com.carti.ribs.RibCoroutinesConfig
import com.carti.ribs.RibDispatchersProvider
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewHost
import com.carti.ribs.samples.multi.login.LoginPresentableListener
import com.carti.ribs.samples.multi.login.LoginViewContract
import com.carti.ribs.samples.multi.login.LoginViewState
import com.carti.ribs.samples.multi.settings.SettingsPresentableListener
import com.carti.ribs.samples.multi.settings.SettingsViewContract
import com.carti.ribs.samples.multi.settings.SettingsViewState
import com.carti.ribs.samples.multi.signup.SignupPresentableListener
import com.carti.ribs.samples.multi.signup.SignupViewContract
import com.carti.ribs.samples.multi.signup.SignupViewState
import com.carti.ribs.samples.multi.todo.TodoPresentableListener
import com.carti.ribs.samples.multi.todo.TodoViewContract
import com.carti.ribs.samples.multi.todo.TodoViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 피처 4개를 루트가 조립한 실제 트리를 가짜 뷰로 돌려 화면 흐름을 확인한다. */
class MultiFeatureAppTest {
    private lateinit var views: FakeViews
    private lateinit var app: MultiFeatureApp

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = SingleDispatcher(StandardTestDispatcher())
        views = FakeViews()
        app = MultiFeatureApp(views)
        app.start(views.host)
    }

    @AfterTest
    fun tearDown() {
        app.stop()
        RibCoroutinesConfig.reset()
    }

    @Test
    fun startShowsLogin() {
        assertIs<FakeLoginView>(views.root.current)
    }

    @Test
    fun blankNameShowsErrorAndStaysOnLogin() {
        val login = views.root.current as FakeLoginView

        login.listener!!.didTapLogin("  ")

        assertEquals(EMPTY_NAME_ERROR, login.state.errorMessage)
        assertIs<FakeLoginView>(views.root.current)
    }

    @Test
    fun loginShowsTodoWithTrimmedName() {
        login("  kim ")

        val todo = assertIs<FakeTodoView>(views.root.current)
        assertEquals("kim", todo.state!!.userName)
        assertTrue(todo.state!!.items.isNotEmpty())
        assertTrue(todo.state!!.items.none { it.isDone })
    }

    @Test
    fun toggleMarksItemDone() {
        val todo = login("kim")
        val id = todo.state!!.items.first().id

        todo.listener!!.didToggle(id)

        assertTrue(todo.state!!.items.first { it.id == id }.isDone)
    }

    @Test
    fun signupCancelReturnsToLogin() {
        (views.root.current as FakeLoginView).listener!!.didTapSignup()
        val signup = assertIs<FakeSignupView>(views.root.current)

        signup.listener!!.didTapCancel()

        assertIs<FakeLoginView>(views.root.current)
        assertEquals(1, views.root.screens.size)
    }

    @Test
    fun signupWithNameShowsTodo() {
        (views.root.current as FakeLoginView).listener!!.didTapSignup()
        val signup = views.root.current as FakeSignupView

        signup.listener!!.didTapSignup("lee")

        assertEquals("lee", assertIs<FakeTodoView>(views.root.current).state!!.userName)
    }

    @Test
    fun signupBlankNameShowsError() {
        (views.root.current as FakeLoginView).listener!!.didTapSignup()
        val signup = views.root.current as FakeSignupView

        signup.listener!!.didTapSignup("")

        assertEquals(EMPTY_NAME_ERROR, signup.state.errorMessage)
        assertIs<FakeSignupView>(views.root.current)
    }

    @Test
    fun settingsCoversTodoAndCloseKeepsTodoState() {
        val todo = login("kim")
        val id = todo.state!!.items.first().id
        todo.listener!!.didToggle(id)

        todo.listener!!.didTapSettings()
        val settings = assertIs<FakeSettingsView>(views.root.current)
        assertEquals("kim", settings.state!!.userName)
        assertFalse(todo in views.root.screens)

        settings.listener!!.didTapClose()

        assertEquals(todo, views.root.current)
        assertTrue(todo.state!!.items.first { it.id == id }.isDone)
    }

    @Test
    fun backPressClosesSettingsThenFallsThrough() {
        val todo = login("kim")
        todo.listener!!.didTapSettings()

        assertTrue(app.handleBackPress())
        assertEquals(todo, views.root.current)
        assertFalse(app.handleBackPress())
    }

    @Test
    fun backPressCancelsSignup() {
        (views.root.current as FakeLoginView).listener!!.didTapSignup()

        assertTrue(app.handleBackPress())
        assertIs<FakeLoginView>(views.root.current)
    }

    @Test
    fun logoutReturnsToLoginAndClearsScreens() {
        val todo = login("kim")
        todo.listener!!.didTapSettings()
        val settings = views.root.current as FakeSettingsView

        settings.listener!!.didTapLogout()

        assertIs<FakeLoginView>(views.root.current)
        assertEquals(1, views.root.screens.size)
        assertNull(todo.listener)
        assertNull(settings.listener)
    }

    @Test
    fun stopDetachesTree() {
        val login = views.root.current as FakeLoginView
        assertNotNull(login.listener)

        app.stop()

        assertNull(login.listener)
        assertNull(views.host.shown)
    }

    private fun login(name: String): FakeTodoView {
        (views.root.current as FakeLoginView).listener!!.didTapLogin(name)
        return views.root.current as FakeTodoView
    }

    private companion object {
        const val EMPTY_NAME_ERROR = "이름을 입력하세요"
    }
}

private class FakeViews : RootDependency {
    val root = FakeRootView()
    val host = FakeHost()
    override fun makeRootView(): RootViewControllable = root
    override fun makeLoginView(): LoginViewContract = FakeLoginView()
    override fun makeSignupView(): SignupViewContract = FakeSignupView()
    override fun makeTodoView(): TodoViewContract = FakeTodoView()
    override fun makeSettingsView(): SettingsViewContract = FakeSettingsView()
}

private class FakeHost : ViewHost<RootViewControllable> {
    var shown: RootViewControllable? = null
    override fun show(view: RootViewControllable) {
        shown = view
    }

    override fun hide(view: RootViewControllable) {
        if (shown === view) shown = null
    }
}

/** 보이는 화면을 쌓아 두고 마지막 것을 현재 화면으로 본다. */
private class FakeRootView : RootViewControllable {
    val screens = mutableListOf<ViewControllable>()
    val current: ViewControllable? get() = screens.lastOrNull()
    override fun showScreen(screen: ViewControllable) {
        screens.remove(screen)
        screens.add(screen)
    }

    override fun hideScreen(screen: ViewControllable) {
        screens.remove(screen)
    }
}

private class FakeLoginView : LoginViewContract {
    override var listener: LoginPresentableListener? = null
    var state = LoginViewState()
    override fun render(state: LoginViewState) {
        this.state = state
    }
}

private class FakeSignupView : SignupViewContract {
    override var listener: SignupPresentableListener? = null
    var state = SignupViewState()
    override fun render(state: SignupViewState) {
        this.state = state
    }
}

private class FakeTodoView : TodoViewContract {
    override var listener: TodoPresentableListener? = null
    var state: TodoViewState? = null
    override fun render(state: TodoViewState) {
        this.state = state
    }
}

private class FakeSettingsView : SettingsViewContract {
    override var listener: SettingsPresentableListener? = null
    var state: SettingsViewState? = null
    override fun render(state: SettingsViewState) {
        this.state = state
    }
}

/** 피처가 코루틴을 쓰지 않아도 활성 구간 스코프가 메인 디스패처를 만들므로 테스트 디스패처를 넣는다. */
private class SingleDispatcher(dispatcher: CoroutineDispatcher) : RibDispatchersProvider {
    override val Main: CoroutineDispatcher = dispatcher
    override val MainImmediate: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}
