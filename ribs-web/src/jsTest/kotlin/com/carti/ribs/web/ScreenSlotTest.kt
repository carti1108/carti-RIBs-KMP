package com.carti.ribs.web

import com.carti.ribs.Interactor
import com.carti.ribs.Router
import com.carti.ribs.ViewRouter
import com.carti.ribs.mount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ScreenSlotTest {
    private class FakeView(override val screenKey: String) : WebViewControllable

    @Test
    fun startsEmpty() {
        assertNull(ScreenSlot().getSnapshot())
    }

    @Test
    fun show_setsCurrentAndNotifies() {
        val slot = ScreenSlot()
        var calls = 0
        slot.subscribe { calls++ }
        val view = FakeView("login")

        slot.show(view)

        assertSame(view, slot.getSnapshot()?.view)
        assertEquals("login", slot.getSnapshot()?.screenKey)
        assertEquals(1, calls)
    }

    @Test
    fun hide_clearsOnlyWhenViewIsCurrent() {
        val slot = ScreenSlot()
        val old = FakeView("login")
        val new = FakeView("home")
        slot.show(old)
        slot.show(new)

        slot.hide(old)

        assertSame(new, slot.getSnapshot()?.view)
        slot.hide(new)
        assertNull(slot.getSnapshot())
    }

    @Test
    fun showingSameViewAgain_doesNotNotify() {
        val slot = ScreenSlot()
        val view = FakeView("login")
        slot.show(view)
        val before = slot.getSnapshot()
        var calls = 0
        slot.subscribe { calls++ }

        slot.show(view)

        assertSame(before, slot.getSnapshot())
        assertEquals(0, calls)
    }

    // RIB 생명주기와 함께: mount는 attach 후 보여 주고, unmount는 detach 후 숨긴다
    @Test
    fun mountAndUnmount_followRouterLifecycle() {
        val slot = ScreenSlot()
        val view = FakeView("login")
        val router = TestRouter(view)

        val mount = router.mount(slot)
        assertSame(view, slot.getSnapshot()?.view)
        assertEquals(true, router.interactor.isAttached())

        mount.unmount()
        assertNull(slot.getSnapshot())
        assertEquals(false, router.interactor.isAttached())
    }

    private class TestInteractor : Interactor<Unit, Router<*>>(Unit)
    private class TestRouter(view: FakeView) : ViewRouter<WebViewControllable, TestInteractor>(view, TestInteractor())
}
