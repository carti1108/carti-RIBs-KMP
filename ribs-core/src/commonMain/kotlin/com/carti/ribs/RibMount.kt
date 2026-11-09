package com.carti.ribs

/**
 * RIB 트리 바깥의 화면 컨테이너. 플랫폼 어댑터가 구현한다(ADR-0004).
 * 예: Android Activity의 루트 ViewGroup, iOS UIWindow, 웹의 DOM 컨테이너.
 */
public interface ViewHost<in V : ViewControllable> {
    public fun show(view: V)
    public fun hide(view: V)
}

/**
 * RIB이 없는 호스트에 붙인 하위 트리의 핸들. [unmount]하면 하위 트리 전체를 detach한다.
 * 원본 RIBs에는 없는 확장 API다(docs/architecture.md "원본과 다른 점").
 */
public class RibMount internal constructor(private val onUnmount: () -> Unit) {
    public var isMounted: Boolean = true
        private set

    /** 하위 트리를 detach한다. 여러 번 불러도 한 번만 동작한다. */
    public fun unmount() {
        if (!isMounted) return
        isMounted = false
        onUnmount()
    }
}

/** 뷰 없이 이 Router의 하위 트리를 활성화한다. */
public fun Router<*>.mount(savedInstanceState: RibState? = null): RibMount {
    dispatchAttach(savedInstanceState)
    return RibMount(onUnmount = ::dispatchDetach)
}

/**
 * 이 Router의 하위 트리를 활성화한 뒤 뷰를 [host]에 보여 준다. 순서는 Android `RibActivity`와 같다
 * (attach 후 뷰 추가). [RibMount.unmount]는 detach 후 뷰를 숨긴다.
 */
public fun <V : ViewControllable> ViewRouter<V, *>.mount(
    host: ViewHost<V>,
    savedInstanceState: RibState? = null,
): RibMount {
    dispatchAttach(savedInstanceState)
    host.show(view)
    return RibMount(
        onUnmount = {
            dispatchDetach()
            host.hide(view)
        },
    )
}
