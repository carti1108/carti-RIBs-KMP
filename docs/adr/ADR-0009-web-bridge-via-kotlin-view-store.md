### ADR-0009 웹(React) 연동은 Kotlin이 뷰 인터페이스를 구현하고 화면 상태 저장소를 내보내는 방식으로 한다
- 상태: 채택
- 날짜: 2026-11-12
- 배경: 브라우저에서 RIB 트리를 돌리고 화면은 React로 그리려 한다(KeyManage KMP 전환 실험). ADR-0006은 Swift가 뷰 인터페이스(`XxxPresentable`)를 구현하게 했다. 웹에서도 같은 방식이 맞는지, React가 상태를 어떻게 받을지 정해야 한다.
- 결정: 뷰 인터페이스의 구현은 플랫폼 소스(`jsMain`)의 Kotlin이 맡는다. 이 구현은 Presenter의 메서드 호출을 화면 상태 스냅숏으로 바꿔 `ViewStore`에 담고, React는 `useSyncExternalStore(subscribe, getSnapshot)`로 구독만 한다. 사용자 입력은 React가 이 Kotlin 구현의 내보낸 함수를 불러 리스너로 전달한다. 자식 화면은 `ViewHost` 구현인 `ScreenSlot`이 "지금 보이는 뷰"를 스냅숏으로 내보내고, React 슬롯 컴포넌트가 그린다(Swift `RIBScreenSlot`과 같은 역할). 공통 코드와 ADR-0004·0006의 계약은 바꾸지 않는다.
- 고려했던 대안:
  (1) ADR-0006처럼 TypeScript가 뷰 인터페이스를 구현: 플랫폼 간 모양은 같지만, Kotlin/JS에서 내보낸 인터페이스를 TypeScript가 구현하는 기능은 아직 실험 단계다. React 컴포넌트는 함수라 `showLoading(true)` 같은 명령형 호출을 상태로 바꾸는 코드를 화면마다 TypeScript로 써야 해서 기각했다.
  (2) Flow를 JS로 내보내 React가 직접 구독: `@JsExport`로 Flow·suspend를 내보낼 수 없어 콜백 어댑터를 직접 유지보수해야 한다. ADR-0006이 피한 문제와 같아 기각했다.
  (3) Compose for Web으로 뷰까지 Kotlin: 기존 React 화면을 버려야 하고 이 라이브러리의 목표(뷰는 플랫폼 툴킷)와 맞지 않아 기각했다.
- 결과: React 쪽은 이미 쓰는 `useSyncExternalStore` 패턴만 알면 되고, 화면 상태 변환까지 Kotlin에 남는다. 구독은 `ViewStore`가 관리하고 생명주기는 Kotlin 스코프 안에서 끝난다. 반면 화면마다 상태 클래스를 `@JsExport`해야 하고 내보내기 제약(`Long`은 `bigint`, `List`는 `Array`, `suspend` 불가)을 지켜야 한다. Kotlin을 고치면 다시 컴파일해야 Vite가 바뀐 패키지를 본다. React StrictMode의 이중 마운트와 RIB 생명주기가 부딪치지 않도록 루트 마운트는 컴포넌트 밖(진입점)에서 한다.
- 관련: ADR-0004, ADR-0005, ADR-0006
