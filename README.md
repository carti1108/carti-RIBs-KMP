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
| M1 | Interactor, Router, Presenter, ViewRouter, Builder/Component, Worker, LaunchRouter, mount/unmount | 완료 (JVM, JS, Android). iOS/macOS는 CI 또는 Mac에서 확인 필요 |
| M2 | xcframework, Flow → Swift 브리지, SwiftUI 샘플 | 예정 (Mac 필요) |

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
Apple 타깃은 Windows/Linux에서 건너뛰고, CI의 macOS 잡에서 테스트합니다.

## 문서

- [아키텍처 규칙](docs/architecture.md)
- [원본 대비 파일 비교](docs/upstream-comparison.md)
- [ADR 목록](docs/adr/README.md)
- [작업 규칙](AGENTS.md)
