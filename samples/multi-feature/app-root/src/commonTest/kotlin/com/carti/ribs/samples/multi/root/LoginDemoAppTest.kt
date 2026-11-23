package com.carti.ribs.samples.multi.root

import com.carti.ribs.RibCoroutinesConfig
import com.carti.ribs.RibDispatchersProvider
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewHost
import com.carti.ribs.samples.multi.login.LoginPresentableListener
import com.carti.ribs.samples.multi.login.LoginViewContract
import com.carti.ribs.samples.multi.login.LoginViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** 로그인 피처만 띄우는 데모 진입점. 부모 대신 사건을 기록한다. */
class LoginDemoAppTest {
    private val view = DemoLoginView()
    private val events = mutableListOf<String>()
    private val root = DemoRootView()
    private lateinit var app: LoginDemoApp

    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = DemoDispatchers(StandardTestDispatcher())
        app = LoginDemoApp(
            dependency = object : LoginDemoDependency {
                override fun makeRootView(): RootViewControllable = root
                override fun makeLoginView(): LoginViewContract = view
            },
            onEvent = { events += it },
        )
        app.start(
            object : ViewHost<RootViewControllable> {
                override fun show(view: RootViewControllable) {}
                override fun hide(view: RootViewControllable) {}
            },
        )
    }

    @AfterTest
    fun tearDown() {
        app.stop()
        RibCoroutinesConfig.reset()
    }

    @Test
    fun showsLoginAndReportsEvents() {
        assertEquals(view, root.shown)
        val listener = assertNotNull(view.listener)

        listener.didTapLogin("kim")
        listener.didTapSignup()

        assertEquals(listOf("didLogin(kim)", "didRequestSignup"), events)
    }

    @Test
    fun stopDetachesLogin() {
        app.stop()

        assertNull(view.listener)
    }
}

private class DemoRootView : RootViewControllable {
    var shown: ViewControllable? = null
    override fun showScreen(screen: ViewControllable) {
        shown = screen
    }

    override fun hideScreen(screen: ViewControllable) {
        if (shown === screen) shown = null
    }
}

private class DemoLoginView : LoginViewContract {
    override var listener: LoginPresentableListener? = null
    override fun render(state: LoginViewState) {}
}

private class DemoDispatchers(dispatcher: CoroutineDispatcher) : RibDispatchersProvider {
    override val Main: CoroutineDispatcher = dispatcher
    override val MainImmediate: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}
