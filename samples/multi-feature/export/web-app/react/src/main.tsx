import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { WebMultiFeatureApp } from 'multi-feature-kotlin'
import { App } from './App'

// RIB 트리는 React 밖에서 한 번만 붙인다. StrictMode의 이중 마운트가 attach를 두 번 하지 않게 한다(ADR-0009)
// 할 일 목록은 같은 출처의 /api/todos에서 읽는다. 개발 서버와 빌드 결과에는 public/api/todos가 있다(ADR-0013)
const app = new WebMultiFeatureApp(`${window.location.origin}/api`)
app.start()
window.addEventListener('pagehide', () => app.stop(), { once: true })

const container = document.getElementById('root')
if (container === null) throw new Error('#root not found')
createRoot(container).render(
  <StrictMode>
    <App rootSlot={app.rootSlot} />
  </StrictMode>,
)
