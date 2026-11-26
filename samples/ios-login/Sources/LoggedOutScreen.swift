import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그아웃 상태 RIB의 어댑터. 상태는 공유 코드가 `render`로 한 번에 넘기고, 입력은 `listener`로 돌려준다.
@MainActor
@Observable
final class LoggedOutScreen: NSObject, @preconcurrency LoggedOutViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any LoggedOutPresentableListener)?
    private(set) var state = LoggedOutViewState(isLoading: false, errorMessage: nil)

    func renderLoggedOut(state: LoggedOutViewState) {
        self.state = state
    }

    func makeView() -> AnyView {
        AnyView(LoggedOutScreenView(screen: self))
    }
}

/// 관찰 가능한 어댑터 상태를 읽어 순수 뷰에 값으로 넘긴다.
private struct LoggedOutScreenView: View {
    let screen: LoggedOutScreen

    var body: some View {
        LoggedOutView(errorMessage: screen.state.errorMessage, isLoading: screen.state.isLoading) { [weak screen] name in
            screen?.listener?.didTapLogin(rawName: name)
        }
    }
}
