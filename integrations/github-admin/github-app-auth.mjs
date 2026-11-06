/** Backend-only GitHub App installation token issuer. Never import in a PWA. */
import { createPrivateKey, sign } from 'node:crypto';
const API = 'https://api.github.com';
const enc = value => Buffer.from(JSON.stringify(value)).toString('base64url');

export function makeAppJwt({ appId, privateKeyPem, nowSeconds = Math.floor(Date.now() / 1000) }) {
  if (!/^\d{1,15}$/.test(String(appId))) throw new TypeError('Invalid GitHub App ID');
  if (typeof privateKeyPem !== 'string' || !privateKeyPem.includes('PRIVATE KEY'))
    throw new TypeError('GitHub App private key missing');
  const input = enc({alg:'RS256',typ:'JWT'}) + '.' +
                enc({iat:nowSeconds-60,exp:nowSeconds+540,iss:String(appId)});
  const signature = sign('RSA-SHA256',Buffer.from(input),createPrivateKey(privateKeyPem));
  return input + '.' + signature.toString('base64url');
}

export function createInstallationTokenProvider({ appId, privateKeyPem, installationId,
    repositoryNames, fetchImpl = fetch }) {
  if (!/^\d{1,15}$/.test(String(installationId))) throw new TypeError('Invalid installation ID');
  if (!Array.isArray(repositoryNames) || repositoryNames.length===0 ||
      repositoryNames.some(name=>!/^[a-zA-Z0-9_.-]{1,100}$/.test(name)))
    throw new TypeError('Invalid repository allowlist');
  let cached=null;
  return async function getToken() {
    if(cached && cached.expires>Date.now()+90000)return cached.token;
    const jwt=makeAppJwt({appId,privateKeyPem});
    const response=await fetchImpl(API+'/app/installations/'+installationId+'/access_tokens',{
      method:'POST',
      headers:{
        Authorization:'Bearer '+jwt,
        Accept:'application/vnd.github+json',
        'X-GitHub-Api-Version':'2022-11-28',
        'Content-Type':'application/json',
      },
      body:JSON.stringify({repositories:repositoryNames,permissions:{secrets:'write'}}),
    });
    if(!response.ok)throw new Error('GitHub installation authorization failed (HTTP '+response.status+')');
    const data=await response.json();
    if(typeof data.token!=='string'||!Number.isFinite(Date.parse(data.expires_at)))
      throw new Error('Invalid GitHub installation token response');
    cached={token:data.token,expires:Date.parse(data.expires_at)};
    return cached.token;
  };
}
