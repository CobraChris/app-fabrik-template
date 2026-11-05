# Sanftwecker (Test): Android-Wecker mit Galaxy-Watch-Benachrichtigungen

**Status: erste funktionale Vorschau, noch kein verlässlicher Nachtwecker.**

Die vorherige `Watch-Benachrichtigungstest`-App hat erfolgreich gezeigt, dass Benachrichtigungen auch bei gesperrtem Smartphone auf der Galaxy Watch eine Vibration auslösen. Diese neue, getrennte App heißt **Sanftwecker (Test)** und verwendet **keine** Watch-APK oder ADB.

## V1-Funktionen
- **Eine** wiederkehrende Weckzeit, Wochentage Mo–So frei auswählbar, standardmäßig Mo–Fr; anfangs **deaktiviert**.
- Android `AlarmManager.setAlarmClock` für präzise Alarme; Wiederherstellung nach Gerätestart, Update, Uhrzeit- und Zeitzonenänderung.
- Lokaler Original-Weckton über `AudioTrack`, **App-Gain beginnt bei exakt 0**, gleitet über 1–15 Minuten quadratisch zur konfigurierten Obergrenze (10–100 % des App-Ausgangspegels). **Die Android-System-Weckerlautstärke muss über 0 liegen** und beeinflusst den tatsächlichen Schallpegel.
- Foreground-Service mit Medienwiedergabe und zeitlich begrenztem WakeLock; stoppt spätestens nach 15 Minuten.
- Sechs eigenständige, tonlose hochpriorisierte Handy-Benachrichtigungen alle 20 Sekunden ab Alarmbeginn; Samsung Galaxy Wearable kann sie auf die Uhr spiegeln. Wiederholtes Vibrieren ist **nicht garantiert**, besonders im Schlaf-/Nicht-stören-Modus.
- Laufende Tonwiedergabe am Handy stoppen oder 5 Minuten schlummern, über Benachrichtigungsaktionen oder die App.
- Separater Test in 30 Sekunden bei gesperrtem Handy, ohne Änderung der wiederkehrenden Weckzeit.

## Nur auf dem Handy installieren
1. GitHub-Actions-Artefakt `sanftwecker-preview-apk` aus dem neuesten **erfolgreichen** Build entpacken.
2. **Ausschließlich `sanftwecker-preview-debug.apk`** installieren. Das ist eine neue Package-ID (`com.cobrachris.sanftwecker`); die alte App `Watch-Benachrichtigungstest` muss nicht gelöscht werden.
3. `Sanftwecker (Test)` öffnen. Android-Benachrichtigungen und auf Aufforderung einmalig `Alarme und Erinnerungen` für **diese** App erlauben.
4. In **Galaxy Wearable → Uhreneinstellungen → Benachrichtigungen → App-Benachrichtigungen → Sanftwecker (Test)** den Schalter einschalten (nicht mit der bisherigen Test-App verwechseln).
5. Zum ersten Test `Testwecker in 30 Sekunden` nutzen, Handy sperren, Weckton und Watch-Vibration beobachten. Über die dauerhafte Benachrichtigung `Stoppen` betätigen oder die App öffnen.
6. Danach Wochentage, Zeit, Rampendauer und App-Maximum einstellen und **Wecker speichern und aktivieren** drücken. Die App zeigt den nächsten geplanten Alarm an.

## Wichtige Einschränkungen vor echter Nutzung
- **Noch keine Nachtfreigabe:** Tests bei Schlafmodus, Nicht stören, Neustart, Energiesparen, leerem Akku, Bluetooth-Trennung und tagelangem Betrieb fehlen. Bitte den bisherigen zuverlässigen Wecker behalten.
- Galaxy-Watch-Benachrichtigung ist keine native Wear-OS-Alarmfunktion; die Uhr kann stumm bleiben, selbst wenn das Handy klingelt. Im Schlafmodus kann Samsung Benachrichtigungen blockieren.
- Watch-Benachrichtigungen können auch Vibration am Handy auslösen; es gibt keine direkte Rückmeldung über eine erfolgte Watch-Vibration.
- Stoppen und Schlummern auf der **Uhr** sind noch nicht implementiert. In der Android-Handy-Benachrichtigung sind die Aktionen verfügbar.
- Exakte Alarmberechtigung muss bestehen. Wenn Android den App-Prozess durch `Stopp erzwingen` anhält oder die App deinstalliert, werden keine Alarme ausgelöst.
- Die App nimmt **keine** Änderung an globaler Systemlautstärke vor. Eine System-Weckerlautstärke von 0 kann daher zu völliger Stille führen.
- GitHub-Debugsignaturen können von Build zu Build wechseln. Bei Signaturkonflikten nur die Vorschau-App `Sanftwecker (Test)` deinstallieren und erneut installieren; danach Einstellungen und Alarm neu setzen.
- Keine Werbung, kein Abo, keine Cloud, keine externen APIs, kein Tracking und keine Kosten.

## Technischer Aufbau
`alarm/` ist eine von den beiden früheren PoC-Modulen unabhängige Android-App.
`AlarmScheduler` – eine nächste Woche/Tag-Auslösung sowie Vorschau und Snooze.
`NextAlarmMath` – Wochentagslogik, mit JUnit getestet.
`AlarmReceiver` – Hintergrundstart des Foreground-Service.
`RingingService` – eigene Audioerzeugung, Rampenkurve, Watch-Nachrichten, Stop/Snooze.
`RescheduleReceiver` – Wiederherstellung geplanter Alarme nach Systemereignissen.

**Offene Schritte:** reale Samsung-Abnahme, DND/Schlafmodus-Test, Alarm-Zuverlässigkeit unter Force-Stop/Energiesparen, längere Feldtests und ggf. vereinfachte Signierung/Installation.
