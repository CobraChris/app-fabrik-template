package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** No Android dependencies; catches MediaSession stale-position regressions. */
public final class SpotifySeekRulesTest {
    @Test public void seekingWhilePlayingUsesElapsedTime() {
        // Spotify reported 60 s two seconds ago. Forward should seek to 77 s.
        assertEquals(77_000L, SpotifySeekRules.seekTarget(
            60_000L, true, 1.0f, 10_000L, 12_000L, 180_000L, 15_000));
        assertEquals(47_000L, SpotifySeekRules.seekTarget(
            60_000L, true, 1.0f, 10_000L, 12_000L, 180_000L, -15_000));
    }

    @Test public void pausedSessionDoesNotDrift() {
        assertEquals(75_000L, SpotifySeekRules.seekTarget(
            60_000L, false, 1.0f, 10_000L, 90_000L, 180_000L, 15_000));
    }

    @Test public void respectsPlaybackSpeed() {
        assertEquals(90_000L, SpotifySeekRules.seekTarget(
            60_000L, true, 1.5f, 10_000L, 20_000L, 180_000L, 15_000));
    }

    @Test public void clampsAtTrackStartAndDuration() {
        assertEquals(0L, SpotifySeekRules.seekTarget(
            2_000L, false, 1f, 10_000L, 11_000L, 180_000L, -15_000));
        assertEquals(180_000L, SpotifySeekRules.seekTarget(
            178_000L, true, 1f, 10_000L, 30_000L, 180_000L, 15_000));
    }

    @Test public void invalidTimestampsNeverAdvancePosition() {
        assertEquals(75_000L, SpotifySeekRules.seekTarget(
            60_000L, true, 1f, 13_000L, 12_000L, 180_000L, 15_000));
        assertEquals(75_000L, SpotifySeekRules.seekTarget(
            60_000L, true, 1f, 0L, 12_000L, 180_000L, 15_000));
    }

    @Test public void invalidSpeedsNeverExtrapolatePosition() {
        assertEquals(75_000L, SpotifySeekRules.seekTarget(
            60_000L, true, Float.NaN, 10_000L, 12_000L, 180_000L, 15_000));
        assertEquals(75_000L, SpotifySeekRules.seekTarget(
            60_000L, true, -1f, 10_000L, 12_000L, 180_000L, 15_000));
        assertEquals(75_000L, SpotifySeekRules.seekTarget(
            60_000L, true, Float.POSITIVE_INFINITY, 10_000L, 12_000L, 180_000L, 15_000));
    }

    @Test public void unknownDurationAndUnknownPositionBehaveSafely() {
        assertEquals(125_000L, SpotifySeekRules.seekTarget(
            100_000L, true, 1f, 10_000L, 20_000L, -1L, 15_000));
        assertEquals(-1L, SpotifySeekRules.seekTarget(
            -1L, true, 1f, 10_000L, 20_000L, 180_000L, 15_000));
    }
}
