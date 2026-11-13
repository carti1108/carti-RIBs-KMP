# 원본(uber/RIBs-iOS) 대비 파일 비교

기준: [uber/RIBs-iOS](https://github.com/uber/RIBs-iOS) `main` 브랜치, `RIBs/Classes/` (2026-11-13 확인). [ADR-0010](adr/ADR-0010-uber-ribs-ios-as-reference.md).
우리 쪽: `ribs-core/src/commonMain/kotlin/com/carti/ribs/`.

표기: ✅ 같음(이름·순서 유지, Rx→코루틴 치환만) · 🔶 형태 변경 · ⛔ 의도적 제외

## RIBs/Classes

| 원본 파일 | 상태 | 우리 파일 | 비고 |
|---|---|---|---|
| `Interactor.swift` (`InteractorScope`, `Interactable`, `Interactor`) | ✅ | `Interactor.kt` | `activate`/`deactivate`/`didBecomeActive`/`willResignActive`, 중복 호출은 무시. `isActiveStream`은 `StateFlow<Boolean>` |
| `Interactor.swift` `confineTo(_:)` | ✅ | `Interactor.kt` `Flow.confineTo(scope)` | `combineLatest` → `combine`. 활성화 순간 마지막 값을 다시 내보내는 것도 같다 |
| `Interactor.swift` `disposeOnDeactivate` | 🔶 | `Interactor.coroutineScope` | 활성 구간마다 새 스코프, `willResignActive` 뒤 취소(ADR-0002) |
| `PresentableInteractor.swift` | ✅ | `Interactor.kt` | 누수 감지 없음 |
| `Presenter.swift` (`Presentable`, `Presenter`) | ✅ | `Presenter.kt` | |
| `Router.swift` (`RouterLifecycle`, `RouterScope`, `Routing`, `Router`) | 🔶 | `Router.kt` | 순서 동일. `RouterLifecycle.didLoad` → `DID_LOAD`. `children`은 읽기 전용 복사본. 원본은 Interactor 활성 스트림을 구독해 하위 트리를 켜고 끄지만, 우리는 attach/detach/mount에서 직접 한다. 중복 attach는 `assert` 대신 `check` |
| `ViewableRouter.swift` | ✅ | `LaunchRouter.kt` | `V : ViewControllable` 경계로 런타임 캐스트 대신 컴파일 시점에 확인. 누수 감지 없음 |
| `LaunchRouter.swift` | 🔶 | `LaunchRouter.kt` | `launch(from: UIWindow)` → `launch(host: ViewHost<V>)`. 순서 동일(뷰 표시 → 활성화 → load). `deinit` 대신 `shutdown()` |
| `ViewControllable.swift` | 🔶 | `RibMount.kt` | `uiviewController` 없음. 불투명한 표식(ADR-0004) |
| `Builder.swift` (`Buildable`, `Builder`) | ✅ | `Component.kt` | |
| `ComponentizedBuilder.swift` | 🔶 | `Component.kt` | 인자 레이블·반환 타입 오버로드가 없어 `build`/`buildComponentAndRouter`/`buildRouter`로 나눔. 같은 Component 재사용 검사는 약한 참조가 필요해 제외 |
| `MultiStageComponentizedBuilder.swift` | ⛔ | — | 쓰는 곳이 없다. 필요해지면 추가 |
| `DI/Component.swift` | ✅ | `Component.kt` | `shared { }`는 `val x by shared { }`(lazy). 락 없음(ADR-0005) |
| `DI/Dependency.swift` | ✅ | `Component.kt` | `Dependency`, `EmptyDependency`, `EmptyComponent` |
| `Worker/Worker.swift` (`Working`, `Worker`) | 🔶 | `Worker.kt` | `didStart`/`didStop` 순서 동일. `disposeOnStop` → `Worker.coroutineScope`. 활성 스트림은 `MainImmediate`에서 구독. `stop()`이 구독도 끊는다(원본은 `deinit`) |
| `Workflow/Workflow.swift` | ⛔ | — | Rx 기반 단계 실행기. 딥링크 처리 범위 밖 |
| `LeakDetector/*` | ⛔ | — | `deinit`과 약한 참조가 공통 Kotlin에 없음 |
| `Concurrency/*`, `Extensions/*` | ⛔ | — | Swift Concurrency/Rx 보조 |

## 원본에 없는 것

| 파일 | 내용 | 근거 |
|---|---|---|
| `RibState.kt`, `RibStateCodec.kt`, `Router.attachChild(child, tag)`, `Router.saveInstanceState()`, `Interactor.savedInstanceState`/`onSaveInstanceState` | Android 프로세스 재시작 뒤 트리 복원. iOS 샘플은 연결하지 않음 | [ADR-0008](adr/ADR-0008-ribstate-string-encoding.md) |
| `Interactor.handleBackPress()`, `Router.handleBackPress()` | Android 시스템 뒤로 가기 | `ribs-compose` |
| `RibMount.kt`의 `ViewHost`, `mount`, `RibMount` | RIB 없는 호스트에 하위 트리 장착. `unmount`는 원본 `deinit` 순서 | 확장 API |
| `RibDispatchers.kt`, `LifecycleScope.kt` | 코루틴 디스패처 주입과 활성 구간 스코프. Uber `rib-coroutines`의 형태를 따름 | [ADR-0002](adr/ADR-0002-coroutines-instead-of-rx.md) |
| `RibThreading.kt` | 메인 스레드 검사기 주입 | [ADR-0007](adr/ADR-0007-injected-main-thread-check.md) |
| `ribs-web`의 `ViewStore`, `ScreenSlot`, `CurrentScreen` | React가 구독할 화면 상태 저장소와 자식 화면 자리 | [ADR-0009](adr/ADR-0009-web-bridge-via-kotlin-view-store.md) |
