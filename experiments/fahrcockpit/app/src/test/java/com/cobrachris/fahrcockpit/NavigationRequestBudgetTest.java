package com.cobrachris.fahrcockpit;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NavigationRequestBudgetTest {
    @Test public void permitsOnlyTwentyRequestsOnSameUtcDay() {
        long day = 20_000L;
        assertTrue(NavigationRequestBudget.mayReserve(day, 0, day));
        assertTrue(NavigationRequestBudget.mayReserve(day, 19, day));
        assertFalse(NavigationRequestBudget.mayReserve(day, 20, day));
        assertFalse(NavigationRequestBudget.mayReserve(day, 21, day));
        assertFalse(NavigationRequestBudget.mayReserve(day, -1, day));
    }

    @Test public void newUtcDayResetsAllowanceWithoutAcceptingClockRollback() {
        long day = 20_000L;
        assertTrue(NavigationRequestBudget.mayReserve(day, 20, day + 1));
        assertFalse(NavigationRequestBudget.mayReserve(day, 2, day - 1));
        assertTrue(NavigationRequestBudget.mayReserve(-1L, 0, day));
        assertFalse(NavigationRequestBudget.mayReserve(day, 0, -1L));
    }
}
