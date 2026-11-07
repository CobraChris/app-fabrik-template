package com.cobrachris.fahrcockpit;

/**
 * Documented external broadcast contract. Source:
 * https://www.blitzer.de/questions/wie-richte-ich-den-automatischen-start-stopp-per-tasker-ein/
 *
 * The provider does not document any status/acknowledgement interface.
 */
public final class BlitzerContract {
    public static final String PACKAGE = "de.blitzer.plus";
    public static final String RECEIVER = "de.blitzer.service.BlitzerBroadcastReceiver";
    public static final String ACTION_MODE = "de.blitzer.APP_MODE";
    public static final String ACTION_STOP = "de.blitzer.KILL_APP";
    public static final String EXTRA_MODE = "mode";

    public enum Command {
        START,
        WIDGET,
        BACKGROUND,
        STOP
    }

    private BlitzerContract() {
    }

    public static String actionFor(Command command) {
        return command == Command.STOP ? ACTION_STOP : ACTION_MODE;
    }

    /** Returns null for STOP, which does not require an extra. */
    public static String modeFor(Command command) {
        switch (command) {
            case START:
                return "START";
            case WIDGET:
                return "WIDGET";
            case BACKGROUND:
                return "BACKGROUND";
            case STOP:
                return null;
            default:
                throw new IllegalArgumentException("Unknown command");
        }
    }
}
