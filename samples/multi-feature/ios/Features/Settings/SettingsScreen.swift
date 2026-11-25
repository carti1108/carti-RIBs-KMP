import AppShared
import RIBsKMPSwiftUI
import SwiftUI

/// 설정 피처의 어댑터.
@MainActor
@Observable
public final class SettingsScreen: NSObject, @preconcurrency SettingsViewContract, SwiftUIRenderable {
    @ObservationIgnored public weak var listener: (any SettingsPresentableListener)?
    private(set) var userName = ""

    override public init() {}

    // 다른 피처의 `render(state:)`와 Obj-C 셀렉터가 겹쳐 Kotlin/Native가 인자 이름을 바꿔 내보낸다(이슈 #3).
    public func render(state_ state: SettingsViewState) {
        userName = state.userName
    }

    public func makeView() -> AnyView {
        AnyView(SettingsScreenView(screen: self))
    }
}

/// 관찰 가능한 어댑터 상태를 읽어 순수 뷰에 값으로 넘긴다.
private struct SettingsScreenView: View {
    let screen: SettingsScreen

    var body: some View {
        SettingsView(
            userName: screen.userName,
            onLogout: { [weak screen] in screen?.listener?.didTapLogout() },
            onClose: { [weak screen] in screen?.listener?.didTapClose() }
        )
    }
}
