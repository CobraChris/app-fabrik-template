package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class VerifiedUpdatePolicyTest {
    private static final String RELEASE =
        "https://github.com/CobraChris/app-fabrik-template/releases/download/" +
        "fahrcockpit-v35/fahrcockpit-release.apk";
    private static final String SHA =
        "sha256:78e28a89bfbdccf8c5ca1660be0f472235c322b49b76a93f53e02981be79ba1b";

    @Test public void onlyNewOfficialSignedDigestIsAllowed() {
        assertTrue(VerifiedUpdatePolicy.isApproved(RELEASE, SHA, 35, 34));
        assertFalse(VerifiedUpdatePolicy.isApproved(RELEASE, SHA, 35, 35));
        assertFalse(VerifiedUpdatePolicy.isApproved(RELEASE, SHA, 35, 36));
        assertFalse(VerifiedUpdatePolicy.isApproved(RELEASE, null, 35, 34));
        assertFalse(VerifiedUpdatePolicy.isApproved(RELEASE, "sha256:1234", 35, 34));
        assertFalse(VerifiedUpdatePolicy.isApproved(RELEASE + "?download=1", SHA, 35, 34));
        assertFalse(VerifiedUpdatePolicy.isApproved(
            RELEASE.replace("github.com/", "github.com.evil.example/"),
            SHA, 35, 34));
        assertFalse(VerifiedUpdatePolicy.isApproved(
            RELEASE.replace("/fahrcockpit-v35/", "/sanftwecker-v35/"), SHA, 35, 34));
    }

    @Test public void digestParserRequiresExactSha256() {
        assertEquals(SHA.substring(7), VerifiedUpdatePolicy.parseSha256(SHA));
        assertEquals(SHA.substring(7),
            VerifiedUpdatePolicy.parseSha256(SHA.toUpperCase().replace("SHA256:", "sha256:")));
        assertNull(VerifiedUpdatePolicy.parseSha256("sha512:" + SHA.substring(7)));
        assertNull(VerifiedUpdatePolicy.parseSha256("sha256:" + SHA.substring(7) + "abcd"));
        assertNull(VerifiedUpdatePolicy.parseSha256(null));
    }

    @Test public void redirectsStayOnTrustedHttpsHosts() {
        assertTrue(VerifiedUpdatePolicy.isTrustedDownloadHop(RELEASE));
        assertTrue(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "https://release-assets.githubusercontent.com/abcdef?token=a"));
        assertFalse(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "http://release-assets.githubusercontent.com/abcdef"));
        assertFalse(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "https://github.com.evil.example/download"));
        assertFalse(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "https://evil.example/download"));
        assertFalse(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "https://evil.example@github.com:444/download"));
        assertFalse(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "https://user@github.com/download"));
        assertFalse(VerifiedUpdatePolicy.isTrustedDownloadHop(
            "https://github.com/download#fragment"));
    }
}
