### ADR-0014 쓰기 요청·인증 헤더·상태 분류는 공유 데이터 계층이 하고, 전송은 받은 요청을 그대로 보낸다
- 상태: 채택
- 날짜: 2026-11-26
- 배경:
  - ADR-0013이 확인한 것은 `GET /todos`와 재시도뿐이다. 실제 앱에는 쓰기 요청, `Authorization: Bearer` 헤더, 웹과 출처가 다른 API 서버, RFC 9457 Problem Details 오류 본문이 필요하다. (이슈 #6의 3번)
  - ADR-0012는 API 클라이언트와 저장소 분리를 쓰기 요청과 상태 분류가 들어올 때로 미뤘다.
  - 브라우저는 CORS 검사에 실패한 요청을 네트워크 오류로 처리하고, `fetch`는 원인을 페이지에 알려 주지 않는다(Fetch Standard). carti-RIBs-Swift ADR-0034도 같은 이유로 CORS 거부를 따로 분류하지 않았다.
- 결정:
  - `HttpRequest`에 `method`(`GET`·`POST`·`PATCH`·`DELETE`), `headers`, `body: String?`(UTF-8 텍스트)를 둔다. 기본값이 있어 `HttpRequest(url)`은 그대로다. 생성할 때 헤더 이름(빈 이름, `:`·공백), 값의 줄바꿈, GET 본문, 본문 없는 `Content-Type`을 `require`로 막는다.
  - `KtorHttpTransport`는 메서드·헤더·본문을 그대로 보낸다. Ktor는 `Content-Type`을 헤더로 받지 않으므로 본문의 `TextContent`에 싣는다. 본문이 있는데 `Content-Type`이 없으면 `text/plain`, charset이 없으면 UTF-8이다.
  - 인증은 `BearerTokenTransport(inner, token: suspend () -> String?)`가 붙인다. 보낼 때마다 `token`을 불러 null이 아니면 `Authorization: Bearer <토큰>`을 더한다. 이미 `Authorization`이 있으면 `require`로 실패한다. 플랫폼 전송은 인증을 모른다.
  - `TodoApiClient`를 `RemoteTodoRepository`에서 떼어 낸다. 요청을 만들고 상태를 분류한다: 응답 없음 `Unreachable`, 401 `Unauthorized(problem)`, 409 `Conflict(problem)`, 그 밖의 2xx가 아닌 상태 `BadStatus(status, problem)`, 2xx인데 본문을 못 읽음 `InvalidResponse`. 취소는 감싸지 않는다. 쓰기 API로 `createTodo(title)`(`POST /todos`)을 둔다.
  - 오류 본문은 `ProblemDetails.parse`로 읽는다. 표준 멤버(`type`·`title`·`status`·`detail`·`instance`)만 읽고, `type`이 없으면 `about:blank`, 타입이 틀린 멤버는 그 멤버만 버린다(RFC 9457 §3.1). JSON 객체가 아니면 null이다. 응답 헤더(`application/problem+json`)는 보지 않는다.
  - CORS 거부는 `Unreachable`로 둔다.
  - `RemoteTodoRepository`는 `TodoApiClient`의 모든 실패를 `TodoRepositoryException.Unavailable`로 줄인다. 피처와 화면은 바꾸지 않는다. 401을 피처가 따로 다루는 일은 이슈 #6의 4번에서 한다.
  - 토큰을 어디서 받는지(로그인 결과, 플랫폼 저장소)는 정하지 않는다. `MultiFeatureApp`은 아직 `BearerTokenTransport`를 쓰지 않는다. 401 뒤 갱신과 함께 4번에서 정한다.
- 고려했던 대안:
  (1) 플랫폼 전송이나 Ktor 플러그인(`Auth`의 `bearer`)으로 토큰을 붙이기: Ktor `Auth`는 401 뒤 갱신까지 제공한다. 대신 인증이 전송 구현에 묶여 `HttpTransport`를 다른 구현으로 바꾸면 사라지고, 테스트에서 가짜 전송으로 확인할 수 없다. 4번(동시 401은 갱신 하나를 기다림, 이미 바뀐 토큰이면 갱신 없이 재시도)을 공유 코드에서 확인하려고 데코레이터로 둔다. 기각.
  (2) CORS 거부를 별도 오류로 분류하기: 브라우저가 원인을 숨겨 구분할 방법이 없다. 기각. CORS 설정은 서버 배포 체크리스트로 다룬다.
  (3) 본문을 `ByteArray`로: Ktor로는 보낼 수 있지만 지금 API는 JSON만 쓴다. 바이너리 업로드가 필요해지면 바꾼다. 보류.
  (4) 응답 헤더를 넘겨 `Content-Type`으로 Problem Details를 가리기: `HttpResponse`와 전송 구현이 바뀐다. JSON 객체가 아닌 본문은 파싱에서 걸러진다. 다른 이유로 응답 헤더가 필요해질 때 함께 넣는다. 보류.
  (5) 웹 다른 출처 e2e를 Vitest(jsdom)로: Node `fetch`는 CORS와 프리플라이트를 적용하지 않아 확인할 수 없다. 이미 쓰는 Karma ChromeHeadless(`jsBrowserTest`)에서 실제 브라우저로 확인한다. Playwright 같은 도구를 새로 들이지 않는다.
  (6) 할 일 추가 화면을 세 플랫폼에 만들기: 피처·뷰 계약·플랫폼 뷰가 모두 바뀐다. 이 결정이 확인할 것은 전송 경로라 테스트로 충분하다. 기각.
- 결과:
  - 공통 테스트가 요청 검증, 데코레이터, 상태 분류, Problem Details 읽기를 JVM·JS에서 확인한다.
  - 실제 요청:
    - JVM(OkHttp): 로컬 서버로 `BearerTokenTransport`·`createTodo`를 거친 POST(UTF-8 한글 본문), PATCH 본문, 본문·`Content-Type` 없는 DELETE.
    - Android(OkHttp, 에뮬레이터): 같은 경우와 401 Problem Details 분류. `android-app` 계측 테스트가 `todo-data`를 테스트 의존성으로 받아 확인한다.
    - 웹(Ktor Js, ChromeHeadless): `karma.config.d/cross-origin-api.js`가 Karma와 다른 출처(`127.0.0.1:47821`, `TODO_API_TEST_PORT`로 바꿈)에 서버를 띄운다. 브라우저가 `Authorization`·`Content-Type`을 보고 프리플라이트를 먼저 보내고, 이어서 POST가 나간다. 401·409·503의 Problem Details, PATCH 본문, 본문 없는 DELETE를 확인한다. 서버가 CORS를 허용하지 않으면 POST는 서버에 닿지 않고 `Unreachable`이 된다. 서버 주소는 `client.todoApi`로 넘긴다(`client.args`는 Kotlin 테스트 러너가 쓴다). Node(`jsNodeTest`)에서는 이 테스트가 아무것도 하지 않는다.
  - API 서버는 `Access-Control-Allow-Origin`과, 프리플라이트에 `Access-Control-Allow-Methods`·`Access-Control-Allow-Headers: Authorization, Content-Type`을 줘야 한다. 빠지면 앱은 "응답 없음"만 안다.
  - iOS(Darwin)는 Mac에서 확인하지 않았다. 바뀐 iOS 전용 코드는 없다.
  - 잘못된 `Content-Type` 문자열은 Ktor가 보내기 전에 실패하고 `HttpTransportException`이 된다.
- 관련: ADR-0012, ADR-0013, 이슈 #6, carti-RIBs-Swift ADR-0034
