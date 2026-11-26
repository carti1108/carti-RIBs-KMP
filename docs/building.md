# 빌드 실측

`samples/multi-feature`의 웹 산출물 크기와 증분 빌드 시간을 기록합니다(이슈 #6의 7번). 같은 일을 carti-RIBs-Swift는 `docs/building.md`에 남겼습니다.

## 측정 환경

2026-11-26. Windows 11 Pro, Intel Core Ultra 9 185H, RAM 32GB. Gradle 9.7.1(데몬 JVM Temurin 21.0.13), Kotlin Gradle 플러그인 2.3.21, AGP 9.5.1, Gradle 빌드 캐시 켬(`org.gradle.caching=true`). Node 26.13.0, Vite 6.3.5.

iOS는 Mac에서 따로 쟀습니다(아래 "iOS 증분 빌드").

## 웹 산출물 크기

React 앱의 릴리스 번들(`vite build`의 `dist/assets/index-*.js`)입니다. Kotlin 산출물(`jsBrowserProductionLibraryDistribution`)과 React가 한 파일에 들어갑니다. gzip은 레벨 9, brotli는 품질 11(Node `zlib`).

| 시점 | 커밋 | 압축 전 | gzip | brotli |
|---|---|---|---|---|
| 데이터 계층 전 | e246461 | 277KB | 87KB | 75KB |
| + 데이터 계층, kotlinx.serialization (ADR-0012) | 432db66 | 480KB | 147KB | 124KB |
| + Ktor 전송, 쓰기 요청, 입력 브리지 (ADR-0013~0016) | 8a60cb7 | 821KB | 244KB | 201KB |

- kotlinx.serialization이 brotli 기준 약 49KB, Ktor가 약 77KB를 더합니다. 432db66 → 8a60cb7 사이에는 Ktor 말고도 쓰기 요청·Problem Details·입력 브리지 코드가 들어 있지만, 대부분은 Ktor입니다(Ktor 모듈만 트리 셰이킹 전 658KB, ADR-0013).
- carti-RIBs-Swift의 같은 샘플(Wasm 릴리스 + `wasm-opt`)은 brotli 2.2MB입니다(Swift `docs/building.md`). 이 샘플은 그 약 9%입니다. 두 번들이 담은 기능이 완전히 같지는 않습니다.
- 서버는 `.js`를 brotli나 gzip으로 압축해 보내야 합니다.
- Ktor JS의 `import('ws')` 자리는 빈 모듈(37B)로 따로 나옵니다(ADR-0013).

```bash
./gradlew :samples:multi-feature:export:web-app:jsBrowserProductionLibraryDistribution
cd samples/multi-feature/export/web-app/react && npx vite build
node -e "const fs=require('fs'),z=require('zlib');for(const f of fs.readdirSync('dist/assets').filter(f=>f.endsWith('.js'))){const b=fs.readFileSync('dist/assets/'+f);console.log(f,b.length,z.gzipSync(b,{level:9}).length,z.brotliCompressSync(b,{params:{[z.constants.BROTLI_PARAM_QUALITY]:11}}).length)}"
```

예전 시점은 그 커밋을 임시 `git worktree`로 꺼내 같은 명령으로 쟀습니다.

## 증분 빌드

한 곳을 고친 뒤 Android 앱과 웹 Kotlin 산출물을 다시 빌드했습니다. 시간은 Gradle 명령 하나의 벽시계 시간이고 데몬은 떠 있는 상태입니다. 한 번씩만 재서 1~2초 차이는 잡음으로 봅니다. "다시 컴파일"은 실행된 Kotlin 컴파일 태스크입니다(태스크가 실행돼도 Kotlin 증분 컴파일이 바뀐 파일만 다시 컴파일할 수 있습니다).

- Android: `./gradlew :samples:multi-feature:android-app:assembleDebug`
- 웹: `./gradlew :samples:multi-feature:export:web-app:jsBrowserProductionLibraryDistribution` (`vite build`는 따로 약 5초)

| 시나리오 | 고친 곳 | Android | Android 다시 컴파일 | 웹 | 웹 다시 컴파일 |
|---|---|---|---|---|---|
| 깨끗한 빌드 | `clean` 뒤 `--no-build-cache` | 12.3초 | 모듈 13개 전부 | 20.6초 | 모듈 13개 전부 + 링크 |
| 변경 없음 | - | 4.3초 | 없음 | 3.2초 | 없음 |
| 피처 impl 본문 | `todo-impl` `Todo.kt`의 private 함수 문자열 | 5.0초 | `todo-impl` | 9.5초 | `todo-impl`, `app-root`, `web-app` 링크 |
| 피처 api 공개 API | `todo-api`에 공개 함수 추가 | 5.3초 | `todo-api`, `todo-impl`, `todo-data`, `app-root`, `android-app` | 10.0초 | `todo-api`, `todo-impl`, `todo-data`, `app-root`, `web-app`, 링크 |
| 코어 본문 | `ribs-core` `Router.kt`의 오류 문구 | 4.5초 | `ribs-core` | 10.9초 | 모듈 13개 전부 + 링크 |

- Android(JVM 바이트코드)는 공개 API가 바뀌지 않으면 고친 모듈만 다시 컴파일합니다. 다른 피처(`login`, `signup`, `settings`)는 공개 API를 고쳐도 다시 컴파일되지 않습니다.
- 웹(Kotlin/JS)은 고친 모듈의 klib이 바뀌면 그 모듈에 의존하는 모듈의 컴파일 태스크가 모두 다시 돕니다. 코어 본문만 고쳐도 13개 모듈의 컴파일 태스크가 모두 실행됩니다. 마지막 링크(`compileProductionLibraryKotlinJs`)는 어떤 수정에서든 돕니다.
- 고친 피처와 상관없는 피처가 다시 빌드되지 않는다는 점은 두 플랫폼 모두 확인됩니다(코어 수정은 예외).
- 깨끗한 빌드 뒤 첫 "변경 없음"에서는 다른 타깃의 `clean`이 지운 산출물을 빌드 캐시에서 되살리느라 8.5초가 걸렸습니다. 표에는 두 번째 실행을 적었습니다.

### iOS 증분 빌드

2026-11-26, Mac에서 쟀습니다. Apple M1, RAM 8GB, macOS 27.0.1, Xcode 27.0. Gradle·Kotlin 버전과 빌드 캐시 설정은 위와 같고, 데몬 JVM은 Android Studio 내장 JBR 21입니다. 위 표와 PC가 달라서 두 표의 시간을 서로 비교하지 않습니다.

- iOS: `./gradlew :samples:multi-feature:export:ios-app:assembleAppSharedDebugXCFramework` (`iosArm64`, `iosSimulatorArm64` 두 타깃을 컴파일·링크)

| 시나리오 | 고친 곳 | iOS | iOS 다시 컴파일 |
|---|---|---|---|
| 깨끗한 빌드 | `clean` 뒤 `--no-build-cache` | 21.1초 | 모듈 13개 전부 + 링크 2개 |
| 변경 없음 | - | 2.1초 | 없음 |
| 피처 impl 본문 | `todo-impl` `Todo.kt`의 `require` 문구 | 11.3초 | `todo-impl`, `app-root`, `ios-app`, 링크 2개 |
| 피처 api 공개 API | `todo-api`에 공개 함수 추가 | 11.3초 | `todo-api`, `todo-impl`, `todo-data`, `app-root`, `ios-app`, 링크 2개 |
| 코어 본문 | `ribs-core` `Router.kt`의 오류 문구 | 11.9초 | 모듈 13개 전부 + 링크 2개 |

- 타깃마다 컴파일과 링크가 따로 돌아서, 한 시나리오에서 실행되는 태스크 수는 웹의 두 배입니다. 고친 범위와 상관없이 11~12초로 비슷하게 걸리므로, 시간 대부분은 매번 도는 링크(`linkDebugFrameworkIos*`) 두 개가 차지하는 것으로 보입니다. 태스크별 시간을 따로 재지는 않았습니다.
- 어떤 모듈이 다시 컴파일되는지는 웹과 같습니다. 고친 모듈의 klib이 바뀌면 그 모듈에 의존하는 모듈의 컴파일 태스크가 모두 돕니다. 코어 본문만 고쳐도 13개 모듈이 모두 다시 컴파일됩니다.
- 깨끗한 빌드가 짧은 이유는 `~/.konan`의 Kotlin/Native 의존성 캐시(표준 라이브러리, 코루틴 등)가 남아 있기 때문입니다. `~/.konan`까지 비운 첫 빌드는 이보다 깁니다. CI 첫 실행에서 두 타깃 빌드가 6분 52초, 캐시가 찬 두 번째 실행에서 1분 39초였습니다(`docs/handoff.md`).
- 이 시간에는 Xcode의 Swift 빌드가 들어 있지 않습니다. 개발 중에 시뮬레이터 하나만 쓴다면 `linkDebugFrameworkIosSimulatorArm64`만 돌려 링크를 하나로 줄일 수 있습니다.

## Windows 호스트에서 되는 범위

2026-11-26, 위와 같은 PC에서 확인했습니다(이슈 #6의 8번).

| 대상 | Windows에서 | 확인한 명령 |
|---|---|---|
| 코어 검사(테스트·공개 API·공유 코드 규칙) | 됨 | `./gradlew :ribs-core:check` |
| 모든 멀티플랫폼 모듈 테스트(JVM, JS Node, JS ChromeHeadless) | 됨 | `./gradlew allTests` (86초, 캐시 일부) |
| 웹 샘플 빌드·Vitest·릴리스 번들 | 됨 | Gradle 태스크 + `npm test`, `npm run build` |
| Android 샘플 빌드·에뮬레이터 계측 테스트 | 됨 | `connectedDebugAndroidTest` (API 37 에뮬레이터) |
| 웹 샘플을 Tauri(WebView2)로 | 됨 | 아래 "Tauri" |
| iOS 프레임워크(`AppShared.xcframework`)·Swift 앱·XCUITest | 안 됨 | Mac 필요. Apple 타깃은 선언만 되어 있고 Windows에서는 건너뛴다(`kotlin.native.ignoreDisabledTargets=true`) |

Windows에서 걸리는 점:

- `npm run kotlin`은 `./gradlew`를 부르는데 npm이 Windows에서 `cmd`로 스크립트를 돌려 실패합니다. 스크립트 안의 Gradle 태스크를 Git Bash나 PowerShell에서 직접 실행합니다.
- 새 워크트리에는 `local.properties`가 없습니다. Android 빌드는 `ANDROID_HOME`(예: `%LOCALAPPDATA%\Android\Sdk`)을 지정하거나 `local.properties`에 `sdk.dir`을 둡니다.
- 경로가 길어지면(`node_modules` 아래) `git worktree remove`가 "Filename too long"으로 지우지 못할 수 있습니다.

## Tauri

`samples/multi-feature/export/web-app/tauri`는 웹 샘플의 릴리스 번들(`react/dist`)을 Tauri 웹뷰에 싣는 셸입니다. Rust 명령과 플러그인은 없고, 웹 샘플 코드는 바뀌지 않습니다. Tauri CLI 2.12.4, `tauri` 2.12.5, Rust 1.99.1.

```bash
./gradlew :samples:multi-feature:export:web-app:jsBrowserProductionLibraryDistribution
cd samples/multi-feature/export/web-app/react && npm ci && npx vite build
cd ../tauri && npm ci && npx tauri build --no-bundle   # 첫 빌드 약 2분 20초
./src-tauri/target/release/multi-feature-tauri.exe
```

Windows(WebView2, Edge 158)에서 확인한 것:

- 페이지 출처는 `http://tauri.localhost`입니다. 할 일 목록은 그 출처의 `/api/todos`(빌드에 들어간 `public/api/todos`)를 Ktor Js 엔진의 `fetch`로 읽습니다.
- `WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS=--remote-debugging-port=9223`로 띄워 CDP로 이름을 넣고 로그인했을 때, 제목이 "carti님의 할 일"로 바뀌고 목록 3개가 그려지며 오류가 없었습니다.
- 실행 파일은 9.1MB입니다(릴리스, 번들 없음).
- CSP는 `default-src 'self'; connect-src 'self' ipc: http://ipc.localhost`입니다. 다른 출처 API를 쓰려면 `connect-src`에 그 출처를 더해야 합니다.

macOS(WKWebView)는 확인하지 않았습니다(Mac 필요). WKWebView의 출처는 `tauri://localhost`라 같은 출처 `fetch`가 되는지 따로 봐야 합니다.
