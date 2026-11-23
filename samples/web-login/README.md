# 웹 샘플 (React)

같은 `login-shared` 트리를 React로 그리는 샘플입니다. 구조는 ADR-0009를 따릅니다. iOS·Android와 달리 뷰 계약은 Kotlin(`jsMain`)이 구현하고, React는 화면 상태를 구독하고 함수를 부르기만 합니다. 시나리오 목록은 [iOS 샘플 README](../ios-login/README.md#샘플-시나리오)에 있습니다.

| 위치 | 역할 |
|---|---|
| `ribs-web` | Kotlin/JS 어댑터. `ViewStore`(화면 상태 스냅숏), `ScreenSlot`(자식 화면 자리), `WebViewControllable` |
| `react/ribs-react` | React 어댑터. `useViewStore` 훅과 `RibSlot` 컴포넌트. iOS의 `RIBSlotView`, Android의 `RibSlot`에 해당 |
| `samples/web-login` | 뷰 계약의 웹 구현과 진입점 `WebLoginApp`(Kotlin), `react/`에 Vite 앱과 흐름 테스트 |

- RIB 트리는 React 밖(`main.tsx`)에서 붙인다. StrictMode의 이중 마운트가 attach를 두 번 하지 않는다
- JS로 내보내는 화면 상태에는 `Long` 대신 `Int`, `List` 대신 `Array`를 쓴다

## 실행

```bash
./gradlew :ribs-web:check                                     # ViewStore·ScreenSlot 테스트(Node, Chrome Headless)
cd samples/web-login/react && npm install && npm run kotlin   # Kotlin 빌드 결과 생성
npm test                                                      # 로그인 → 목록 → 상세 → 로그아웃 흐름(Vitest)
npm run dev
```

브라우저 테스트는 `CHROME_BIN`에 Chrome 경로가 필요합니다.
