package com.carti.ribs

/**
 * 플랫폼 뷰를 가리키는 불투명한 타입(ADR-0004). RIBs-iOS `ViewControllable`에 대응하며,
 * 프레임워크는 내용을 모른다. 실제 화면 붙이기와 떼기는 플랫폼 어댑터가 맡는다.
 */
public interface ViewControllable

/**
 * RIB 트리 바깥의 화면 컨테이너. 플랫폼 어댑터가 구현한다(ADR-0004).
 * 예: Android Activity의 루트 ViewGroup, iOS UIWindow, 웹의 DOM 컨테이너.
 */
public interface ViewHost<in V : ViewControllable> {
    public fun show(view: V)
    public fun hide(view: V)
}

/**
 * RIB이 없는 호스트에 붙인 하위 트리의 핸들. [unmount]하면 하위 트리 전체를 비활성화한다.
 * 원본 RIBs에는 없는 확장 API다(docs/upstream-comparison.md).
 */
public class RibMount internal constructor(private val onUnmount: () -> Unit) {
    public var isMounted: Boolean = true
        private set

    /** 하위 트리를 비활성화한다. 여러 번 불러도 한 번만 동작한다. */
    public fun unmount() {
        if (!isMounted) return
        onUnmount()
        isMounted = false
    }
}

/** 뷰 없이 이 Router의 하위 트리를 활성화하고 로드한다. */
public fun Router<*>.mount(savedInstanceState: RibState? = null): RibMount {
    activateAsRoot(savedInstanceState)
    return RibMount(onUnmount = ::deactivateAsRoot)
}

/**
 * 뷰를 [host]에 보여 준 뒤 하위 트리를 활성화한다. 순서는 RIBs-iOS `LaunchRouter.launch`와 같다
 * (뷰 표시 → 활성화 → 로드). [RibMount.unmount]는 비활성화 후 뷰를 숨긴다.
 */
public fun <V : ViewControllable> ViewableRouter<*, V>.mount(
    host: ViewHost<V>,
    savedInstanceState: RibState? = null,
): RibMount {
    checkMainThread("Router.mount")
    check(!interactable.isActive) { "${this::class.simpleName} is already active" }
    host.show(viewController)
    activateAsRoot(savedInstanceState)
    return RibMount(
        onUnmount = {
            deactivateAsRoot()
            host.hide(viewController)
        },
    )
}

private fun Router<*>.activateAsRoot(savedInstanceState: RibState?) {
    checkMainThread("Router.mount")
    check(!interactable.isActive) { "${this::class.simpleName} is already active" }
    prepare(savedInstanceState, tag ?: Router.defaultTag(this))
    setSubtreeActive(true)
    load()
}

/** RIBs-iOS `Router.deinit`과 같은 순서: 자기 Interactor 비활성화 → 자식 전체 detach. */
private fun Router<*>.deactivateAsRoot() {
    checkMainThread("Router.unmount")
    interactable.deactivate()
    children.forEach(::detachChild)
}
