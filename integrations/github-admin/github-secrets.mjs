/**
 * GitHub Secrets:write adapter for a future trusted MCP backend.
 * The LLM NEVER supplies plaintext; the server resolves approved vault references.
 */
const API='https://api.github.com';
const HEADERS={Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};

export function validateRepository(value,allowed) {
  if(typeof value!=='string'||!/^[-\w.]{1,100}\/[-\w.]{1,100}$/.test(value)||
      value.includes('..')||!allowed.has(value))
    throw new TypeError('Repository not on administrator allowlist');
  return value;
}
export function validateSecretName(value,allowedNames) {
  if(typeof value!=='string'||!/^[A-Z][A-Z0-9_]{2,100}$/.test(value)||!allowedNames.has(value))
    throw new TypeError('Secret name not approved');
  return value;
}
export function validateVaultRef(value,allowedRefs) {
  if(typeof value!=='string'||!/^vault:\/\/[a-z0-9/_-]{1,120}$/.test(value)||
      !allowedRefs.has(value))throw new TypeError('Secret reference not enrolled');
  return value;
}

export class GitHubSecretsAdmin {
  constructor({allowedRepositories,allowedSecretNames,allowedVaultRefs,
               getInstallationToken,readVaultSecret,sealForGitHub,authorizeWrite,fetchImpl=fetch}) {
    if([getInstallationToken,readVaultSecret,sealForGitHub,authorizeWrite,fetchImpl]
        .some(f=>typeof f!=='function'))
      throw new TypeError('Trusted auth, vault, encryption, approval and HTTP adapters required');
    this.repos=new Set(allowedRepositories||[]);
    this.names=new Set(allowedSecretNames||[]);
    this.refs=new Set(allowedVaultRefs||[]);
    this.getInstallationToken=getInstallationToken;
    this.readVaultSecret=readVaultSecret;
    this.sealForGitHub=sealForGitHub;
    this.authorizeWrite=authorizeWrite;
    this.fetchImpl=fetchImpl;
  }
  async request(url,token,init={}) {
    const result=await this.fetchImpl(url,{
      ...init,
      headers:{...HEADERS,Authorization:'Bearer '+token,...(init.headers||{})},
    });
    if(!result.ok)throw new Error('GitHub secret operation failed (HTTP '+result.status+')');
    return result;
  }
  async listSecretNames(repository) {
    const repo=validateRepository(repository,this.repos);
    const token=await this.getInstallationToken();
    const res=await this.request(API+'/repos/'+repo+'/actions/secrets?per_page=100',token);
    const data=await res.json();
    return (Array.isArray(data.secrets)?data.secrets:[])
      .filter(item=>this.names.has(item.name))
      .map(item=>({name:item.name,updated_at:item.updated_at||null}));
  }
  async installFromVault({repository,secretName,vaultRef}) {
    const repo=validateRepository(repository,this.repos);
    const name=validateSecretName(secretName,this.names);
    const ref=validateVaultRef(vaultRef,this.refs);
    // Authorization MUST be checked by trusted backend, not an LLM-provided boolean.
    if(await this.authorizeWrite({repository:repo,secretName:name,vaultRef:ref})!==true)
      throw new Error('Repository owner approval required');
    const token=await this.getInstallationToken();
    const pub=await this.request(API+'/repos/'+repo+'/actions/secrets/public-key',token);
    const key=await pub.json();
    if(typeof key.key!=='string'||!/^[a-zA-Z0-9+/=]+$/.test(key.key)||
        !/^[\w-]{1,128}$/.test(key.key_id||''))
      throw new Error('Invalid GitHub public key response');
    const secret=await this.readVaultSecret(ref);
    if(typeof secret!=='string'||secret.length===0||secret.length>100000)
      throw new Error('Invalid vault secret');
    const encrypted=await this.sealForGitHub(secret,key.key);
    if(typeof encrypted!=='string'||encrypted.length===0)throw new Error('Encryption failed');
    await this.request(API+'/repos/'+repo+'/actions/secrets/'+name,token,{
      method:'PUT',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({encrypted_value:encrypted,key_id:key.key_id}),
    });
    return {repository:repo,secret_name:name,status:'stored',plaintext_returned:false};
  }
}
