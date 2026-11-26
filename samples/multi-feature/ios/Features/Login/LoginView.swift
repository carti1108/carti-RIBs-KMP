import SwiftUI

/// 로그인 화면의 순수 뷰. 값과 콜백만 받고 공유 모듈 타입을 모른다(ADR-0011).
/// 입력 중인 이름은 화면 지역 상태라 뷰가 직접 가진다.
struct LoginView: View {
    let errorMessage: String?
    let onLogin: (String) -> Void
    let onSignup: () -> Void
    @State private var name = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("로그인").font(.largeTitle.bold())
            TextField("이름", text: $name)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("nameField")
            if let errorMessage {
                Text(errorMessage).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            Button("로그인") { onLogin(name) }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("loginButton")
            Button("가입하기", action: onSignup)
                .accessibilityIdentifier("signupLink")
        }
        .padding(24)
    }
}

#Preview("기본") { LoginView(errorMessage: nil, onLogin: { _ in }, onSignup: {}) }
#Preview("오류") { LoginView(errorMessage: "이름을 입력하세요", onLogin: { _ in }, onSignup: {}) }
