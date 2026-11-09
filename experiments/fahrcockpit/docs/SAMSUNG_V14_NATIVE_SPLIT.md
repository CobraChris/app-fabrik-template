# FahrCockpit Build 14 – echter Split als Standard

Auslöser: Der Nutzer bestätigte am 09.11.2026 nach Build 13, dass Google Maps in Hoch-/Querformat weiterhin verdeckt wird. Die gewünschte Lösung ist ein **eigener fester Bereich für Spotify**, nicht ein verschobenes Overlay.

## Umsetzung
- App-Symbol startet standardmäßig `SplitCockpitActivity`. Diese fordert Google Maps per `NEW_TASK | LAUNCH_ADJACENT` als eigenes Fenster an.
- Vor dem Start und beim Reentry werden beide alten Overlay-Dienste beendet. **Kein automatischer Overlay-Fallback**, selbst wenn Samsung den Split nicht bestätigt.
- Spotify, Track-Seeking, ±15 s, Tankstellen, Saphe, Einstellungen bleiben im nativen Cockpit verfügbar.
- Bisherige schwebende Bedienmodi bleiben optional im Hauptmenü, aber nicht im Standard-Fahrmodus.
- Der Nutzer kann die Split-Trennlinie beim stehenden Fahrzeug einstellen und mit Samsung App-Paar speichern. Im Hochformat ist eine übereinanderliegende Teilung normal; im Querformat nebeneinander.
- **Keine** neue Berechtigung, kein Google-Maps-SDK, kein Google-Cloud-Billing, keine neuen Secrets, kein Eingriff in den Release- oder Signierworkflow.
- Kann Samsung Google Maps nicht in die zweite Fläche bringen, zeigt die App einen Hinweis. Es wird **keine** verdeckende Bedienleiste gestartet.

## Abnahme nach signiertem Release
1. Bestehende App aktualisieren, nicht deinstallieren.
2. FahrCockpit-Appsymbol starten; Maps und Cockpit müssen zwei separate Android-Fenster erhalten (oder bei Samsung-Ablehnung verständliche Hinweise anzeigen).
3. Im Stand Google Maps Routenstart, eigene Position, Zieleingabe/Tastatur sowie Navigation im Hoch- und Querformat prüfen.
4. Spotify-Funktionen prüfen. Sicherstellen, dass keine automatische zweite Überlagerung erscheint.
5. Reale Geräte-Abnahme ist noch ausstehend; GitHub-CI kann Samsungs Multi-Window-Verhalten nicht bestätigen.
