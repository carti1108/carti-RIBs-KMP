import AppShared
import LoginFeature
import RIBsKMPSwiftUI
import SettingsFeature
import SignupFeature
import SwiftUI
import TodoFeature

@main
struct MultiFeatureSampleApp: App {
    @State private var launcher = AppLauncher()

    var body: some Scene {
        WindowGroup {
            RIBSlotView(slot: launcher.windowSlot)
                .onAppear { launcher.start() }
        }
    }
}

/// 앱 최상단 의존성. 피처 모듈의 화면으로 각 피처의 뷰 생성 함수를 채운다.
@MainActor
final class AppComponent: NSObject, @preconcurrency RootDependency {
    func makeRootView() -> any RootViewControllable { RootScreen() }
    func makeLoginView() -> any LoginViewContract { LoginScreen() }
    func makeSignupView() -> any SignupViewContract { SignupScreen() }
    func makeTodoView() -> any TodoViewContract { TodoScreen() }
    func makeSettingsView() -> any SettingsViewContract { SettingsScreen() }
}

/// 루트 RIB의 뷰. 자식 RIB 화면 하나를 보여 준다.
@MainActor
final class RootScreen: NSObject, @preconcurrency RootViewControllable, SwiftUIRenderable {
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

/// 공유 코드의 `MultiFeatureApp`을 시작하고 루트 뷰를 창에 붙이는 `ViewHost`.
@MainActor
final class AppLauncher: NSObject, @preconcurrency ViewHost {
    let windowSlot = RIBScreenSlot()
    private lazy var app = MultiFeatureApp(dependency: AppComponent())
    private var isStarted = false

    func start() {
        guard !isStarted else { return }
        isStarted = true
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

final class FoundationMainThreadChecker: NSObject, MainThreadChecker {
    func isMainThread() -> Bool {
        Thread.isMainThread
    }
}
