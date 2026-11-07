package de.cobrachris.fahrcockpit;

import org.junit.Test;
import static org.junit.Assert.*;

public class SeekMathTest {
    @Test public void forwardAndBackward15Seconds() {
        assertEquals(35_000L, SeekMath.target(20_000L, 15_000L, 120_000L));
        assertEquals(5_000L, SeekMath.target(20_000L, -15_000L, 120_000L));
    }
    @Test public void clampToStartAndTrackEnd() {
        assertEquals(0L, SeekMath.target(3_000L, -15_000L, 120_000L));
        assertEquals(120_000L, SeekMath.target(118_000L, 15_000L, 120_000L));
    }
    @Test public void unknownPositionIsRejectedAndOverflowIsClamped() {
        assertEquals(-1L, SeekMath.target(-1L, 15_000L, 120_000L));
        assertEquals(120_000L, SeekMath.target(Long.MAX_VALUE - 1, 15_000L, 120_000L));
        assertEquals(0L, SeekMath.target(0L, Long.MIN_VALUE, -1L));
    }
}
