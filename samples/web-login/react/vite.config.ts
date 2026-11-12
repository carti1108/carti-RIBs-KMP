import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const fromHere = (path: string) => fileURLToPath(new URL(path, import.meta.url))

// Kotlin 빌드 결과(`npm run kotlin`)와 ribs-react 소스를 별칭으로 가져온다
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      'web-login-kotlin': fromHere('../build/dist/js/productionLibrary/carti-ribs-kmp-samples-web-login.mjs'),
      'ribs-react': fromHere('../../../react/ribs-react/src/index.tsx'),
    },
  },
  test: {
    environment: 'jsdom',
  },
})
