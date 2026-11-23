# Android 샘플 (Compose)

iOS 샘플과 같은 `login-shared` 트리와 뷰 계약을 Compose로 구현한 앱입니다. 시나리오 목록은 [iOS 샘플 README](../ios-login/README.md#샘플-시나리오)에 있습니다.

- `ribs-compose` 모듈의 `ComposeRenderable`·`ComposeScreenSlot`·`RibSlot`: iOS의 `SwiftUIRenderable`·`RIBScreenSlot`·`RIBSlotView`에 해당
- `RibBackPressedCallback`: 시스템 뒤로 가기를 루트 `Router.handleBackPress()`로 보내고, 트리가 처리하지 않으면 시스템 기본 동작을 따른다
- 프로세스가 종료됐다 다시 시작되면 `SavedStateHandle`에 넣어 둔 `RibState` 문자열로 트리를 되살린다(ADR-0008). iOS는 RIBs-iOS처럼 복원을 연결하지 않는다
- `LoginViewModel`이 트리를 소유하는 `ViewHost`라서 화면 회전에도 트리가 유지되고, 화면을 떠날 때 `shutdown`한다
- UI 테스트(`LoginFlowTest`)는 iOS `LoginFlowUITests`와 같은 세 흐름을 확인한다

## 실행

SDK 경로가 필요합니다(`local.properties`의 `sdk.dir` 또는 `ANDROID_HOME`).

```bash
./gradlew :samples:android-login:installDebug
./gradlew :samples:android-login:connectedDebugAndroidTest   # 에뮬레이터나 기기 필요
```
