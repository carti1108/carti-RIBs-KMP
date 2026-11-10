import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그아웃 상태 RIB의 뷰. 상태는 공유 코드가 `showError`로 넘기고, 입력은 `listener`로 돌려준다.
@MainActor
@Observable
final class LoggedOutScreen: NSObject, @preconcurrency LoggedOutPresentable, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any LoggedOutPresentableListener)?
    private(set) var errorMessage: String?

    func showError(message: String?) {
        errorMessage = message
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
                .accessibilityIdentifier("nameField")
            if let message = screen.errorMessage {
                Text(message).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            Button("로그인") { screen.listener?.didTapLogin(rawName: name) }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("loginButton")
        }
        .padding(24)
    }
}
