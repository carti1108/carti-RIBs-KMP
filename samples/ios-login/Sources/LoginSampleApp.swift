import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

@main
struct LoginSampleApp: App {
    @State private var launcher = AppLauncher()

    var body: some Scene {
        WindowGroup {
            RIBSlotView(slot: launcher.windowSlot)
                .onAppear { launcher.start() }
        }
    }
}

/// 공유 코드의 `LoginApp`을 시작하고, 루트 뷰를 창에 붙이는 `ViewHost` 역할을 한다.
@MainActor
final class AppLauncher: NSObject, @preconcurrency ViewHost {
    let windowSlot = RIBScreenSlot()
    private lazy var app = LoginApp(viewFactory: SwiftUIViewFactory())
    private var isStarted = false

    func start() {
        guard !isStarted else { return }
        isStarted = true
        // 공유 코드의 생명주기 호출이 메인 스레드인지 검사한다(ADR-0007).
        RibThreadingConfig.shared.mainThreadChecker = FoundationMainThreadChecker()
        app.start(host: self)
    }

    func show(view: any ViewControllable) {
        windowSlot.show(anyView: view)
    }

    func hide(view: any ViewControllable) {
        windowSlot.hide(anyView: view)
    }
}

/// Foundation으로 메인 스레드 여부를 알려 주는 검사기. 어느 스레드에서 불려도 안전하다.
final class FoundationMainThreadChecker: NSObject, MainThreadChecker {
    func isMainThread() -> Bool {
        Thread.isMainThread
    }
}
