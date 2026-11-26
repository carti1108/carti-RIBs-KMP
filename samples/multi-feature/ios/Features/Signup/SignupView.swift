import SwiftUI

/// 가입 화면의 순수 뷰. 입력 중인 이름은 화면 지역 상태라 뷰가 직접 가진다(ADR-0011).
struct SignupView: View {
    let errorMessage: String?
    let onSignup: (String) -> Void
    let onCancel: () -> Void
    @State private var name = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("가입").font(.largeTitle.bold())
            TextField("이름", text: $name)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("nameField")
            if let errorMessage {
                Text(errorMessage).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            Button("가입") { onSignup(name) }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("signupButton")
            Button("취소", action: onCancel)
                .accessibilityIdentifier("cancelButton")
        }
        .padding(24)
    }
}

#Preview("기본") { SignupView(errorMessage: nil, onSignup: { _ in }, onCancel: {}) }
#Preview("오류") { SignupView(errorMessage: "이미 있는 이름입니다", onSignup: { _ in }, onCancel: {}) }
