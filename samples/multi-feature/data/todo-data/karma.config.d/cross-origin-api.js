// 브라우저 테스트(jsBrowserTest)용 다른 출처 API 서버.
// Karma 페이지(localhost)와 출처가 다른 127.0.0.1:<포트>에서 듣는다. 테스트는 client.todoApi로 주소를 받는다
// (client.args는 Kotlin 테스트 러너가 쓴다).
// 경로 /t/<테스트 id>/... 로 테스트마다 요청 기록을 나누고, /_log/<테스트 id>로 기록을 돌려준다.
// /t/<id>/no-cors/... 는 CORS 헤더를 주지 않는다(브라우저가 요청을 막는다).
const http = require('http')

const port = Number(process.env.TODO_API_TEST_PORT || 47821)
const logs = new Map()

function record(id, entry) {
  if (!logs.has(id)) logs.set(id, [])
  logs.get(id).push(entry)
}

function send(res, status, body, contentType) {
  if (body === undefined) {
    res.writeHead(status).end()
    return
  }
  res.writeHead(status, { 'Content-Type': contentType || 'application/json; charset=utf-8' })
  res.end(typeof body === 'string' ? body : JSON.stringify(body))
}

function problem(res, status, title) {
  send(res, status, { type: 'https://api.example.test/problems/' + status, title, status }, 'application/problem+json')
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://127.0.0.1')
  const logMatch = url.pathname.match(/^\/_log\/([^/]+)$/)
  const testMatch = url.pathname.match(/^\/t\/([^/]+)(\/no-cors)?(\/.*)$/)
  const cors = !(testMatch && testMatch[2])
  if (cors && req.headers.origin) {
    res.setHeader('Access-Control-Allow-Origin', req.headers.origin)
    res.setHeader('Vary', 'Origin')
  }

  if (logMatch) {
    send(res, 200, logs.get(logMatch[1]) || [])
    return
  }
  if (!testMatch) {
    send(res, 404)
    return
  }
  const [, id, , path] = testMatch

  if (req.method === 'OPTIONS') {
    record(id, {
      method: 'OPTIONS',
      path,
      requestMethod: req.headers['access-control-request-method'] || null,
      requestHeaders: req.headers['access-control-request-headers'] || null,
    })
    if (cors) {
      res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE')
      res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type')
    }
    send(res, 204)
    return
  }

  let body = ''
  req.setEncoding('utf8')
  req.on('data', (chunk) => (body += chunk))
  req.on('end', () => {
    record(id, {
      method: req.method,
      path,
      authorization: req.headers.authorization || null,
      contentType: req.headers['content-type'] || null,
      body,
    })
    if (req.method === 'POST' && path === '/todos') {
      if (req.headers.authorization !== 'Bearer good-token') return problem(res, 401, '토큰이 필요합니다')
      const title = JSON.parse(body).title
      if (title === '중복') return problem(res, 409, '이미 있는 할 일')
      return send(res, 201, { id: 1, title, done: false })
    }
    if (req.method === 'GET' && path === '/todos') return problem(res, 503, '점검 중')
    if (req.method === 'PATCH' && path === '/todos/1') return send(res, 200, { id: 1, title: '우유', done: true })
    if (req.method === 'DELETE' && path === '/todos/1') return send(res, 204)
    send(res, 404)
  })
})

server.on('error', (error) => {
  throw new Error(`cross-origin API test server could not listen on ${port} (set TODO_API_TEST_PORT): ${error.message}`)
})
server.listen(port, '127.0.0.1')
server.unref()

config.client = config.client || {}
config.client.todoApi = `http://127.0.0.1:${port}`
