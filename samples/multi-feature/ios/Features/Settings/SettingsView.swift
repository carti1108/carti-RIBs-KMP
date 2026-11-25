import SwiftUI

/// 설정 화면의 순수 뷰(ADR-0011).
struct SettingsView: View {
    let userName: String
    let onLogout: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("설정").font(.largeTitle.bold())
            Text("\(userName)(으)로 로그인됨").accessibilityIdentifier("settingsUserLabel")
            Button("로그아웃", action: onLogout)
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("logoutButton")
            Button("닫기", action: onClose)
                .accessibilityIdentifier("closeButton")
        }
        .padding(24)
    }
}

#Preview { SettingsView(userName: "carti", onLogout: {}, onClose: {}) }
