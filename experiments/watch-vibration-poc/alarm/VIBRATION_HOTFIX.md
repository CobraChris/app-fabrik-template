# Sanftwecker Beta 0.6.1 – Sicherheitskorrektur für Vibration

**Fehlerbericht (06.11.2026):** Nach Beta 0.6.0 lief eine Handyvibration unerwartet lange; ein sichtbares Fenster zum Stoppen fehlte. Die Galaxy Watch vibrierte bei diesem Versuch nicht. Nach einem manuellen „Stopp erzwingen“ ließ sich die Vibration beenden.

## Sofortmaßnahmen im neuen Code

- **Kein unendlich wiederholter Android-Vibrationsauftrag mehr** (\`VibrationEffect.createWaveform(...,-1)\` statt Repeat-Index 0).
- Einzelne Hardwarevibrationen sind endlich und können höchstens so lang sein wie ein gewählter Rhythmus (maximale Motor-An-Phase 4 Sekunden).
- Bei echtem Alarm: höchstens **12 Sekunden** Handymuster; ggf. wird ein neuer endlicher Rhythmus gestartet, aber eine harte Stoppgrenze bricht ihn ab.
- Beim **Vibrationsvorschautest**: Ein einzelner Rhythmus, maximal **5 Sekunden**, anschließend wird der Motor ausdrücklich abgebrochen.
- Die Vorschau zeigt **vor Beginn** einen Dialog mit **SOFORT STOPPEN**. Er verschwindet nach spätestens 5 Sekunden; Schließen des Dialogs stoppt die Vorschau ebenfalls.
- Sowohl in der Wecker-Liste als auch in der Detailseite gibt es einen gut sichtbaren Button **SOFORT STOPPEN – Ton und Vibration**, der die aktive Audio-/Haptikausgabe beendet und geplante Kurztests abbricht.
- Aktiver Alarm nutzt einen neuen Android-Hochprioritätskanal mit Stoppaktion in der dauerhaft sichtbaren Benachrichtigung. \`Sanftwecker → Klingelbildschirm erlauben\` bietet für Android 14+ den direkten Weg zur Vollbild-Berechtigung, falls nicht erlaubt.
- Der Galaxy-Watch-Kanal verwendet wieder den **bereits erfolgreich getesteten Kanal \`sanftwecker_watch_v3\`**. Die zusätzlichen, zuvor eingeführten Musterkanäle werden nicht mehr genutzt. Die Dauer/Rhythmik der Watch kann Samsung bei Benachrichtigungsspiegelung weiter überschreiben.

## Einschränkungen

Die Android-Benachrichtigungsweiterleitung erlaubt keine verlässliche Kontrolle der Watch-Vibrationsdauer. Ein echter länger vibrierender Watch-Wecker benötigt eine gesonderte native Wear-OS-App und eine eigene Alarmdarstellung auf der Uhr. Dies wird nicht in dieser Sicherheitskorrektur umgesetzt.

Die Alarm-Vollbildansicht kann Android auch bei angeforderter Full-Screen-Intent-Berechtigung unterschiedlich behandeln (insbesondere bei entsperrtem Bildschirm). Die Stoppen-Aktion in der Alarmbenachrichtigung und die Not-Stopp-Buttons bleiben daher die erforderlichen Alternativen.

**Nicht für den alleinigen Nachtwecker freigegeben.** Neue Builds, Unit-Tests und Lint ersetzen keinen Samsung-Gerätetest. Beta 0.6.0 nicht erneut verwenden. Wer Beta 0.6.1 später testet, soll zuerst nur die maximal fünfsekündige Vorschau prüfen und danach einen regulären Alarm.

### Automatisierte Regressionstests
- Alle fünf Vorschauen sind zeitlich begrenzt (≤ 5 Sekunden)
- Die Alarm-Vibration ist auf 12 Sekunden begrenzt
- Die API verwendet ausschließlich den nicht wiederholenden Waveform-Modus
- Die „fast durchgehende“ Vorschau endet nach höchstens 4,5 Sekunden
