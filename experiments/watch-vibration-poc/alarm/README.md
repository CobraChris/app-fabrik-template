# Sanftwecker (Test) · Android-Beta v0.3.0

**Status: fertig gebauter Entwicklungsstand, noch nicht auf Samsung-Hardware für Nachtbetrieb abgenommen.** Nicht als einzigen Wecker nutzen.

## Ziel

Eine werbefreie, rein lokale Android-Wecker-App, die **ab exakt 0 % eigenem Audio-Gain** startet und ihre Lautstärke sehr langsam steigert. Gleichzeitig erzeugt sie eigenständige, tonlose Benachrichtigungen zur Spiegelung auf Samsung Galaxy Watches. Der vorausgehende Test mit `Watch-Benachrichtigungstest` hat die Spiegelung bei gesperrtem Telefon bestätigt; ob diese neuen Alarmbenachrichtigungen im Schlafmodus vibrieren, ist noch offen.

## Implementiert

- Drei unabhängige Wecker (Werktage, Wochenende, Zusatz) mit Namen, Weckzeit, frei wählbaren Wochentagen und Aktivierung. Alle drei sind bei Neuinstallation **deaktiviert**.
- `AlarmManager.setAlarmClock` für jede aktivierte Weckzeit mit separatem `PendingIntent`; Uhrzeit und Wochentag werden bei jedem Feuern neu geplant, nicht als starres 24-Stunden-Intervall.
- Neuplanung nach Neustart, App-Update, Zeitzonen-/Uhrzeitwechsel und nach erneut erteilter exakter Alarmberechtigung.
- **Ton-Verzögerung 0–300 Sekunden**: Galaxy Watch kann zuerst vibrieren, bevor der Handyton beginnt (abhängig von Samsungs Spiegelung).
- Quadratischer, sehr sanfter Gain-Anstieg: `gain = maxGain × (elapsed / rampDuration)²`, beginnt exakt bei null. 1–30 Minuten einstellbar; App-Maximum 5–100 % des aktuellen Android-System-Weckerpegels. Kein Heraufsetzen der globalen Handylautstärke.
- Drei ausschließlich im Code erzeugte Töne, keine Musikdateien, keine Medienrechte oder Server.
- 0–12 eigenständige Benachrichtigungen im Abstand von 10–120 Sekunden, auf einem separaten Kanal `Vibration an Galaxy Watch` (hochpriorisiert, ohne Sound, mit Vibration). Die frühere Test-App nutzt eine andere Paket-ID und bleibt unverändert.
- Stoppen, Schlummern 1–30 Minuten und automatisches Stoppen nach spätestens 20 Minuten; Bedienelemente in der App und Android-Benachrichtigung. Aktionen auf der Galaxy Watch sind über gespiegelt angezeigte Benachrichtigungsaktionen **möglicherweise**, aber nicht garantiert verfügbar.
- WakeLock mit Zeitlimit, dauerhafte Vordergrund-Benachrichtigung nur während des aktiven Klingelns und gesonderte Zustandsprotokollierung (lokal).
- Diagnostik: für jeden Wecker die nächste geplante Auslösung, letzter interner Alarmzustand, Benachrichtigungen, exakte Alarmberechtigung, null eingestellte Systemalarm-Lautstärke und Batteriesparmodus.

## Automatische Qualitätssicherung

`gradle -p experiments/watch-vibration-poc :alarm:testDebugUnitTest :alarm:assembleDebug`

JUnit prüft Wochentagslogik, Werktag/Wochenende, die DST-Umstellungen in Deutschland, Null-Gain, sanfte Kurve, Grenzfälle und die Standardprofile. Der Github-Workflow prüft außerdem, dass keine Internet-/Speicherberechtigungen eingeführt werden und erstellt die APK.

## Spätere Samsung-Abnahme

1. Nur `sanftwecker-beta-debug.apk` aus dem neuesten erfolgreich abgeschlossenen Build auf dem **Handy** installieren. Die App heißt ausdrücklich `Sanftwecker (Test)`.
2. Android `Benachrichtigungen` und `Alarme und Erinnerungen` erlauben.
3. In `Galaxy Wearable → Uhreneinstellungen → Benachrichtigungen → App-Benachrichtigungen` **Sanftwecker (Test)** aktivieren (nicht `Watch-Benachrichtigungstest`).
4. Einen **Testwecker in 30 Sekunden** auslösen. Ton und Watch bei **gesperrtem** Handy prüfen. Bei gesetzter Tonverzögerung ertönt zunächst kein Ton; die Watch kann zuerst vibrieren.
5. Stoppen/Schlummern ausprobieren; die Konfiguration eines Werktag- und eines Wochenendalarms testen.
6. Später: Schlafmodus, Nicht stören, Batterierestriktionen, Neustart, Änderungen von Uhrzeit/Zeitzone, Bluetooth-Trennung und längere Nacht-Tests. Erst danach den bisherigen Wecker ersetzen.

**Für den ersten Versuch:** Tonverzögerung 0–30 Sekunden, Lautstärkeanstieg 5–10 Minuten, maximale App-Lautstärke 30–45 %, sechs Watch-Impulse im 20-Sekunden-Abstand. Die richtige Lautstärke muss am Gerät ermittelt werden.

## Technische Grenzen und Risiko

- Galaxy Watch wird ausschließlich über Samsungs Standard-Benachrichtigungsspiegelung angesprochen. Keine Wear-OS-App, kein ADB, keine versteckten Schnittstellen. **Samsung kann Watch-Vibrationen filtern, gruppieren oder bei Schlafmodus/Nicht stören vollständig unterdrücken.** Die Android-App kann den tatsächlichen Empfang der Uhr nicht messen.
- Die App prüft nicht, ob die Watch verbunden ist. Deshalb muss auch bei Verbindungsverlust der Handyton zuverlässig genug sein.
- Die Gesamtlautstärke hängt von Samsungs System-Weckerpegel ab. Bei System-Weckerlautstärke 0 hört man trotz App-Kurve nichts.
- Foreground-Service, Energiesparmodus und gerätespezifische Android-Beschränkungen benötigen einen realen Nacht-/Standby-Test. Ein erfolgreicher CI-Build ist **keine** Nachtfreigabe.
- Debug-Signatur von GitHub Actions ist vorerst nicht stabil. Bei APK-Signaturkonflikten die alte **Sanftwecker (Test)**-App deinstallieren und neue Version installieren; dabei gehen lokale Wecker-Einstellungen verloren. Für spätere Updates wird eine sichere dauerhafte Releasesignatur benötigt, die **nicht** im öffentlichen Repository liegen darf.
- Alle Daten bleiben auf dem Gerät. Keine Werbung, kein Account, kein Tracking, keine Netzwerkberechtigung, kein kostenpflichtiger Dienst.

## Entwicklungsaufbau

- `AlarmProfile` / `AlarmRepository`: drei unabhängige, validierte lokale Weckerprofile.
- `NextAlarmMath` / `AlarmScheduler`: lokale Wochentagsrechnung und exakte Systemalarme.
- `AlarmReceiver` / `RescheduleReceiver`: Auslösung und Systemereignisse.
- `RingingService`: AudioTrack mit Null-Start, Gain-Kurve, Watch-Benachrichtigungen, Stop, Schlummern.
- `MainActivity`: komplett ohne Drittanbieterbibliotheken programmierte Android-Bedienung.
- `GentleGain`: reine, separat getestete Lautstärkekurve.

Die Android- und Wear-OS-Dateien des früheren PoC bleiben als Forschungsergebnis im Repository, sind für diese Beta **nicht** nötig.
