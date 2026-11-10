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
        app.start(host: self)
    }

    func show(view: any ViewControllable) {
        windowSlot.show(anyView: view)
    }

    func hide(view: any ViewControllable) {
        windowSlot.hide(anyView: view)
    }
}
