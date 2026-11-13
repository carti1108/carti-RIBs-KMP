# 아키텍처 규칙

이 프레임워크는 Uber RIBs-iOS를 Kotlin Multiplatform 공유 코드로 옮긴 것이다([ADR-0010](adr/ADR-0010-uber-ribs-ios-as-reference.md)). 이름과 생명주기 순서는 원본을 따르고, 공유 코드로 옮길 수 없는 부분만 바꾼다. 파일별 대응은 [upstream-comparison.md](upstream-comparison.md)에 있다.

## 구성 요소의 책임

| 구성 요소 | 하는 일 | 하지 않는 일 |
|---|---|---|
| **Interactor** | 비즈니스 로직, 도메인 상태, 활성 구간 동안의 비동기 작업. 뷰 입력(`XxxPresentableListener`)을 받아 해석하고 `presenter`로 상태를 보낸다. `router`로 라우팅을 요청한다. | 다른 RIB의 Interactor나 자식 Router를 직접 쥐지 않는다. |
| **View** (`XxxPresentable`) | 상태를 그리고 원시 입력만 전달한다. 보통 뷰가 `Presentable`을 직접 구현한다. | 로직을 갖지 않는다. 공유 코드에 UI 툴킷 타입으로 등장하지 않는다([ADR-0004](adr/ADR-0004-opaque-view-controllable.md)). |
| **Presenter** (선택) | 뷰와 Interactor 사이에 변환이 많을 때만 따로 둔다(`Presenter<V>`). 생명주기는 없다. | 비즈니스 결정을 하지 않는다. |
| **Router** | 자식 RIB의 attach/detach와 트리 구조만 다룬다. 만들어질 때 `interactor.router = this`로 자신을 넣는다. | 비즈니스 판단을 하지 않는다. |
| **Builder** | Component를 만들고 Router·Interactor·뷰를 조립한다. 런타임 파라미터는 `build(...)`로 받는다. | 상태를 갖지 않는다. |
| **Component / Dependency** | Dependency는 부모가 제공해야 하는 것, Component는 이 RIB이 만들어 자식에게 넘기는 것이다([ADR-0003](adr/ADR-0003-manual-component-di.md)). | 의존성을 필요 이상으로 위 RIB에서 만들지 않는다. |
| **Worker** | Interactor 활성 구간에 맞춰 도는 작업. `start(interactor)` 뒤 활성일 때만 `didStart`가 불린다. | 뷰나 Router를 직접 다루지 않는다. |

## 뷰 계약 명명

플랫폼이 구현하는 뷰 계약은 해당 RIB 파일에 두고, RIBs-iOS 규약으로 이름 붙인다([ADR-0006](adr/ADR-0006-swift-bridge-via-view-interfaces.md)).

| 방향 | 이름 | 예 |
|---|---|---|
| Interactor → 뷰 | `XxxPresentable : Presentable, ViewControllable` | `LoggedOutPresentable.showError(message)` |
| 뷰 → Interactor | `XxxPresentableListener` | `LoggedOutPresentableListener.didTapLogin(rawName)` |
| Router → 뷰 (자식 화면만 담는 뷰) | `XxxViewControllable` | `RootViewControllable.showScreen(screen)` |

- 원본은 `XxxPresentable`과 `XxxViewControllable`을 나누고 한 뷰가 둘 다 구현한다. 플랫폼 팩토리가 뷰 하나를 돌려주도록 이 프레임워크의 샘플은 한 인터페이스에 합쳐 둔다.
- 플랫폼 뷰는 `listener`를 약한 참조로 둔다. 공유 코드와 플랫폼 객체 사이의 순환 참조는 Kotlin/Native GC가 회수하지 못한다.
- 뷰 계약은 메인 스레드에서만 부른다. 프레임워크가 가로챌 수 없는 호출 지점에서는 `checkMainThread()`를 쓴다([ADR-0007](adr/ADR-0007-injected-main-thread-check.md)). Interactor와 Worker의 `coroutineScope`는 메인 디스패처다.

## 라우팅 규약

- 모든 `routeToX()`에는 짝이 되는 `routeAwayFromX()`가 있다. 둘은 같은 소유자(부모 Interactor)가 호출한다.
- 자식이 부모에게 알릴 때는 Listener를 쓴다. 이때 "닫혔다" 같은 상태 통지가 아니라 사용자 의도(`didCompleteX`, `didRequestY`)로 표현한다. 다음 라우팅은 부모가 정한다.
- 의존성은 그것이 필요한 가장 낮은 RIB에서 만든다.
- `!!`는 쓰지 않는다. 계약 위반은 `check`/`require`로 메시지와 함께 실패시킨다.

## 생명주기 순서 (원본과 동일)

attach할 때 (`parent.attachChild(child)` 또는 `attachChild(child, tag)`):
1. 자식을 children에 추가
2. (확장) tag로 찾은 저장 상태를 자식 Interactor에 넘김
3. 자식 Interactor `activate()` → `didBecomeActive()`
4. 자식의 기존 하위 트리를 위에서 아래로 활성화
5. 자식 Router `load()` → 처음 한 번만 `didLoad()`. 고정 자식은 여기서 붙인다

detach할 때 (`parent.detachChild(child)`):
1. 자식 Interactor `deactivate()` → `willResignActive()` → 활성 스코프 취소
2. 자식의 하위 트리를 위에서 아래로 비활성화. 하위 트리는 떼지 않고 그대로 둔다
3. 자식을 children에서 제거 (붙어 있지 않았으면 아무 일도 없다)

루트 시작(`LaunchRouter.launch(host)`): 뷰 표시 → 활성화 → `load()`.
루트 종료(`shutdown()`, 원본의 `deinit`): 루트 비활성화 → 자식 전체 detach → 뷰 숨김.

모든 생명주기 호출은 단일 스레드에서 한다([ADR-0005](adr/ADR-0005-single-thread-lifecycle.md)).
