package io.github.cobrachris.chatlinkopener;

import java.net.URI;
import java.net.URISyntaxException;

/** Validates a browser destination received through an untrusted App Link. */
public final class TargetUrl {
    private static final int MAX_LENGTH = 4096;
    private static final String LINK_HOST = "cobrachris.github.io";

    private TargetUrl() {}

    /** Returns null rather than dispatching unsafe or malformed URLs. */
    public static URI parse(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > MAX_LENGTH) {
            return null;
        }
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isEmpty()) return null;
            if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) return null;
            if (uri.getRawUserInfo() != null) return null;
            if (host.equalsIgnoreCase(LINK_HOST)) return null;
            return uri;
        } catch (URISyntaxException | IllegalArgumentException error) {
            return null;
        }
    }
}
