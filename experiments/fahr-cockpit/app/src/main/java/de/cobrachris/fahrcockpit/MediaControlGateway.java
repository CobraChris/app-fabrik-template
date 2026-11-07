package de.cobrachris.fahrcockpit;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import java.util.List;

/**
 * Native Android MediaSession transport controls. No Spotify OAuth or Web API.
 * Only talks to the official Spotify app's currently active MediaSession.
 */
public final class MediaControlGateway {
    public enum Command { PREVIOUS, NEXT, PLAY_PAUSE, REWIND_15, FORWARD_15 }
    private MediaControlGateway() {}

    public static String perform(Context context, Command command) {
        MediaSessionManager manager =
            (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (manager == null) return "Mediensystem nicht verfügbar.";
        final List<MediaController> sessions;
        try {
            sessions = manager.getActiveSessions(
                new ComponentName(context, MediaAccessListenerService.class));
        } catch (SecurityException exception) {
            return "Bitte Medien-/Benachrichtigungszugriff freigeben.";
        }
        MediaController spotify = null;
        for (MediaController session : sessions) {
            if ("com.spotify.music".equals(session.getPackageName())) {
                spotify = session;
                break;
            }
        }
        if (spotify == null) return "Keine aktive Spotify-Wiedergabe gefunden.";
        PlaybackState state = spotify.getPlaybackState();
        if (state == null) return "Spotify meldet noch keinen Wiedergabestatus.";
        long actions = state.getActions();
        MediaController.TransportControls controls = spotify.getTransportControls();

        if (command == Command.PREVIOUS) {
            if (!supports(actions, PlaybackState.ACTION_SKIP_TO_PREVIOUS))
                return "Vorheriger Titel wird hier nicht unterstützt.";
            controls.skipToPrevious();
            return null;
        }
        if (command == Command.NEXT) {
            if (!supports(actions, PlaybackState.ACTION_SKIP_TO_NEXT))
                return "Nächster Titel wird hier nicht unterstützt.";
            controls.skipToNext();
            return null;
        }
        if (command == Command.PLAY_PAUSE) {
            boolean playing = state.getState() == PlaybackState.STATE_PLAYING;
            long wanted = playing ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY;
            if (!supports(actions, wanted) && !supports(actions, PlaybackState.ACTION_PLAY_PAUSE))
                return "Play/Pause wird hier nicht unterstützt.";
            if (playing) controls.pause();
            else controls.play();
            return null;
        }
        if (!supports(actions, PlaybackState.ACTION_SEEK_TO))
            return "Spulen wird für diesen Inhalt nicht unterstützt.";
        long delta = command == Command.REWIND_15 ? -15_000L : 15_000L;
        MediaMetadata metadata = spotify.getMetadata();
        long duration = metadata == null ? -1L :
            metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long target = SeekMath.target(state.getPosition(), delta, duration);
        if (target < 0) return "Die aktuelle Position ist nicht verfügbar.";
        controls.seekTo(target);
        return null;
    }

    private static boolean supports(long actions, long action) {
        return (actions & action) != 0L;
    }
}
