import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const root = resolve('prototypes/chatgpt-link-opener');
const source = join(root, 'app/src/main/java/io/github/cobrachris/chatlinkopener/TargetUrl.java');
const selfTest = join(root, 'tests/TargetUrlSelfTest.java');

test('Android-Link-Öffner: HTTP(S)-Validierung durch Java-Selbsttest', () => {
  const folder = mkdtempSync(join(tmpdir(), 'chat-link-'));
  try {
    const compile = spawnSync('javac', ['-encoding', 'UTF-8', '-d', folder, source, selfTest], { encoding: 'utf8' });
    assert.equal(compile.error, undefined, String(compile.error));
    assert.equal(compile.status, 0, compile.stderr);
    const run = spawnSync('java', ['-cp', folder, 'io.github.cobrachris.chatlinkopener.TargetUrlSelfTest'], { encoding: 'utf8' });
    assert.equal(run.error, undefined, String(run.error));
    assert.equal(run.status, 0, run.stderr);
    assert.match(run.stdout, /13 checks passed/);
  } finally {
    rmSync(folder, { recursive: true, force: true });
  }
});

test('Android-Link-Öffner: App Link ist auf einen festen Host/Pfad begrenzt', () => {
  const manifest = readFileSync(join(root, 'app/src/main/AndroidManifest.xml'), 'utf8');
  assert.match(manifest, /android:autoVerify="true"/);
  assert.match(manifest, /android:host="cobrachris\.github\.io"/);
  assert.match(manifest, /android:pathPrefix="\/open\/"/);
  assert.doesNotMatch(manifest, /android:host="\*"/);
});

test('Android-Link-Öffner: Asset-Links-Vorlage ist noch nicht produktiv signiert', () => {
  const example = JSON.parse(readFileSync(join(root, 'verification/assetlinks.example.json'), 'utf8'));
  assert.equal(example[0].target.package_name, 'io.github.cobrachris.chatlinkopener');
  assert.equal(example[0].target.sha256_cert_fingerprints[0], 'REPLACE_WITH_REAL_APK_SIGNING_CERTIFICATE_SHA256');
});
