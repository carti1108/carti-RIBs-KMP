import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 할 일 피처의 뷰. Kotlin `Array<TodoItem>`은 Swift에서 `KotlinArray`라 배열로 바꿔 둔다.
@MainActor
@Observable
public final class TodoScreen: NSObject, @preconcurrency TodoViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any TodoPresentableListener)?
    private(set) var userName = ""
    private(set) var items: [TodoItem] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    override public init() {}

    public func render(state: TodoViewState) {
        userName = state.userName
        items = (0..<state.items.size).compactMap { state.items.get(index: $0) }
        isLoading = state.isLoading
        errorMessage = state.errorMessage
    }

    public func makeView() -> AnyView {
        AnyView(TodoView(screen: self))
    }
}

private struct TodoView: View {
    let screen: TodoScreen

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("\(screen.userName)님의 할 일")
                .font(.title2.bold())
                .accessibilityIdentifier("welcomeLabel")
            if screen.isLoading {
                ProgressView().accessibilityIdentifier("loadingIndicator")
            }
            if let message = screen.errorMessage {
                Text(message)
                    .foregroundStyle(.red)
                    .accessibilityIdentifier("errorLabel")
                Button("다시 시도") { screen.listener?.didTapRetry() }
                    .accessibilityIdentifier("retryButton")
            }
            ForEach(screen.items, id: \.id) { item in
                Button {
                    screen.listener?.didToggle(id: item.id)
                } label: {
                    Label(item.title, systemImage: item.isDone ? "checkmark.square" : "square")
                }
                .accessibilityIdentifier("todoRow-\(item.id)")
                .accessibilityValue(item.isDone ? "done" : "todo")
            }
            Button("설정") { screen.listener?.didTapSettings() }
                .accessibilityIdentifier("settingsButton")
        }
        .padding(24)
    }
}
