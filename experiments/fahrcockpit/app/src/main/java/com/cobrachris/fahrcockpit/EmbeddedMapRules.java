package com.cobrachris.fahrcockpit;

import java.net.URI;

/** Restrict main-frame navigation to the dedicated, public HTTPS map host. */
final class EmbeddedMapRules {
    static final String HOST = "cobrachris.github.io";
    static final String PATH = "/app-fabrik-template/apps/fahrcockpit-google-v18/";
    static final String URL = "https://" + HOST + PATH;

    private EmbeddedMapRules() {}

    static boolean allowsOrigin(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                && HOST.equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && uri.getRawUserInfo() == null;
        } catch (Exception invalid) {
            return false;
        }
    }

    static boolean allowsPage(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !HOST.equalsIgnoreCase(uri.getHost())
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawUserInfo() != null) return false;
            URI normalized = uri.normalize();
            return normalized.getPath() != null
                && normalized.getPath().startsWith(PATH)
                && uri.getRawPath() != null
                && !uri.getRawPath().contains("%2e")
                && !uri.getRawPath().contains("%2E");
        } catch (Exception invalid) {
            return false;
        }
    }
}
