### ADR-0010 Uber RIBs-iOS를 원본으로 삼는다
- 상태: 채택
- 날짜: 2026-11-13
- 배경: ADR-0001은 Android 구현을 원본으로 삼았지만, DI(ADR-0003)와 뷰 추상화(ADR-0004)는 이미 RIBs-iOS 방식을 따르고 있다. Android 원본의 Presenter 로드/언로드, Interactor가 Router를 갖는 구조, `Bundle` 기반 attach는 Activity/Dagger에 맞춘 것이라 공유 코드에서는 대체물을 계속 만들어야 했다. RIBs-iOS(`uber/RIBs-iOS`)는 플랫폼 결합이 적어 공유 코드로 옮길 때 바꿀 부분이 더 적다.
- 결정: `uber/RIBs-iOS`의 `RIBs/Classes`를 원본으로 삼는다. 타입·메서드 이름과 생명주기 순서(`activate`/`deactivate`, `Router.load`, attach 시 활성화 후 로드)를 따른다. Rx는 ADR-0002대로 코루틴/Flow로 바꾼다(`isActiveStream`은 `StateFlow`, `disposeOnDeactivate`는 `coroutineScope`). Android 원본에만 있는 기능 중 플랫폼 어댑터가 쓰는 상태 복원(ADR-0008)과 뒤로 가기는 확장으로 남긴다.
- 고려했던 대안:
  (1) ADR-0001 유지: 이미 구현돼 있다. 하지만 Android 전용 구조를 계속 대체해야 하고, DI·뷰는 iOS식이라 두 원본이 섞여 기준이 흐려져 기각했다.
  (2) iOS 이름만 별칭으로 추가: 기존 코드를 깨지 않는다. 하지만 같은 역할의 API가 둘이 되고 공개 배포 전이라 호환을 지킬 이유가 없어 기각했다.
- 결과: 별도 Presenter 생명주기, Interactor의 Router 소유, `dispatchAttach`/`dispatchDetach`가 사라지고 iOS 템플릿과 같은 모양의 RIB을 쓴다. Swift 개발자가 익숙한 이름을 그대로 쓴다. 반면 공개 API가 전부 깨지고, Rx 구독으로 되던 하위 트리 활성 전파는 Router의 attach/detach에서 직접 처리한다. Kotlin에는 `deinit`과 약한 참조가 공통으로 없어서 소멸 시 비활성화, 누수 감지, `ComponentizedBuilder`의 새 인스턴스 검사는 옮기지 않는다. 차이는 [upstream-comparison.md](../upstream-comparison.md)에 남긴다.
- 관련: ADR-0001(대체), ADR-0002, ADR-0003, ADR-0004, ADR-0008
