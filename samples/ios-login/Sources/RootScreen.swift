import LoginShared
import RIBsKMPSwiftUI
import SwiftUI

/// 루트 RIB의 뷰. 자식 RIB 화면 하나를 보여 준다.
@MainActor
final class RootScreen: NSObject, @preconcurrency RootViewControllable, SwiftUIRenderable {
    private let slot = RIBScreenSlot()

    func showScreen(screen: any ViewControllable) {
        slot.show(anyView: screen)
    }

    func hideScreen(screen: any ViewControllable) {
        slot.hide(anyView: screen)
    }

    func makeView() -> AnyView {
        AnyView(RIBSlotView(slot: slot).animation(.default, value: slot.current.map(ObjectIdentifier.init)))
    }
}
