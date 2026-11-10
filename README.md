# carti-RIBs-KMP

Uber RIBs(Router-Interactor-Builder) 아키텍처를 Kotlin Multiplatform 공유 코드로 옮긴 프레임워크입니다.
원본은 [uber/RIBs](https://github.com/uber/RIBs)의 Android 구현이고, 이름과 생명주기 순서를 그대로 따릅니다.

- 비동기: Rx 없이 코루틴/Flow (`rib-coroutines` 방식)
- DI: Dagger 없이 수동 `Component` / `Dependency`
- View: 공유 코드는 불투명한 `ViewControllable`만 알고, 화면은 플랫폼 어댑터가 맡음

## 상태

| 단계 | 내용 | 상태 |
|---|---|---|
| M0 | Gradle KMP 골격, 테스트 실행 | 완료 (JVM, JS) |
| M1 | Interactor, Router, Presenter, ViewRouter, Builder/Component, Worker, LaunchRouter, mount/unmount | 완료 (JVM, JS, Android, iOS, macOS) |
| M2 | xcframework, Swift 뷰 어댑터(ADR-0006), SwiftUI 샘플 | 완료 (iOS 시뮬레이터 UI 테스트로 확인) |
| M3 | `ViewPresenter`·`EmptyPresenter`, 메인 스레드 검사(ADR-0007), 샘플 명명 정리 | 완료 |

## 빠른 시작

```kotlin
class RootBuilder(dependency: EmptyDependency) : Builder<EmptyDependency>(dependency) {
    fun build(): RootRouter {
        val component = RootComponent(dependency)
        return RootRouter(rootView, RootInteractor(), ChildBuilder(component))
    }
}

val root = RootBuilder(EmptyComponent()).build()
root.launch(host)   // host: 플랫폼이 구현한 ViewHost
// ...
root.shutdown()
```

전체 예시는 [SampleRibs.kt](ribs-core/src/commonTest/kotlin/com/carti/ribs/sample/SampleRibs.kt)에 있습니다.

## 빌드와 검증

JDK 17 이상이 필요합니다 (Android Studio 내장 JBR 사용 가능).

```bash
./gradlew :ribs-core:jvmTest :ribs-core:jsTest   # 공통 테스트
./gradlew :ribs-core:check                        # 테스트 + 공개 API 검사 + 공유 코드 규칙 검사
./gradlew :ribs-core:updateKotlinAbi              # 공개 API를 의도적으로 바꿨을 때 덤프 갱신
```

Android 타깃은 SDK 경로가 필요합니다(`local.properties`의 `sdk.dir` 또는 `ANDROID_HOME`).
Apple 타깃은 Windows/Linux에서 건너뛰고, Mac에서 `./gradlew :ribs-core:iosSimulatorArm64Test :ribs-core:macosArm64Test`로 테스트합니다.
Swift용 xcframework는 `./gradlew :ribs-core:assembleRIBsKMPXCFramework`로 만들고, `ribs-core/build/XCFrameworks/{debug,release}/RIBsKMP.xcframework`에 생깁니다(정적 프레임워크, Swift에서 `import RIBsKMP`).

## iOS 샘플 (SwiftUI)

공유 코드의 RIB 트리(Root → LoggedOut ⇄ LoggedIn)를 SwiftUI로 띄우는 샘플입니다. 구조는 ADR-0006을 따릅니다.

| 위치 | 역할 |
|---|---|
| `samples/login-shared` | 샘플 RIB(Kotlin). 뷰 계약과 `LoginApp` 진입점. `ribs-core`를 export한 `LoginShared.xcframework`를 만든다 |
| `swift/RIBsKMPSwiftUI` | Kotlin에 의존하지 않는 Swift 어댑터(`SwiftUIRenderable`, `RIBScreenSlot`, `RIBSlotView`) |
| `samples/ios-login` | Tuist로 만드는 iOS 앱. 뷰 계약을 SwiftUI로 구현하고 XCUITest로 흐름을 검증한다 |

앱에는 Kotlin 프레임워크를 하나만 넣을 수 있습니다. 그래서 앱 전용 Kotlin 모듈이 `ribs-core`를 export하고, Swift 어댑터는 Kotlin 타입을 모르는 채로 다운캐스트로 연결합니다.

```bash
./gradlew :samples:login-shared:assembleLoginSharedDebugXCFramework
cd samples/ios-login && tuist generate
```

Xcode에서 `LoginSample` 스킴을 arm64 시뮬레이터로 실행하거나 테스트합니다. Swift 어댑터 테스트는 `cd swift/RIBsKMPSwiftUI && swift test`로 실행합니다.

## 문서

- [아키텍처 규칙](docs/architecture.md)
- [원본 대비 파일 비교](docs/upstream-comparison.md)
- [ADR 목록](docs/adr/README.md)
- [작업 규칙](AGENTS.md)
