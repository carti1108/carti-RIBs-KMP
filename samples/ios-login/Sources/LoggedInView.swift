import SwiftUI

/// 로그인 화면의 순수 뷰. 자식 RIB 화면(목록·상세)은 어댑터가 `child`로 넘긴다(ADR-0011).
struct LoggedInView<Child: View>: View {
    let userName: String
    let sessionSeconds: Int
    let onLogout: () -> Void
    @ViewBuilder let child: () -> Child

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("\(userName)님, 환영합니다").font(.title2.bold()).accessibilityIdentifier("welcomeLabel")
            Text("로그인 \(sessionSeconds)초").accessibilityIdentifier("sessionLabel")
            Button("로그아웃", action: onLogout)
                .buttonStyle(.bordered)
                .accessibilityIdentifier("logoutButton")
            child()
            Spacer()
        }
        .padding(24)
    }
}

#Preview {
    LoggedInView(userName: "carti", sessionSeconds: 42, onLogout: {}) {
        Text("자식 화면 자리")
    }
}
