package com.carti.ribs.samples.login

import com.carti.ribs.ViewControllable

/*
 * 플랫폼이 구현하는 뷰 계약(ADR-0006). 상태는 공유 코드가 뷰 메서드를 불러 넘기고,
 * 사용자 입력은 뷰가 리스너를 불러 넘긴다. Flow는 플랫폼에 노출하지 않는다.
 */

/** 플랫폼이 RIB마다 뷰를 만들어 주는 팩토리. 루트 Dependency로 주입한다. */
interface LoginViewFactory {
    fun makeRootView(): RootViewControllable
    fun makeLoggedOutView(): LoggedOutViewControllable
    fun makeLoggedInView(): LoggedInViewControllable
}

/** 자식 RIB 화면 하나를 보여 주는 컨테이너. */
interface RootViewControllable : ViewControllable {
    fun showScreen(screen: ViewControllable)
    fun hideScreen(screen: ViewControllable)
}

interface LoggedOutViewListener {
    fun didTapLogin(rawName: String)
}

interface LoggedOutViewControllable : ViewControllable {
    /** 활성 구간에만 설정된다. 비활성이면 null이라 입력이 무시된다. */
    var listener: LoggedOutViewListener?
    fun showError(message: String?)
}

interface LoggedInViewListener {
    fun didTapLogout()
}

interface LoggedInViewControllable : ViewControllable {
    var listener: LoggedInViewListener?
    fun showUserName(name: String)
}
