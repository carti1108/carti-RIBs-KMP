import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그아웃 상태 RIB의 뷰. 상태는 공유 코드가 `render`로 한 번에 넘기고, 입력은 `listener`로 돌려준다.
@MainActor
@Observable
final class LoggedOutScreen: NSObject, @preconcurrency LoggedOutViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any LoggedOutPresentableListener)?
    private(set) var state = LoggedOutViewState(isLoading: false, errorMessage: nil)

    func render(state: LoggedOutViewState) {
        self.state = state
    }

    func makeView() -> AnyView {
        AnyView(LoggedOutView(screen: self))
    }
}

private struct LoggedOutView: View {
    let screen: LoggedOutScreen
    @State private var name = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("로그인").font(.largeTitle.bold())
            TextField("이름", text: $name)
                .textFieldStyle(.roundedBorder)
                .disabled(screen.state.isLoading)
                .accessibilityIdentifier("nameField")
            if let message = screen.state.errorMessage {
                Text(message).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            if screen.state.isLoading {
                ProgressView().accessibilityIdentifier("loadingIndicator")
            }
            Button("로그인") { screen.listener?.didTapLogin(rawName: name) }
                .buttonStyle(.borderedProminent)
                .disabled(screen.state.isLoading)
                .accessibilityIdentifier("loginButton")
        }
        .padding(24)
    }
}
