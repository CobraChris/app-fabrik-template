package com.cobrachris.fahrcockpit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * One-click transfer of an existing browser-only Google Demo Key.
 *
 * This is intentionally NOT a remote secret-sync service:
 * - A random single-use session AES key is passed only in an HTTPS URL fragment;
 * - the browser encrypts its existing Google demo key locally with AES-GCM;
 * - only ciphertext returns to the package-specific Android deep link;
 * - the session expires in five minutes and is consumed once.
 *
 * The Google Demo Key itself is scoped to the existing trusted website origin.
 */
final class GoogleKeyTransfer {
    static final String EXTRA_KEY = "fahrcockpit_google_import_key_v1";
    private static final String PREFS = "fahrcockpit_google_transfer_v1";
    private static final String SESSION = "session";
    private static final String EXPIRES = "expires";
    private static final long VALID_MS = 5L * 60L * 1000L;
    private static final String PAGE =
        "https://cobrachris.github.io/app-fabrik-template/apps/fahrcockpit-google-v18/transfer.html";
    private static final Pattern GOOGLE =
        Pattern.compile("^AIza[0-9A-Za-z_-]{25,90}$");
    private static final int BASE64_FLAGS =
        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    private GoogleKeyTransfer() {}

    static boolean validKey(String key) {
        return key != null && GOOGLE.matcher(key).matches();
    }

    static boolean isImportStart(Uri uri) {
        return uri != null && "fahrcockpit".equals(uri.getScheme())
            && "import".equals(uri.getHost()) && uri.getPort() == -1
            && (uri.getUserInfo() == null) && (uri.getQuery() == null);
    }

    static boolean isTransfer(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return false;
        Uri uri = intent.getData();
        return uri != null && "fahrcockpit".equals(uri.getScheme())
            && "transfer".equals(uri.getHost()) && uri.getPort() == -1
            && uri.getUserInfo() == null && uri.getQueryParameter("data") != null;
    }

    private static SharedPreferences state(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean openBrowser(Activity activity) {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        String session = Base64.encodeToString(secret, BASE64_FLAGS);
        state(activity).edit().putString(SESSION, session)
            .putLong(EXPIRES, System.currentTimeMillis() + VALID_MS).commit();
        Arrays.fill(secret, (byte) 0);
        Uri url = Uri.parse(PAGE + "#k=" + session);
        try {
            // Live-Karte's installed Chrome PWA and Chrome share same-origin
            // storage. Android WebView itself has a different data profile.
            activity.startActivity(new Intent(Intent.ACTION_VIEW, url)
                .setPackage("com.android.chrome"));
            return true;
        } catch (RuntimeException chromeUnavailable) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, url));
                return true;
            } catch (RuntimeException noBrowser) {
                state(activity).edit().clear().apply();
                return false;
            }
        }
    }

    static String consume(Context context, Intent intent) {
        if (!isTransfer(intent)) return null;
        SharedPreferences prefs = state(context);
        String session = prefs.getString(SESSION, "");
        long expires = prefs.getLong(EXPIRES, 0);
        // The session must not be reusable even after invalid ciphertext.
        prefs.edit().clear().commit();
        if (System.currentTimeMillis() > expires || session.isEmpty()) return null;
        try {
            String token = intent.getData().getQueryParameter("data");
            if (token == null || token.length() > 1024 ||
                    !token.matches("[A-Za-z0-9_-]+")) return null;
            byte[] packed = Base64.decode(token, BASE64_FLAGS);
            if (packed.length < 12 + 16 + 25 || packed.length > 200) return null;
            byte[] iv = Arrays.copyOfRange(packed, 0, 12);
            byte[] ciphertext = Arrays.copyOfRange(packed, 12, packed.length);
            byte[] rawSession = Base64.decode(session, BASE64_FLAGS);
            if (rawSession.length != 32) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(rawSession, "AES"), new GCMParameterSpec(128, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            Arrays.fill(rawSession, (byte) 0);
            Arrays.fill(decrypted, (byte) 0);
            return validKey(result) ? result : null;
        } catch (Exception invalidOrTampered) {
            return null;
        }
    }
}
