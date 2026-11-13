package com.carti.ribs

/** Interactor가 상태를 보내는 대상의 표식. RIBs-iOS `Presentable`에 대응한다. */
public interface Presentable

/**
 * 뷰와 Interactor 사이에 따로 둔 Presenter. RIBs-iOS `Presenter<ViewControllerType>`를 따른다.
 * 보통은 뷰가 [Presentable]을 직접 구현하므로 필요할 때만 쓴다. 생명주기는 없다.
 */
public open class Presenter<V>(public val viewController: V) : Presentable
