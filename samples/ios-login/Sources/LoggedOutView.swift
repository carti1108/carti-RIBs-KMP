import SwiftUI

/// 로그아웃 화면의 순수 뷰. 값과 콜백만 받고 공유 모듈 타입을 모른다(ADR-0011).
/// 입력 중인 이름은 화면 지역 상태라 뷰가 직접 가진다.
struct LoggedOutView: View {
    let errorMessage: String?
    let isLoading: Bool
    let onLogin: (String) -> Void
    @State private var name = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("로그인").font(.largeTitle.bold())
            TextField("이름", text: $name)
                .textFieldStyle(.roundedBorder)
                .disabled(isLoading)
                .accessibilityIdentifier("nameField")
            if let errorMessage {
                Text(errorMessage).foregroundStyle(.red).accessibilityIdentifier("errorLabel")
            }
            if isLoading {
                ProgressView().accessibilityIdentifier("loadingIndicator")
            }
            Button("로그인") { onLogin(name) }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading)
                .accessibilityIdentifier("loginButton")
        }
        .padding(24)
    }
}

#Preview("기본") { LoggedOutView(errorMessage: nil, isLoading: false, onLogin: { _ in }) }
#Preview("오류") { LoggedOutView(errorMessage: "이름을 입력하세요", isLoading: false, onLogin: { _ in }) }
#Preview("로딩") { LoggedOutView(errorMessage: nil, isLoading: true, onLogin: { _ in }) }
