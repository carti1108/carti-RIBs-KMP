package com.carti.ribs.samples.multi.root

import com.carti.ribs.Component
import com.carti.ribs.Interactor
import com.carti.ribs.LaunchRouter
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouting
import com.carti.ribs.samples.multi.login.LoginBuildable
import com.carti.ribs.samples.multi.login.LoginDependency
import com.carti.ribs.samples.multi.login.LoginListener
import com.carti.ribs.samples.multi.settings.SettingsBuildable
import com.carti.ribs.samples.multi.settings.SettingsDependency
import com.carti.ribs.samples.multi.settings.SettingsListener
import com.carti.ribs.samples.multi.signup.SignupBuildable
import com.carti.ribs.samples.multi.signup.SignupDependency
import com.carti.ribs.samples.multi.signup.SignupListener
import com.carti.ribs.samples.multi.todo.TodoBuildable
import com.carti.ribs.samples.multi.todo.TodoDependency
import com.carti.ribs.samples.multi.todo.TodoListener

/** 루트 뷰 계약. 자식 RIB 화면 하나를 보여 준다. */
interface RootViewControllable : ViewControllable {
    fun showScreen(screen: ViewControllable)
    fun hideScreen(screen: ViewControllable)
}

/**
 * 앱 최상단 의존성. 플랫폼이 구현한다.
 * 피처마다 자기 `XxxDependency`(뷰 생성 함수)를 선언하고, 여기서는 그것들을 합치기만 한다.
 */
interface RootDependency : LoginDependency, SignupDependency, TodoDependency, SettingsDependency {
    fun makeRootView(): RootViewControllable
}

/** 피처 의존성을 플랫폼 구현에 위임한다. */
class RootComponent(dependency: RootDependency) :
    Component<RootDependency>(dependency),
    LoginDependency by dependency,
    SignupDependency by dependency,
    TodoDependency by dependency,
    SettingsDependency by dependency

/**
 * 화면 흐름: 로그인 ⇄ 가입 → 할 일 → 설정(할 일 위에 겹침) → 로그아웃 시 로그인.
 * 설정을 닫으면 할 일 화면이 상태를 유지한 채 다시 보인다.
 */
class RootInteractor : Interactor(), LoginListener, SignupListener, TodoListener, SettingsListener {
    var router: RootRouter? = null
    private var userName: String? = null

    override fun didBecomeActive() {
        router?.routeToLogin()
    }

    override fun handleBackPress(): Boolean {
        val router = router ?: return false
        return when {
            router.isSettingsAttached -> {
                didCloseSettings()
                true
            }
            router.isSignupAttached -> {
                didCancelSignup()
                true
            }
            else -> false
        }
    }

    override fun didLogin(userName: String) = enterTodo(userName)

    override fun didSignUp(userName: String) = enterTodo(userName)

    override fun didRequestSignup() {
        router?.routeAwayFromLogin()
        router?.routeToSignup()
    }

    override fun didCancelSignup() {
        router?.routeAwayFromSignup()
        router?.routeToLogin()
    }

    override fun didRequestSettings() {
        val name = userName ?: return
        router?.routeToSettings(name)
    }

    override fun didCloseSettings() {
        router?.routeAwayFromSettings()
    }

    override fun didRequestLogout() {
        userName = null
        router?.routeAwayFromSettings()
        router?.routeAwayFromTodo()
        router?.routeToLogin()
    }

    private fun enterTodo(userName: String) {
        this.userName = userName
        router?.routeAwayFromLogin()
        router?.routeAwayFromSignup()
        router?.routeToTodo(userName)
    }
}

/** 자식은 `-api`의 Buildable과 [ViewableRouting]으로만 다룬다. 구현 타입을 모른다. */
class RootRouter(
    interactor: RootInteractor,
    view: RootViewControllable,
    private val loginBuilder: LoginBuildable,
    private val signupBuilder: SignupBuildable,
    private val todoBuilder: TodoBuildable,
    private val settingsBuilder: SettingsBuildable,
) : LaunchRouter<RootInteractor, RootViewControllable>(interactor, view) {
    private var login: ViewableRouting? = null
    private var signup: ViewableRouting? = null
    private var todo: ViewableRouting? = null
    private var settings: ViewableRouting? = null

    val isSignupAttached: Boolean get() = signup != null
    val isSettingsAttached: Boolean get() = settings != null

    init {
        interactor.router = this
    }

    fun routeToLogin() {
        if (login == null) login = attach(loginBuilder.build(interactor))
    }

    fun routeAwayFromLogin() {
        login = detach(login)
    }

    fun routeToSignup() {
        if (signup == null) signup = attach(signupBuilder.build(interactor))
    }

    fun routeAwayFromSignup() {
        signup = detach(signup)
    }

    fun routeToTodo(userName: String) {
        if (todo == null) todo = attach(todoBuilder.build(interactor, userName))
    }

    fun routeAwayFromTodo() {
        todo = detach(todo)
    }

    /** 할 일 RIB은 붙여 둔 채 화면만 가리고 설정을 보여 준다. */
    fun routeToSettings(userName: String) {
        if (settings != null) return
        todo?.let { viewController.hideScreen(it.viewControllable) }
        settings = attach(settingsBuilder.build(interactor, userName))
    }

    fun routeAwayFromSettings() {
        if (settings == null) return
        settings = detach(settings)
        todo?.let { viewController.showScreen(it.viewControllable) }
    }

    private fun attach(child: ViewableRouting): ViewableRouting {
        attachChild(child)
        viewController.showScreen(child.viewControllable)
        return child
    }

    private fun detach(child: ViewableRouting?): ViewableRouting? {
        if (child == null) return null
        detachChild(child)
        viewController.hideScreen(child.viewControllable)
        return null
    }
}
