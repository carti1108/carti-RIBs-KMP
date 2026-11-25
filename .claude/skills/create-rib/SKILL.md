---
name: create-rib
description: carti-RIBs-KMP 프레임워크로 새 RIB을 만든다. "RIB 만들어줘", "새 화면 RIB 추가", "create rib"처럼 RIB(Builder·Interactor·Router·뷰 계약) 생성을 요청할 때 사용한다. 뼈대는 `./gradlew newRib`가 만들고, 이 스킬은 요구사항에 맞게 로직·뷰 계약·부모 연결·테스트를 채운다.
---

# create-rib

뼈대 생성은 Gradle 태스크에 맡기고, 요구사항에 맞는 내용만 채운다. 템플릿을 손으로 다시 쓰지 않는다.

## 1. 정보 확인

사용자에게 없는 것만 묻는다.
- RIB 이름(PascalCase)과 패키지
- 공유 코드 소스 루트(`.../src/commonMain/kotlin`)
- 플랫폼 화면이 필요한지: iOS(`swiftDir`, Kotlin 프레임워크 이름 `swiftModule`), Android(`composeDir`, `composePackage`)
- 부모 RIB이 무엇인지, 부모에게 알릴 사용자 의도가 무엇인지

## 2. 뼈대 생성

레포 루트에서 실행한다. 이미 있는 파일은 덮어쓰지 않고 실패한다.

```bash
./gradlew newRib -Pname=<Name> -Ppackage=<package> -PkotlinDir=<commonMain 루트> \
  [-PswiftDir=<dir> -PswiftModule=<Framework>] [-PcomposeDir=<dir> -PcomposePackage=<package>]
```

## 3. 내용 채우기

docs/architecture.md를 기준으로 한다. 꼭 지킬 것:
- 구조는 RIBs-iOS 템플릿을 따른다(ADR-0010). Interactor는 `PresentableInteractor<XxxPresentable>`, Router는 `ViewableRouter<XxxInteractor, XxxPresentable>`이고, Router `init`에서 `interactor.router = this`로 자신을 넣는다.
- 뷰 계약은 `XxxPresentable : Presentable`(Interactor → 뷰), `XxxPresentableListener`(뷰 → Interactor), `XxxViewControllable`(Router → 뷰)로 나누고, 플랫폼 뷰는 둘을 묶은 `XxxViewContract`를 구현한다. Router는 `XxxViewControllable`만 받는다. 입력 검증은 Interactor가 한다. Interactor는 화면 상태를 `XxxViewState` 하나로 모아 `render(state)`로 넘긴다. 템플릿의 `XxxViewState.title`/`didTapClose`를 실제 요구로 바꾼다.
- 플랫폼 뷰는 순수 뷰와 어댑터로 나눈다(ADR-0011). 순수 뷰(`XxxView`)는 값과 콜백만 받고 공유 모듈·`ribs-*`를 import하지 않으며 Preview를 둔다. 어댑터(`XxxScreen`)가 뷰 계약을 구현하고 상태를 순수 뷰 인자로 풀어 넘긴다. 자식 화면 슬롯은 순수 뷰에 Compose는 `child: @Composable () -> Unit`, SwiftUI는 `@ViewBuilder child: () -> Child`로 넘긴다. Kotlin 컬렉션·숫자 타입 변환(`KotlinArray`, `Todo` → `TodoRow`)은 어댑터가 한다.
- iOS 어댑터의 `render(state:)`는 다른 RIB과 Obj-C 셀렉터가 겹치면 Kotlin/Native가 `render(state_:)`처럼 이름을 바꿔 내보낸다. Xcode 오류가 요구하는 라벨로 맞춘다(이슈 #3).
- 자식 → 부모는 `XxxListener`에 사용자 의도(`didRequest…`, `didComplete…`)로 표현하고, 라우팅은 부모가 한다.
- `Dependency`에는 부모가 줘야 하는 것만 둔다. 여러 자식이 함께 쓰는 객체는 부모 Component에서 `shared { }`로 만든다.
- 비동기 작업은 Interactor `coroutineScope`(메인)에서 띄운다. 활성 구간 동안 도는 작업은 `Worker`를 만들어 `didBecomeActive`에서 `worker.start(this)`하고, `didStart`에서 `coroutineScope`(메인)에 띄운다. 복원 상태는 `didBecomeActive`에서 `savedInstanceState`로 읽는다.
- 저장할 상태가 있으면 `onSaveInstanceState`에 String·Long·Double·Boolean·List만 넣는다(ADR-0008).
- 뒤로 가기로 닫혀야 하는 자식이면 부모 Interactor의 `handleBackPress`에서 자식을 떼고 true를 돌려준다.
- iOS 화면의 `listener`는 `weak`로 둔다.

## 4. 부모 연결

- 부모 Component가 새 `XxxDependency`를 구현하게 한다.
- 부모 Router에 `routeToXxx()` / `routeAwayFromXxx()` 짝을 만든다.
- 부모 Component는 `makeXxxView()`를 만들 수 없으므로 부모 `Dependency`에 같은 함수를 선언해 위로 넘긴다. 최상단 `RootDependency`를 구현하는 플랫폼 `AppComponent`(Android·Web·iOS)에 실제 뷰 생성을 추가한다.
- 다른 RIB의 Dependency나 중앙 팩토리에 끼워 넣지 않는다. 각 RIB은 자기 Dependency에 선언된 것만 본다.

## 5. 검증

테스트를 먼저 요구사항에 맞게 고친 뒤(RED) 구현을 채운다(GREEN).
- 공유 코드: `./gradlew :<module>:allTests`
- iOS 화면을 만들었으면 앱 빌드, Android 화면을 만들었으면 `assembleDebug`
- 끝나면 `./gradlew check`
