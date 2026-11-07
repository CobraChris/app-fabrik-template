package com.cobrachris.fahrcockpit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public final class BlitzerContractTest {
    @Test
    public void usesOfficialPackageAndReceiver() {
        assertEquals("de.blitzer.plus", BlitzerContract.PACKAGE);
        assertEquals("de.blitzer.service.BlitzerBroadcastReceiver",
            BlitzerContract.RECEIVER);
    }

    @Test
    public void startAndDisplayModesUseAppModeAction() {
        for (BlitzerContract.Command cmd : new BlitzerContract.Command[] {
            BlitzerContract.Command.START,
            BlitzerContract.Command.WIDGET,
            BlitzerContract.Command.BACKGROUND
        }) {
            assertEquals("de.blitzer.APP_MODE", BlitzerContract.actionFor(cmd));
        }
        assertEquals("mode", BlitzerContract.EXTRA_MODE);
        assertEquals("START", BlitzerContract.modeFor(BlitzerContract.Command.START));
        assertEquals("WIDGET", BlitzerContract.modeFor(BlitzerContract.Command.WIDGET));
        assertEquals("BACKGROUND", BlitzerContract.modeFor(BlitzerContract.Command.BACKGROUND));
    }

    @Test
    public void stopUsesKillActionWithoutModeExtra() {
        assertEquals("de.blitzer.KILL_APP",
            BlitzerContract.actionFor(BlitzerContract.Command.STOP));
        assertNull(BlitzerContract.modeFor(BlitzerContract.Command.STOP));
    }
}
