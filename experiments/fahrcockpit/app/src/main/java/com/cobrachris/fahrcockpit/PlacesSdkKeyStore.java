package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Separate Google Places Android-restricted API key is entered only on the phone.
 * Stored with AndroidKeyStore-backed AES-GCM, not in Git, chat, Actions
 * secrets or a public APK. Separate Keystore alias; Navigation SDK credential remains untouched.
 */
final class PlacesSdkKeyStore {
    private static final String ALIAS = "fahrcockpit-places-sdk-key-aes-v1";
    private static final String PREFS = "fahrcockpit_places_sdk_local_v1";
    private static final String VALUE = "ciphertext";
    private static final Pattern KEY = Pattern.compile("^AIza[A-Za-z0-9_-]{25,90}$");
    private PlacesSdkKeyStore() {}

    static boolean validKey(String value) {
        return value != null && KEY.matcher(value).matches();
    }

    static boolean save(Context context, String value) {
        if (!validKey(value)) return false;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            SecretKey key;
            if (ks.containsAlias(ALIAS)) {
                KeyStore.SecretKeyEntry entry =
                    (KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null);
                key = entry.getSecretKey();
            } else {
                KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build());
                key = generator.generateKey();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plain = value.getBytes(StandardCharsets.US_ASCII);
            byte[] encrypted = cipher.doFinal(plain);
            Arrays.fill(plain, (byte)0);
            byte[] iv = cipher.getIV();
            if (iv.length != 12) return false;
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            String token = Base64.encodeToString(packed, Base64.NO_WRAP);
            Arrays.fill(packed, (byte)0);
            return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .edit().putString(VALUE,token).commit();
        } catch (Exception unavailable) {
            return false; // Never leak key or crypto internals through logs.
        }
    }

    static String read(Context context) {
        try {
            SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            String packedString=prefs.getString(VALUE,"");
            if (packedString==null || packedString.isEmpty()
                    || packedString.length()>500) return "";
            byte[] packed=Base64.decode(packedString,Base64.NO_WRAP);
            if (packed.length<12+16+30 || packed.length>250) return "";
            byte[] iv=Arrays.copyOfRange(packed,0,12);
            byte[] data=Arrays.copyOfRange(packed,12,packed.length);
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (!ks.containsAlias(ALIAS)) return "";
            SecretKey key=((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
            byte[] plain=cipher.doFinal(data);
            String result=new String(plain,StandardCharsets.US_ASCII);
            Arrays.fill(plain,(byte)0);
            return validKey(result)?result:"";
        } catch(Exception unavailable) {
            return "";
        }
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .remove(VALUE).commit();
        // Keep the Android Keystore alias stable; no app/signing reset.
    }
}
