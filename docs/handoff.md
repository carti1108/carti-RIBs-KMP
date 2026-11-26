# 인수인계 (2026-11-26)

이슈 #6("실제 앱 도입 전 확인")을 Windows PC에서 진행한 상태와, 이어서 할 일을 적습니다. 다음 작업자(사람이든 에이전트든)는 이 문서와 이슈 #6의 진행 댓글을 먼저 읽습니다.

- 브랜치: `carti1108/chore-check-issues` (원격에 푸시됨, PR은 만들지 않음)
- 이슈 #6 진행 댓글에는 브랜치를 `carti1108/mooneye`로 잘못 적었습니다. 실제 브랜치는 위의 것입니다.

## 한 것

| #6 항목 | 결정 | 상태 |
|---|---|---|
| 1. 공유 데이터 계층 | [ADR-0012](adr/ADR-0012-sample-todo-data-layer.md) | 완료 |
| 2. HTTP 전송 | [ADR-0013](adr/ADR-0013-ktor-http-transport.md) Ktor 하나, 엔진은 타깃별 의존성 | 완료 |
| 3. 쓰기 요청·인증·Problem Details·다른 출처 | [ADR-0014](adr/ADR-0014-write-requests-auth-problem-details.md) | 완료(iOS는 읽기 요청만 확인) |
| 4. 401 경로·토큰 갱신 | [ADR-0015](adr/ADR-0015-unauthorized-path-and-token-refresh.md) | 완료 |
| 5. 플랫폼 입력 브리지(웹) | [ADR-0016](adr/ADR-0016-platform-input-as-view-action.md) | 완료 |
| 6. CI | `.github/workflows/ci.yml` (check, web, android, ios) | 완료(네 잡 통과) |
| 7. 실측 | [building.md](building.md) | 완료 |
| 8. 실행 환경 | [building.md](building.md) "Windows 호스트", "Tauri" | 완료(Windows WebView2, macOS WKWebView) |

샘플 구조 요약(`samples/multi-feature`):

- `feature/todo-api`: `TodoRepository`, `TodoRepositoryException`(`Unavailable`, `Unauthorized`), 뷰 계약(`TodoViewState`의 `isLoading`·`errorMessage`·`notice`, 리스너의 `didTapRetry`·`didCopyList`·`didReceiveFile`), `TodoListener.didRequireSignIn`
- `data/todo-data`: `HttpTransport`/`HttpRequest`(메서드·헤더·UTF-8 본문), `KtorHttpTransport`, `BearerTokenTransport`(401 갱신), `TodoApiClient`(상태 분류), `ProblemDetails`, `RemoteTodoRepository`, `SampleTodoServer`
- `app-root`: `MultiFeatureApp(dependency)`(샘플 서버), `MultiFeatureApp(dependency, todoBaseUrl)`(Ktor로 실제 요청)
- 플랫폼: 웹은 항상 같은 출처 `/api/todos`, Android는 인텐트 값 `todoBaseUrl`, iOS는 실행 인자 `-todoBaseURL`
- `export/web-app/tauri`: 웹 번들을 Tauri로 싣는 셸

## Mac에서 할 일

Windows에서는 iOS를 한 번도 빌드하지 못했습니다. 아래 순서로 확인합니다. 앞 단계가 깨지면 뒤 단계는 의미가 없습니다.

1~4는 Mac(Xcode 27, iPhone 17 시뮬레이터)에서 끝냈습니다. 이 브랜치는 `feat/ios-pure-view-split`에 머지됐습니다.

