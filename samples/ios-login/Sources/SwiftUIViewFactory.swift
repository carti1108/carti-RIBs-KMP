import LoginShared

/// 공유 코드가 RIB마다 요청하는 뷰를 SwiftUI 구현으로 만들어 준다.
@MainActor
final class SwiftUIViewFactory: NSObject, @preconcurrency LoginViewFactory {
    func makeRootView() -> any RootViewControllable { RootScreen() }
    func makeLoggedOutView() -> any LoggedOutViewControllable { LoggedOutScreen() }
    func makeLoggedInView() -> any LoggedInViewControllable { LoggedInScreen() }
}
