# Sanftwecker Beta 0.7.0 – Wecker, Multi-Timer, Stoppuhr

**Stand: 06.11.2026.** Die bestehenden Wecker bleiben technisch und datenseitig erhalten. Es kommen zwei getrennte, vollständig lokale Funktionsbereiche hinzu.

## Navigation

Drei einheitlich dunkel gestaltete Seiten: **Wecker | Timer | Stoppuhr**. Die Wecker-Seite mit ihrem Alarm-Stopp und den bisherigen Weckkarten wurde nicht ersetzt.

## Multi-Timer

- Zwei Kacheln pro Zeile, beliebige Namen, Favoriten und Sortierung, je Timer Start/Pause/Reset, Bearbeiten, Duplizieren, Löschen.
- Schnellanlage für **45 Sekunden** und **2 Minuten**; Timer-Dauer 1 Sekunde bis 24 Stunden, maximal 50 Timer.
- Jeder Timer hat eine **eigene eindeutige ID** und einen eigenen Android-AlarmManager-PendingIntent, getrennt von den Weckern.
- Exakter Ablauf über \`setExactAndAllowWhileIdle\`, Android-Berechtigung **„Alarme und Erinnerungen“** nötig. Bei deaktivierten App-Benachrichtigungen wird Start verhindert bzw. eine Warnung gezeigt.
- Fertige Timer erhalten **nur eine Android-Benachrichtigung** über einen eigenen Kanal \`sanftwecker_timer_done_v1\` mit System-Benachrichtigungston und kurzem Vibrationsmuster; Quittieren-Schaltfläche vorhanden. **Keine** endlose Vibration oder Wiederverwendung des problematischen Alarm-Vibrators. Der Timer bleibt im Programm als **Fertig** sichtbar.
- Samsung Galaxy Wearable kann diese App-Benachrichtigungen je nach Freigabe auf der Uhr anzeigen. Haptik, Zustellung und Nicht-stören-Verhalten sind **nicht garantiert**.
- Daten lokal in eigenen SharedPreferences, kein Account, keine Berechtigung für Internet/Standort.
- Hintergrund: Countdown bleibt per Systemalarm auch bei gesperrtem Display aktiv. Beim Neustart werden **noch nicht abgelaufene** Timer wieder eingeplant; während des ausgeschalteten Geräts abgelaufene Timer werden als fertig markiert, aber **nicht nachträglich laut abgespielt**.

## Stoppuhr

- Mehrere unabhängige Stoppuhren: Name, Favorit, Start/Pause, Reset und **Runden-/Zwischenzeiten** (bis 300 pro Stoppuhr), mindestens 50 Sessions speicherbar.
- Monotone Zeitmessung via \`SystemClock.elapsedRealtime()\`, Anzeige **HH:MM:SS.mmm** und Rundenliste mit kumulierter sowie einzelner Rundenzeit.
- Messungen laufen beim App-Wechsel weiter, auch wenn der Bildschirm gesperrt wird; Prozessneustarts innerhalb desselben Handy-Starts werden berücksichtigt.
- Nach einem echten **Handy-Neustart** werden laufende Stoppuhren aus Gründen der Messgenauigkeit angehalten; bereits gesicherte Runden und Zeiten bleiben vorhanden.
- Keine Berechtigung für Benachrichtigungen, Mikrofon, Standort oder Netzwerk erforderlich.

## Sicherheitsgrenzen

- Der **Wecker** und sein Not-Stopp bleiben getrennt von den Timern.
- Timer verwenden **keine** direkte Handy-Dauervibration; bei Ablauf eine kurze Systembenachrichtigung.
- Vollbild-Wecker und direkte Watch-Vibration wurden in der Beta 0.6.1 noch nicht vollständig durch Samsung-Hardwaretests abgenommen. Diese Beta **nicht als alleinigen Wecker verwenden**.
- Kein Wetter, keine Werbung, keine Analytics, kein Abo und **keine kostenpflichtigen APIs**.
- Es werden keine externen Icons, Bilder oder Schriften kopiert; Darstellung mit nativen Android-Widgets.

## Geplanter Samsung-Abnahmetest

1. Öffnen: drei Tabs wechseln, gespeicherte Wecker unverändert sehen.
2. Zwei Timer gleichzeitig starten, einen pausieren, den anderen bei gesperrtem Display ablaufen lassen.
3. Timer-Nachricht und Quittieren-Funktion testen; Galaxy-Watch-Mirroring nur als optionalen Empfangstest betrachten.
4. Zwei Stoppuhren unabhängig starten, Zwischenzeiten speichern, App schließen/erneut öffnen; Zeiten prüfen.
5. Neustart: aktive Timer kontrollieren, Stoppuhren erwartungsgemäß pausiert.
6. Unverändert wichtig: Alarm-Stoppknopf, begrenzte Handyvibration und große Klingelansicht auf Samsung prüfen.

Automatisierte Tests: \`:alarm:testDebugUnitTest :alarm:assembleDebug :alarm:lintDebug\`.