1. (완료) Kotlin 프레임워크와 Swift 앱 빌드
   ```bash
   ./gradlew :samples:multi-feature:export:ios-app:assembleAppSharedDebugXCFramework
   cd samples/multi-feature/ios && tuist generate
   ```
   이번에 바뀐 Swift: `ios/Features/Todo/TodoScreen.swift`(로딩·오류·다시 시도), `ios/App/MultiFeatureSampleApp.swift`(`-todoBaseURL`), `ios/UITests/MultiFeatureFlowUITests.swift`(목록 대기). Kotlin 쪽 변경으로 Swift에 노출되는 것: `TodoViewState` 필드 3개, 리스너 메서드 4개, `MultiFeatureApp(dependency:todoBaseUrl:)`. iOS 뷰는 새 리스너 메서드를 부르지 않으므로 구현할 것은 없습니다.
2. (완료, 2개 통과) XCUITest(`MultiFeatureSample` 스킴). 이슈 #4의 "기존 UI 테스트 통과" 항목도 여기서 닫힙니다.
3. (완료) Darwin 엔진 실제 요청: 시뮬레이터에서 `-todoBaseURL http://127.0.0.1:<포트>/api`로 띄워 목록을 받는지. `http://`에 ATS 예외가 필요한지 확인합니다(Swift 쪽은 시뮬레이터 `127.0.0.1`에 필요 없었다). 이슈 #6의 3번 "세 플랫폼에서 실제 요청"은 iOS가 빠진 채 체크돼 있습니다.
   - 결과: `python3 -m http.server`로 `react/public`을 서빙하고 `-todoBaseURL http://127.0.0.1:8765/api`로 띄우니 서버 로그에 `GET /api/todos 200`이 찍히고 목록 3개가 보였다. ATS 예외 없이 됐다. 서버를 내리면 "할 일 목록을 불러오지 못했습니다"와 다시 시도 버튼이 나오고, 서버를 다시 올린 뒤 다시 시도하면 목록을 받는다. 쓰기 요청과 401은 iOS에서 확인하지 않았다(쓰는 피처가 없다).
4. (완료, `feat/ios-pure-view-split`) 이슈 #4의 남은 일: `samples/ios-login`과 `multi-feature/ios/Features/*`를 순수 뷰와 어댑터로 분리(ADR-0011), Swift 스캐폴드 템플릿 컴파일.
5. (완료) CI에 iOS 잡 추가(`macos-latest`, Kotlin/Native 캐시 `~/.konan`). 1~2가 통과한 뒤에 더합니다.
   - 잡 내용: XCFramework 2개, `swift test`(RIBsKMPSwiftUI), ios-login·multi-feature XCUITest. Tuist는 `jdx/mise-action`으로 4.191.5를 받는다. 시뮬레이터는 러너에 있는 첫 iPhone을 고른다. 같은 명령을 Mac에서 돌려 모두 통과했다(XCUITest 4개, 2개).
6. (완료) iOS 증분 빌드 측정. 다른 PC라 별도 표로 `docs/building.md` "iOS 증분 빌드"에 적었다. 수정 시나리오는 모두 11~12초였다.
7. (완료) Tauri macOS(WKWebView): 출처가 `tauri://localhost`라 같은 출처 `/api/todos` 요청이 되는지 봅니다. 로그인 뒤 목록 3개가 그려졌다(사람이 직접 입력해 확인). `docs/building.md` "Tauri" 참고.
8. (점검 완료, 결정 남음) 이슈 #3(Swift API 품질: async/await, sealed 매핑) 검토. 점검 결과는 [swift-api-review.md](swift-api-review.md)에 있다. #3의 완료 기준이 ADR-0006과 충돌해서 어느 쪽으로 갈지 소유자가 정해야 한다.

## 알고 넘어갈 것

의도적으로 단순하게 둔 곳입니다. 해당 ADR에 이유가 있습니다.

