package com.carti.ribs.samples.login

import com.carti.ribs.RibState
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

    /** [savedState]는 이전 실행에서 [saveState]가 돌려준 문자열이다. 없으면 처음부터 시작한다. */
    fun start(host: ViewHost<RootViewControllable>, savedState: String? = null) {
        router.launch(host, savedState?.let(RibState::decodeFromString))
    }

    /** 플랫폼 상태 저장소에 넣을 문자열(ADR-0008). */
    fun saveState(): String = router.saveInstanceStateInternal().encodeToString()

    /** 시스템 뒤로 가기. 트리가 처리하면 true, 아니면 플랫폼 기본 동작을 따른다. */
    fun handleBackPress(): Boolean = router.handleBackPress()

    fun stop() {
        router.shutdown()
    }
}
