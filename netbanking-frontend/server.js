/**
 * NetBanking Frontend Static Server
 * Zero-dependency, native Node.js HTTP server for serving the Oracle JET Redwood frontend on port 8000.
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8000;
const ROOT_DIR = path.join(__dirname, 'src');
const GATEWAY = new URL(process.env.API_GATEWAY_URL || 'http://127.0.0.1:8080');
if (!['http:', 'https:'].includes(GATEWAY.protocol)) throw new Error('API_GATEWAY_URL must use HTTP or HTTPS');

function proxyApi(req, res) {
  // Only same-origin browser requests may use this proxy. Never allow arbitrary CORS.
  if (req.headers.origin) {
    let origin;
    try { origin = new URL(req.headers.origin); } catch { res.writeHead(403); res.end(); return; }
    if (origin.host !== req.headers.host) { res.writeHead(403); res.end(); return; }
  }
  const headers = { ...req.headers, host: GATEWAY.host };
  // This is now a server-to-server request; the browser origin was checked above.
  for (const name of Object.keys(headers)) {
    if (name === 'origin' || name === 'forwarded' || name.startsWith('x-forwarded-') ||
        ['connection', 'keep-alive', 'proxy-authorization', 'proxy-authenticate', 'te', 'trailer', 'transfer-encoding', 'upgrade'].includes(name)) {
      delete headers[name];
    }
  }
  const transport = GATEWAY.protocol === 'https:' ? https : http;
  const upstream = transport.request({
    protocol: GATEWAY.protocol, hostname: GATEWAY.hostname, port: GATEWAY.port,
    method: req.method, path: req.url, headers
  }, response => {
    const responseHeaders = { ...response.headers };
    delete responseHeaders.connection;
    res.writeHead(response.statusCode, responseHeaders);
    response.on('error', () => res.destroy());
    response.pipe(res);
  });
  upstream.setTimeout(30000, () => upstream.destroy(new Error('Gateway timeout')));
  upstream.on('error', () => {
    if (res.headersSent) { res.destroy(); return; }
    res.writeHead(502, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Banking API unavailable. Check the gateway and backend services.' }));
  });
  req.on('aborted', () => upstream.destroy());
  res.on('close', () => { if (!res.writableEnded) upstream.destroy(); });
  req.pipe(upstream);
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject'
};

const server = http.createServer((req, res) => {
  if (/^\/api(?:\/|\?|$)/.test(req.url)) { proxyApi(req, res); return; }
  if (!['GET', 'HEAD'].includes(req.method)) { res.writeHead(405); res.end(); return; }
  let safePath;
  try { safePath = decodeURIComponent(req.url.split('?')[0]); }
  catch { res.writeHead(400); res.end('Invalid path'); return; }
  if (safePath.includes('\0')) { res.writeHead(400); res.end('Invalid path'); return; }
  let filePath = path.resolve(ROOT_DIR, '.' + safePath);
  const relative = path.relative(ROOT_DIR, filePath);
  if (relative.startsWith('..') || path.isAbsolute(relative)) { res.writeHead(403); res.end('Forbidden'); return; }
  if (safePath === '/') filePath = path.join(ROOT_DIR, 'index.html');
  // Operational endpoints must not be exposed by the public frontend.
  if (/^\/(actuator|eureka|swagger-ui|v3\/api-docs)(\/|$)/.test(safePath)) {
    res.writeHead(404); res.end('Not Found'); return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      // Fallback for SPA routing: serve index.html
      const ext = path.extname(safePath).toLowerCase();
      if (!ext || ext === '.html') {
        filePath = path.join(ROOT_DIR, 'index.html');
      } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
        return;
      }
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    fs.readFile(filePath, (readErr, content) => {
      if (readErr) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('500 Internal Server Error');
      } else {
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(req.method === 'HEAD' ? undefined : content);
      }
    });
  });
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Port ${PORT} is already in use by another process.`);
  } else {
    console.error('HTTP Server Error:', err);
  }
  process.exitCode = 1;
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', (reason) => {
  console.error('Unhandled Rejection:', reason);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('==================================================================');
  console.log(`  NETBANKING ORACLE JET REDWOOD PORTAL RUNNING ON PORT ${server.address().port}    `);
  console.log(`  URL: http://localhost:${PORT}                                     `);
  console.log(`  Proxying /api/* to ${GATEWAY.origin}`);
  console.log('==================================================================');
});
