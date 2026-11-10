package com.carti.ribs

/** 현재 스레드가 UI 메인 스레드인지 알려 준다. 공유 코드는 이를 알 수 없으므로 플랫폼이 주입한다(ADR-0007). */
public fun interface MainThreadChecker {
    public fun isMainThread(): Boolean
}

/**
 * 메인 스레드 검사 설정(ADR-0005, ADR-0007). 원본에는 없는 확장이다.
 * 앱 시작 시 한 번 설정한다. 예: iOS `Thread.isMainThread`, Android `Looper.myLooper() == Looper.getMainLooper()`.
 */
public object RibThreadingConfig {
    /** null이면 검사하지 않는다(기본값). JVM·JS 단위 테스트에는 영향이 없다. */
    public var mainThreadChecker: MainThreadChecker? = null

    public fun reset() {
        mainThreadChecker = null
    }
}

/**
 * [operation]이 메인 스레드에서 불렸는지 확인하고, 아니면 [IllegalStateException]으로 실패한다.
 * 검사기가 없으면 아무 일도 하지 않는다. 뷰 계약을 부르기 전처럼 프레임워크가 가로챌 수 없는 지점에서 쓴다.
 */
public fun checkMainThread(operation: String) {
    val checker = RibThreadingConfig.mainThreadChecker ?: return
    check(checker.isMainThread()) { "$operation must be called on the main thread" }
}
