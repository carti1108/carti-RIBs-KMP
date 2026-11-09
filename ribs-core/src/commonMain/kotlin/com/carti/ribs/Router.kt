package com.carti.ribs

/**
 * RIB 트리의 노드. Uber RIBs Android `Router<I>`를 따른다.
 *
 * 자식 attach/detach는 다음 단계에서 구현한다.
 */
public abstract class Router<I : InteractorType>(public val interactor: I)
