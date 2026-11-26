import SwiftUI

/// 목록 한 줄에 그릴 값. 공유 모듈의 `Todo`를 순수 뷰에 들이지 않으려고 둔다(ADR-0011).
struct TodoRow: Identifiable, Equatable {
    let id: Int64
    let title: String
    let isDone: Bool
}

/// 할 일 목록의 순수 뷰.
struct TodoListView: View {
    let rows: [TodoRow]
    let onTapRow: (Int64) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(rows) { row in
                Button((row.isDone ? "✓ " : "") + row.title) { onTapRow(row.id) }
                    .accessibilityIdentifier("todoRow-\(row.id)")
            }
        }
    }
}

/// 할 일 상세의 순수 뷰.
struct TodoDetailView: View {
    let title: String
    let isDone: Bool
    let onBack: () -> Void
    let onToggleDone: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("← 목록", action: onBack).accessibilityIdentifier("backButton")
            Text(title).font(.title3.bold()).accessibilityIdentifier("detailTitle")
            Button(isDone ? "완료 취소" : "완료로 표시", action: onToggleDone)
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("toggleDoneButton")
        }
    }
}

#Preview("목록") {
    TodoListView(
        rows: [TodoRow(id: 1, title: "장보기", isDone: false), TodoRow(id: 2, title: "운동", isDone: true)],
        onTapRow: { _ in }
    )
}

#Preview("상세") { TodoDetailView(title: "장보기", isDone: false, onBack: {}, onToggleDone: {}) }
