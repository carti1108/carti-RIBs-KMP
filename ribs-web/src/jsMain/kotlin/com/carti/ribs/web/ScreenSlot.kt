@file:OptIn(ExperimentalJsExport::class)

package com.carti.ribs.web

import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewHost

/**
 * React로 그릴 수 있는 RIB 뷰(ADR-0009). [screenKey]로 React가 그릴 컴포넌트를 고른다.
 * 구현은 플랫폼 소스(`jsMain`)의 Kotlin이 맡고, 화면 상태는 보통 [ViewStore]로 내보낸다.
 * `ViewControllable`을 JS로 내보내지 않으므로 이 인터페이스도 내보내지 않는다. React는 [CurrentScreen]으로 받는다.
 */
public interface WebViewControllable : ViewControllable {
    public val screenKey: String
}

/** [ScreenSlot]이 지금 보여 주는 화면. React는 [screenKey]로 컴포넌트를 고르고 [view]를 그 컴포넌트에 넘긴다. */
@JsExport
public class CurrentScreen internal constructor(
    public val screenKey: String,
    /** 앱이 내보낸 뷰 구현 객체. React 쪽에서 해당 화면의 타입으로 쓴다. */
    public val view: Any,
) {
    // 뷰는 equals를 재정의할 수 있으므로 같은 객체인지로만 비교한다
    override fun equals(other: Any?): Boolean = other is CurrentScreen && other.view === view
    override fun hashCode(): Int = view.hashCode()
}

/**
 * 자식 RIB 화면 하나를 담는 자리. Swift `RIBScreenSlot`에 해당한다.
 * Router가 [show]·[hide]를 부르면 구독한 React 슬롯 컴포넌트가 다시 그린다.
 */
@JsExport
public class ScreenSlot : ViewHost<WebViewControllable> {
    private val store = ViewStore<Holder>(Holder(null))

    /** 지금 보이는 화면. 비어 있으면 null. 바뀌기 전까지 같은 객체를 돌려준다. */
    public fun getSnapshot(): CurrentScreen? = store.getSnapshot().screen

    public fun subscribe(listener: () -> Unit): () -> Unit = store.subscribe(listener)

    @JsExport.Ignore
    override fun show(view: WebViewControllable) {
        store.set(Holder(CurrentScreen(view.screenKey, view)))
    }

    /** 지금 보이는 뷰일 때만 비운다. 이미 다른 뷰로 바뀌었다면 무시한다. */
    @JsExport.Ignore
    override fun hide(view: WebViewControllable) {
        if (store.getSnapshot().screen?.view === view) store.set(Holder(null))
    }

    // 내보낸 클래스 안의 data class는 copy()가 내보내기 경고를 내므로 equals만 직접 둔다
    private class Holder(val screen: CurrentScreen?) {
        override fun equals(other: Any?): Boolean = other is Holder && other.screen == screen
        override fun hashCode(): Int = screen?.hashCode() ?: 0
    }
}
