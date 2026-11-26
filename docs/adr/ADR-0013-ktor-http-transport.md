### ADR-0013 HTTP 전송은 Ktor 클라이언트 하나로 구현하고 엔진은 타깃별 의존성으로 고른다
- 상태: 채택
- 날짜: 2026-11-26
- 배경:
  - ADR-0012는 플랫폼이 만들 것을 `HttpTransport` 하나로 줄이고 샘플 서버만 두었다. 요청이 실제로 나가고 응답이 공유 코드까지 오는 경로는 확인되지 않았다. (이슈 #6의 2번)
  - carti-RIBs-Swift는 ADR-0033으로 플랫폼 스택을 직접 쓴다(iOS `URLSession`, Web `fetch`, Android는 Kotlin `HttpURLConnection`을 swift-java로 부름). Swift에는 세 플랫폼을 함께 지원하는 HTTP 클라이언트가 없고, Android에서 `URLSession`을 쓰면 ABI마다 약 72MB가 늘어서 이렇게 골랐다. 그 결과 Android는 취소해도 연결이 바로 끊기지 않는다.
  - KMP에는 공통 HTTP 클라이언트(Ktor)가 있다. 3번(쓰기 요청·인증 헤더)과 4번(401 갱신)이 전송 위에 쌓인다.
  - AGENTS.md는 공유 코드에 `expect/actual`을 금지한다.
- 결정:
  - `todo-data`의 `commonMain`에 `KtorHttpTransport(client: HttpClient = HttpClient())` 하나를 둔다. Ktor 3.3.1.
  - 엔진은 타깃별 의존성으로 하나씩 넣는다: JVM·Android `ktor-client-okhttp`, iOS `ktor-client-darwin`, JS `ktor-client-js`. 엔진 없이 만든 `HttpClient()`는 그 타깃에 들어 있는 엔진을 고른다. `expect/actual`이 필요 없다.
  - 응답을 받으면 상태 코드와 UTF-8 본문을 그대로 돌려준다(`expectSuccess`는 기본값 false). 취소(`CancellationException`)는 그대로 올리고, 그 밖의 실패는 모두 `HttpTransportException`으로 감싼다. JS 엔진은 연결 실패를 `Exception`이 아닌 `kotlin.Error`("Fail to fetch")로 던지므로 `Throwable`을 받는다.
  - 코루틴이 취소되면 Ktor가 엔진의 요청을 끊는다(OkHttp는 call 취소, JS는 `AbortController`). 요청을 돌리는 스레드는 Ktor 엔진이 고른다. `RibDispatchers`에는 IO가 없고, 전송 구현이 디스패처를 고르지 않으므로 이 규칙과 부딪치지 않는다.
  - 앱 연결: `MultiFeatureApp(dependency, todoBaseUrl)` 생성자를 더한다. 플랫폼은 서버 주소만 넘기고 전송·저장소는 `app-root`가 조립한다(ADR-0012의 대안 (3)은 이 형태로 정리한다).
    - Web: 항상 실제 전송을 쓴다. 같은 출처의 `/api/todos`를 읽고, 개발 서버와 빌드 결과에는 정적 `public/api/todos`가 있다.
    - Android: 인텐트 값 `todoBaseUrl`이 있으면 실제 전송, 없으면 샘플 서버. `INTERNET` 권한을 더하고, 디버그 빌드만 `127.0.0.1`·`10.0.2.2`·`localhost`로 평문 HTTP를 허용한다.
    - iOS: 실행 인자 `-todoBaseURL <주소>`가 있으면 실제 전송, 없으면 샘플 서버.
- 고려했던 대안:
  (1) 플랫폼 스택 직접 사용(Swift ADR-0033과 같음): 의존성이 없고 웹 번들이 가볍다. Swift와 1:1로 비교된다. 대신 구현과 취소 처리가 세 벌이고, Android는 블로킹 I/O를 돌릴 디스패처가 따로 필요하다. 3·4번이 전송 위에 쌓일 때 플랫폼마다 동작이 갈라질 수 있어 기각했다(소유자 결정, 2026-11-26).
  (2) `expect fun platformHttpTransport()`: 엔진 선택이 코드에 드러난다. AGENTS.md가 공유 코드의 `expect/actual`을 금지한다. 기각.
  (3) 엔진을 플랫폼 앱이 고르고 `HttpClient`를 넘기기: 엔진을 바꾸기 쉽다. 대신 iOS Swift 코드가 Kotlin `HttpClient`와 엔진 설정을 만들어야 하고, Swift에 노출되는 Kotlin 타입이 는다(ADR-0006). 기각.
  (4) Android 엔진을 `ktor-client-android`(HttpURLConnection)로: OkHttp 의존성이 없다. OkHttp는 call 취소로 연결을 바로 끊고, Swift 쪽 Android의 남은 문제(취소 뒤 연결이 남음)를 피할 수 있다. 실제 앱에서 OkHttp를 흔히 쓴다. OkHttp를 골랐다.
- 결과:
  - 공통 테스트(MockEngine)가 상태·본문 전달, 엔진 실패 → `HttpTransportException`, 취소 전파를 JVM·JS에서 확인한다.
  - `jvmTest`가 실제 OkHttp 엔진으로 로컬 서버에 요청해 503 전달, 연결 거부, 취소하면 서버가 연결 끊김을 보는지 확인한다. 엔진마다 동작이 달라 commonTest가 아닌 jvmTest에 둔다.
  - 웹 Vitest는 Node 테스트 서버로 실제 요청을 보낸다(정상, 503 뒤 다시 시도, 연결 실패). Vitest의 jsdom 환경이 전역 `AbortController`를 jsdom 것으로 바꾸고 Node `fetch`가 그 signal을 거부해서, 테스트에서만 Node의 `AbortController`로 되돌린다.
  - Android 계측 테스트는 기기 안 테스트 서버로 요청한다(503 → 다시 시도 → 서버 목록).
  - Ktor JS는 Node에서 WebSocket을 쓸 때만 `import('ws')`를 부른다. Vite가 이를 해석하지 못해 `vite.config.ts`에서 빈 모듈로 바꾼다. JS 의존성 잠금 파일에 `ws`가 더해진다.
  - 웹 릴리스 번들(`vite build`)은 810KB(gzip 241KB)다. Ktor 모듈은 트리 셰이킹 전 658KB(gzip 114KB)다. Ktor를 넣기 전 번들 크기는 재지 않았다(이슈 #6의 7번).
  - 시간 제한(HttpTimeout)은 두지 않았다. 엔진 기본값을 따른다.
  - iOS(Darwin 엔진, 실행 인자 연결)는 Mac에서 빌드·실행하지 않았다. `http://` 주소를 쓰려면 ATS 예외가 필요한지도 확인하지 않았다.
- 관련: ADR-0006, ADR-0009, ADR-0012, 이슈 #6, carti-RIBs-Swift ADR-0033
