import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const base = new URL('../experiments/fahrcockpit/', import.meta.url);
const read = (path) => readFileSync(new URL(path, base), 'utf8');

test('FahrCockpit uses explicit official Blitzer.de PRO receiver and commands', () => {
  const c = read('app/src/main/java/com/cobrachris/fahrcockpit/BlitzerContract.java');
  const bridge = read('app/src/main/java/com/cobrachris/fahrcockpit/BlitzerBridge.java');
  assert.match(c, /de\.blitzer\.plus/);
  assert.match(c, /de\.blitzer\.service\.BlitzerBroadcastReceiver/);
  assert.match(c, /de\.blitzer\.APP_MODE/);
  assert.match(c, /de\.blitzer\.KILL_APP/);
  for (const mode of ['START', 'WIDGET', 'BACKGROUND']) {
    assert.match(c, new RegExp('"' + mode + '"'));
  }
  assert.match(bridge, /intent\.setClassName\(BlitzerContract\.PACKAGE, BlitzerContract\.RECEIVER\)/);
  assert.match(bridge, /context\.sendBroadcast\(intent\)/);
  assert.match(bridge, /SENT_UNCONFIRMED/);
});

test('FahrCockpit avoids automatic start and privileged permissions', () => {
  const a = read('app/src/main/java/com/cobrachris/fahrcockpit/MainActivity.java');
  const m = read('app/src/main/AndroidManifest.xml');
  assert.match(a, /KEY_LAST_START_REQUEST/);
  assert.match(a, /enabledSwitch\.setOnCheckedChangeListener/);
  assert.match(a, /BlitzerContract\.Command\.STOP/);
  assert.match(a, /stopNow\.setOnClickListener\(v -> sendMainCommand\(false\)\)/);
  assert.doesNotMatch(m, /uses-permission/);
  assert.match(m, /<package android:name="de\.blitzer\.plus"/);
  assert.match(m, /android:allowBackup="false"/);
});
