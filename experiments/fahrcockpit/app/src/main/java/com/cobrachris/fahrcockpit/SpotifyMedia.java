package com.cobrachris.fahrcockpit;

import android.os.SystemClock;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import java.util.List;

final class SpotifyMedia {
    private static final String SPOTIFY = "com.spotify.music";

    enum Action { TOGGLE, PREVIOUS, NEXT, SEEK_BACK, SEEK_FORWARD }

    static final class State {
        final String line;
        final boolean connected;
        final boolean playing;
        final boolean canSeek;
        final boolean canPrevious;
        final boolean canNext;
        final long positionMs;
        final long durationMs;

        State(String line, boolean connected, boolean playing,
              boolean canSeek, boolean canPrevious, boolean canNext,
              long positionMs, long durationMs) {
            this.line = line;
            this.connected = connected;
            this.playing = playing;
            this.canSeek = canSeek;
            this.canPrevious = canPrevious;
            this.canNext = canNext;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
        }
    }

    private SpotifyMedia() {}

    private static MediaController spotify(Context context) {
        try {
            if (!Permissions.isMediaListenerEnabled(context)) {
                return null;
            }
            MediaSessionManager manager = (MediaSessionManager)
                context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (manager == null) return null;
            ComponentName listener = new ComponentName(context,
                SpotifyNotificationListener.class);
            List<MediaController> sessions = manager.getActiveSessions(listener);
            for (MediaController controller : sessions) {
                if (SPOTIFY.equals(controller.getPackageName())) return controller;
            }
        } catch (SecurityException | IllegalStateException ignored) {
            // The listener can be disabled or disconnected by Android.
        }
        return null;
    }

    static State read(Context context) {
        MediaController controller = spotify(context);
        if (controller == null) {
            // Distinguish a missing Android grant from a granted but inactive
            // Spotify session. Both appeared identical in the first device test.
            return new State(SpotifyDisplayRules.notConnected(
                    Permissions.isMediaListenerEnabled(context)),
                    false, false, false, false, false, -1L, -1L);
        }
        PlaybackState playback = controller.getPlaybackState();
        long actions = playback == null ? 0 : playback.getActions();
        MediaMetadata metadata = controller.getMetadata();
        String title = metadata == null ? null :
                metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata == null ? null :
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String label = SpotifyDisplayRules.trackLabel(title, artist);
        boolean playing = playback != null && playback.getState() == PlaybackState.STATE_PLAYING;
        long duration = metadata == null ? -1L :
            metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long position = playback == null ? -1L : SpotifySeekRules.seekTarget(
            playback.getPosition(), playing, playback.getPlaybackSpeed(),
            playback.getLastPositionUpdateTime(), SystemClock.elapsedRealtime(),
            duration, 0);
        return new State(label, true, playing,
            (actions & PlaybackState.ACTION_SEEK_TO) != 0,
            (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0,
            (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0,
            position, duration);
    }

    /** User-triggered seek; requires Spotify to advertise SEEK_TO and duration. */
    static String seekAbsolute(Context context, long positionMs) {
        MediaController controller = spotify(context);
        if (controller == null) return "Spotify nicht verbunden.";
        PlaybackState playback = controller.getPlaybackState();
        MediaMetadata metadata = controller.getMetadata();
        long duration = metadata == null ? -1L
            : metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        if (playback == null || (playback.getActions() & PlaybackState.ACTION_SEEK_TO) == 0L
                || duration <= 0L) {
            return "Dieser Spotify-Inhalt erlaubt kein freies Spulen.";
        }
        try {
            controller.getTransportControls().seekTo(Math.max(0L,
                Math.min(duration, positionMs)));
            return "Wiedergabeposition angefordert";
        } catch (RuntimeException ignored) {
            return "Spotify hat die neue Wiedergabeposition nicht angenommen.";
        }
    }

    static String send(Context context, Action action) {
        MediaController controller = spotify(context);
        if (controller == null) return "Spotify-Mediensitzung fehlt oder Medienzugriff ist aus.";
        PlaybackState playback = controller.getPlaybackState();
        if (playback == null) return "Spotify meldet keinen Wiedergabestatus.";
        long allowed = playback.getActions();
        MediaController.TransportControls controls = controller.getTransportControls();
        try {
            switch (action) {
                case TOGGLE:
                    if (playback.getState() == PlaybackState.STATE_PLAYING
                        && (allowed & PlaybackState.ACTION_PAUSE) != 0) {
                        controls.pause(); return "Pause angefordert";
                    }
                    if ((allowed & PlaybackState.ACTION_PLAY) != 0) {
                        controls.play(); return "Wiedergabe angefordert";
                    }
                    break;
                case PREVIOUS:
                    if ((allowed & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
                        controls.skipToPrevious(); return "Vorheriger Titel angefordert";
                    }
                    break;
                case NEXT:
                    if ((allowed & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
                        controls.skipToNext(); return "Nächster Titel angefordert";
                    }
                    break;
                case SEEK_BACK:
                case SEEK_FORWARD:
                    if ((allowed & PlaybackState.ACTION_SEEK_TO) != 0) {
                        MediaMetadata meta = controller.getMetadata();
                        long duration = meta != null
                            && meta.containsKey(MediaMetadata.METADATA_KEY_DURATION)
                                ? meta.getLong(MediaMetadata.METADATA_KEY_DURATION) : -1L;
                        int delta = action == Action.SEEK_BACK ? -15_000 : 15_000;
                        long target = SpotifySeekRules.seekTarget(
                            playback.getPosition(),
                            playback.getState() == PlaybackState.STATE_PLAYING,
                            playback.getPlaybackSpeed(),
                            playback.getLastPositionUpdateTime(),
                            SystemClock.elapsedRealtime(),
                            duration, delta);
                        if (target < 0) break;
                        controls.seekTo(target);
                        return "15 Sekunden springen angefordert";
                    }
                    break;
            }
        } catch (RuntimeException ignored) {
            return "Spotify hat den Befehl nicht angenommen.";
        }
        return "Dieser Befehl wird vom aktuellen Spotify-Inhalt nicht unterstützt.";
    }
}
