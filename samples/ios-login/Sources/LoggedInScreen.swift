import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 로그인 상태 RIB의 어댑터. 자식 RIB 화면(목록·상세) 슬롯을 `LoggedInView`의 child로 넘긴다.
@MainActor
@Observable
final class LoggedInScreen: NSObject, @preconcurrency LoggedInViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any LoggedInPresentableListener)?
    private(set) var state = LoggedInViewState(userName: "", sessionSeconds: 0)
    @ObservationIgnored let slot = RIBScreenSlot()

    func renderLoggedIn(state: LoggedInViewState) {
        self.state = state
    }

    func showScreen(screen: any ViewControllable) {
        slot.show(anyView: screen)
    }

    func hideScreen(screen: any ViewControllable) {
        slot.hide(anyView: screen)
    }

    func makeView() -> AnyView {
        AnyView(LoggedInScreenView(screen: self))
    }
}

/// 관찰 가능한 어댑터 상태를 읽어 순수 뷰에 값으로 넘긴다.
private struct LoggedInScreenView: View {
    let screen: LoggedInScreen

    var body: some View {
        LoggedInView(
            userName: screen.state.userName,
            sessionSeconds: Int(screen.state.sessionSeconds),
            onLogout: { [weak screen] in screen?.listener?.didTapLogout() }
        ) {
            RIBSlotView(slot: screen.slot)
        }
    }
}
