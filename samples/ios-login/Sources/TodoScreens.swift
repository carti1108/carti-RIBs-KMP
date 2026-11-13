import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 할 일 목록 RIB의 뷰.
@MainActor
@Observable
final class TodoListScreen: NSObject, @preconcurrency TodoListViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any TodoListPresentableListener)?
    private(set) var todos: [Todo] = []

    func showTodos(todos: [Todo]) {
        self.todos = todos
    }

    func makeView() -> AnyView {
        AnyView(TodoListView(screen: self))
    }
}

private struct TodoListView: View {
    let screen: TodoListScreen

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(screen.todos, id: \.id) { todo in
                Button((todo.isDone ? "✓ " : "") + todo.title) { screen.listener?.didTapTodo(id: todo.id) }
                    .accessibilityIdentifier("todoRow-\(todo.id)")
            }
        }
    }
}

/// 할 일 상세 RIB의 뷰. 뒤로 버튼은 공유 코드에 닫기를 요청하고, 닫는 일은 부모 RIB이 한다.
@MainActor
@Observable
final class TodoDetailScreen: NSObject, @preconcurrency TodoDetailViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any TodoDetailPresentableListener)?
    private(set) var title = ""
    private(set) var isDone = false

    func showTodo(title: String, isDone: Bool) {
        self.title = title
        self.isDone = isDone
    }

    func makeView() -> AnyView {
        AnyView(TodoDetailView(screen: self))
    }
}

private struct TodoDetailView: View {
    let screen: TodoDetailScreen

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("← 목록") { screen.listener?.didTapBack() }.accessibilityIdentifier("backButton")
            Text(screen.title).font(.title3.bold()).accessibilityIdentifier("detailTitle")
            Button(screen.isDone ? "완료 취소" : "완료로 표시") { screen.listener?.didTapToggleDone() }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("toggleDoneButton")
        }
    }
}
