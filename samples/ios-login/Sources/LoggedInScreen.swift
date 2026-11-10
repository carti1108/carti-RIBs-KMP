import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그인 상태 RIB의 뷰.
@MainActor
@Observable
final class LoggedInScreen: NSObject, @preconcurrency LoggedInPresentable, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any LoggedInPresentableListener)?
    private(set) var userName = ""

    func showUserName(name: String) {
        userName = name
    }

    func makeView() -> AnyView {
        AnyView(LoggedInView(screen: self))
    }
}

private struct LoggedInView: View {
    let screen: LoggedInScreen

    var body: some View {
        VStack(spacing: 16) {
            Text("\(screen.userName)님, 환영합니다").font(.title.bold()).accessibilityIdentifier("welcomeLabel")
            Button("로그아웃") { screen.listener?.didTapLogout() }
                .buttonStyle(.bordered)
                .accessibilityIdentifier("logoutButton")
        }
        .padding(24)
    }
}
