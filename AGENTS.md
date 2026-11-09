# 작업 규칙

이 레포에서 작업하는 사람과 에이전트가 따르는 규칙입니다.

## 기준

- **원본은 [uber/RIBs](https://github.com/uber/RIBs) Android 구현**이다(ADR-0001). 새 API를 만들기 전에 원본에 같은 역할의 파일이 있는지 먼저 확인하고, 있으면 이름·시그니처·호출 순서를 맞춘다.
- 원본과 다르게 해야 하면 이유를 [docs/upstream-comparison.md](docs/upstream-comparison.md)에 남긴다. 방향이 바뀌는 결정이면 ADR을 쓴다.
- 원본에서 deprecated인 API는 옮기지 않는다.

## 문서

- ADR은 [docs/adr/](docs/adr/README.md)에 둔다. 파일 하나에 하나, `ADR-<네 자리>-<영문-슬러그>.md`. 대안과 기각 이유가 없으면 쓰지 않는다.
- 채택된 ADR 내용은 고치지 않는다. 결정이 바뀌면 새 ADR을 쓰고 옛 것의 상태만 `대체됨`으로 바꾼다.
- 계층 규칙과 생명주기 순서는 [docs/architecture.md](docs/architecture.md)가 기준이다.

## 코드

- 공유 코드(`commonMain`)에는 플랫폼·UI 툴킷 타입을 두지 않는다. `expect/actual`과 `!!`도 쓰지 않는다. `checkRibsRules`가 검사한다.
- 공개 API는 `explicitApi()`로 가시성을 명시한다. 공개 API를 바꾸면 `updateKotlinAbi`로 덤프를 갱신해 같은 커밋에 넣는다.
- 계약 위반은 `check`/`require`로 메시지와 함께 실패시킨다.
- 생명주기 메서드는 단일 스레드에서 호출된다고 가정한다(ADR-0005). 내부에 락을 두지 않는다.
- 비동기 작업은 활성 구간 `coroutineScope`에 띄운다. 디스패처는 `RibDispatchers`로만 고른다.

## 테스트

- 테스트를 먼저 쓴다(RED → GREEN → 정리).
- 테스트는 `commonTest`에 쓰고 JVM과 JS 양쪽에서 통과해야 한다. 디스패처는 `RibCoroutinesConfig`로 테스트 디스패처를 주입한다.
- 플랫폼 전용 mock 도구(Mockito, MockK)에 기대지 않는다. 가짜 구현은 손으로 만든다.
- 커밋 전 `./gradlew :ribs-core:check`를 통과시킨다.

## 커밋

- 의미 단위로 나눈다. 묻지 않고 바로 나눈다.
- 메시지는 `<type>: <설명>` 형식이고 설명은 한국어로 쓴다. type은 feat, fix, refactor, docs, test, chore, perf, ci.
- 원격 푸시, 저장소 공개, PR 생성은 소유자 확인 후에만 한다.

## 환경

- 개발 PC는 Windows다. iOS/macOS 빌드와 Swift 연동은 Mac에서만 확인할 수 있다.
- JDK는 Android Studio 내장 JBR을 쓸 수 있다 (`JAVA_HOME`을 그 경로로 지정).
