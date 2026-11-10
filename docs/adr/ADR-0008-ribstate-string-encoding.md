### ADR-0008 RibState는 허용 타입을 제한하고 문자열 하나로 저장한다
- 상태: 채택
- 날짜: 2026-11-10
- 배경: `RibState`는 Android `Bundle`을 대신하지만 아무 값이나 담는 메모리 속 객체였다. Android는 백그라운드 앱의 프로세스를 수시로 종료하므로, 원본 RIBs처럼 상태를 `Bundle`로 넘겨 프로세스 재시작 뒤 트리를 되살릴 수 있어야 한다. RIBs-iOS에는 같은 기능이 없다.
- 결정: `RibState`의 값을 `String`, `Long`, 유한한 `Double`, `Boolean`, `null`, 이들의 `List`, 중첩 `RibState`로 제한하고 생성 시점에 `require`로 확인한다. 공유 코드가 `encodeToString()`으로 JSON 문자열 하나를 만들고 `decodeFromString()`으로 되살린다. Android는 그 문자열을 `SavedStateHandle`/`Bundle`에 넣는다. iOS는 RIBs-iOS와 같이 상태 복원을 연결하지 않는다.
- 고려했던 대안:
  (1) kotlinx.serialization: `@Serializable` 타입을 그대로 담을 수 있다. 하지만 `ribs-core`에 컴파일러 플러그인과 런타임 의존성이 생기고, 생명주기 저장 상태에 도메인 타입을 넣는 습관을 만든다. 그래서 기각했다.
  (2) 플랫폼별 변환(Android `Bundle`, iOS 딕셔너리): 각 플랫폼 저장소의 타입을 그대로 쓴다. 하지만 어댑터가 둘이 되고 허용 타입 규칙도 양쪽에 따로 두어야 해서 기각했다.
- 결과: 형식 규칙이 공유 코드 한 곳에 있고 의존성이 없다. 허용되지 않는 값은 저장할 때가 아니라 넣는 순간 실패한다. 반면 `Int`는 `Long`으로 바꿔 넣어야 하고, JS에서는 `Int`와 `Double`을 구분할 수 없어 `Int`가 거부되지 않고 `Double`로 저장된다. 저장 크기는 Android `Bundle` 한도(약 500KB 권장 이하)를 넘지 않게 화면 식별자 수준으로만 쓴다.
- 관련: ADR-0001, ADR-0004
