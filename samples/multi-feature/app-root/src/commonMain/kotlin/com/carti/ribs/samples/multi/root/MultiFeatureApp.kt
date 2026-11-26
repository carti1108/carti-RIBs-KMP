package com.carti.ribs.samples.multi.root

import com.carti.ribs.ViewHost
import com.carti.ribs.samples.multi.login.LoginBuilder
import com.carti.ribs.samples.multi.settings.SettingsBuilder
import com.carti.ribs.samples.multi.signup.SignupBuilder
import com.carti.ribs.samples.multi.todo.TodoBuilder
import com.carti.ribs.samples.multi.todo.TodoRepository
import com.carti.ribs.samples.multi.todo.data.KtorHttpTransport
import com.carti.ribs.samples.multi.todo.data.RemoteTodoRepository
import com.carti.ribs.samples.multi.todo.data.SampleTodoServer

/**
 * 플랫폼 진입점. 플랫폼이 구현한 [RootDependency]를 받아 루트 RIB을 만든다.
 * 피처 impl의 Builder를 아는 곳은 여기뿐이다. 루트 Router는 `-api`의 Buildable만 본다.
 * 데이터 계층(todo-data)을 아는 곳도 여기뿐이다. 저장소를 주지 않으면 [SampleTodoServer]를 쓴다(ADR-0012).
 * 서버 주소를 주면 Ktor로 실제 요청을 보낸다(ADR-0013).
 * 생명주기 호출은 메인 스레드에서 한다(ADR-0005).
 */
class MultiFeatureApp(dependency: RootDependency, todoRepository: TodoRepository) {
    constructor(dependency: RootDependency) : this(
        dependency,
        RemoteTodoRepository(SampleTodoServer(), SampleTodoServer.BASE_URL),
    )

    /** [todoBaseUrl] 아래 `/todos`로 실제 요청을 보낸다. 끝에 `/`를 붙이지 않는다. */
    constructor(dependency: RootDependency, todoBaseUrl: String) : this(
        dependency,
        RemoteTodoRepository(KtorHttpTransport(), todoBaseUrl),
    )

    private val router: RootRouter = run {
        val component = RootComponent(dependency, todoRepository)
        RootRouter(
            interactor = RootInteractor(),
            view = dependency.makeRootView(),
            loginBuilder = LoginBuilder(component),
            signupBuilder = SignupBuilder(component),
            todoBuilder = TodoBuilder(component),
            settingsBuilder = SettingsBuilder(component),
        )
    }

    fun start(host: ViewHost<RootViewControllable>) {
        router.launch(host)
    }

    /** 시스템 뒤로 가기. 트리가 처리하면 true, 아니면 플랫폼 기본 동작을 따른다. */
    fun handleBackPress(): Boolean = router.handleBackPress()

    fun stop() {
        router.shutdown()
    }
}
