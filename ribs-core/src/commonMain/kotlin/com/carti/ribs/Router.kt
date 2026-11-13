package com.carti.ribs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Router 생명주기 이벤트. RIBs-iOS `RouterLifecycle`에 대응한다. */
public enum class RouterLifecycle { DID_LOAD }

/** RIBs-iOS `RouterScope`. */
public interface RouterScope {
    public val lifecycle: Flow<RouterLifecycle>
}

/**
 * RIB 트리의 노드. RIBs-iOS `Routing`. 테스트에서 가짜 자식 Router를 쓸 수 있도록
 * [Router]가 내부에서 부르는 멤버를 모두 여기에 둔다.
 */
public interface Routing : RouterScope {
    public val interactable: Interactable
    public val children: List<Routing>
    public fun load()
    public fun attachChild(child: Routing)
    public fun detachChild(child: Routing)
}

/**
 * RIB 트리의 노드. RIBs-iOS `Router<InteractorType>`를 따른다.
 *
 * 순서(원본과 같음):
 * - [attachChild]: 자식 Interactor 활성화 → 자식 하위 트리 활성화 → 자식 [load](처음 한 번 [didLoad])
 * - [detachChild]: 자식 Interactor 비활성화 → 자식 하위 트리 비활성화 → 목록에서 제거
 *
 * 떼어 낸 자식의 하위 트리는 원본처럼 그대로 남고 비활성 상태가 된다. 다시 붙이면 함께 활성화된다.
 *
 * 원본과 다른 점: 원본은 Interactor의 활성 스트림을 구독해 하위 트리를 켜고 끄지만, 여기서는
 * attach/detach/mount에서 직접 처리한다. `deinit` 정리와 누수 감지는 없다. 같은 자식을 두 번 붙이면
 * 원본의 `assert` 대신 `check`로 실패한다. 생명주기 진입점은 메인 스레드인지 확인한다(ADR-0007).
 * 상태 복원(ADR-0008)과 [handleBackPress]는 Android 어댑터를 위한 확장이다.
 */
public open class Router<I : Interactable>(public val interactor: I) : Routing {

    final override val interactable: Interactable = interactor

    private val childList = mutableListOf<Routing>()
    private val lifecycleEvents = MutableSharedFlow<RouterLifecycle>(extraBufferCapacity = 1)
    private var didLoadFlag = false
    private var savedState: RibState? = null

    final override val children: List<Routing> get() = childList.toList()

    final override val lifecycle: Flow<RouterLifecycle> = lifecycleEvents.asSharedFlow()

    /** attach될 때 정해지는 식별자. 자식 저장 상태의 키로 쓴다. */
    public var tag: String? = null
        private set

    final override fun load() {
        if (didLoadFlag) return
        didLoadFlag = true
        lifecycleEvents.tryEmit(RouterLifecycle.DID_LOAD)
        didLoad()
    }

    /** 처음 [load]될 때 한 번 호출된다. 고정된 자식은 여기서 붙인다. */
    protected open fun didLoad() {}

    final override fun attachChild(child: Routing) {
        attachChild(child, defaultTag(child))
    }

    /** [tag]를 키로 저장 상태를 복원하며 붙인다. 원본에 없는 확장이다. */
    public fun attachChild(child: Routing, tag: String) {
        checkMainThread("Router.attachChild")
        check(childList.none { it === child }) {
            "Attempt to attach child: ${defaultTag(child)}, which is already attached to ${defaultTag(this)}."
        }
        childList += child
        (child as? Router<*>)?.prepare(childStateFor(tag), tag)
        // 원본과 같이 로드 전에 활성화한다. Router는 보통 didLoad에서 고정 자식을 붙이므로 그 전에 활성이어야 한다.
        child.setSubtreeActive(true)
        child.load()
    }

    final override fun detachChild(child: Routing) {
        checkMainThread("Router.detachChild")
        child.setSubtreeActive(false)
        childList.removeAll { it === child }
    }

    /** 뒤로 가기를 처리했으면 true. 기본은 Interactor에 묻는다. 원본 iOS에는 없다. */
    public open fun handleBackPress(): Boolean = (interactor as? Interactor)?.handleBackPress() ?: false

    /** 이 Router와 하위 트리의 상태를 저장한다. 원본에 없는 확장이다(ADR-0008). */
    public fun saveInstanceState(): RibState {
        val childStates = childList.filterIsInstance<Router<*>>().associate { child ->
            val childTag = checkNotNull(child.tag) { "${defaultTag(child)} has no tag" }
            childTag to child.saveInstanceState()
        }
        return RibState(
            mapOf(
                KEY_INTERACTOR to ((interactor as? Interactor)?.saveInstanceState() ?: RibState()),
                KEY_CHILD_ROUTERS to RibState(childStates),
            ),
        )
    }

    internal fun prepare(state: RibState?, tag: String) {
        savedState = state
        this.tag = tag
        (interactor as? Interactor)?.restore(state?.get(KEY_INTERACTOR) as? RibState)
    }

    private fun childStateFor(tag: String): RibState? =
        (savedState?.get(KEY_CHILD_ROUTERS) as? RibState)?.get(tag) as? RibState

    public companion object {
        public const val KEY_CHILD_ROUTERS: String = "Router.childRouters"
        public const val KEY_INTERACTOR: String = "Router.interactor"

        internal fun defaultTag(routing: Routing): String = routing::class.simpleName ?: "Router"
    }
}

/** 이 노드부터 전위 순회로 Interactor를 켜거나 끈다. 원본 `setSubtreeActive`와 같은 순서다. */
internal fun Routing.setSubtreeActive(active: Boolean) {
    if (active && !interactable.isActive) interactable.activate()
    if (!active && interactable.isActive) interactable.deactivate()
    children.forEach { it.setSubtreeActive(active) }
}
