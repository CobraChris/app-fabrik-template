/**
 * GitHub App signing-secret provisioning primitives.
 * Intended ONLY for a private controller repository, not a browser/PWA.
 * Does not log secret values or return decrypted keys to a caller.
 */
import { createCipheriv, createDecipheriv, createPrivateKey, hkdfSync, randomBytes } from 'node:crypto';

export const SECRET_NAMES=Object.freeze([
  'APP_SIGNING_KEYSTORE_BASE64',
  'APP_SIGNING_STORE_PASSWORD',
  'APP_SIGNING_KEY_ALIAS',
  'APP_SIGNING_KEY_PASSWORD',
]);

export function targetRepository(value) {
  if (typeof value!=='string' ||
      !/^CobraChris\/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$/.test(value) ||
      value.includes('..') || value.endsWith('.')) {
    throw new TypeError('Target repository must be owned by CobraChris');
  }
  return value;
}
export function backupFileName(repository) {
  const repo=targetRepository(repository).split('/')[1];
  return 'backups/'+repo+'.json.enc';
}
export function masterKey(base64) {
  if(typeof base64!=='string'||!/^[A-Za-z0-9+/]{43}=$/.test(base64))
    throw new TypeError('Invalid 32-byte recovery key');
  const key=Buffer.from(base64,'base64');
  if(key.length!==32 || key.toString('base64')!==base64)
    throw new TypeError('Invalid 32-byte recovery key');
  return key;
}
/**
 * A past recovery-key value was printed in chat, so it MUST NOT independently
 * decrypt signing backups. Derive a distinct 256-bit backup key from the PRIVATE
 * GitHub App PEM plus the repository name. The user key acts only as HKDF salt.
 * The original PEM must remain backed up for future restoration/rotation.
 */
export function deriveBackupKey(privateKeyPem, recoveryKeyBase64, repository) {
  const name=targetRepository(repository);
  const salt=masterKey(recoveryKeyBase64);
  if(typeof privateKeyPem!=='string'||!privateKeyPem)
    throw new TypeError('Missing private App key for backup encryption');
  const key=createPrivateKey(privateKeyPem.replaceAll('\\n','\n'));
  const canonicalDer=key.export({format:'der',type:'pkcs8'});
  const keyBytes=Buffer.from(hkdfSync('sha256',canonicalDer,salt,
    Buffer.from('App-Fabrik signing backup 2026 v2 | '+name,'utf8'),32));
  return keyBytes.toString('base64');
}

export function protectBackup(repository,record,keyBase64) {
  const name=targetRepository(repository);
  if(!record || typeof record!=='object' ||
      SECRET_NAMES.some(k=>typeof record[k]!=='string'||record[k].length<1))
    throw new TypeError('Missing secret material');
  const key=masterKey(keyBase64),iv=randomBytes(12);
  const cipher=createCipheriv('aes-256-gcm',key,iv);
  cipher.setAAD(Buffer.from(name));
  const payload=Buffer.concat([
    cipher.update(JSON.stringify({schema:1,repo:name,values:record}),'utf8'),
    cipher.final()
  ]);
  return JSON.stringify({
    schema:1,algorithm:'aes-256-gcm',repository:name,
    iv:iv.toString('base64'),tag:cipher.getAuthTag().toString('base64'),
    ciphertext:payload.toString('base64')
  })+'\n';
}
export function recoverBackup(repository,envelope,keyBase64) {
  const name=targetRepository(repository);
  const doc=JSON.parse(envelope);
  if(doc.schema!==1||doc.algorithm!=='aes-256-gcm'||doc.repository!==name)
    throw new TypeError('Backup repository mismatch');
  const iv=Buffer.from(doc.iv,'base64'),tag=Buffer.from(doc.tag,'base64');
  if(iv.length!==12||tag.length!==16)throw new TypeError('Invalid IV or authentication tag');
  const decrypt=createDecipheriv('aes-256-gcm',masterKey(keyBase64),iv);
  decrypt.setAAD(Buffer.from(name));decrypt.setAuthTag(tag);
  const raw=Buffer.concat([
    decrypt.update(Buffer.from(doc.ciphertext,'base64')),decrypt.final()
  ]);
  const recovered=JSON.parse(raw.toString('utf8'));
  if(recovered.schema!==1||recovered.repo!==name ||
      SECRET_NAMES.some(k=>typeof recovered.values?.[k]!=='string'||!recovered.values[k]))
    throw new TypeError('Invalid decrypted signing material');
  return recovered.values;
}
export function missingSecrets(existing) {
  if(!Array.isArray(existing) || existing.some(x=>typeof x!=='string'))
    throw new TypeError('Invalid secret-name listing');
  const names=new Set(existing);
  return SECRET_NAMES.filter(name=>!names.has(name));
}
