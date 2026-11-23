import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그인 피처의 뷰. 앱과 LoginDemo가 같이 쓴다.
@MainActor
@Observable
public final class LoginScreen: NSObject, @preconcurrency LoginViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any LoginPresentableListener)?
    private(set) var state = LoginViewState(errorMessage: nil)

    override public init() {}

    public func render(state: LoginViewState) {
        self.state = state
    }

    public func makeView() -> AnyView {
        AnyView(LoginView(screen: self))
    }
}

private struct LoginView: View {
    let screen: LoginScreen
    @State private var name = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("로그인").font(.largeTitle.bold())
            TextField("이름", text: $name)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("nameField")
            if let message = screen.state.errorMessage {
                Text(message).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            Button("로그인") { screen.listener?.didTapLogin(rawName: name) }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("loginButton")
            Button("가입하기") { screen.listener?.didTapSignup() }
                .accessibilityIdentifier("signupLink")
        }
        .padding(24)
    }
}
