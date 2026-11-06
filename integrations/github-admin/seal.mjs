/** libsodium sealed-box cryptography. Backend-only, never include in a public PWA. */
export async function sealForGitHub(plaintext,base64PublicKey) {
  const {default:sodium}=await import('libsodium-wrappers');
  await sodium.ready;
  const publicKey=sodium.from_base64(base64PublicKey,sodium.base64_variants.ORIGINAL);
  if(publicKey.length!==sodium.crypto_box_PUBLICKEYBYTES)
    throw new TypeError('Invalid GitHub public key length');
  return sodium.to_base64(sodium.crypto_box_seal(sodium.from_string(plaintext),publicKey),
                          sodium.base64_variants.ORIGINAL);
}
