import AppShared
import LoginFeature
import RIBsKMPSwiftUI
import SwiftUI

/// 로그인 피처만 띄우는 데모 앱. 부모 RIB 대신 Kotlin `LoginDemoApp`이 넘겨 주는 사건을 화면 아래에 찍는다.
@main
struct LoginDemo: App {
    @State private var launcher = DemoLauncher()

    var body: some Scene {
        WindowGroup {
            VStack {
                RIBSlotView(slot: launcher.windowSlot)
                Divider()
                ForEach(Array(launcher.events.enumerated()), id: \.offset) { _, event in
                    Text(event).font(.caption.monospaced())
                }
            }
            .onAppear { launcher.start() }
        }
    }
}

@MainActor
final class DemoComponent: NSObject, @preconcurrency LoginDemoDependency {
    func makeRootView() -> any RootViewControllable { DemoRootScreen() }
    func makeLoginView() -> any LoginViewContract { LoginScreen() }
}

@MainActor
@Observable
final class DemoLauncher: NSObject, @preconcurrency ViewHost {
    @ObservationIgnored let windowSlot = RIBScreenSlot()
    private(set) var events: [String] = []
    @ObservationIgnored private var app: LoginDemoApp?

    func start() {
        guard app == nil else { return }
        let app = LoginDemoApp(dependency: DemoComponent()) { [weak self] event in
            self?.events.append(event)
        }
        self.app = app
        app.start(host: self)
    }

    func show(view: any ViewControllable) {
        windowSlot.show(anyView: view)
    }

    func hide(view: any ViewControllable) {
        windowSlot.hide(anyView: view)
    }
}

/// 로그인 화면 하나를 담는 루트 뷰.
@MainActor
final class DemoRootScreen: NSObject, @preconcurrency RootViewControllable, SwiftUIRenderable {
    private let slot = RIBScreenSlot()

    func showScreen(screen: any ViewControllable) {
        slot.show(anyView: screen)
    }

    func hideScreen(screen: any ViewControllable) {
        slot.hide(anyView: screen)
    }

    func makeView() -> AnyView {
        AnyView(RIBSlotView(slot: slot))
    }
}
