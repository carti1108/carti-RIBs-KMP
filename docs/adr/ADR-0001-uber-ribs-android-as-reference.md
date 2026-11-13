### ADR-0001 Uber RIBs Android를 원본으로 삼는다
- 상태: 대체됨 (→ ADR-0010)
- 날짜: 2026-11-09
- 배경: RIBs를 Kotlin Multiplatform으로 새로 구현한다. 기준이 될 원본이 없으면 API 이름과 생명주기 순서가 작업자마다 달라지고, 기존 RIBs 사용자가 익숙하게 쓸 수 없다.
- 결정: `uber/RIBs`의 Android 구현(코어와 `rib-coroutines`)을 원본으로 삼는다. 클래스·메서드 이름과 생명주기 호출 순서를 그대로 따르고, 공유 코드로 옮길 수 없는 부분만 바꾼다. 바꾼 부분은 [architecture.md](../architecture.md)의 "원본과 다른 점" 표에 남긴다.
- 고려했던 대안:
  (1) RIBs-iOS(Swift)를 원본으로 삼기: 수동 DI와 `ViewControllable`은 KMP에 잘 맞는다. 하지만 언어가 달라 시그니처를 옮길 때 해석이 많이 들어가고, 코루틴 연동 선례가 없어 기각했다.
  (2) 원본 없이 RIBs 원칙만 따라 새로 설계하기: 자유도는 높다. 하지만 기존 사용자가 학습한 것을 그대로 쓸 수 없고, 설계 논쟁이 늘어나 기각했다.
- 결과: Android 테스트가 검증하는 동작을 테스트 기준으로 그대로 쓸 수 있다. 반면 Android 전용 개념(Bundle, Activity, Dagger)은 하나씩 대체해야 하고, 그 차이를 계속 관리해야 한다. Interactor는 원본처럼 자기 Router를 가진다.
- 관련: ADR-0002, ADR-0003, ADR-0004
