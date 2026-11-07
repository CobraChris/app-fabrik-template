package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class UpdateUrlPolicyTest {
    private static final String BASE=
            "https://github.com/CobraChris/app-fabrik-template/releases/download/";
    @Test public void acceptsOnlySignedReleaseAssetFromKnownRepo(){
        assertTrue(UpdateUrlPolicy.isAllowed(BASE+"sanftwecker-v9/sanftwecker-release.apk"));
        assertFalse(UpdateUrlPolicy.isAllowed(BASE+"sanftwecker-v9/sanftwecker-beta-debug.apk"));
        assertFalse(UpdateUrlPolicy.isAllowed(BASE+"sanftwecker-v9/sanftwecker-release.apk?x=1"));
        assertFalse(UpdateUrlPolicy.isAllowed("http://github.com/CobraChris/app-fabrik-template/releases/download/sanftwecker-v9/sanftwecker-release.apk"));
        assertFalse(UpdateUrlPolicy.isAllowed("https://evil.example/releases/download/sanftwecker-v9/sanftwecker-release.apk"));
        assertFalse(UpdateUrlPolicy.isAllowed(BASE+"sanftwecker-v9/../sanftwecker-release.apk"));
    }
    @Test public void parsesOnlyNumericOwnedReleaseTags(){
        assertEquals(9,UpdateUrlPolicy.versionCode("sanftwecker-v9"));
        assertEquals(118,UpdateUrlPolicy.versionCode("sanftwecker-v118"));
        assertEquals(-1,UpdateUrlPolicy.versionCode("untrusted"));
        assertEquals(-1,UpdateUrlPolicy.versionCode("sanftwecker-v0.beta"));
    }
    @Test public void releaseAssetMustMatchActualReleaseTag(){
        assertTrue(UpdateUrlPolicy.isForTag(BASE+"sanftwecker-v10/sanftwecker-release.apk",
                "sanftwecker-v10"));
        assertFalse(UpdateUrlPolicy.isForTag(BASE+"sanftwecker-v9/sanftwecker-release.apk",
                "sanftwecker-v10"));
        assertFalse(UpdateUrlPolicy.isForTag(BASE+"sanftwecker-v10/sanftwecker-beta-debug.apk",
                "sanftwecker-v10"));
    }
    @Test public void versionCodesStayInAndroidRange(){
        assertEquals(10,UpdateUrlPolicy.versionCode("sanftwecker-v10"));
        assertEquals(2100000000L,UpdateUrlPolicy.versionCode("sanftwecker-v2100000000"));
        assertEquals(-1,UpdateUrlPolicy.versionCode("sanftwecker-v2100000001"));
        assertEquals(-1,UpdateUrlPolicy.versionCode("sanftwecker-v01"));
        assertEquals(-1,UpdateUrlPolicy.versionCode("sanftwecker-v0"));
    }
    @Test public void neverSuggestsDowngrade(){
        assertFalse(UpdateUrlPolicy.isNewer(8,9));
        assertFalse(UpdateUrlPolicy.isNewer(9,9));
        assertTrue(UpdateUrlPolicy.isNewer(10,9));
    }
}
