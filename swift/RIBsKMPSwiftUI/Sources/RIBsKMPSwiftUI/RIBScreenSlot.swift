import Observation
import SwiftUI

/// 자식 RIB 화면 하나를 담는 자리. Router가 `show`/`hide`를 부르면 연결된 `RIBSlotView`가 다시 그려진다.
@MainActor
@Observable
public final class RIBScreenSlot {
    public private(set) var current: SwiftUIRenderable?

    public init() {}

    public func show(_ screen: SwiftUIRenderable) {
        current = screen
    }

    /// 지금 보이는 화면일 때만 비운다. 이미 다른 화면으로 바뀌었다면 무시한다.
    public func hide(_ screen: SwiftUIRenderable) {
        guard current === screen else { return }
        current = nil
    }

    /// 공유 코드가 넘긴 불투명한 뷰를 보여 준다. SwiftUI로 그릴 수 없는 객체면 보여 주지 않고 false를 돌려준다.
    @discardableResult
    public func show(anyView: AnyObject) -> Bool {
        guard let screen = anyView as? SwiftUIRenderable else { return false }
        show(screen)
        return true
    }

    /// 공유 코드가 넘긴 불투명한 뷰를 숨긴다.
    public func hide(anyView: AnyObject) {
        guard let screen = anyView as? SwiftUIRenderable else { return }
        hide(screen)
    }
}

/// `RIBScreenSlot`의 현재 화면을 그리는 SwiftUI 뷰. 비어 있으면 아무것도 그리지 않는다.
public struct RIBSlotView: View {
    private let slot: RIBScreenSlot

    public init(slot: RIBScreenSlot) {
        self.slot = slot
    }

    public var body: some View {
        if let screen = slot.current {
            screen.makeView()
        }
    }
}
