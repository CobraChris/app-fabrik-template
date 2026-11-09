package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class EmbeddedMapRulesTest {
    @Test public void onlyHttpsMapHostIsAllowed() {
        assertEquals("https://cobrachris.github.io/app-fabrik-template/apps/fahrcockpit-map/",
            EmbeddedMapRules.URL);
        assertTrue(EmbeddedMapRules.allowsPage(EmbeddedMapRules.URL));
        assertTrue(EmbeddedMapRules.allowsPage(EmbeddedMapRules.URL + "index.html#map"));
        assertFalse(EmbeddedMapRules.allowsPage(
            "https://cobrachris.github.io.evil.com/app-fabrik-template/apps/fahrcockpit-map/"));
        assertFalse(EmbeddedMapRules.allowsPage(
            "http://cobrachris.github.io/app-fabrik-template/apps/fahrcockpit-map/"));
        assertFalse(EmbeddedMapRules.allowsPage(
            "https://cobrachris.github.io/meine-livekarte/"));
        assertFalse(EmbeddedMapRules.allowsPage(
            "https://cobrachris.github.io/app-fabrik-template/apps/fahrcockpit-map/../../other/"));
        assertFalse(EmbeddedMapRules.allowsPage("javascript:alert(1)"));
        assertFalse(EmbeddedMapRules.allowsPage(null));
    }
}
