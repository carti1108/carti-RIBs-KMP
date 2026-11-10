package com.carti.ribs

/**
 * 뷰를 들고 있는 [Presenter]. Uber RIBs Android `ViewPresenter<V : View>`에서 `View`를 [ViewControllable]로 바꾼 것이다.
 */
public abstract class ViewPresenter<V : ViewControllable>(
    /** 이 Presenter가 맡은 뷰. */
    public val view: V,
) : Presenter()

/** 아무 일도 하지 않는 [Presenter]. Uber RIBs `EmptyPresenter`를 따른다. */
public open class EmptyPresenter : Presenter()
