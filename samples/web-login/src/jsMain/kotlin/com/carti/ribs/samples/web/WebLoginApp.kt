@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.samples.web

import com.carti.ribs.ViewHost
import com.carti.ribs.samples.login.DelayedAuthService
import com.carti.ribs.samples.login.LoggedInViewContract
import com.carti.ribs.samples.login.LoggedOutViewContract
import com.carti.ribs.samples.login.LoginApp
import com.carti.ribs.samples.login.LoginViewFactory
import com.carti.ribs.samples.login.RootViewControllable
import com.carti.ribs.samples.login.TodoDetailViewContract
import com.carti.ribs.samples.login.TodoListViewContract
import com.carti.ribs.web.ScreenSlot

/**
 * React 앱의 진입점. 루트 RIB을 [rootSlot]에 붙인다.
 * React StrictMode의 이중 마운트와 겹치지 않도록 컴포넌트 밖(main.tsx)에서 [start]·[stop]을 부른다(ADR-0009).
 */
@JsExport
class WebLoginApp(authDelayMillis: Int) {
    val rootSlot: ScreenSlot = ScreenSlot()
    private val app = LoginApp(WebViewFactory(), DelayedAuthService(authDelayMillis.toLong()))

    fun start() {
        app.start(
            object : ViewHost<RootViewControllable> {
                override fun show(view: RootViewControllable) = rootSlot.show(view as WebRootView)
                override fun hide(view: RootViewControllable) = rootSlot.hide(view as WebRootView)
            },
        )
    }

    fun handleBack(): Boolean = app.handleBackPress()

    fun stop() = app.stop()
}

private class WebViewFactory : LoginViewFactory {
    override fun makeRootView(): RootViewControllable = WebRootView()
    override fun makeLoggedOutView(): LoggedOutViewContract = WebLoggedOutView()
    override fun makeLoggedInView(): LoggedInViewContract = WebLoggedInView()
    override fun makeTodoListView(): TodoListViewContract = WebTodoListView()
    override fun makeTodoDetailView(): TodoDetailViewContract = WebTodoDetailView()
}
