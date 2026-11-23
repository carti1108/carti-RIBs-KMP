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

    override public init() {}

    public func render(state: TodoViewState) {
        userName = state.userName
        items = (0..<state.items.size).compactMap { state.items.get(index: $0) }
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
