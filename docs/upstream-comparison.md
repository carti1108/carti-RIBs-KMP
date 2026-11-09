# 원본(uber/RIBs) 대비 파일 비교

기준: [uber/RIBs](https://github.com/uber/RIBs) `main` 브랜치, `libraries/` 아래 Android 모듈 (2026-11-09 확인).
우리 쪽: `ribs-core/src/commonMain/kotlin/com/carti/ribs/`.

표기: ✅ 같음(이름·순서 유지, Rx→코루틴 치환만) · 🔶 형태 변경 · ⛔ 의도적 제외 · ⏳ 아직 없음

## rib-base

| 원본 파일 | 상태 | 우리 파일 | 비고 |
|---|---|---|---|
| `Interactor.kt` | ✅ | `Interactor.kt` | attach: ACTIVE → presenter 로드 → `didBecomeActive`, detach: presenter 언로드 → `willResignActive` → INACTIVE. presenter는 생성자 주입만(원본의 `@Inject` 필드 주입 없음) |
| `InteractorType.kt` | ✅ | `Interactor.kt` 안 | Router 제네릭 경계용 |
| `lifecycle/InteractorEvent.kt` | ✅ | `Interactor.kt` 안 | ACTIVE / INACTIVE |
| `Router.kt` | ✅ | `Router.kt` | 순서 동일. 차이: 같은 인스턴스 중복 attach 실패, 붙지 않은 자식 detach 무시, 이벤트·누수 감지·메인 스레드 검사 없음 |
| `Presenter.kt` | ✅ | `Presenter.kt` | `lifecycleFlow`, `isLoaded`, 로드/언로드 순서 동일. `requestScope()` 대신 `coroutineScope`. 중복 로드 가드 추가 |
| `lifecycle/PresenterEvent.kt` | ✅ | `Presenter.kt` 안 | LOADED / UNLOADED |
| `Builder.kt` | 🔶 | `Component.kt` 안 `Builder<D>` | 원본은 Dagger Component를 받음. 우리는 `Dependency`를 받음 |
| `Bundle.kt` | 🔶 | `RibState.kt` | 읽기 전용 Map 래퍼. 직렬화 미정 |
| `RibCoroutineWorker.kt` | ✅ | `RibCoroutineWorker.kt` | `onStart(scope)`, `onStop(cause)`, `bind`, `BindWorkerHandle.unbind()`. `asWorker`/`asRibCoroutineWorker` 변환은 Rx Worker가 없어 생략 |
| `Worker.kt`, `WorkerBinder.kt`, `WorkerUnbinder.kt`, `WorkerScopeProvider.kt`, `lifecycle/WorkerEvent.kt` | ⛔ | — | 원본에서 deprecated, Rx 기반 |
| `BasicInteractor.kt` | ⏳ | — | presenter 없는 Interactor 편의 클래스. 지금은 `Interactor<Unit, R>`로 대체 가능 |
| `EmptyPresenter.kt` | ⏳ | — | 빈 Presenter. 추가 비용 작음 |
| `BasicRouter.kt` | ⏳ | — | 원본에서 deprecated 표시된 편의 클래스. 필요성 낮음 |
| `InteractorBaseComponent.kt`, `InteractorComponent.kt`, `InteractorModule.kt`, `InteractorAndViewModule.kt` | ⛔ | — | Dagger 전용(ADR-0003) |
| `Rib.kt`, `RibBuilder.kt`, `RibInteractor.kt` | ⛔ | — | Dagger/어노테이션 기반 코드 생성 보조 |
| `Initializer.kt` | ⛔ | — | 어노테이션(`@Initializer`) |
| `LazyBackingProperty.kt` | ⛔ | — | Java 상호운용용 지연 프로퍼티. Kotlin `lazy`로 충분 |
| `FlowAsScope.kt` | ⛔ | — | Flow를 AutoDispose ScopeProvider로 바꾸는 Rx 브리지 |
| `RibEvents.kt`, `RibEventType.kt`, `RibRouterEvent.kt` | ⏳ | — | 전역 RIB 이벤트 버스. 디버깅·분석용. 필요해지면 Flow로 추가 |
| `RibRefWatcher.kt` | ⛔ | — | 누수 감지. 이번 범위 제외 |

## rib-coroutines

| 원본 파일 | 상태 | 우리 파일 | 비고 |
|---|---|---|---|
| `RibDispatchers.kt` | ✅ | `RibDispatchers.kt` | `Main`, `Default`, `Unconfined`. 원본의 `IO`는 JS에 없어서 뺌. `Main.immediate` 미사용(아래 차이 참고) |
| `RibCoroutinesConfig.kt` | 🔶 | `RibDispatchers.kt` 안 | 디스패처 교체만 있음. 원본의 전역 예외 핸들러, `shouldCoroutineScopeFailSilentlyOnLifecycleEnded` 없음 |
| `RibCoroutineScopes.kt` | 🔶 | `Interactor.coroutineScope`, `Presenter.coroutineScope` | 원본은 `ScopeProvider` 확장 + WeakHashMap 캐시. 우리는 클래스 멤버로 활성 구간마다 새 스코프. `Application.coroutineScope`는 Android 전용이라 없음 |

## rib-android / rib-android-core

| 원본 파일 | 상태 | 우리 파일 | 비고 |
|---|---|---|---|
| `ViewRouter.kt` | 🔶 | `ViewRouter.kt` | `V : View` → `V : ViewControllable` |
| `RibActivity.kt` | 🔶 | `LaunchRouter.kt` + `RibMount.kt` | attach 후 뷰 표시, detach 후 뷰 숨김. 루트 컨테이너는 `ViewHost<V>` |
| `ViewBuilder.kt` | ⏳ | — | 뷰를 만드는 Builder. 플랫폼 어댑터 단계에서 결정 |
| `ViewPresenter.kt` | ⏳ | — | 뷰를 들고 있는 Presenter |
| `BasicViewRouter.kt` | ⏳ | — | `BasicRouter`의 뷰 버전 |
| `PredictiveBackHandler.kt` | ⏳ | — | 네비게이션 이벤트 브리지 단계 |
| `ActivityContext.kt`, `ActivityStarter.kt`, `IntentCreator*.kt`, `IntentFactory.kt`, `RxActivityEvents.kt`, `RibDebugOverlay.kt`, `XRay.kt` | ⛔ | — | Android 전용 |
| rib-android-core 전체 | ⛔ | — | Android Activity 위임 |

## 우리에게만 있는 것

| 파일 | 내용 |
|---|---|
| `Component.kt`의 `Dependency`, `EmptyDependency`, `EmptyComponent`, `Component.shared` | RIBs-iOS 방식 수동 DI(ADR-0003) |
| `RibMount.kt`의 `ViewHost`, `mount`, `RibMount` | RIB 없는 호스트에 하위 트리 장착 |

## 확인된 미세한 차이 (후속 검토 대상)

1. **`Main.immediate`**: 원본 `coroutineScope`는 `RibDispatchers.Main.immediate`를 쓴다. 우리는 `Main`을 쓰므로, `didBecomeActive`에서 `launch`한 코드가 바로 실행되지 않고 한 번 디스패치된다. `RibDispatchersProvider`에 `MainImmediate`를 추가해 맞추는 것이 좋다.
2. **스코프 이름·예외 핸들러**: 원본은 스코프에 `CoroutineName(클래스명)`과 전역 예외 핸들러를 붙인다. 우리는 둘 다 없다.
3. **비활성 후 `coroutineScope` 접근**: 원본은 `LifecycleEndedException`(설정에 따라 이미 취소된 스코프 반환). 우리는 `IllegalStateException`.