- 샘플은 토큰을 발급하지 않아 앱이 `BearerTokenTransport`를 조립하지 않습니다(ADR-0015). 실제 앱은 로그인 결과와 플랫폼 저장소를 `token`·`refresh`로 넣습니다.
- 토큰 갱신을 시작한 요청이 취소되면 갱신도 취소되고 다음 요청이 다시 갱신합니다. Swift는 끝까지 돌립니다(ADR-0015 대안 5).
- `createTodo`의 401은 피처까지 오지 않습니다. 쓰는 피처가 없습니다.
- 요청 시간 제한(Ktor `HttpTimeout`)이 없습니다. 엔진 기본값을 따릅니다.
- `vite.config.ts`가 Ktor JS의 `import('ws')`를 빈 모듈로 바꿉니다. 웹 샘플이 WebSocket을 쓰면 `ws`를 의존성으로 받습니다(`shortcut:` 주석).
- 웹 Vitest는 jsdom 한계 때문에 두 가지를 테스트 파일 안에서만 채웁니다. 전역 `AbortController`를 Node 것으로 되돌리고(Node `fetch`가 jsdom signal을 거부), `Blob.text()`를 `FileReader`로 채웁니다. 실제 브라우저 경로는 `todo-data`의 `jsBrowserTest`(ChromeHeadless)와 Tauri에서 확인했습니다.
- `todo-data`의 브라우저 테스트 서버(`karma.config.d/cross-origin-api.js`)는 고정 포트 47821을 씁니다(`TODO_API_TEST_PORT`로 바꿈). 브라우저에서 주소를 못 받으면 실패하도록 해 두었습니다. Node(`jsNodeTest`)에서는 이 테스트가 아무것도 하지 않습니다.
- 웹 번들은 데이터 계층 전 brotli 75KB에서 201KB가 됐습니다(serialization 약 49KB, Ktor 약 77KB).

## 검증 명령

```bash
./gradlew :ribs-core:check allTests                 # 코어 검사와 모든 멀티플랫폼 테스트(JVM, JS Node, ChromeHeadless)
./gradlew :samples:multi-feature:android-app:connectedDebugAndroidTest   # 에뮬레이터 필요
cd samples/multi-feature/export/web-app/react && npm ci && npm test && npm run build   # Kotlin/JS 라이브러리를 먼저 빌드
```

Windows 환경의 함정은 `docs/building.md`의 "Windows 호스트에서 되는 범위"에 있습니다(`npm run kotlin`, `local.properties`, 긴 경로).

## CI

`.github/workflows/ci.yml`은 모든 브랜치 push와 포크 PR에서 돕니다. 같은 저장소 브랜치의 PR은 그 브랜치의 push로 이미 돌아서 `pull_request`로는 다시 돌리지 않습니다.

첫 실행(run 45775812294, 커밋 76102c5)에서 세 잡이 모두 통과했습니다.

| 잡 | 내용 | 걸린 시간 |
|---|---|---|
| check | `:ribs-core:check allTests` (JVM, JS Node, ChromeHeadless, 다른 출처 브라우저 테스트 포함) | 3분 37초 |
| web | Kotlin/JS 라이브러리 2개, Vitest(web-login 3개, multi-feature 11개), `npm run build` | 2분 3초 |
| android | 앱·계측 APK 빌드, API 35 에뮬레이터에서 `connectedDebugAndroidTest` 2개 모듈 | 4분 48초 |

- Gradle 캐시는 `actions/setup-java`의 `cache: gradle`입니다. 첫 실행이라 캐시 효과는 아직 재지 않았습니다.
- iOS 잡은 `~/.konan`을 `actions/cache`로 캐시합니다(키: `gradle/libs.versions.toml` 해시).
- iOS 잡 첫 실행(run 45781269356, 커밋 23c9eaf, `macos-latest`)은 21분 49초 걸렸고 통과했습니다. 캐시가 빈 상태였습니다. Kotlin 프레임워크 6분 52초, `swift test` 39초, ios-login XCUITest 7분 43초, multi-feature XCUITest 4분 43초. 캐시가 찬 뒤의 시간은 아직 재지 않았습니다. 같은 실행에서 check 3분 22초, web 2분 3초, android 5분 28초.
