package com.carti.ribs.samples.login

/**
 * 플랫폼이 RIB마다 뷰를 만들어 주는 팩토리. 루트 Dependency로 주입한다(ADR-0006).
 * 각 뷰 계약(`XxxPresentable`)은 해당 RIB 파일에 있다.
 */
interface LoginViewFactory {
    fun makeRootView(): RootViewControllable
    fun makeLoggedOutView(): LoggedOutPresentable
    fun makeLoggedInView(): LoggedInPresentable
    fun makeTodoListView(): TodoListPresentable
    fun makeTodoDetailView(): TodoDetailPresentable
}
