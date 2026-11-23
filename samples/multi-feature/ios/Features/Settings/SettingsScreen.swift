import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 설정 피처의 뷰.
@MainActor
@Observable
public final class SettingsScreen: NSObject, @preconcurrency SettingsViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any SettingsPresentableListener)?
    private(set) var userName = ""

    override public init() {}

    public func render(state: SettingsViewState) {
        userName = state.userName
    }

    public func makeView() -> AnyView {
        AnyView(SettingsView(screen: self))
    }
}

private struct SettingsView: View {
    let screen: SettingsScreen

    var body: some View {
        VStack(spacing: 16) {
            Text("설정").font(.largeTitle.bold())
            Text("\(screen.userName)(으)로 로그인됨").accessibilityIdentifier("settingsUserLabel")
            Button("로그아웃") { screen.listener?.didTapLogout() }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("logoutButton")
            Button("닫기") { screen.listener?.didTapClose() }
                .accessibilityIdentifier("closeButton")
        }
        .padding(24)
    }
}
