package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public final class CockpitViewRulesTest {
    @Test public void driveButtonTogglesBetweenFullAndDriving() {
        assertEquals(CockpitViewRules.Mode.DRIVING,
            CockpitViewRules.driveToggle(CockpitViewRules.Mode.FULL));
        assertEquals(CockpitViewRules.Mode.FULL,
            CockpitViewRules.driveToggle(CockpitViewRules.Mode.DRIVING));
    }

    @Test public void driveButtonCanRestoreDirectlyFromCollapsed() {
        assertEquals(CockpitViewRules.Mode.DRIVING,
            CockpitViewRules.driveToggle(CockpitViewRules.Mode.COLLAPSED));
    }

    @Test public void automaticLandscapeOnlyCompactsFullView() {
        assertTrue(CockpitViewRules.shouldAutoDrive(
            true, true, CockpitViewRules.Mode.FULL));
        assertFalse(CockpitViewRules.shouldAutoDrive(
            false, true, CockpitViewRules.Mode.FULL));
        assertFalse(CockpitViewRules.shouldAutoDrive(
            true, false, CockpitViewRules.Mode.FULL));
        assertFalse(CockpitViewRules.shouldAutoDrive(
            true, true, CockpitViewRules.Mode.COLLAPSED));
    }

    @Test public void buttonsHaveClearReturnLabel() {
        assertEquals("Fahrt",
            CockpitViewRules.driveButtonLabel(CockpitViewRules.Mode.FULL));
        assertEquals("Voll",
            CockpitViewRules.driveButtonLabel(CockpitViewRules.Mode.DRIVING));
        assertEquals("Fahrt",
            CockpitViewRules.driveButtonLabel(CockpitViewRules.Mode.COLLAPSED));
    }
}
