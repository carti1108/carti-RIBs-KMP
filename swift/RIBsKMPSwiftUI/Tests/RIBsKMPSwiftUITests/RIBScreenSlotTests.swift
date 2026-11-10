import SwiftUI
import Testing
@testable import RIBsKMPSwiftUI

@MainActor
private final class FakeScreen: SwiftUIRenderable {
    func makeView() -> AnyView { AnyView(EmptyView()) }
}

@MainActor
struct RIBScreenSlotTests {
    @Test func startsEmpty() {
        let slot = RIBScreenSlot()

        #expect(slot.current == nil)
    }

    @Test func showReplacesCurrentScreen() {
        let slot = RIBScreenSlot()
        let first = FakeScreen()
        let second = FakeScreen()

        slot.show(first)
        slot.show(second)

        #expect(slot.current === second)
    }

    @Test func hideClearsOnlyTheShownScreen() {
        let slot = RIBScreenSlot()
        let shown = FakeScreen()
        let stale = FakeScreen()
        slot.show(shown)

        slot.hide(stale)
        #expect(slot.current === shown)

        slot.hide(shown)
        #expect(slot.current == nil)
    }

    @Test func showReturnsFalseForNonRenderableView() {
        let slot = RIBScreenSlot()

        let accepted = slot.show(anyView: NSObject())

        #expect(accepted == false)
        #expect(slot.current == nil)
    }
}
