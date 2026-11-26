import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig, type Plugin } from 'vitest/config'

const fromHere = (path: string) => fileURLToPath(new URL(path, import.meta.url))

// Ktor JS는 Node에서 WebSocket을 열 때만 `import('ws')`를 부른다. 이 앱은 WebSocket을 쓰지 않으므로
// 빈 모듈로 풀어 Vite가 해석에 실패하지 않게 한다.
// shortcut: WebSocket을 쓰면 실패한다, 웹 샘플에 WebSocket이 필요해지면 ws를 의존성으로 받는다
const ktorWsStub: Plugin = {
  name: 'ktor-ws-stub',
  resolveId: (id) => (id === 'ws' ? '\0ktor-ws-stub' : null),
  load: (id) => (id === '\0ktor-ws-stub' ? 'export default undefined' : null),
}

// Kotlin 빌드 결과(`npm run kotlin`)와 ribs-react 소스를 별칭으로 가져온다
export default defineConfig({
  plugins: [react(), ktorWsStub],
  resolve: {
    alias: {
      'multi-feature-kotlin': fromHere('../build/dist/js/productionLibrary/carti-ribs-kmp-samples-multi-feature-export-web-app.mjs'),
      'ribs-react': fromHere('../../../../../react/ribs-react/src/index.tsx'),
    },
  },
  test: {
    environment: 'jsdom',
  },
})
