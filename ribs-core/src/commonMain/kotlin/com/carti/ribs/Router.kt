package com.carti.ribs

/**
 * RIB 트리의 노드. Uber RIBs Android `Router<I>`를 따른다.
 *
 * 순서(원본과 같음):
 * - attach: [didLoad](처음 한 번) → [willAttach] → Interactor 활성화
 * - detach: Interactor 비활성화 → [willDetach] → 자식 전체 detach
 *
 * 원본과 다른 점: 같은 인스턴스를 두 번 붙이면 실패하고, 붙어 있지 않은 자식을 떼면 아무 일도 하지 않는다.
 * RIB 이벤트 버스와 누수 감지(RibRefWatcher)는 아직 없다.
 * 생명주기 메서드는 [RibThreadingConfig]에 검사기가 있으면 메인 스레드인지 확인한다(ADR-0007).
 */
public abstract class Router<I : InteractorType>(public open val interactor: I) {

    private val children = mutableListOf<Router<*>>()
    private var isLoaded = false
    private var savedState: RibState? = null

    /** attach될 때 정해지는 식별자. 자식 저장 상태의 키로 쓴다. */
    public var tag: String? = null
        private set

    init {
        attachToInteractor()
    }

    /** Interactor에 이 Router를 연결한다. 원본처럼 하위 클래스에서 바꿀 수 있다. */
    protected open fun attachToInteractor() {
        (interactor as? Interactor<*, *>)?.setRouterInternal(this)
    }

    /** 처음 attach될 때 한 번 호출된다. */
    protected open fun didLoad() {}

    /** attach될 때마다 Interactor 활성화 직전에 호출된다. */
    protected open fun willAttach() {}

    /** detach될 때 Interactor 비활성화 직후, 자식 detach 전에 호출된다. */
    protected open fun willDetach() {}

    public fun getChildren(): List<Router<*>> = children.toList()

    public open fun handleBackPress(): Boolean = interactor.handleBackPress()

    public fun attachChild(childRouter: Router<*>) {
        attachChild(childRouter, defaultTag(childRouter))
    }

    public fun attachChild(childRouter: Router<*>, tag: String) {
        checkMainThread("Router.attachChild")
        check(children.none { it === childRouter }) { "${defaultTag(childRouter)} is already attached" }
        children += childRouter
        childRouter.dispatchAttach(childStateFor(tag), tag)
    }

    public fun detachChild(childRouter: Router<*>) {
        checkMainThread("Router.detachChild")
        val removed = children.remove(childRouter)
        if (!removed) return
        childRouter.dispatchDetach()
    }

    public fun dispatchAttach(savedInstanceState: RibState?) {
        dispatchAttach(savedInstanceState, defaultTag(this))
    }

    public fun dispatchAttach(savedInstanceState: RibState?, tag: String) {
        checkMainThread("Router.dispatchAttach")
        if (!isLoaded) {
            isLoaded = true
            didLoad()
        }
        this.savedState = savedInstanceState
        this.tag = tag
        willAttach()
        asInteractor().dispatchAttach(savedInstanceState?.get(KEY_INTERACTOR) as? RibState)
    }

    public fun dispatchDetach() {
        checkMainThread("Router.dispatchDetach")
        asInteractor().dispatchDetach()
        willDetach()
        children.toList().forEach(::detachChild)
    }

    /** 이 Router와 하위 트리의 상태를 저장한다. */
    public fun saveInstanceStateInternal(): RibState {
        val childStates = children.associate { child ->
            val childTag = checkNotNull(child.tag) { "${defaultTag(child)} has no tag" }
            childTag to child.saveInstanceStateInternal()
        }
        return RibState(
            mapOf(
                KEY_INTERACTOR to asInteractor().saveInstanceState(),
                KEY_CHILD_ROUTERS to RibState(childStates),
            ),
        )
    }

    private fun childStateFor(tag: String): RibState? =
        (savedState?.get(KEY_CHILD_ROUTERS) as? RibState)?.get(tag) as? RibState

    private fun asInteractor(): Interactor<*, *> =
        checkNotNull(interactor as? Interactor<*, *>) {
            "${interactor::class.simpleName} must extend Interactor"
        }

    public companion object {
        public const val KEY_CHILD_ROUTERS: String = "Router.childRouters"
        public const val KEY_INTERACTOR: String = "Router.interactor"

        private fun defaultTag(router: Router<*>): String = router::class.simpleName ?: "Router"
    }
}
