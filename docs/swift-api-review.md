# Swift 노출 점검

이슈 #3의 "공개 계약 목록화와 Swift 노출 형태 점검", "iOS 샘플에서 Swift 사용 코드 확인"을 정리한 문서입니다. 2026-11-26에 `AppShared.xcframework`(multi-feature, Kotlin 2.3.21)가 만든 Obj-C 헤더를 보고 썼습니다. 무엇을 바꿀지는 정하지 않았습니다.

## 현재 경계

ADR-0006에 따라 Swift는 Flow나 suspend를 직접 다루지 않습니다. iOS 샘플의 Swift 코드가 공유 코드와 만나는 곳은 아래뿐입니다.

| Swift가 하는 일 | 계약 | Swift에서 보이는 형태 |
|---|---|---|
| 뷰 계약 구현 | `XxxPresentable.render(state:)` | `renderXxx(state:)` (`@ObjCName`으로 고정) |
| 입력 전달 | `XxxPresentableListener` | `didToggle(id: Int32)`, `didTapRetry()` 등 동기 메서드 |
| 상태 읽기 | `XxxViewState` | 클래스. `String?`, `Bool`은 그대로, `Int`는 `Int32` |
| 목록 읽기 | `TodoViewState.items: Array<TodoItem>` | `KotlinArray<TodoItem>`. 어댑터가 `size`·`get(index:)`로 `[TodoRow]`를 만든다 |
| 앱 시작 | `MultiFeatureApp.start(host:)`, `ViewHost` | `ViewHost` 프로토콜. Kotlin의 타입 매개변수는 사라진다 |
| 뷰 생성 | `RootDependency.makeXxxView()` | 프로토콜 메서드 |

iOS 샘플의 Swift 코드는 `async`, `for await`, sealed 타입을 하나도 쓰지 않습니다.

## 헤더에서 보이는 문제

이슈 #3의 세 항목을 헤더와 대조했습니다.

1. **코루틴·Flow**: Swift가 구현하거나 호출하는 계약에는 없습니다. 다만 export된 `ribs-core` 때문에 헤더에 코루틴 관련 타입 14개(`StateFlow`, `CoroutineScope`, `Continuation` 등)가 나옵니다. Swift에서 보이기만 하고 쓰이지는 않습니다.
   - `Interactor.isActiveStream: StateFlow`, `Router.lifecycle: Flow`가 `Kotlinx_coroutines_coreStateFlow`/`Flow` 프로토콜로 나온다. Swift에서 구독하려면 `FlowCollector`를 구현하고 `collect(collector:completionHandler:)`를 불러야 하고, 취소 수단이 없다.
   - `TodoRepository.fetchTodos()`(suspend)는 `fetchTodos(completionHandler:)`로 나온다. Swift는 이 메서드를 `async throws`로도 가져오지만 취소는 Kotlin에 전달되지 않는다. 샘플에서는 Kotlin 구현만 쓰고 Swift는 이 메서드를 부르지 않는다.
2. **제네릭**: 인터페이스 `ViewHost<V>`, `LaunchRouting<V>`의 타입 매개변수는 Obj-C 프로토콜로 바뀌면서 사라진다(Obj-C 프로토콜은 제네릭을 지원하지 않는다). 예를 들어 `ViewHost`는 `show(view: any ViewControllable)`가 된다. 클래스(`Component<D>`, `Router<I>`, `ViewableRouter<I, V>`)는 경량 제네릭으로 남는다. Swift 어댑터는 `any ViewControllable`을 받아 다운캐스트한다(ADR-0006).
3. **sealed**: `TodoRepositoryException`, `AuthResult`는 서브클래스 계층으로 나와서, Swift `switch`가 모든 경우를 다뤘는지 컴파일러가 확인해 주지 않는다. 두 타입 모두 Kotlin 안에서만 쓰이고 Swift 경계를 넘지 않는다. 뷰 상태는 sealed 대신 nullable 필드와 `Bool`로 표현한다(`TodoViewState`의 `isLoading`, `errorMessage`).

그 밖에 확인한 것:

- 여러 계약의 `render(state:)`가 Obj-C 셀렉터에서 겹치던 문제는 `@ObjCName("render<Name>")`으로 해결했다(PR #5).
- `TodoViewState.items`가 `List`가 아니라 `Array`인 이유는 `@JsExport` 때문이다. `List`라면 Swift는 `[TodoItem]`을 바로 받는다.
- 두 샘플의 `TodoItem.id`는 `Int`(Swift `Int32`)와 `Long`(Swift `Int64`)으로 서로 다르다.

## 정해야 할 것

이슈 #3의 완료 기준("iOS 샘플이 `async/await`·`for await`로 코어를 쓴다")은 채택된 ADR-0006("공유 코드가 Swift에 Flow나 suspend를 노출하지 않는다")과 충돌한다. 두 방향이 있다.

- **ADR-0006을 유지한다**: #3의 완료 기준을 "Swift 경계에 Flow·suspend·sealed가 없음을 규칙으로 고정한다"로 바꾼다. 남는 일은 경계 타입 가이드(위 표), 그리고 필요하면 `ribs-core`의 Flow 속성을 Swift 헤더에서 숨기는 것(`@HiddenFromObjC`)이다.
- **ADR-0006을 대체한다**: SKIE나 Swift Export를 다시 검토하는 새 ADR을 쓴다. ADR-0006이 기각한 이유(비공식 툴체인, Kotlin 버전 종속, 실험 단계)가 지금도 맞는지부터 확인해야 한다.
