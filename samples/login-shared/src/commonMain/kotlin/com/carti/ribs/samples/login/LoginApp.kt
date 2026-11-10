package com.carti.ribs.samples.login

import com.carti.ribs.ViewHost

/**
 * 플랫폼 진입점. 뷰 팩토리와 인증 서비스를 받아 루트 RIB을 만들고, [start]/[stop]으로 트리를 시작하고 끝낸다.
 * 생명주기 호출은 메인 스레드에서 한다(ADR-0005).
 */
class LoginApp(viewFactory: LoginViewFactory, authService: AuthService) {
    private val router: RootRouter = RootBuilder(
        object : RootDependency {
            override val viewFactory: LoginViewFactory = viewFactory
            override val authService: AuthService = authService
        },
    ).build()

    fun start(host: ViewHost<RootViewControllable>) {
        router.launch(host)
    }

    fun stop() {
        router.shutdown()
    }
}
