import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 할 일 목록 RIB의 어댑터. 공유 `Todo`를 `TodoRow`로 바꿔 넘긴다.
@MainActor
@Observable
final class TodoListScreen: NSObject, @preconcurrency TodoListViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any TodoListPresentableListener)?
    private(set) var rows: [TodoRow] = []

    func showTodos(todos: [Todo]) {
        rows = todos.map { TodoRow(id: $0.id, title: $0.title, isDone: $0.isDone) }
    }

    func makeView() -> AnyView {
        AnyView(TodoListScreenView(screen: self))
    }
}

private struct TodoListScreenView: View {
    let screen: TodoListScreen

    var body: some View {
        TodoListView(rows: screen.rows) { [weak screen] id in screen?.listener?.didTapTodo(id: id) }
    }
}

/// 할 일 상세 RIB의 어댑터. 뒤로 버튼은 공유 코드에 닫기를 요청하고, 닫는 일은 부모 RIB이 한다.
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
        AnyView(TodoDetailScreenView(screen: self))
    }
}

private struct TodoDetailScreenView: View {
    let screen: TodoDetailScreen

    var body: some View {
        TodoDetailView(
            title: screen.title,
            isDone: screen.isDone,
            onBack: { [weak screen] in screen?.listener?.didTapBack() },
            onToggleDone: { [weak screen] in screen?.listener?.didTapToggleDone() }
        )
    }
}
