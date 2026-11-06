# Sanftwecker (Test) – Beta 0.8.1

**Aktueller Stand, 06.11.2026:** Signierte Beta 0.8.0 (Build 9) und Beta 0.8.1 (Build 10) wurden automatisch erstellt und als GitHub-Prereleases veröffentlicht. Die Android-Build-Tests und der Zertifikatsvergleich für das In-place-Update sind erfolgreich. **Samsung-/Galaxy-Watch-Abnahme bleibt offen.** Siehe [aktuelle Update-Anleitung](UPDATE_SETUP.md).

Die App verbleibt bis zur Abnahme im eigenen Entwicklungszweig und wird nicht produktiv in `main` übernommen.

---

## Entwicklungshistorie: Beta 0.7.0

## Neues Klingel-Design (06.11.2026)

Nach erfolgreichem Samsung-Praxistest (regulärer Alarm, Ton und Galaxy-Watch-Vibration wurden bestätigt) erhält die Android-App einen eigenständigen Weckbildschirm im Stil der vom Nutzer bereitgestellten Referenzbilder:

- Vollflächiger, **original programmierter türkis-dunkler Facettenhintergrund** (keine fremden Bilddateien, keine Wetter-API).
- Name des aktiven Alarms oben; **sehr große, zweizeilige aktuelle Uhrzeit** (HH über MM) in heller, dünner Schrift.
- **Zum Abstellen nach rechts schieben:** großer, weißer Schieber mit Schloss-Symbol; Abstellen erst beim Loslassen ab 90 % Weg, damit unbeabsichtigtes Ziehen nicht ausschaltet.
- Separater **Schlummern-Button**, der die zum Wecker passende Minutenzahl anzeigt und bei deaktivierter Schlummerfunktion verschwindet.
- Erreichbare **„Alternativ: direkt stoppen“**-Aktion für Menschen, die nicht schieben können oder Screenreader nutzen.
- Zeitanzeige aktualisiert sich im Vordergrund. Die Anzeige wird geschlossen, wenn der Alarmdienst ein Ende signalisiert, beispielsweise beim Stoppen über eine Benachrichtigung.
- \`setShowWhenLocked\` und \`setTurnScreenOn\` bleiben erhalten, aber die Gerätesperre wird **nicht** programmgesteuert aufgehoben. Ob Android eine Fullscreen-Ansicht anzeigt, hängt weiterhin von der Systemberechtigung ab.

## Schonende neue Standardwerte

Nur für **neu angelegte** Wecker (bereits gespeicherte bleiben unverändert):
- Galaxy-Watch-Benachrichtigungen: sofort, 6 Impulse alle 20 Sekunden
- Handyton: **+10 Sekunden Verzögerung** statt direkt / sehr lange Wartezeit
- Lautstärke: **1 Minute sanfter Anstieg** ab tatsächlichem Audio-Gain 0
- Maximaler **App-Pegel 55 %** relativ zur Android-System-Weckerlautstärke

Der Pegel ist kein absoluter Schalldruck: Bei zu niedriger Android-Weckerlautstärke bleibt die Ausgabe leise. Die Detailseite bietet deshalb die Aktion **„Sanft & sicher: empfohlene Werte übernehmen“** für einen vorhandenen Wecker – nur nach Bestätigung und anschließendem Speichern.

## Schutz vor unbeabsichtigten Änderungen

Bestehende Wecker behalten ihren bisherigen Namen, Uhrzeit, Wochentage, Aktivierung, Tonverzögerung, Kurve, Lautstärke-Obergrenze und Watch-Impulse. Keine automatische Migration der vorhandenen Pegel-/Alarmwerte. Die Test-APK wird nur auf dem separaten Entwicklungszweig gebaut; kein Merge in \`main\`.

## Automatisierte Prüfung

Gradle: \`:alarm:testDebugUnitTest :alarm:assembleDebug :alarm:lintDebug\`.
Neue Tests: Startverzögerung, Rampenende, Maximalpegel und unverändertes Kopieren benutzerdefinierter Wecker. Ein erfolgreiches CI-Ergebnis bestätigt nicht die tatsächliche Darstellung auf einem Samsung-Gerät.

## Noch zu testen

1. Zeigt die neue **Vollbild-Klingelansicht** bei gesperrtem Handy zuverlässig die große, zweizeilige Uhrzeit? Android kann stattdessen auch eine Benachrichtigung zeigen.
2. Wird ein Wisch unter 90 % abgebrochen, und stoppt ein Wisch über 90 % tatsächlich?
3. Stoppt die separate Schlummertaste den Ton und plant sie korrekt die nächste Auslösung?
4. Schließt die Ansicht auch beim Stoppen über die Android-Benachrichtigung?
5. Vibriert die Galaxy Watch weiterhin, wenn der Bildschirm gesperrt ist?
6. Sind die neuen Standardwerte angenehm und zugleich rechtzeitig hörbar? Der globale Android-Weckerpegel ist separat einzustellen.

**Bis zur erfolgreichen Nacht-/Neustartabnahme den bisherigen zuverlässig funktionierenden Wecker parallel weiterverwenden.**

---

# Vorheriger Funktionsumfang (Beta 0.4.x)

**Stand: native Android-App, automatischer Build und Unit-Tests vorhanden. Samsung-Abnahme noch offen.**

## Ziel

Eine ruhige, werbefreie Android-Wecker-App mit sanftem Start bei **0 % eigenem Audiogain** und Galaxy-Watch-Vibration über Samsung-Benachrichtigungsspiegelung. Keine Watch-APK, kein ADB, kein Konto und keine kostenpflichtigen Dienste.

## Neue Oberfläche

Die bisherige Form mit drei festen Profilen wurde ersetzt durch:
- Dunkle **Wecker-Liste** mit großen Uhrzeiten, sieben Wochentags-Chips, separatem Ein/Aus-Schalter, Kurzstatus, Menü und Plus-Button.
- **Dynamisch viele Alarmprofile**, mindestens 20 unterstützt; maximale ID 1000. Feste Altprofile werden, sofern gespeichert, bei einem In-App-Update migriert.
- **Menü** mit Bearbeiten, Duplizieren, einmaligem Überspringen, Rückgängig, einmaliger Änderung der nächsten Weckzeit und Löschen.
- **Detailansicht** mit Namen, Uhrzeit, Tagen, Schaltern, drei Melodien, Tonverzögerung, Lautstärkeanstieg, Maximalpegel, Uhr-Vibration, Schlummern, Auto-Stopp, Testknöpfen und Diagnostik.
- **Aktive Alarmansicht** mit großen Stoppen-/Schlummern-Tasten; Android-Benachrichtigung mit entsprechenden Aktionen.

## Alarm-Engine

- Pro Wecker eine eigene stabile ID und ein eigenes `PendingIntent`, präzise mit `AlarmManager.setAlarmClock` geplant.
- Einmalige Ausnahmen über `skipAt` sowie eine nächste abweichende Auslösung `oneTimeAt` und deren ersetzten Basiszeitpunkt.
- Nach Auslösung wird ein normaler Wecker neu geplant; Neustart, App-Update, Uhrzeit-, Zeitzonen- und Berechtigungsänderungen führen zur Neuberechnung.
- Eigener AudioTrack-Ton (keine fremden Audiodateien); Gain 0 bei Start und konfigurierbare quadratische Rampe.
- Optionen: Tonverzögerung 0–300 s, Anstieg 1–30 Min., Maximalpegel 5–100 % des **System-Weckerpegels**, Auto-Stopp 1–30 Min., Schlummern an/aus 1–30 Min., 0–12 Uhrbenachrichtigungen im Abstand von 10–120 s.
- Separater 30-Sekunden-Systemtest und 15-Sekunden-Tonprobe; keine dauerhafte Weckeraktivierung erforderlich.
- Laufende Audiowiedergabe und Watch-Impulse werden beim Stoppen entfernt. Wenn Audioinitialisierung scheitert, können Watch-Impulse weiterhin gesendet werden.

## Besonderheiten und Einschränkungen

- **Galaxy Watch:** Samsung Galaxy Wearable leitet einzelne Benachrichtigungen weiter. Die Uhr kann im Schlaf-/Nicht-stören-Modus stumm bleiben. Es gibt **keine** verbindliche Zustellbestätigung und keine garantierte Fernbedienung über die Uhr.
- **Batterie und Neustart:** Herstellerseitige Einschränkungen lassen sich nicht allein durch Kompilierung prüfen. Realtests sind notwendig.
- **Vollbild-Alarm:** Die App fordert `USE_FULL_SCREEN_INTENT`. Abhängig von Android-Version und Nutzerberechtigung erscheint ggf. nur eine deutlich sichtbare Alarmbenachrichtigung.
- **Systemlautstärke:** App verändert weder System- noch Weckerlautstärke. Wenn die System-Weckerlautstärke 0 ist, bleibt sie auch beim steigenden App-Gain stumm.
- **Stabile APK-Updates:** GitHub Actions signiert im Testmodus; neue Runner können verschiedene Debug-Signaturen erzeugen. Ein einfacher Austausch der APK kann daher eine Deinstallation erfordern. Für reale Nutzung braucht es eine sichere dauerhafte Releasesignatur.
- **Sicherheitsfreigabe:** Nicht als einzigen Wecker verwenden, bis Tests mit gesperrtem Handy, Schlafmodus, Energiesparmodus und nach Neustart bestanden sind.
- **Datenschutz:** Alle Einstellungen lokal, keine Internetberechtigung, keine Werbung, keine Analyse- oder Cloudbibliotheken.

## Automatisierte Qualitätssicherung

`gradle -p experiments/watch-vibration-poc :alarm:testDebugUnitTest :alarm:assembleDebug :alarm:lintDebug`

Die JUnit-Tests decken Kalenderlogik (Wochentage, DST), Gain-Kurve, Standardprofile, dynamische IDs sowie einmalige Ausnahmen ab. GitHub Actions erstellt ausschließlich das Phone-APK `sanftwecker-beta-debug.apk`. Unter anderem werden Internet-, Standort- und Speicherberechtigungen ausgeschlossen.

## Späterer Gerätetest (noch nicht ausführen)

1. **Nur** `sanftwecker-beta-debug.apk` auf dem Samsung-Handy installieren. Die neue App heißt **„Sanftwecker (Test)“**, nicht „Watch-Benachrichtigungstest“.
2. Android-Berechtigungen `Benachrichtigungen`, `Alarme und Erinnerungen` gewähren; optional Full-Screen-Intent zulassen.
3. In Galaxy Wearable bei App-Benachrichtigungen **„Sanftwecker (Test)“** freigeben.
4. 30-Sekunden-Test mit gesperrtem Handy, sanftem Ton und Watch-Vibration. Dann Stoppen und Schlummern prüfen.
5. An mehreren Tagen und mit aktiver Schlafroutine sowie nach einem Neustart testen; bis zur Freigabe den bisherigen Wecker beibehalten.

Siehe `PRODUCT_REQUIREMENTS.md` für das vollständige Pflichtenheft.
\n\n## Multi-Timer und Stoppuhr\nSiehe [TIME_TOOLS.md](TIME_TOOLS.md) für Technik und Abnahmetests.\n