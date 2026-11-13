import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그인 상태 RIB의 뷰. 머리말과 자식 RIB 화면(목록·상세) 하나를 그린다.
@MainActor
@Observable
final class LoggedInScreen: NSObject, @preconcurrency LoggedInViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any LoggedInPresentableListener)?
    private(set) var userName = ""
    private(set) var sessionSeconds: Int64 = 0
    @ObservationIgnored let slot = RIBScreenSlot()

    func showUserName(name: String) {
        userName = name
    }

    func showSessionSeconds(seconds: Int64) {
        sessionSeconds = seconds
    }

    func showScreen(screen: any ViewControllable) {
        slot.show(anyView: screen)
    }

    func hideScreen(screen: any ViewControllable) {
        slot.hide(anyView: screen)
    }

    func makeView() -> AnyView {
        AnyView(LoggedInView(screen: self))
    }
}

private struct LoggedInView: View {
    let screen: LoggedInScreen

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("\(screen.userName)님, 환영합니다").font(.title2.bold()).accessibilityIdentifier("welcomeLabel")
            Text("로그인 \(screen.sessionSeconds)초").accessibilityIdentifier("sessionLabel")
            Button("로그아웃") { screen.listener?.didTapLogout() }
                .buttonStyle(.bordered)
                .accessibilityIdentifier("logoutButton")
            RIBSlotView(slot: screen.slot)
            Spacer()
        }
        .padding(24)
    }
}
