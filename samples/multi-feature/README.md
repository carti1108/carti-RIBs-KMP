# 피처 4개 모듈화 샘플

로그인·가입·할 일·설정 피처를 Gradle 모듈로 나누고, 세 플랫폼이 각자 방식으로 가져다 쓰는 샘플입니다.

```
feature/<이름>-api    계약: 뷰 계약, Dependency, Listener, Buildable
feature/<이름>-impl   Builder·Interactor·Router (-api만 의존)
data/todo-data        todo-api의 TodoRepository 구현. HttpTransport, RemoteTodoRepository, SampleTodoServer
app-root              루트 RIB. impl과 data를 아는 유일한 모듈. MultiFeatureApp, LoginDemoApp
export/ios-app        코드 없이 export 설정만. AppShared.xcframework 하나로 묶는다
export/web-app        웹 뷰 구현(Kotlin/JS)과 React 앱(react/)
android-app           Compose 앱. 묶음 없이 app-root를 바로 의존
ios/                  Tuist. Swift 피처 모듈 4개 + 앱 + LoginDemo
```

화면 흐름: 로그인 ⇄ 가입 → 할 일 → 설정(할 일 위에 겹침) → 로그아웃. 설정을 닫으면 할 일 RIB이 붙어 있던 그대로 돌아옵니다.

할 일 목록은 RIB이 활성화될 때 `TodoRepository`에서 불러옵니다. 실패하면 오류와 다시 시도 버튼을 보여 주고, RIB이 떨어지면 요청이 취소됩니다. 샘플 앱은 네트워크 없이 `SampleTodoServer`의 JSON을 씁니다(ADR-0012).

## 모듈 경계

- 피처끼리는 서로를 모릅니다. 루트 Router는 `-api`의 `XxxBuildable`과 `ViewableRouting`만 보고, impl의 Builder는 `MultiFeatureApp`에서 넣습니다.
- 피처마다 `XxxDependency`에 자기 뷰 생성 함수를 선언하고, `RootDependency`는 그것들을 합치기만 합니다. 플랫폼은 `RootDependency` 하나를 구현합니다.
- 피처는 데이터 계층을 모릅니다. 할 일 피처는 `todo-api`의 `TodoRepository`만 보고, 구현(`todo-data`)은 `MultiFeatureApp`이 넣습니다. 그래서 `RootDependency`는 할 일 피처의 뷰 부분(`TodoViewFactory`)만 받습니다.
- iOS 프레임워크는 하나만 만듭니다. 피처별로 나누면 Kotlin 런타임과 `ribs-core` 타입이 프레임워크마다 따로 생겨 트리를 연결할 수 없습니다. 대신 Swift 쪽을 Tuist 정적 프레임워크로 피처별로 나눕니다.

## 실행

```bash
./gradlew :samples:multi-feature:app-root:allTests                     # 공유 흐름(JVM·JS 등)
./gradlew :samples:multi-feature:data:todo-data:allTests                # 데이터 계층
./gradlew :samples:multi-feature:android-app:connectedDebugAndroidTest  # 에뮬레이터 필요

cd samples/multi-feature/export/web-app/react
npm install && npm run kotlin && npm test

./gradlew :samples:multi-feature:export:ios-app:assembleAppSharedDebugXCFramework   # Mac
cd samples/multi-feature/ios && tuist generate
```

iOS는 `MultiFeatureSample`(전체 앱)과 `LoginDemo`(로그인 피처만) 스킴이 있습니다. LoginDemo는 LoginFeature만 빌드하므로 다른 Swift 피처를 고쳐도 다시 빌드하지 않습니다. Kotlin 쪽은 둘 다 같은 `AppShared`를 씁니다.

## 확인 상태

- 공유 흐름: JVM·JS 테스트 통과 (`MultiFeatureAppTest` 17개, `LoginDemoAppTest` 2개, `RemoteTodoRepositoryTest` 7개)
- 웹: Vitest·tsc 통과
- Android: 에뮬레이터(API 37) UI 테스트 2개 통과
- iOS: Windows에서 작성만 했고 Mac에서 빌드·UI 테스트로 확인하지 않았다
