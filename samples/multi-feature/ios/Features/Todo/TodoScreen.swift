import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 할 일 피처의 어댑터. Kotlin `Array<TodoItem>`은 Swift에서 `KotlinArray`라 `TodoRow` 배열로 바꿔 넘긴다.
@MainActor
@Observable
public final class TodoScreen: NSObject, @preconcurrency TodoViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any TodoPresentableListener)?
    private(set) var userName = ""
    private(set) var rows: [TodoRow] = []

    override public init() {}

    public func renderTodo(state: TodoViewState) {
        userName = state.userName
        rows = (0..<state.items.size).compactMap { state.items.get(index: $0) }
            .map { TodoRow(id: $0.id, title: $0.title, isDone: $0.isDone) }
    }

    public func makeView() -> AnyView {
        AnyView(TodoScreenView(screen: self))
    }
}

/// 관찰 가능한 어댑터 상태를 읽어 순수 뷰에 값으로 넘긴다.
private struct TodoScreenView: View {
    let screen: TodoScreen

    var body: some View {
        TodoView(
            userName: screen.userName,
            rows: screen.rows,
            onToggle: { [weak screen] id in screen?.listener?.didToggle(id: id) },
            onSettings: { [weak screen] in screen?.listener?.didTapSettings() }
        )
    }
}
