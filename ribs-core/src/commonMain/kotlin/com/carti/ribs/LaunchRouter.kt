package com.carti.ribs

/**
 * 앱의 루트 RIB을 시작하고 끝내는 Router.
 *
 * Android 원본은 `RibActivity`가 루트 Router를 attach하고 뷰를 붙인다. Activity는 공유 코드에 둘 수 없으므로
 * 그 역할을 RIBs-iOS `LaunchRouter.launch(from:)` 형태로 옮겼다. [launch]는 Activity `onCreate`,
 * [shutdown]은 `onDestroy`에 해당한다.
 */
public abstract class LaunchRouter<V : ViewControllable, I : InteractorType>(
    view: V,
    interactor: I,
) : ViewRouter<V, I>(view, interactor) {

    private var activeMount: RibMount? = null

    /** 루트 트리를 활성화하고 뷰를 [host]에 보여 준다. */
    public fun launch(host: ViewHost<V>, savedInstanceState: RibState? = null) {
        check(activeMount == null) { "${this::class.simpleName} is already launched" }
        activeMount = mount(host, savedInstanceState)
    }

    /** 트리 전체를 detach하고 뷰를 숨긴다. 시작 전이거나 이미 끝났으면 아무 일도 하지 않는다. */
    public fun shutdown() {
        activeMount?.unmount()
        activeMount = null
    }
}
