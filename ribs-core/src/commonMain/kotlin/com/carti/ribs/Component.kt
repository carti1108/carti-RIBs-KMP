package com.carti.ribs

/**
 * 부모 RIB이 제공해야 하는 의존성의 표식(ADR-0003). 각 RIB은 이 인터페이스를 상속해
 * 필요한 것만 선언하고, 부모의 [Component]가 그것을 구현한다.
 */
public interface Dependency

/** 아무것도 필요 없는 RIB의 Dependency. RIBs-iOS `EmptyDependency`에 대응한다. */
public interface EmptyDependency : Dependency

/** 루트 RIB에 넘기는 빈 Component. RIBs-iOS `EmptyComponent`에 대응한다. */
public class EmptyComponent : EmptyDependency

/**
 * RIB이 만들고 자식에게 넘기는 의존성 묶음. Dagger Component를 손으로 쓴 형태다(ADR-0003).
 *
 * 자식 RIB의 Dependency 인터페이스를 구현해 자식 Builder에 자신을 넘긴다.
 * Component 안에서 한 번만 만들 객체는 [shared]로 선언한다.
 */
public open class Component<D : Dependency>(public val dependency: D) {

    /**
     * 이 Component 인스턴스 안에서 한 번만 만들어지는 값. 처음 접근할 때 만든다.
     * RIBs-iOS `shared { }`에 대응하며, Kotlin에서는 `val x by shared { ... }`로 쓴다.
     */
    protected fun <T> shared(factory: () -> T): Lazy<T> = lazy(factory)
}

/**
 * Component를 만들고 Router·Interactor·Presenter를 조립하는 구성 요소.
 * Uber RIBs Android `Builder<Dependency>`를 따른다.
 *
 * 하위 클래스는 `build(...)`를 정의한다. 런타임 파라미터(리스너, 화면 인자)는 `build`의 인자로 받고,
 * 오래 사는 의존성은 [dependency]에서 꺼낸다.
 */
public abstract class Builder<D : Dependency>(protected val dependency: D)
