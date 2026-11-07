import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const base = new URL('../experiments/fahrcockpit/', import.meta.url);
const read = (path) => readFileSync(new URL(path, base), 'utf8');
const java = (name) => read('app/src/main/java/com/cobrachris/fahrcockpit/' + name + '.java');

test('Blitzer.de PRO uses documented receiver and commands', () => {
  const c = java('BlitzerContract');
  const bridge = java('BlitzerBridge');
  for (const word of ['de.blitzer.plus', 'de.blitzer.service.BlitzerBroadcastReceiver',
    'de.blitzer.APP_MODE', 'de.blitzer.KILL_APP', '"START"', '"WIDGET"', '"BACKGROUND"']) {
    assert.ok(c.includes(word), 'missing official protocol: ' + word);
  }
  assert.match(bridge, /intent\.setClassName\(BlitzerContract\.PACKAGE, BlitzerContract\.RECEIVER\)/);
  assert.match(bridge, /context\.sendBroadcast\(intent\)/);
  assert.match(bridge, /SENT_UNCONFIRMED/);
});

test('Overlay requires explicit user gesture, draggable and dismissible', () => {
  const main = java('MainActivity');
  const overlay = java('OverlayService');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(main, /Settings\.canDrawOverlays/);
  assert.match(main, /startForegroundService\(/);
  assert.match(overlay, /TYPE_APPLICATION_OVERLAY/);
  assert.match(overlay, /window\.updateViewLayout\(panel, params\)/);
  assert.match(overlay, /stopSelf\(\)/);
  assert.match(overlay, /BlitzerContract\.Command\.STOP/);
  assert.match(overlay, /BlitzerContract\.Command\.START/);
  assert.match(overlay, /START_NOT_STICKY/);
  assert.match(manifest, /android:foregroundServiceType="specialUse"/);
  assert.match(manifest, /android:exported="false"/);
  assert.match(manifest, /android:allowBackup="false"/);
  assert.doesNotMatch(manifest, /BOOT_COMPLETED/);
});

test('Spotify media uses the Android notification-listener session', () => {
  const media = java('SpotifyMedia');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(media, /getActiveSessions\(listener\)/);
  assert.match(media, /ACTION_SEEK_TO/);
  assert.match(media, /controls\.seekTo\(target\)/);
  assert.match(media, /controls\.skipToNext/);
  assert.match(manifest, /BIND_NOTIFICATION_LISTENER_SERVICE/);
  assert.doesNotMatch(manifest, /MEDIA_CONTENT_CONTROL/);
});

test('Saphe integration uses the official installed app package without fake remote control', () => {
  const warning = java('WarningSource');
  const overlay = java('OverlayService');
  const main = java('MainActivity');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(warning, /my\.saphelink/);
  assert.match(warning, /getLaunchIntentForPackage/);
  assert.match(warning, /getSharedPreferences/);
  assert.match(warning, /"saphe"/);
  assert.match(warning, /"blitzer"/);
  assert.match(warning, /"none"/);
  assert.match(main, /Saphe Link öffnen/);
  assert.match(overlay, /WarningSource\.selected/);
  assert.match(manifest, /<package android:name="my\.saphelink"/);
  assert.doesNotMatch(warning, /BLUETOOTH_SCAN|BluetoothGatt|de\.blitzer\.APP_MODE/);
});

test('Fuel searches on user action, stores key locally and does not poll', () => {
  const fuel = java('FuelActivity');
  const rules = java('FuelRules');
  assert.match(fuel, /search\.setOnClickListener\(v -> search\(\)\)/);
  assert.match(fuel, /getSharedPreferences\(PREFS, MODE_PRIVATE\)/);
  assert.match(fuel, /location\.getLatitude\(\)/);
  assert.match(fuel, /new Thread\(\(\) ->/);
  assert.match(rules, /creativecommons\.tankerkoenig\.de\/json\/list\.php/);
  assert.match(rules, /"e5"/);
  assert.match(rules, /"e10"/);
  assert.match(rules, /"diesel"/);
  assert.doesNotMatch(fuel, /TimerTask|ScheduledExecutor/);
});
