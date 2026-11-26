import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 가입 피처의 어댑터.
@MainActor
@Observable
public final class SignupScreen: NSObject, @preconcurrency SignupViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any SignupPresentableListener)?
    private(set) var state = SignupViewState(errorMessage: nil)

    override public init() {}

    public func renderSignup(state: SignupViewState) {
        self.state = state
    }

    public func makeView() -> AnyView {
        AnyView(SignupScreenView(screen: self))
    }
}

/// 관찰 가능한 어댑터 상태를 읽어 순수 뷰에 값으로 넘긴다.
private struct SignupScreenView: View {
    let screen: SignupScreen

    var body: some View {
        SignupView(
            errorMessage: screen.state.errorMessage,
            onSignup: { [weak screen] name in screen?.listener?.didTapSignup(rawName: name) },
            onCancel: { [weak screen] in screen?.listener?.didTapCancel() }
        )
    }
}
