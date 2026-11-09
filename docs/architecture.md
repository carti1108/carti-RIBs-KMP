# 아키텍처 규칙

이 프레임워크는 Uber RIBs Android를 Kotlin Multiplatform 공유 코드로 옮긴 것이다([ADR-0001](adr/ADR-0001-uber-ribs-android-as-reference.md)). 이름과 생명주기 순서는 원본을 따르고, 공유 코드로 옮길 수 없는 부분만 바꾼다.

## 구성 요소의 책임

| 구성 요소 | 하는 일 | 하지 않는 일 |
|---|---|---|
| **Interactor** | 비즈니스 로직, 도메인 상태, 활성 구간 동안의 비동기 작업. 자기 Router를 통해 라우팅을 요청한다. | ViewModel을 알지 않는다. 다른 RIB의 Interactor나 자식 Router를 직접 쥐지 않는다. |
| **Presenter** | 도메인 상태를 ViewModel로 바꾸고 포맷팅한다. 뷰 입력(`did…`)을 사용자 의도로 해석해 Interactor에 전달한다. | ViewModel을 Interactor로 넘기지 않는다. 비즈니스 결정을 하지 않는다. |
| **View** | ViewModel을 그리고 원시 입력만 전달한다. | 로직을 갖지 않는다. 공유 코드에 UI 툴킷 타입으로 등장하지 않는다([ADR-0004](adr/ADR-0004-opaque-view-controllable.md)). |
| **Router** | 자식 RIB의 attach/detach와 트리 구조만 다룬다. | 비즈니스 판단을 하지 않는다. |
| **Builder** | Component를 만들고 Router·Interactor·Presenter를 조립한다. 런타임 파라미터는 `build(...)`로 받는다. | 상태를 갖지 않는다. |
| **Component / Dependency** | Dependency는 부모가 제공해야 하는 것, Component는 이 RIB이 만들어 자식에게 넘기는 것이다([ADR-0003](adr/ADR-0003-manual-component-di.md)). | 의존성을 필요 이상으로 위 RIB에서 만들지 않는다. |

## 라우팅 규약

- 모든 `routeToX()`에는 짝이 되는 `routeAwayFromX()`가 있다. 둘은 같은 소유자(부모 Interactor)가 호출한다.
- 자식이 부모에게 알릴 때는 Listener를 쓴다. 이때 "닫혔다" 같은 상태 통지가 아니라 사용자 의도(`didCompleteX`, `didRequestY`)로 표현한다. 다음 라우팅은 부모가 정한다.
- 의존성은 그것이 필요한 가장 낮은 RIB에서 만든다.
- `!!`는 쓰지 않는다. 계약 위반은 `check`/`require`로 메시지와 함께 실패시킨다.

## 생명주기 순서 (원본과 동일)

attach할 때:
1. 부모 Router의 `attachChild(child)`
2. `child.dispatchAttach(savedState)`
3. 자식 Interactor 활성화 → `didBecomeActive(savedState)`
4. 자식 Router의 `didLoad()`

detach할 때:
1. 부모 Router의 `detachChild(child)`
2. 자식의 하위 트리를 아래쪽부터 detach
3. 자식 Interactor의 `willResignActive()` → 활성 스코프 취소
4. `child.dispatchDetach()`

모든 생명주기 호출은 단일 스레드에서 한다([ADR-0005](adr/ADR-0005-single-thread-lifecycle.md)).

## 원본과 다른 점

| Android 원본 | 이 프레임워크 | 근거 |
|---|---|---|
| `lifecycle(): Observable<InteractorEvent>` | `lifecycle: SharedFlow<InteractorEvent>` | [ADR-0002](adr/ADR-0002-coroutines-instead-of-rx.md) |
| AutoDispose `ScopeProvider` / `requestScope()` | 활성 구간 `coroutineScope` 취소 | ADR-0002 |
| `Bundle` / `savedInstanceState` | `RibState?` (공통 상태 타입, 형태는 별도 결정) | ADR-0001 |
| `ViewRouter<V : View, I>` | `ViewRouter<V : ViewControllable, I>` | ADR-0004 |
| Dagger Component, `@Inject` | 수동 `Component<D : Dependency>`, 생성자 주입 | ADR-0003 |
| `RibActivity` / `RibFragment` 진입점 | `LaunchRouter.launch(host)` | ADR-0004 |
| (없음) | `mount()` / `unmount()`: RIB 없는 호스트에 하위 트리 장착 | 확장 API |
| `LeakDetector` | 이번 범위에서 제외 | — |
