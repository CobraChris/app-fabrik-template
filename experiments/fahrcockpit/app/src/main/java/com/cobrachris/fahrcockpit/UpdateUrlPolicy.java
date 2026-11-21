package com.cobrachris.fahrcockpit;

/** Only allow a direct, pinned, official APK asset under this GitHub repository. */
public final class UpdateUrlPolicy {
    private static final String PREFIX =
        "https://github.com/CobraChris/app-fabrik-template/releases/download/";
    private static final String ASSET = "/fahrcockpit-release.apk";

    private UpdateUrlPolicy() {}

    public static long versionCode(String tag) {
        if (tag == null || !tag.matches("fahrcockpit-v[1-9][0-9]{0,9}")) return -1;
        try {
            long code = Long.parseLong(tag.substring("fahrcockpit-v".length()));
            return code <= 2_100_000_000L ? code : -1;
        } catch (NumberFormatException ignored) { return -1; }
    }

    /** Limit every install candidate to the exact official release APK path. */
    public static boolean isAllowed(String url) {
        if (url == null || !url.startsWith(PREFIX) || !url.endsWith(ASSET))
            return false;
        String tag = url.substring(PREFIX.length(), url.length() - ASSET.length());
        return versionCode(tag) > 0 && url.equals(PREFIX + tag + ASSET);
    }

    public static boolean isForTag(String url, String tag) {
        return versionCode(tag) > 0 && isAllowed(url)
            && url.equals(PREFIX + tag + ASSET);
    }

    public static boolean isNewer(long published, long installed) {
        return published > installed && published > 0;
    }
}
