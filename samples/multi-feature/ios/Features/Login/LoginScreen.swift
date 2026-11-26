import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그인 피처의 어댑터. 앱과 LoginDemo가 같이 쓴다.
@MainActor
@Observable
public final class LoginScreen: NSObject, @preconcurrency LoginViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any LoginPresentableListener)?
    private(set) var state = LoginViewState(errorMessage: nil)

    override public init() {}

    public func renderLogin(state: LoginViewState) {
        self.state = state
    }

    public func makeView() -> AnyView {
        AnyView(LoginScreenView(screen: self))
    }
}

/// 관찰 가능한 어댑터 상태를 읽어 순수 뷰에 값으로 넘긴다.
private struct LoginScreenView: View {
    let screen: LoginScreen

    var body: some View {
        LoginView(
            errorMessage: screen.state.errorMessage,
            onLogin: { [weak screen] name in screen?.listener?.didTapLogin(rawName: name) },
            onSignup: { [weak screen] in screen?.listener?.didTapSignup() }
        )
    }
}
