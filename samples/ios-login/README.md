# iOS 샘플 (SwiftUI)

공유 코드의 RIB 트리(Root → LoggedOut ⇄ LoggedIn)를 SwiftUI로 띄우는 샘플입니다. 구조는 ADR-0006을 따릅니다.

| 위치 | 역할 |
|---|---|
| `samples/login-shared` | 샘플 RIB(Kotlin). 뷰 계약과 `LoginApp` 진입점. `ribs-core`를 export한 `LoginShared.xcframework`를 만든다 |
| `swift/RIBsKMPSwiftUI` | Kotlin에 의존하지 않는 Swift 어댑터(`SwiftUIRenderable`, `RIBScreenSlot`, `RIBSlotView`) |
| `samples/ios-login` | Tuist로 만드는 iOS 앱. 뷰 계약을 SwiftUI로 구현하고 XCUITest로 흐름을 검증한다 |

앱에는 Kotlin 프레임워크를 하나만 넣을 수 있습니다. 그래서 앱 전용 Kotlin 모듈이 `ribs-core`를 export하고, Swift 어댑터는 Kotlin 타입을 모르는 채로 다운캐스트로 연결합니다.

## 실행

Mac에서만 가능합니다.

```bash
./gradlew :samples:login-shared:assembleLoginSharedDebugXCFramework
cd samples/ios-login && tuist generate
```

Xcode에서 `LoginSample` 스킴을 arm64 시뮬레이터로 실행하거나 테스트합니다. 로그인은 약 0.8초 지연되고, 이름을 `fail`로 넣으면 실패 흐름을 볼 수 있습니다. Swift 어댑터 테스트는 `cd swift/RIBsKMPSwiftUI && swift test`로 실행합니다.

`ribs-core`만 xcframework로 쓰려면 `./gradlew :ribs-core:assembleRIBsKMPXCFramework`를 실행합니다. `ribs-core/build/XCFrameworks/{debug,release}/RIBsKMP.xcframework`에 정적 프레임워크가 생기고, Swift에서 `import RIBsKMP`로 씁니다. Apple 타깃 테스트는 `./gradlew :ribs-core:iosSimulatorArm64Test :ribs-core:macosArm64Test`입니다.

## 샘플 시나리오

| 시나리오 | 다루는 것 |
|---|---|
| 로그인(LoggedOut) | `PresentableInteractor` 입력 검증, 비동기 요청과 취소, 중복 요청 무시 |
| 할 일 목록 → 상세 | 화면 스택, Builder 런타임 인자, 뒤로 가기(자식부터 처리) |
| 상세에서 완료 표시 | 공유 `TodoStore`(Component `shared`)를 통한 결과 전달, Flow 구독 |
| 세션 시간 표시 | `Worker`, 로그아웃 시 정지, 메인 디스패처 스코프에서 뷰 갱신 |
| 상태 저장·복원 | 로그인 사용자, 열린 상세, 완료 항목을 `RibState`로 저장(Android만 연결) |

세 플랫폼 샘플이 같은 `login-shared` 트리를 씁니다.
