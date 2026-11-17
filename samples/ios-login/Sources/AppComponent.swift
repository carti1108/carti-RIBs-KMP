import LoginShared

/// 앱 최상단 의존성. 공유 코드가 RIB마다 요청하는 뷰를 SwiftUI 구현으로 만들어 준다.
@MainActor
final class AppComponent: NSObject, @preconcurrency RootDependency {
    let authService: any AuthService

    init(authService: any AuthService) {
        self.authService = authService
    }

    func makeRootView() -> any RootViewControllable { RootScreen() }
    func makeLoggedOutView() -> any LoggedOutViewContract { LoggedOutScreen() }
    func makeLoggedInView() -> any LoggedInViewContract { LoggedInScreen() }
    func makeTodoListView() -> any TodoListViewContract { TodoListScreen() }
    func makeTodoDetailView() -> any TodoDetailViewContract { TodoDetailScreen() }
}
