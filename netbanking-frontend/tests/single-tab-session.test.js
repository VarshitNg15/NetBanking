const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const source = fs.readFileSync(path.join(__dirname, '../src/js/services/apiService.js'), 'utf8');

function storage(initial = {}) {
  const values = new Map(Object.entries(initial));
  return {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key),
    copy: () => storage(Object.fromEntries(values))
  };
}

let nextId = 0;
function tab(shared, own = storage()) {
  const listeners = {};
  const window = {
    crypto: { randomUUID: () => `tab-${++nextId}` },
    setInterval: () => 1,
    addEventListener: (name, callback) => { listeners[name] = callback; }
  };
  let api;
  vm.runInNewContext(source, {
    define: (_deps, factory) => { api = factory(); },
    localStorage: shared,
    sessionStorage: own,
    window,
    console
  });
  return { api, storage: own, listeners };
}

test('a duplicated tab cannot inherit the account session', () => {
  const shared = storage();
  const first = tab(shared);
  first.api.setSession({ accessToken: 'access', refreshToken: 'refresh', customerId: 'C1', roles: [] });
  const duplicate = tab(shared, first.storage.copy());

  assert.equal(first.api.getToken(), 'access');
  assert.equal(duplicate.api.getToken(), null);
  assert.equal(duplicate.api.getRefreshToken(), null);
  assert.equal(shared.getItem('nb_access_token'), null);
});

test('refresh keeps the same tab signed in and another account can use another tab', () => {
  const shared = storage();
  const first = tab(shared);
  first.api.setSession({ accessToken: 'first', refreshToken: 'refresh', customerId: 'C1', roles: [] });
  const second = tab(shared);
  second.api.setSession({ accessToken: 'second', refreshToken: 'refresh2', customerId: 'C2', roles: [] });

  first.listeners.pagehide();
  const reloaded = tab(shared, first.storage);
  assert.equal(reloaded.api.getToken(), 'first');
  assert.equal(second.api.getToken(), 'second');
});
