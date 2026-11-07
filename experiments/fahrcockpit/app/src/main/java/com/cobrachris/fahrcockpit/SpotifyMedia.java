package com.cobrachris.fahrcockpit;

import android.app.NotificationManager;
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

        State(String line, boolean connected, boolean playing,
              boolean canSeek, boolean canPrevious, boolean canNext) {
            this.line = line;
            this.connected = connected;
            this.playing = playing;
            this.canSeek = canSeek;
            this.canPrevious = canPrevious;
            this.canNext = canNext;
        }
    }

    private SpotifyMedia() {}

    private static MediaController spotify(Context context) {
        try {
            if (!NotificationManager.getEnabledListenerPackages(context)
                    .contains(context.getPackageName())) {
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
            return new State("Spotify nicht verbunden – Medienzugriff prüfen",
                    false, false, false, false, false);
        }
        PlaybackState playback = controller.getPlaybackState();
        long actions = playback == null ? 0 : playback.getActions();
        MediaMetadata metadata = controller.getMetadata();
        String title = metadata == null ? null :
                metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata == null ? null :
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String label = (title == null || title.isEmpty())
                ? "Spotify verbunden" : title;
        if (artist != null && !artist.isEmpty()) label += " · " + artist;
        boolean playing = playback != null && playback.getState() == PlaybackState.STATE_PLAYING;
        return new State(label, true, playing,
            (actions & PlaybackState.ACTION_SEEK_TO) != 0,
            (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0,
            (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
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
                        long position = playback.getPosition();
                        if (position < 0) break;
                        long delta = action == Action.SEEK_BACK ? -15000L : 15000L;
                        long target = Math.max(0L, position + delta);
                        MediaMetadata meta = controller.getMetadata();
                        if (meta != null && meta.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
                            long duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION);
                            if (duration > 0) target = Math.min(duration, target);
                        }
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
