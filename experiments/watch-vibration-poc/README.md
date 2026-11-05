# Galaxy Watch: Benachrichtigungs-PoC (ohne Watch-App)

**Status:** Manueller Machbarkeitstest, **kein** Wecker und kein verlässlicher Alarm.

## Idee
Wir wollen zunächst nur herausfinden, ob die Galaxy Watch durch **gespiegelte Smartphone-Benachrichtigungen** vibriert. Dies nutzt Samsung Galaxy Wearable statt eigener Wear-OS-Installation. Keine ADB-Verbindung, kein WLAN-Debugging, keine Watch-APK.

## Test
1. Alte App `Watch-Vibrationstest` kann auf dem Handy bleiben; die neue APK hat absichtlich eine **andere applicationId** (kein Signaturkonflikt).
2. Aus dem PR-GitHub-Actions-Build das Artefakt `watch-notification-poc-apk` herunterladen, entpacken und `watch-notification-debug.apk` **nur auf dem Smartphone** installieren.
3. Die App **Watch-Benachrichtigungstest** öffnen und die Android-Benachrichtigungsberechtigung erteilen.
4. Auf dem Smartphone in **Galaxy Wearable → Uhreneinstellungen → Benachrichtigungen → App-Benachrichtigungen** die Weiterleitung für **Watch-Benachrichtigungstest** erlauben. Bei Bedarf zusätzlich Anzeige auf der Uhr auch bei verwendetem Handy konfigurieren.
5. Den Button **Testbenachrichtigung jetzt senden** antippen. Prüfen, ob auf der Uhr eine Benachrichtigung erscheint und ob sie vibriert.
6. Den Button **In 10 Sekunden senden (Handy sperren)** antippen, sofort das Handy sperren und erneut prüfen.
7. Falls die Uhr nicht vibriert: Watch-Benachrichtigungen, stumme Benachrichtigungen, DND/Schlafmodus sowie Benachrichtigungskanal prüfen. Das Hotel-WLAN spielt für diesen Versuch keine Rolle.

## Technische Details
- Handy: Android SDK 35, POST_NOTIFICATIONS (seit Android 13).
- Lokal erstellte Android-Notification mit HIGH-Priority-Kanal, ohne Ton, mit kurzer Vibration.
- Keine separate Watch-App, kein Cloudserver, kein Account, keine API und keine Datenübertragung durch den PoC selbst.
- **Eine gesendete Benachrichtigung ist kein Beleg für eine Watch-Vibration.** Spiegelung und Vibrationsmuster werden durch Samsung/Wear OS und Nutzereinstellungen bestimmt.
- Die Benachrichtigung kann auch das Smartphone kurz vibrieren lassen.
- Die App enthält noch keine Zeitplanung, sanften Weckton, Alarmwiederholung oder Zuverlässigkeitsmechanismen.
- Eine Watch-Benachrichtigung kann durch Ruhe-/Schlafmodus oder andere Benachrichtigungseinstellungen unterdrückt werden; deshalb vorerst nicht als Wecker verwenden.
- Nach dem Test ADB-Debugging und kabelloses Debugging auf der Uhr deaktivieren. GeminiMan wird für diesen PoC nicht mehr gebraucht.

## Build
Der bestehende PR-Workflow `.github/workflows/watch-vibration-poc.yml` baut nur noch das Phone-Modul. Im Artefakt `watch-notification-poc-apk` liegt `watch-notification-debug.apk`. Wegen Debug-Signatur-Änderungen ist die APK nur ein Testprodukt.

## Historie
Die ursprünglichen `watch/`-Dateien im Experiment dokumentieren den früheren, nicht mehr benötigten direkten Wear-OS-Ansatz und sind **nicht Teil dieses Builds**. Nichts davon muss auf der Galaxy Watch installiert werden.
