# 빌드 실측

`samples/multi-feature`의 웹 산출물 크기와 증분 빌드 시간을 기록합니다(이슈 #6의 7번). 같은 일을 carti-RIBs-Swift는 `docs/building.md`에 남겼습니다.

## 측정 환경

2026-11-26. Windows 11 Pro, Intel Core Ultra 9 185H, RAM 32GB. Gradle 9.7.1(데몬 JVM Temurin 21.0.13), Kotlin Gradle 플러그인 2.3.21, AGP 9.5.1, Gradle 빌드 캐시 켬(`org.gradle.caching=true`). Node 26.13.0, Vite 6.3.5.

iOS는 Mac에서만 잴 수 있어 아래 표에 없습니다.

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
