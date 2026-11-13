package com.cobrachris.fahrcockpit;
import static org.junit.Assert.*;
import org.junit.Test;
public final class NavigationSdkKeyStoreTest {
    @Test public void keyMustBeGenuineGoogleApiShape() {
        assertTrue(NavigationSdkKeyStore.validKey("AIza" + "A".repeat(35)));
        assertFalse(NavigationSdkKeyStore.validKey("AIza"));
        assertFalse(NavigationSdkKeyStore.validKey("other" + "A".repeat(40)));
        assertFalse(NavigationSdkKeyStore.validKey("AIza" + "A".repeat(35) + "\n"));
        assertFalse(NavigationSdkKeyStore.validKey(null));
    }
}
