const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { spawn } = require('node:child_process');
const path = require('node:path');
const { once } = require('node:events');

function request(port, url, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const req = http.request({ hostname: '127.0.0.1', port, path: url, method, headers }, res => {
      const chunks = [];
      res.on('data', chunk => chunks.push(chunk));
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: Buffer.concat(chunks) }));
    });
    req.on('error', reject);
    req.end(body);
  });
}

test('same-origin frontend and API tunnel boundary', async t => {
  let received;
  const gateway = http.createServer((req, res) => {
    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', () => {
      received = { url: req.url, method: req.method, headers: req.headers, body: Buffer.concat(chunks).toString() };
      if (req.url.includes('download')) {
        res.writeHead(200, { 'Content-Type': 'application/pdf', 'Content-Disposition': 'attachment; filename="statement.pdf"' });
        res.end(Buffer.from([0, 1, 255, 10]));
      } else {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end('{"message":"test upstream"}');
      }
    });
  });
  gateway.listen(0, '127.0.0.1');
  await once(gateway, 'listening');
  t.after(() => gateway.close());
  const child = spawn(process.execPath, [path.join(__dirname, 'server.js')], {
    env: { ...process.env, PORT: '0', API_GATEWAY_URL: `http://127.0.0.1:${gateway.address().port}` },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  t.after(() => child.kill());
  const port = await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Frontend startup timed out')), 5000);
    child.on('error', reject);
    child.stdout.on('data', data => {
      const match = data.toString().match(/RUNNING ON PORT (\d+)/);
      if (match) { clearTimeout(timer); resolve(Number(match[1])); }
    });
  });
  assert.equal((await request(port, '/')).status, 200);
  const result = await request(port, '/api/v1/auth/login?test=1', {
    method: 'POST', headers: { Host: 'demo.trycloudflare.com', Origin: 'https://demo.trycloudflare.com', Authorization: 'Bearer test', 'Content-Type': 'application/json' },
    body: '{"test":true}'
  });
  assert.equal(result.status, 401);
  assert.equal(received.url, '/api/v1/auth/login?test=1');
  assert.equal(received.method, 'POST');
  assert.equal(received.body, '{"test":true}');
  assert.equal(received.headers.authorization, 'Bearer test');
  assert.equal(received.headers.origin, undefined);
  assert.equal((await request(port, '/api/v1/accounts', { headers: { Origin: 'https://unrelated.example' } })).status, 403);
  const download = await request(port, '/api/v1/statements/1/download');
  assert.deepEqual(download.body, Buffer.from([0, 1, 255, 10]));
  assert.match(download.headers['content-disposition'], /statement.pdf/);
  assert.equal((await request(port, '/actuator/health')).status, 404);
  assert.equal((await request(port, '/%2e%2e/server.js')).status, 403);
  assert.equal((await request(port, '/%ZZ')).status, 400);
  assert.equal((await request(port, '/js/services/apiService.js', { method: 'HEAD' })).body.length, 0);
  await new Promise(resolve => gateway.close(resolve));
  const unavailable = await request(port, '/api/v1/accounts');
  assert.equal(unavailable.status, 502);
  assert.match(unavailable.headers['content-type'], /application\/json/);
});
