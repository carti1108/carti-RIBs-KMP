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

## 뷰 계약 명명

플랫폼이 구현하는 뷰 계약은 해당 RIB 파일에 두고, RIBs-iOS 규약으로 이름 붙인다([ADR-0006](adr/ADR-0006-swift-bridge-via-view-interfaces.md)).

| 방향 | 이름 | 예 |
|---|---|---|
| 공유 코드 → 뷰 | `XxxPresentable : ViewControllable` | `LoggedOutPresentable.showError(message)` |
| 뷰 → 공유 코드 | `XxxPresentableListener` | `LoggedOutPresentableListener.didTapLogin(rawName)` |
| 루트처럼 자식 화면만 담는 뷰 | `XxxViewControllable` | `RootViewControllable.showScreen(screen)` |

- Android 원본은 Interactor 안에 `interface XxxPresenter`를 두고 View가 구현한다. 이 프레임워크에서는 `Presenter`가 생명주기를 가진 클래스 이름이라서, 같은 이름의 뷰 계약과 헷갈리지 않게 iOS 규약을 쓴다.
- 플랫폼 뷰는 `listener`를 약한 참조로 둔다. 공유 코드와 플랫폼 객체 사이의 순환 참조는 Kotlin/Native GC가 회수하지 못한다.
- 뷰 계약은 메인 스레드에서만 부른다. 프레임워크가 가로챌 수 없는 호출 지점에서는 `checkMainThread()`를 쓴다([ADR-0007](adr/ADR-0007-injected-main-thread-check.md)).

## 라우팅 규약

- 모든 `routeToX()`에는 짝이 되는 `routeAwayFromX()`가 있다. 둘은 같은 소유자(부모 Interactor)가 호출한다.
- 자식이 부모에게 알릴 때는 Listener를 쓴다. 이때 "닫혔다" 같은 상태 통지가 아니라 사용자 의도(`didCompleteX`, `didRequestY`)로 표현한다. 다음 라우팅은 부모가 정한다.
- 의존성은 그것이 필요한 가장 낮은 RIB에서 만든다.
- `!!`는 쓰지 않는다. 계약 위반은 `check`/`require`로 메시지와 함께 실패시킨다.

## 생명주기 순서 (원본과 동일)

attach할 때 (`parent.attachChild(child, tag)`):
1. 자식을 children에 추가
2. `child.dispatchAttach(tag로 찾은 저장 상태, tag)`
3. 자식 Router의 `didLoad()` (처음 한 번만)
4. 자식 Router의 `willAttach()`
5. 자식 Interactor 활성화 → `didBecomeActive(savedState)`

자식의 자식은 여기서 붙지 않는다. 자식 Interactor나 Router가 직접 `attachChild`로 붙인다.

detach할 때 (`parent.detachChild(child)`):
1. 자식을 children에서 제거 (붙어 있지 않았으면 여기서 끝)
2. 자식 Interactor의 `willResignActive()` → 활성 스코프 취소
3. 자식 Router의 `willDetach()`
4. 자식의 자식들을 같은 순서로 detach (위에서 아래로)

모든 생명주기 호출은 단일 스레드에서 한다([ADR-0005](adr/ADR-0005-single-thread-lifecycle.md)).

## 원본과 다른 점

| Android 원본 | 이 프레임워크 | 근거 |
|---|---|---|
| `lifecycle(): Observable<InteractorEvent>` | `lifecycle: SharedFlow<InteractorEvent>` | [ADR-0002](adr/ADR-0002-coroutines-instead-of-rx.md) |
| AutoDispose `ScopeProvider` / `requestScope()` | 활성 구간 `coroutineScope` 취소 | ADR-0002 |
| `Bundle` / `savedInstanceState` | `RibState?`. 값은 String·Long·Double·Boolean·List·RibState만, `encodeToString()`으로 문자열 하나로 저장. iOS는 복원을 연결하지 않음 | [ADR-0008](adr/ADR-0008-ribstate-string-encoding.md) |
| `ViewRouter<V : View, I>` | `ViewRouter<V : ViewControllable, I>` | ADR-0004 |
| Dagger Component, `@Inject` | 수동 `Component<D : Dependency>`, 생성자 주입 | ADR-0003 |
| `RibActivity` / `RibFragment` 진입점 | `LaunchRouter.launch(host)` / `shutdown()` (attach 후 뷰 표시, detach 후 뷰 숨김) | ADR-0004 |
| 루트 뷰 컨테이너(`ViewGroup`, `UIWindow`) | `ViewHost<V>` 인터페이스, 플랫폼 어댑터가 구현 | ADR-0004 |
| (없음) | `Router.mount()` / `ViewRouter.mount(host)` → `RibMount.unmount()`: RIB 없는 호스트에 하위 트리 장착 | 확장 API |
| `LeakDetector`, `RibRefWatcher` | 이번 범위에서 제외 | — |
| 같은 tag 중복 attach 시 경고만 | 같은 **인스턴스** 중복 attach는 실패, 같은 tag는 허용 | 인스턴스 중복은 Interactor 이중 활성화로 이어짐 |
| 붙지 않은 자식 detach 시에도 `dispatchDetach` 호출 | 아무 일도 하지 않음 | 비활성 Interactor의 이중 비활성화 방지 |
| RIB 이벤트 버스(`RibEvents`), breadcrumb 로그 | 아직 없음 | 필요해지면 Flow로 추가 |
| 메인 스레드 검사 | 플랫폼이 주입한 `MainThreadChecker`로 생명주기 진입점에서 검사(기본 꺼짐), 뷰 호출 전에는 `checkMainThread()` | [ADR-0007](adr/ADR-0007-injected-main-thread-check.md) |
| Presenter `lifecycle()` Observable, `requestScope()` | `lifecycleFlow`, 로드 구간 `coroutineScope` | ADR-0002 |
| Presenter 중복 로드·로드 전 언로드에 가드 없음 | 실패시킴 | 로드 구간 스코프가 겹치지 않게 |
| Rx `Worker` + `WorkerBinder` (원본에서 deprecated) | 옮기지 않음. `RibCoroutineWorker` + `CoroutineScope.bind`만 제공 | ADR-0002 |
| `RibCoroutineWorker`의 `RibActionEmitter`(이벤트 보고) | 없음 | RIB 이벤트 버스 미도입 |
