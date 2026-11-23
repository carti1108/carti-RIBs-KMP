import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 가입 피처의 뷰.
@MainActor
@Observable
public final class SignupScreen: NSObject, @preconcurrency SignupViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any SignupPresentableListener)?
    private(set) var state = SignupViewState(errorMessage: nil)

    override public init() {}

    public func render(state: SignupViewState) {
        self.state = state
    }

    public func makeView() -> AnyView {
        AnyView(SignupView(screen: self))
    }
}

private struct SignupView: View {
    let screen: SignupScreen
    @State private var name = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("가입").font(.largeTitle.bold())
            TextField("이름", text: $name)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("nameField")
            if let message = screen.state.errorMessage {
                Text(message).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            Button("가입") { screen.listener?.didTapSignup(rawName: name) }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("signupButton")
            Button("취소") { screen.listener?.didTapCancel() }
                .accessibilityIdentifier("cancelButton")
        }
        .padding(24)
    }
}
