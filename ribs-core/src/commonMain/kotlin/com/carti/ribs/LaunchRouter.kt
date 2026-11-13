package com.carti.ribs

/**
 * 뷰를 가진 Router. RIBs-iOS `ViewableRouting`에 대응한다. [viewControllable]은 불투명한 뷰다(ADR-0004).
 */
public interface ViewableRouting : Routing {
    public val viewControllable: ViewControllable
}

/** RIBs-iOS `ViewableRouter<InteractorType, ViewControllerType>`를 따른다. 누수 감지는 없다. */
public open class ViewableRouter<I : Interactable, V : ViewControllable>(
    interactor: I,
    public val viewController: V,
) : Router<I>(interactor), ViewableRouting {
    final override val viewControllable: ViewControllable = viewController
}

/** 앱의 루트 RIB을 시작하는 Router. RIBs-iOS `LaunchRouting`에 대응한다. */
public interface LaunchRouting<V : ViewControllable> : ViewableRouting {
    public fun launch(host: ViewHost<V>, savedInstanceState: RibState? = null)
    public fun shutdown()
}

/**
 * RIBs-iOS `LaunchRouter`를 따른다. 원본의 `launch(from: UIWindow)`는 [ViewHost]를 받는다.
 * 순서(원본과 같음): 뷰 표시 → 활성화 → [load].
 *
 * 원본에 없는 확장: Kotlin에는 `deinit`이 없으므로 [shutdown]으로 트리 전체를 비활성화하고 뷰를 숨긴다.
 * 저장 상태를 넘기면 복원한다(ADR-0008).
 */
public open class LaunchRouter<I : Interactable, V : ViewControllable>(
    interactor: I,
    viewController: V,
) : ViewableRouter<I, V>(interactor, viewController), LaunchRouting<V> {

    private var activeMount: RibMount? = null

    final override fun launch(host: ViewHost<V>, savedInstanceState: RibState?) {
        check(activeMount == null) { "${this::class.simpleName} is already launched" }
        activeMount = mount(host, savedInstanceState)
    }

    /** 트리 전체를 비활성화하고 뷰를 숨긴다. 시작 전이거나 이미 끝났으면 아무 일도 하지 않는다. */
    final override fun shutdown() {
        activeMount?.unmount()
        activeMount = null
    }
}
