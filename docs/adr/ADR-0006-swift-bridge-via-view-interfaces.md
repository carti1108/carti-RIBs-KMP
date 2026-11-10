### ADR-0006 Swift 연동은 뷰 인터페이스 호출로 하고 Flow 브리지 도구를 쓰지 않는다
- 상태: 채택
- 날짜: 2026-11-10
- 배경: ADR-0002에서 Rx 대신 코루틴/Flow를 쓰기로 하면서, Swift가 Flow를 받으려면 브리지가 따로 필요하다는 비용을 남겼다. M2(xcframework, SwiftUI 샘플)를 시작하기 전에 Kotlin 상태를 Swift 화면에 어떻게 전달할지 정해야 한다.
- 결정: 공유 코드가 Swift에 Flow나 suspend 함수를 노출하지 않는다. 상태는 공유 코드의 Presenter/Interactor가 Kotlin에 정의한 뷰 인터페이스의 메서드를 호출해 전달하고, 사용자 입력은 Swift가 공유 코드의 리스너 인터페이스를 호출해 전달한다. 뷰 인터페이스의 구현은 플랫폼(SwiftUI/UIKit)이 맡고 DI로 주입한다(ADR-0004의 플랫폼 어댑터).
- 고려했던 대안:
  (1) SKIE(Touchlab의 컴파일러 플러그인): Flow가 `AsyncSequence`로, suspend 함수가 취소를 지원하는 `async`로 바뀌어 Swift 쪽 사용성이 가장 좋다. 하지만 공식 툴체인이 아니고 컴파일러 내부 API에 의존해서 Kotlin 업그레이드가 SKIE 릴리스를 기다려야 한다. 배포하는 xcframework의 Swift API가 SKIE 생성물에 묶이는 것도 문제라 기각했다.
  (2) KMP-NativeCoroutines: 버전 종속은 덜하지만 KSP와 어노테이션, Swift 쪽 래퍼 호출이 필요하다. 외부 의존성이 늘어나는 데 비해 얻는 것이 (1)보다 적어 기각했다.
  (3) `iosMain`에 Flow → 콜백/취소 핸들 어댑터를 직접 작성: 의존성은 없지만 Obj-C 헤더를 거치면서 제네릭 타입이 사라지고, 취소 전파도 직접 유지보수해야 한다. 뷰 인터페이스 방식으로 필요 자체가 없어지므로 지금은 기각했다.
  (4) JetBrains Swift Export: 공식 경로지만 아직 실험 단계이고 Flow/코루틴 지원이 부족해 보류했다.
- 결과: 외부 의존성과 Kotlin 버전 종속이 없고, 원본 RIBs의 Presenter → View 패턴을 그대로 따른다. 생명주기와 구독은 Kotlin의 활성 구간 스코프 안에서 끝나므로 Swift 쪽에 구독 누수가 생기지 않는다. 반면 화면마다 뷰 인터페이스 메서드를 정의해야 해서 코드가 늘고, SwiftUI에서 상태를 `for await`로 직접 구독하는 방식은 쓸 수 없다. Swift가 Flow를 직접 받아야 하는 곳이 생기면 새 ADR로 다시 정한다.
- 관련: ADR-0002, ADR-0004
