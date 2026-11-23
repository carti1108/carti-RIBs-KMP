package com.carti.ribs.samples.multi.root

import com.carti.ribs.Component
import com.carti.ribs.Interactor
import com.carti.ribs.LaunchRouter
import com.carti.ribs.ViewHost
import com.carti.ribs.samples.multi.login.LoginBuilder
import com.carti.ribs.samples.multi.login.LoginDependency
import com.carti.ribs.samples.multi.login.LoginListener

/** 데모 앱이 구현하는 의존성. 로그인 화면과 그 화면을 담을 루트 뷰만 있으면 된다. */
interface LoginDemoDependency : LoginDependency {
    fun makeRootView(): RootViewControllable
}

/**
 * 로그인 피처 하나만 띄우는 데모 진입점. 플랫폼의 피처 데모 앱(iOS `LoginDemo`)이 쓴다.
 * 부모 RIB 대신 사건을 [onEvent]로 넘겨 화면에 찍게 한다. 라우팅은 하지 않는다.
 */
class LoginDemoApp(dependency: LoginDemoDependency, onEvent: (String) -> Unit) {
    private val router = DemoRouter(
        interactor = DemoInteractor(onEvent),
        view = dependency.makeRootView(),
        loginBuilder = LoginBuilder(DemoComponent(dependency)),
    )

    fun start(host: ViewHost<RootViewControllable>) {
        router.launch(host)
    }

    fun stop() {
        router.shutdown()
    }
}

private class DemoComponent(dependency: LoginDemoDependency) :
    Component<LoginDemoDependency>(dependency),
    LoginDependency by dependency

private class DemoInteractor(private val onEvent: (String) -> Unit) : Interactor(), LoginListener {
    override fun didLogin(userName: String) = onEvent("didLogin($userName)")

    override fun didRequestSignup() = onEvent("didRequestSignup")
}

private class DemoRouter(
    interactor: DemoInteractor,
    view: RootViewControllable,
    loginBuilder: LoginBuilder,
) : LaunchRouter<DemoInteractor, RootViewControllable>(interactor, view) {
    private val login = loginBuilder.build(interactor)

    override fun didLoad() {
        attachChild(login)
        viewController.showScreen(login.viewControllable)
    }
}
