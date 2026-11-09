### ADR-0002 Rx 대신 코루틴/Flow를 쓴다
- 상태: 채택
- 날짜: 2026-11-09
- 배경: Android 원본은 RxJava와 AutoDispose로 생명주기 이벤트를 내보내고 구독을 정리한다. RxJava는 JVM 전용이라 KMP 공유 코드에서 쓸 수 없다.
- 결정: 생명주기 이벤트는 `Flow`(`SharedFlow`/`StateFlow`)로 내보내고, 구독 정리는 Interactor 활성 구간에 묶인 `CoroutineScope` 취소로 한다. 형태는 원본의 `rib-coroutines` 모듈(`coroutineScope`, `RibDispatchers`, 코루틴 Worker)을 따른다. 디스패처는 `RibDispatchers`로 주입한다.
- 고려했던 대안:
  (1) Reaktive 같은 KMP용 Rx 구현: 원본 API와 가장 비슷하다. 하지만 외부 의존성이 생기고, Swift 연동 생태계가 코루틴보다 약해 기각했다.
  (2) 콜백 리스너만 쓰기: 의존성이 없다. 하지만 취소와 구조적 동시성을 직접 구현해야 해서 기각했다.
- 결과: 별도 Rx 의존성 없이 모든 타깃에서 같은 방식으로 동작하고, Uber가 이미 만든 코루틴 API 형태를 따를 수 있다. 반면 `Observable` 기반 원본 API(`lifecycle()`, `requestScope()`)는 이름은 유지하되 시그니처가 바뀐다. Swift에서 Flow를 받는 브리지는 따로 필요하다.
- 관련: ADR-0001, ADR-0005
