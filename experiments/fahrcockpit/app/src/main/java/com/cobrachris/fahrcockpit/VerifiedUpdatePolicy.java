package com.cobrachris.fahrcockpit;

import java.net.URI;
import java.util.Locale;

/** Strict release metadata and HTTPS redirect rules for user-initiated updates. */
public final class VerifiedUpdatePolicy {
    private VerifiedUpdatePolicy() {}

    public static String parseSha256(String digest) {
        if (digest == null || !digest.matches("sha256:[a-fA-F0-9]{64}"))
            return null;
        return digest.substring(7).toLowerCase(Locale.ROOT);
    }

    public static boolean isTrustedDownloadHop(String value) {
        if (value == null) return false;
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443))
                return false;
            String host = uri.getHost();
            return "github.com".equalsIgnoreCase(host)
                || "release-assets.githubusercontent.com".equalsIgnoreCase(host);
        } catch (Exception ignored) { return false; }
    }

    public static boolean isApproved(String url, String digest, long published,
                                      long installed) {
        return UpdateUrlPolicy.isAllowed(url)
            && parseSha256(digest) != null
            && UpdateUrlPolicy.isNewer(published, installed);
    }
}
