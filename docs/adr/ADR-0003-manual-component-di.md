### ADR-0003 수동 Component/Dependency로 DI한다
- 상태: 채택
- 날짜: 2026-11-09
- 배경: Android 원본은 Dagger로 Builder와 Component를 구성한다. Dagger는 JVM의 어노테이션 처리에 기대고 있어 KMP 공유 코드에서 쓸 수 없다.
- 결정: RIBs-iOS 방식처럼 `Dependency` 인터페이스(부모가 제공해야 하는 것)와 `Component<D : Dependency>`(이 RIB이 만들고 자식에게 넘기는 것)를 손으로 작성한다. 자식의 Dependency는 부모의 Component가 구현한다. 컴포넌트 안 싱글턴은 `shared { }`로 만든다. Builder의 `build(...)`는 런타임 파라미터만 받는다.
- 고려했던 대안:
  (1) Koin 같은 런타임 DI 컨테이너: 코드가 짧다. 하지만 의존성이 빠졌을 때 런타임에야 알 수 있고, RIB 트리의 계층 범위를 표현하기 어려워 기각했다.
  (2) kotlin-inject 같은 KSP 기반 컴파일 타임 DI: Dagger와 가장 비슷하다. 하지만 코드 생성 도구가 필수가 되어 빌드가 복잡해지므로, 코어에서는 기각했다. 앱 쪽에서 원하면 얹어 쓸 수 있다.
- 결과: 의존성 그래프가 컴파일 타임에 타입으로 검사되고, 외부 도구가 필요 없다. 반면 Component마다 위임 코드를 손으로 써야 하므로 보일러플레이트가 늘어난다. 이 비용은 이후 스캐폴딩 도구로 줄인다.
- 관련: ADR-0001
