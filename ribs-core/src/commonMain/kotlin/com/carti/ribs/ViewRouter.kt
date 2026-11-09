package com.carti.ribs

/**
 * 플랫폼 뷰를 가리키는 불투명한 타입(ADR-0004). 프레임워크는 내용을 모르며,
 * 실제 화면 붙이기와 떼기는 플랫폼 어댑터가 맡는다.
 */
public interface ViewControllable

/**
 * 뷰를 가진 Router. Uber RIBs Android `ViewRouter<V : View, I>`에서 `View`를 [ViewControllable]로 바꾼 것이다.
 */
public abstract class ViewRouter<V : ViewControllable, I : InteractorType>(
    public val view: V,
    interactor: I,
) : Router<I>(interactor)
