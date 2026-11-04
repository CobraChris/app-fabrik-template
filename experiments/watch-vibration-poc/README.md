# Galaxy Watch Vibration – Proof of Concept

**Status:** Experiment, **kein** verlässlicher Wecker. Keine Hintergrund-Weckzeit, kein Ton, kein Schlaftracking.

## Ziel
1. Auf Telefon und Galaxy Watch jeweils die passende **APK aus demselben GitHub-Actions-Build** installieren.
2. Watch-App einmal öffnen. Knopf **Lokal vibrieren** muss 5 Sekunden Vibration auslösen.
3. Handy-App öffnen, auf **Uhr 5 Sekunden vibrieren lassen** tippen.
4. Die Uhr vibriert 5 Sekunden, das Handy zeigt die Empfangsbestätigung.
5. App auf der Uhr schließen und Versuch wiederholen. Auch der manifestbasierte Hintergrund-Listener sollte reagieren.

## Technik
- Natives Android (Java) + natives Wear OS (Java).
- Google Play Services **Wearable Data Layer MessageClient**; keine direkten Bluetooth-Sockets, kein Internet-Backend.
- Watch: `WearableListenerService`, `VIBRATE`-Berechtigung, `VibrationEffect`.
- Antwortnachricht bestätigt, dass die Watch den Vibrationsaufruf ausgeführt hat. Nicht mit einer garantierten Alarmzustellung verwechseln.
- **Identische applicationId und dieselbe Signatur** für Handy/Watch; GitHub Actions baut beide in **einem Job**.
- Apps erzeugen weder Konto noch Kosten und erfassen keine persönlichen Daten.

## CI / Installation
Workflow: `.github/workflows/watch-vibration-poc.yml`.
Erzeugt ein GitHub-Artefakt `watch-vibration-poc-apks` mit:
- `phone-debug.apk` (Smartphone)
- `watch-debug.apk` (Galaxy Watch)

Wear OS wird durch Installation einer APK auf dem Handy **nicht automatisch mitinstalliert**. Watch-Sideloading benötigt z. B. Wear-OS-WLAN-Debugging und einen ADB-Client. Für den späteren echten Wecker müssen wir die Installation und eine dauerhafte App-Signatur noch vereinfachen.

**Wichtig:** Die GitHub-Runner erzeugen Debug-Signaturen neu; APKs verschiedener Builds lassen sich eventuell nicht direkt übereinander installieren. Bei Signaturkonflikten die beiden PoC-Apps auf den jeweiligen Geräten erst deinstallieren; später einen beständigen Release-Key sicher hinterlegen, nicht im Repository.

## Grenzen und Abnahme
- Bei getrennter Watch kann die Nachricht nicht zuverlässig zugestellt werden. `sendMessage` bietet keine garantierte spätere Zustellung.
- Bluetooth-/Datenverbindung, Do-not-disturb, Vibrationsintensität und die Installation der Watch-App beeinflussen den Test.
- Erst nach erfolgreichem PoC folgen unabhängige zeitgesteuerte Alarme auf beiden Geräten, exakter Alarm, Neustartbehandlung und stufenloser Ton.
- Niemals als alleinigen Wecker benutzen, bevor die spätere App umfassend unter Bildschirm-aus-, Schlaf-/Energiespar- und Neustartbedingungen geprüft wurde.
