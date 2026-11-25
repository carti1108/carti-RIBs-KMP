# ADR (Architecture Decision Records)

설계 결정의 기록이다. 결정이 바뀌면 기존 ADR은 고치지 않고 새 ADR을 쓴 뒤, 옛 ADR의 상태만 `대체됨 (→ ADR-<번호>)`으로 바꾼다.

## 규칙

- 파일 하나에 ADR 하나. 이름은 `ADR-<네 자리 번호>-<영문-슬러그>.md`.
- 번호는 프로젝트 전체 순번이고 재사용하지 않는다.
- "고려했던 대안"이 없는 ADR은 쓰지 않는다.
- 상태: `제안` / `채택` / `폐기` / `대체됨 (→ ADR-<번호>)`.

## 틀

```markdown
### ADR-<번호> <제목>
- 상태: 채택
- 날짜: YYYY-MM-DD
- 배경: 왜 지금 이걸 정해야 했는가
- 결정: 무엇을 하기로 했는가 (방향, 한두 문장)
- 고려했던 대안: 다른 선택지와 기각한 이유
- 결과: 이득과 비용/제약 둘 다
- 관련: 관련 ADR, 문서, 커밋
```

## ADR 목록

| 번호 | 제목 | 상태 | 날짜 |
|---|---|---|---|
| [ADR-0001](ADR-0001-uber-ribs-android-as-reference.md) | Uber RIBs Android를 원본으로 삼는다 | 대체됨 (→ ADR-0010) | 2026-11-09 |
| [ADR-0002](ADR-0002-coroutines-instead-of-rx.md) | Rx 대신 코루틴/Flow를 쓴다 | 채택 | 2026-11-09 |
| [ADR-0003](ADR-0003-manual-component-di.md) | 수동 Component/Dependency로 DI한다 | 채택 | 2026-11-09 |
| [ADR-0004](ADR-0004-opaque-view-controllable.md) | 공유 코드는 불투명한 ViewControllable만 안다 | 채택 | 2026-11-09 |
| [ADR-0005](ADR-0005-single-thread-lifecycle.md) | 생명주기 호출은 단일 스레드로 제한한다 | 채택 | 2026-11-09 |
| [ADR-0006](ADR-0006-swift-bridge-via-view-interfaces.md) | Swift 연동은 뷰 인터페이스 호출로 하고 Flow 브리지 도구를 쓰지 않는다 | 채택 | 2026-11-10 |
| [ADR-0007](ADR-0007-injected-main-thread-check.md) | 메인 스레드 검사는 플랫폼이 주입한 검사기로 생명주기 진입점에서 한다 | 채택 | 2026-11-10 |
| [ADR-0008](ADR-0008-ribstate-string-encoding.md) | RibState는 허용 타입을 제한하고 문자열 하나로 저장한다 | 채택 | 2026-11-10 |
| [ADR-0009](ADR-0009-web-bridge-via-kotlin-view-store.md) | 웹(React) 연동은 Kotlin이 뷰 인터페이스를 구현하고 화면 상태 저장소를 내보낸다 | 채택 | 2026-11-12 |
| [ADR-0010](ADR-0010-uber-ribs-ios-as-reference.md) | Uber RIBs-iOS를 원본으로 삼는다 | 채택 | 2026-11-13 |
| [ADR-0011](ADR-0011-pure-view-and-adapter.md) | 샘플 플랫폼 뷰는 순수 뷰와 어댑터로 나눈다 | 채택 | 2026-11-25 |
