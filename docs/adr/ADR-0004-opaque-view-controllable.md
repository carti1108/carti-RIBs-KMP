### ADR-0004 공유 코드는 불투명한 ViewControllable만 안다
- 상태: 채택
- 날짜: 2026-11-09
- 배경: Android 원본의 `ViewRouter`는 `android.view.View`를 직접 다룬다. 공유 코드에 플랫폼 UI 타입이 들어오면 다른 타깃에서 컴파일되지 않는다. 화면 툴킷도 Compose, SwiftUI/UIKit, 웹으로 서로 다르다.
- 결정: 프레임워크는 내용이 비어 있는 `ViewControllable` 인터페이스만 안다. `ViewRouter<V : ViewControllable, I>`는 이 타입으로 뷰를 들고, 실제 화면 붙이기와 떼기는 플랫폼별 어댑터가 맡는다. `expect/actual`은 쓰지 않는다.
- 고려했던 대안:
  (1) `expect class View` + 플랫폼별 `actual`: 원본 시그니처에 가깝다. 하지만 플랫폼마다 한 가지 UI 타입만 고를 수 있어서, 예를 들어 iOS에서 UIKit과 SwiftUI를 함께 지원하지 못한다. 그래서 기각했다.
  (2) Compose Multiplatform을 공통 UI로 고정: 공유 범위가 가장 넓다. 하지만 SwiftUI/UIKit 네이티브 화면을 쓰려는 목적과 맞지 않아 기각했다.
- 결과: 코어는 UI 툴킷과 무관하게 모든 타깃에서 같은 방식으로 테스트된다. 반면 플랫폼마다 뷰 브리지 어댑터를 따로 만들어야 하고, 타입 안정성은 어댑터 쪽에서 캐스팅으로 확보해야 한다.
- 관련: ADR-0001
