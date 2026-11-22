package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * One persistent, fail-closed request gate shared by the Cockpit quick search
 * and the complete fuel settings/search screen. There is no background timer
 * and no retry job: only an explicit user action may call tryBegin().
 */
final class FuelRequestGate {
    private static final Object LOCK = new Object();
    private static final String PREFS = "fahrcockpit_fuel_api_gate_v1";
    private static final String LAST_ATTEMPT = "last_attempt";
    private static final String PAUSE_UNTIL = "pause_until";
    private FuelRequestGate() {}

    static final class Permit {
        final boolean allowed;
        final long remainingMillis;
        final boolean storageError;

        private Permit(boolean allowed, long remainingMillis, boolean storageError) {
            this.allowed = allowed;
            this.remainingMillis = remainingMillis;
            this.storageError = storageError;
        }
        String explanation() {
            if (storageError) return "API-Schutz nicht verfügbar. Kein Preisabruf gestartet.";
            return "Tankerkönig-Abfrage gesperrt. Erneut in "
                + FuelRateRules.remainingLabel(remainingMillis) + " möglich.";
        }
    }

    static Permit tryBegin(Context context) {
        synchronized (LOCK) {
            final SharedPreferences prefs =
                context.getApplicationContext().getSharedPreferences(
                    PREFS, Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            long wait = FuelRateRules.remainingMillis(now,
                prefs.getLong(LAST_ATTEMPT, 0L),
                prefs.getLong(PAUSE_UNTIL, 0L));
            if (wait > 0L) return new Permit(false, wait, false);
            // Commit synchronously BEFORE opening the network connection.
            // If a process crashes, another Activity still honors the slot.
            if (!prefs.edit().putLong(LAST_ATTEMPT, now).commit())
                return new Permit(false, FuelRateRules.MIN_REQUEST_INTERVAL_MS, true);
            return new Permit(true, 0L, false);
        }
    }

    static void reportHttp(Context context, int status) {
        long delay = FuelRateRules.backoffMillis(status);
        if (delay > 0L) pause(context, delay);
    }

    static void reportTransportFailure(Context context) {
        pause(context, FuelRateRules.TRANSIENT_BACKOFF_MS);
    }

    private static void pause(Context context, long millis) {
        synchronized (LOCK) {
            SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            long previous = prefs.getLong(PAUSE_UNTIL, 0L);
            // Preserve an existing, longer cooldown; no retries on failure.
            prefs.edit().putLong(PAUSE_UNTIL,
                Math.max(previous, now + millis)).commit();
        }
    }
}
