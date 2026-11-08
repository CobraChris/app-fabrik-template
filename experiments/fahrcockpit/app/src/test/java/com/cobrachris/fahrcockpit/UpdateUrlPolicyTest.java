package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class UpdateUrlPolicyTest {
    @Test public void acceptsOnlyFahrCockpitRelease() {
        assertTrue(UpdateUrlPolicy.isForTag(
            "https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v4/fahrcockpit-release.apk",
            "fahrcockpit-v4"));
        assertFalse(UpdateUrlPolicy.isForTag(
            "https://github.com/CobraChris/app-fabrik-template/releases/download/sanftwecker-v4/sanftwecker-release.apk",
            "sanftwecker-v4"));
        assertFalse(UpdateUrlPolicy.isForTag(
            "https://evil.example/fahrcockpit-release.apk", "fahrcockpit-v4"));
        assertFalse(UpdateUrlPolicy.isForTag(
            "https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v3/fahrcockpit-release.apk",
            "fahrcockpit-v4"));
    }
    @Test public void rejectsHostSpoofingAndMalformedTags() {
        String exact =
            "https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v5/fahrcockpit-release.apk";
        assertTrue(UpdateUrlPolicy.isForTag(exact, "fahrcockpit-v5"));
        assertFalse(UpdateUrlPolicy.isForTag(
            exact.replace("github.com/", "github.com.evil.example/"), "fahrcockpit-v5"));
        assertFalse(UpdateUrlPolicy.isForTag(exact + "?download=1", "fahrcockpit-v5"));
        assertFalse(UpdateUrlPolicy.isForTag(
            exact.replace("/fahrcockpit-v5/", "/fahrcockpit-v5/../fahrcockpit-v4/"),
            "fahrcockpit-v5"));
        assertEquals(-1L, UpdateUrlPolicy.versionCode("fahrcockpit-v05"));
        assertEquals(-1L, UpdateUrlPolicy.versionCode("fahrcockpit-v5.apk"));
    }

    @Test public void parsesVersionCodesAndPreventsDowngrades() {
        assertEquals(4L, UpdateUrlPolicy.versionCode("fahrcockpit-v4"));
        assertEquals(-1L, UpdateUrlPolicy.versionCode("fahrcockpit-v0"));
        assertEquals(-1L, UpdateUrlPolicy.versionCode("fahrcockpit-v2147483647"));
        assertTrue(UpdateUrlPolicy.isNewer(4, 3));
        assertFalse(UpdateUrlPolicy.isNewer(3, 3));
        assertFalse(UpdateUrlPolicy.isNewer(3, 4));
        assertFalse(UpdateUrlPolicy.isNewer(0, 4));
        assertTrue(UpdateUrlPolicy.isNewer(5, 4));
    }
}
