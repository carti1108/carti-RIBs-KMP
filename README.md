# carti-RIBs-KMP

Uber RIBs(Router-Interactor-Builder) 아키텍처를 Kotlin Multiplatform 공유 코드로 옮긴 프레임워크입니다.
원본은 [uber/RIBs-iOS](https://github.com/uber/RIBs-iOS)이고, 이름과 생명주기 순서를 그대로 따릅니다([ADR-0010](docs/adr/ADR-0010-uber-ribs-ios-as-reference.md)). 처음에는 Android 구현을 원본으로 삼았습니다(ADR-0001, 대체됨).

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
| M4 | Android Compose 샘플, `ribs-compose` 어댑터 모듈, 시스템 뒤로 가기 | 완료 |
| M5 | 비동기 로그인 샘플(로딩·실패·취소, `AuthService`) | 완료 |
| M6 | 할 일 목록·상세 스택, 결과 전달, 세션 Worker, `RibState` 저장·복원(ADR-0008), `newRib` 스캐폴딩 | 완료 |
| - | Android(Compose) 샘플, iOS와 같은 흐름 | 완료 (에뮬레이터 UI 테스트로 확인) |
| M7 | 브라우저 대상, `ribs-web` 어댑터(ADR-0009), React 샘플 | 완료 (Chrome Headless·Vitest로 확인) |
| M8 | 원본을 RIBs-iOS로 전환(ADR-0010): `activate`/`deactivate`, `PresentableInteractor`, `ViewableRouter`, `Router.load`, `Worker`, `ComponentizedBuilder` | 완료 (JVM, JS, Android). iOS 빌드는 Mac 확인 필요 |

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

Xcode에서 `LoginSample` 스킴을 arm64 시뮬레이터로 실행하거나 테스트합니다. 로그인은 약 0.8초 지연되고, 이름을 `fail`로 넣으면 실패 흐름을 볼 수 있습니다. Swift 어댑터 테스트는 `cd swift/RIBsKMPSwiftUI && swift test`로 실행합니다.

## Android 샘플 (Compose)

iOS 샘플과 같은 `login-shared` 트리와 뷰 계약을 Compose로 구현한 앱입니다(`samples/android-login`).

- `ribs-compose` 모듈의 `ComposeRenderable`·`ComposeScreenSlot`·`RibSlot`: iOS의 `SwiftUIRenderable`·`RIBScreenSlot`·`RIBSlotView`에 해당
- `RibBackPressedCallback`: 시스템 뒤로 가기를 루트 `Router.handleBackPress()`로 보내고, 트리가 처리하지 않으면 시스템 기본 동작을 따른다
- 프로세스가 종료됐다 다시 시작되면 `SavedStateHandle`에 넣어 둔 `RibState` 문자열로 트리를 되살린다(ADR-0008). iOS는 RIBs-iOS처럼 복원을 연결하지 않는다
- `LoginViewModel`이 트리를 소유하는 `ViewHost`라서 화면 회전에도 트리가 유지되고, 화면을 떠날 때 `shutdown`한다
- UI 테스트(`LoginFlowTest`)는 iOS `LoginFlowUITests`와 같은 세 흐름을 확인한다

```bash
./gradlew :samples:android-login:installDebug
./gradlew :samples:android-login:connectedDebugAndroidTest   # 에뮬레이터나 기기 필요
```

## 웹 샘플 (React)

같은 `login-shared` 트리를 React로 그리는 샘플입니다. 구조는 ADR-0009를 따릅니다. iOS·Android와 달리 뷰 계약은 Kotlin(`jsMain`)이 구현하고, React는 화면 상태를 구독하고 함수를 부르기만 합니다.

| 위치 | 역할 |
|---|---|
| `ribs-web` | Kotlin/JS 어댑터. `ViewStore`(화면 상태 스냅숏), `ScreenSlot`(자식 화면 자리), `WebViewControllable` |
| `react/ribs-react` | React 어댑터. `useViewStore` 훅과 `RibSlot` 컴포넌트. iOS의 `RIBSlotView`, Android의 `RibSlot`에 해당 |
| `samples/web-login` | 뷰 계약의 웹 구현과 진입점 `WebLoginApp`(Kotlin), `react/`에 Vite 앱과 흐름 테스트 |

- RIB 트리는 React 밖(`main.tsx`)에서 붙인다. StrictMode의 이중 마운트가 attach를 두 번 하지 않는다
- JS로 내보내는 화면 상태에는 `Long` 대신 `Int`, `List` 대신 `Array`를 쓴다

```bash
./gradlew :ribs-web:check                                            # ViewStore·ScreenSlot 테스트(Node, Chrome Headless)
cd samples/web-login/react && npm install && npm run kotlin   # Kotlin 빌드 결과 생성
npm test                                                             # 로그인 → 목록 → 상세 → 로그아웃 흐름(Vitest)
npm run dev
```

브라우저 테스트는 `CHROME_BIN`에 Chrome 경로가 필요합니다.

## 샘플 시나리오

| 시나리오 | 다루는 것 |
|---|---|
| 로그인(LoggedOut) | `PresentableInteractor` 입력 검증, 비동기 요청과 취소, 중복 요청 무시 |
| 할 일 목록 → 상세 | 화면 스택, Builder 런타임 인자, 뒤로 가기(자식부터 처리) |
| 상세에서 완료 표시 | 공유 `TodoStore`(Component `shared`)를 통한 결과 전달, Flow 구독 |
| 세션 시간 표시 | `Worker`, 로그아웃 시 정지, 메인 디스패처 스코프에서 뷰 갱신 |
| 상태 저장·복원 | 로그인 사용자, 열린 상세, 완료 항목을 `RibState`로 저장 |

## RIB 만들기 (스캐폴딩)

```bash
./gradlew newRib -Pname=Profile -Ppackage=com.example.profile -PkotlinDir=shared/src/commonMain/kotlin \
  -PswiftDir=ios/Sources -PswiftModule=Shared -PcomposeDir=android/src/main/kotlin -PcomposePackage=com.example.android
```

공유 코드 RIB(뷰 계약, Dependency, Listener, Builder, Interactor, Router), 테스트, SwiftUI·Compose 화면 뼈대를 만듭니다. 이미 있는 파일은 덮어쓰지 않습니다. Claude Code에서는 `create-rib` 스킬(`.claude/skills/create-rib`)이 이 태스크를 부른 뒤 요구사항에 맞게 내용을 채웁니다.

## 문서

- [아키텍처 규칙](docs/architecture.md)
- [원본 대비 파일 비교](docs/upstream-comparison.md)
- [ADR 목록](docs/adr/README.md)
- [작업 규칙](AGENTS.md)

## 라이선스

carti-RIBs-KMP는 [uber/RIBs](https://github.com/uber/RIBs)(Copyright (c) 2017 Uber Technologies)의 설계와 API를 바탕으로 Kotlin Multiplatform용으로 다시 구현한 파생 프로젝트이며, 원본과 같은 Apache License 2.0으로 배포합니다. 원본과 달라진 점은 [docs/upstream-comparison.md](docs/upstream-comparison.md)에 정리했습니다. 전문은 [LICENSE.txt](LICENSE.txt)를 참고하세요.

carti-RIBs-KMP는 Uber의 공식 프로젝트가 아닙니다.
