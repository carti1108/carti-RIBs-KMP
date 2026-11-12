@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.web

/**
 * 화면 상태 스냅숏 하나와 구독자 목록(ADR-0009). React는 `useSyncExternalStore(store.subscribe, store.getSnapshot)`로 쓴다.
 *
 * 값이 같으면(`equals`) 기존 스냅숏 객체를 그대로 두고 알리지 않는다. React는 `Object.is`로 비교하므로
 * 같은 값에 새 객체를 주면 쓸데없이 다시 그린다. 생명주기와 마찬가지로 단일 스레드에서 부른다고 가정한다(ADR-0005).
 */
@JsExport
public class ViewStore<S : Any>(initial: S) {
    private var snapshot: S = initial
    private var subscriptions: List<Subscription> = emptyList()

    /** 지금 상태. 값이 바뀌기 전까지 같은 객체를 돌려준다. */
    public fun getSnapshot(): S = snapshot

    /** [listener]를 등록하고 해제 함수를 돌려준다. 해제는 여러 번 불러도 한 번만 동작한다. */
    public fun subscribe(listener: () -> Unit): () -> Unit {
        val subscription = Subscription(listener)
        subscriptions = subscriptions + subscription
        return { subscriptions = subscriptions - subscription }
    }

    /** Kotlin 쪽 뷰 구현이 상태를 바꿀 때 쓴다. */
    @JsExport.Ignore
    public fun update(transform: (S) -> S) {
        set(transform(snapshot))
    }

    /** 상태를 [next]로 바꾼다. 값이 같으면 아무 일도 하지 않는다. */
    @JsExport.Ignore
    public fun set(next: S) {
        if (next == snapshot) return
        snapshot = next
        // 알리는 도중 구독·해제가 일어나도 이번 알림 대상은 바뀌지 않는다
        subscriptions.forEach { it.listener() }
    }

    // 같은 함수를 두 번 등록해도 따로 해제되도록 등록마다 별도 객체로 감싼다
    private class Subscription(val listener: () -> Unit)
}
