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
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    override public init() {}

    // 다른 피처의 `render(state:)`와 Obj-C 셀렉터가 겹쳐 Kotlin/Native가 인자 이름을 바꿔 내보낸다(이슈 #3).
    public func render(state___ state: TodoViewState) {
        userName = state.userName
        rows = (0..<state.items.size).compactMap { state.items.get(index: $0) }
            .map { TodoRow(id: $0.id, title: $0.title, isDone: $0.isDone) }
        isLoading = state.isLoading
        errorMessage = state.errorMessage
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
            isLoading: screen.isLoading,
            errorMessage: screen.errorMessage,
            onToggle: { [weak screen] id in screen?.listener?.didToggle(id: id) },
            onRetry: { [weak screen] in screen?.listener?.didTapRetry() },
            onSettings: { [weak screen] in screen?.listener?.didTapSettings() }
        )
    }
}
