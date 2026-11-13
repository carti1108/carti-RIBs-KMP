package com.carti.ribs.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ViewStoreTest {
    private data class Screen(val isLoading: Boolean, val error: String?)

    @Test
    fun getSnapshot_returnsInitialState() {
        val initial = Screen(isLoading = false, error = null)
        assertSame(initial, ViewStore(initial).getSnapshot())
    }

    @Test
    fun update_replacesSnapshotAndNotifiesOnce() {
        val store = ViewStore(Screen(isLoading = false, error = null))
        var calls = 0
        store.subscribe { calls++ }

        store.update { it.copy(isLoading = true) }

        assertEquals(Screen(isLoading = true, error = null), store.getSnapshot())
        assertEquals(1, calls)
    }

    // useSyncExternalStore는 Object.is로 비교한다. 값이 같으면 같은 객체를 돌려줘야 다시 그리지 않는다
    @Test
    fun update_withEqualValue_keepsSameSnapshotAndDoesNotNotify() {
        val store = ViewStore(Screen(isLoading = false, error = null))
        val before = store.getSnapshot()
        var calls = 0
        store.subscribe { calls++ }

        store.update { it.copy() }

        assertSame(before, store.getSnapshot())
        assertEquals(0, calls)
    }

    @Test
    fun unsubscribe_stopsNotifications_andIsIdempotent() {
        val store = ViewStore(Screen(isLoading = false, error = null))
        var calls = 0
        val unsubscribe = store.subscribe { calls++ }

        unsubscribe()
        unsubscribe()
        store.update { it.copy(isLoading = true) }

        assertEquals(0, calls)
    }

    @Test
    fun listenerUnsubscribingDuringNotify_doesNotSkipOtherListeners() {
        val store = ViewStore(Screen(isLoading = false, error = null))
        val calls = mutableListOf<String>()
        var unsubscribeFirst: () -> Unit = {}
        unsubscribeFirst = store.subscribe {
            calls += "first"
            unsubscribeFirst()
        }
        store.subscribe { calls += "second" }

        store.update { it.copy(isLoading = true) }

        assertEquals(listOf("first", "second"), calls)
    }

    @Test
    fun sameListenerSubscribedTwice_isNotifiedTwice_andUnsubscribedSeparately() {
        val store = ViewStore(Screen(isLoading = false, error = null))
        var calls = 0
        val listener: () -> Unit = { calls++ }
        val first = store.subscribe(listener)
        store.subscribe(listener)

        first()
        store.update { it.copy(isLoading = true) }

        assertEquals(1, calls)
    }
}
