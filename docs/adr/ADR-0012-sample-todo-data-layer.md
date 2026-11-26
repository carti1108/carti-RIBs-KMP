### ADR-0012 multi-feature 샘플의 데이터 계층은 `todo-data` 모듈에 두고 JSON은 kotlinx.serialization으로 읽는다
- 상태: 채택
- 날짜: 2026-11-26
- 배경:
  - 이 레포의 목적은 뷰를 뺀 나머지를 플랫폼 사이에 공유하는 것이다. 그 "나머지"에는 응답 해석과 저장소가 들어가는데, `multi-feature`의 할 일 피처는 Interactor 안에 하드코딩한 목록뿐이었다. 데이터 계층을 공유하는 모양이 검증되지 않았다.
  - carti-RIBs-Swift는 같은 일을 ADR-0032로 정했다(공유 `TodoData` 모듈, `HTTPTransport` → API 클라이언트 → 저장소, 샘플 서버). 그 구조를 참고하되 KMP 모듈 경계에 맞춘다. (이슈 #6의 1번)
  - Kotlin 공유 코드에서 JSON을 읽으려면 손으로 디코더를 짜거나 라이브러리를 받아야 한다.
- 결정:
  - `TodoRepository`(`suspend fun fetchTodos(): List<TodoItem>`)와 `TodoRepositoryException`은 `todo-api`에 둔다. 피처 impl은 이 인터페이스만 알고 데이터 계층 모듈에 의존하지 않는다.
  - 저장소 오류는 피처가 다르게 다루는 것만 나눈다. 지금은 `TodoRepositoryException.Unavailable`(다시 시도할 수 있음) 하나다. 취소는 예외 타입으로 감싸지 않고 코루틴의 `CancellationException`을 그대로 올린다. 인증 거절은 401 경로를 다룰 때(이슈 #6의 4번) 하위 타입으로 더한다.
  - 구현은 새 모듈 `samples/multi-feature/data/todo-data`에 둔다.
    - `HttpTransport`: `suspend fun send(HttpRequest): HttpResponse`. 응답을 받지 못하면 `HttpTransportException`. 상태 코드가 무엇이든 응답을 받았으면 돌려준다. 본문은 UTF-8 문자열이다. 플랫폼이 구현할 것은 이것 하나다.
    - `RemoteTodoRepository`: `GET {baseUrl}/todos`를 보내고, 2xx가 아니거나 본문을 읽지 못하거나 응답이 없으면 `Unavailable`로 바꾼다. 원인은 `cause`에 남긴다. 서버 항목 모양(`TodoDto`)은 모듈 안에 두어 서버 필드 이름이 피처로 새지 않게 한다.
    - `SampleTodoServer`: 네트워크 없이 `HttpTransport`를 구현해 JSON을 돌려준다. 샘플 앱은 아직 이것을 쓴다.
  - `todo-data`를 아는 곳은 조립 모듈 `app-root`뿐이다. `MultiFeatureApp(dependency, todoRepository)`로 저장소를 받고, 인자 하나짜리 생성자는 `SampleTodoServer`를 쓴다. 플랫폼 코드는 바뀌지 않는다.
  - 플랫폼이 구현하는 `RootDependency`는 할 일 피처의 뷰 부분(`TodoViewFactory`)만 받는다. 저장소는 `RootComponent`가 넣는다(`TodoDependency = TodoViewFactory + todoRepository`).
  - 목록은 할 일 RIB이 활성화될 때 불러온다. 요청은 활성 구간 `coroutineScope`에 띄워 RIB이 떨어지면 취소된다. `TodoViewState`에 `isLoading`, `errorMessage`를, 리스너에 `didTapRetry()`를 더한다. 불러온 목록은 RIB이 붙어 있는 동안 유지되어 설정 화면을 닫아도 다시 불러오지 않는다.
  - JSON은 kotlinx.serialization(`kotlinx-serialization-json` 1.10.0, 컴파일러 플러그인은 Kotlin 버전과 같음)으로 읽는다. 의존성은 `todo-data`에만 둔다.
- 고려했던 대안:
  (1) JSON 디코더를 손으로 짜기: 의존성이 늘지 않는다. 대신 API마다 디코더를 쓰고 이스케이프·숫자 범위 같은 JSON 규칙을 직접 책임진다. Swift 쪽도 같은 이유로 기각했다. 기각.
  (2) `TodoRepository`를 따로 둔 도메인 모듈에 두기: 피처 `-api`가 이미 `TodoItem`을 갖고 있어 모듈만 하나 늘고 경계는 같아진다. 다른 피처가 같은 저장소를 쓰게 되면 다시 본다. 기각.
  (3) 플랫폼이 `RootDependency`로 저장소까지 주기: 플랫폼마다 전송을 고르는 자리가 생기지만, 지금은 전송 구현이 없어 세 플랫폼이 같은 샘플 서버를 조립하는 코드만 반복된다. 전송 구현을 붙일 때(이슈 #6의 2번) 다시 정한다. 기각.
  (4) Swift처럼 API 클라이언트(`TodoApiClient`)와 저장소를 나누기: 지금 엔드포인트가 하나이고 오류도 한 가지로 줄어들어 나눌 이득이 없다. 쓰기 요청과 상태 분류(이슈 #6의 3번)가 들어올 때 나눈다. 보류.
  (5) 취소를 `TodoRepositoryException.Cancelled`로 감싸기: 코루틴 취소 전파를 깨고 호출자가 다시 던져야 한다. 기각.
  (6) 본문을 `ByteArray`로: 지금 다루는 본문이 모두 JSON 텍스트다. 바이너리 응답이 생기면 바꾼다. 보류.
- 결과:
  - 요청 생성, 상태 코드 확인, JSON 해석, 오류 분류, 로딩·재시도·취소 흐름이 공유 코드에 있고 가짜 전송·가짜 저장소로 JVM·JS 테스트가 확인한다. 웹 Vitest와 Android 에뮬레이터 UI 테스트는 샘플 서버의 JSON을 읽어 목록을 그린다.
  - 웹 Kotlin 산출물(`productionLibrary`, 번들 전)에 kotlinx.serialization 두 모듈이 338KB(gzip 57KB) 붙는다. 산출물 전체 gzip은 160KB다. 번들러 트리 셰이킹 뒤 크기는 재지 않았다(이슈 #6의 7번).
  - 샘플 서버는 중간에 멈추지 않고 바로 응답해서 플랫폼 화면의 로딩 상태는 잠깐이거나 보이지 않는다. 로딩 상태는 공유 테스트에서만 확인한다.
  - iOS Swift 화면과 XCUITest를 함께 고쳤지만 Mac에서 빌드하지 않았다.
- 관련: ADR-0002, ADR-0003, ADR-0005, ADR-0009, ADR-0011, 이슈 #6, carti-RIBs-Swift ADR-0032
