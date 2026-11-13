package com.carti.ribs

/** 부모 RIB이 제공해야 하는 의존성의 표식(ADR-0003). RIBs-iOS `Dependency`에 대응한다. */
public interface Dependency

/** 아무것도 필요 없는 RIB의 Dependency. RIBs-iOS `EmptyDependency`에 대응한다. */
public interface EmptyDependency : Dependency

/** 루트 RIB에 넘기는 빈 Component. RIBs-iOS `EmptyComponent`에 대응한다. */
public open class EmptyComponent : EmptyDependency

/**
 * RIB이 만들고 자식에게 넘기는 의존성 묶음. RIBs-iOS `Component<DependencyType>`를 따른다(ADR-0003).
 *
 * 자식 RIB의 Dependency 인터페이스를 구현해 자식 Builder에 자신을 넘긴다.
 * Component 안에서 한 번만 만들 객체는 [shared]로 선언한다.
 */
public open class Component<D>(public val dependency: D) : Dependency {

    /**
     * 이 Component 인스턴스 안에서 한 번만 만들어지는 값. 처음 접근할 때 만든다.
     * RIBs-iOS `shared { }`에 대응하며, Kotlin에서는 `val x by shared { ... }`로 쓴다.
     */
    protected fun <T> shared(factory: () -> T): Lazy<T> = lazy(factory)
}

/** RIBs-iOS `Buildable`. 각 RIB의 `XxxBuildable` 인터페이스가 상속한다. */
public interface Buildable

/**
 * Component를 만들고 Router·Interactor·뷰를 조립하는 구성 요소. RIBs-iOS `Builder<DependencyType>`를 따른다.
 *
 * 하위 클래스는 `build(...)`를 정의한다. 런타임 파라미터(리스너, 화면 인자)는 `build`의 인자로 받고,
 * 오래 사는 의존성은 [dependency]에서 꺼낸다.
 */
public abstract class Builder<D>(public val dependency: D) : Buildable

/**
 * 빌드할 때마다 새 Component를 만드는 Builder. RIBs-iOS `ComponentizedBuilder`를 따른다.
 *
 * Kotlin은 반환 타입만 다른 오버로드와 인자 레이블이 없어 원본 `build(withDynamicBuildDependency:...)`의
 * 두 형태를 [build]와 [buildComponentAndRouter]로, 재정의 지점 `build(with:_:)`를 [buildRouter]로 나눴다.
 * 원본의 "같은 Component 재사용" 검사는 약한 참조가 필요해 옮기지 않았다.
 */
public abstract class ComponentizedBuilder<C, R, DB, DC>(
    private val componentBuilder: (DC) -> C,
) : Buildable {

    public fun build(dynamicBuildDependency: DB, dynamicComponentDependency: DC): R =
        buildComponentAndRouter(dynamicBuildDependency, dynamicComponentDependency).second

    public fun buildComponentAndRouter(dynamicBuildDependency: DB, dynamicComponentDependency: DC): Pair<C, R> {
        val component = componentBuilder(dynamicComponentDependency)
        return component to buildRouter(component, dynamicBuildDependency)
    }

    protected abstract fun buildRouter(component: C, dynamicBuildDependency: DB): R
}

/** 동적 의존성이 없는 [ComponentizedBuilder]. RIBs-iOS `SimpleComponentizedBuilder`를 따른다. */
public abstract class SimpleComponentizedBuilder<C, R>(
    componentBuilder: () -> C,
) : ComponentizedBuilder<C, R, Unit, Unit>({ componentBuilder() }) {

    public fun build(): R = build(Unit, Unit)

    final override fun buildRouter(component: C, dynamicBuildDependency: Unit): R = buildRouter(component)

    protected abstract fun buildRouter(component: C): R
}
