# Galaxy Watch: Benachrichtigungs-PoC, Standby-Test v2

**Status:** Manueller Machbarkeitstest, **kein** Wecker und kein verlässlicher Alarm.

## Befund
- Sofortige Benachrichtigungen bei aktivem Handybildschirm lösen auf der Galaxy Watch Vibration aus.
- Frühere 10-Sekunden-Verzögerung verwendete `Handler.postDelayed`. Das läuft nicht zuverlässig im Deep Sleep und kann im Hintergrund verzögert oder bei beendetem Prozess verloren gehen.
- Diese Version verwendet für den zeitgesteuerten Test `AlarmManager.setExactAndAllowWhileIdle` mit einem manifestregistrierten `BroadcastReceiver`, der unabhängig von der geöffneten Activity eine Benachrichtigung sendet.
- Erfordert bei Android 12+ den speziellen Zugriff `Alarme und Erinnerungen` (`SCHEDULE_EXACT_ALARM`). Die App öffnet dessen Einstellungsdialog, sofern erforderlich.
- Weckerfunktion mit leise startendem Ton, Wiederholungen und ausreichender Betriebssicherheit wird erst nach dem erfolgreichen Standby-Test entwickelt.

## Auf dem Handy testen
1. Neue `watch-notification-debug.apk` auf dem Handy installieren. Wegen der GitHub-Debugsignatur kann es nötig sein, die vorige **Watch-Benachrichtigungstest**-App vorher zu deinstallieren; **nicht** die Samsung Uhr oder andere Apps entfernen.
2. In Android und in Galaxy Wearable → Uhreneinstellungen → Benachrichtigungen → App-Benachrichtigungen die App **Watch-Benachrichtigungstest** erlauben.
3. `Jetzt senden (Kontrolltest)` drücken und Watch-Vibration prüfen.
4. `In 20 Sekunden senden (Handy sperren)` drücken. Wenn Android um `Alarme und Erinnerungen` bittet: erlauben, zur App zurückkehren und erneut den Test starten.
5. Handy unmittelbar sperren. Uhr beobachten. Dann Handy entsperren und in der Test-App die Zeile `Letzte Benachrichtigung vom Handy ausgelöst` ablesen.
   - Zeitstempel neu + Uhr vibriert → einfache Benachrichtigungsspiegelung auch im Standby funktioniert.
   - Zeitstempel neu + Uhr schweigt → Galaxy Wearable, Watch-Nicht-stören-/Schlafmodus und Benachrichtigungseinstellungen prüfen.
   - Zeitstempel unverändert → Timer/Android-Ausführung bzw. Benachrichtigungsberechtigung prüfen. Zusätzlich prüfen, ob Android die Benachrichtigung im Benachrichtigungsfeld zeigt.

## Technik & Grenzen
- Nur Phone-APK. Keine zusätzliche Watch-App, kein ADB, kein Hotel-WLAN, keine Kosten, keine externen Dienste.
- Smartphone-Benachrichtigungen könnten ebenfalls vibrieren.
- Ob Samsung die Benachrichtigungen auf die Uhr weiterleitet, ist außerhalb der Kontrolle des Codes und ohne Empfangsbestätigung nicht beweisbar.
- In Android kann trotz exakter Berechtigung die praktische Zustellung weiter von Geräte- und Energiebedingungen beeinflusst werden.
- PoC nicht als alleinigen Wecker nutzen. Wenn der Test funktioniert, später Nachtbetrieb, Neustarts und Akkuoptimierung separat testen.
- ADB- und WLAN-Debugging nach dem alten Test ausschalten.
