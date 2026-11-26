import SwiftUI

/// 목록 한 줄에 그릴 값. 공유 모듈의 `TodoItem`을 순수 뷰에 들이지 않으려고 둔다(ADR-0011).
struct TodoRow: Identifiable, Equatable {
    let id: Int32
    let title: String
    let isDone: Bool
}

/// 할 일 화면의 순수 뷰.
struct TodoView: View {
    let userName: String
    let rows: [TodoRow]
    let isLoading: Bool
    let errorMessage: String?
    let onToggle: (Int32) -> Void
    let onRetry: () -> Void
    let onSettings: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("\(userName)님의 할 일")
                .font(.title2.bold())
                .accessibilityIdentifier("welcomeLabel")
            if isLoading {
                ProgressView().accessibilityIdentifier("loadingIndicator")
            }
            if let errorMessage {
                Text(errorMessage)
                    .foregroundStyle(.red)
                    .accessibilityIdentifier("errorLabel")
                Button("다시 시도", action: onRetry)
                    .accessibilityIdentifier("retryButton")
            }
            ForEach(rows) { row in
                Button {
                    onToggle(row.id)
                } label: {
                    Label(row.title, systemImage: row.isDone ? "checkmark.square" : "square")
                }
                .accessibilityIdentifier("todoRow-\(row.id)")
                .accessibilityValue(row.isDone ? "done" : "todo")
            }
            Button("설정", action: onSettings)
                .accessibilityIdentifier("settingsButton")
        }
        .padding(24)
    }
}

#Preview("목록") {
    TodoView(
        userName: "carti",
        rows: [TodoRow(id: 1, title: "장보기", isDone: false), TodoRow(id: 2, title: "운동", isDone: true)],
        isLoading: false,
        errorMessage: nil,
        onToggle: { _ in },
        onRetry: {},
        onSettings: {}
    )
}

#Preview("오류") {
    TodoView(
        userName: "carti",
        rows: [],
        isLoading: false,
        errorMessage: "할 일을 불러오지 못했습니다.",
        onToggle: { _ in },
        onRetry: {},
        onSettings: {}
    )
}
