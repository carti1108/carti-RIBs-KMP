import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { WebLoginApp } from 'web-login-kotlin'
import { App } from './App'

// RIB 트리는 React 밖에서 한 번만 붙인다. StrictMode의 이중 마운트가 attach를 두 번 하지 않게 한다(ADR-0009)
const loginApp = new WebLoginApp(500)
loginApp.start()
window.addEventListener('pagehide', () => loginApp.stop(), { once: true })

const container = document.getElementById('root')
if (container === null) throw new Error('#root not found')
createRoot(container).render(
  <StrictMode>
    <App rootSlot={loginApp.rootSlot} />
  </StrictMode>,
)
