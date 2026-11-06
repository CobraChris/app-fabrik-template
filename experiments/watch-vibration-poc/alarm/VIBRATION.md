# Sanftwecker Beta 0.6.0 – Vibrationsmuster (06.11.2026)

Die Vibrationsmuster sind **pro Wecker** konfigurierbar. In **Wecker-Detail → Vibration** findet man den Rhythmus und den Vorschauknopf.

| Muster | Handy (direkt per Android-Vibrator) | Uhr |
| --- | --- | --- |
| Standard – bisherige kurze Impulse | Kein zusätzlicher direkter Motorbetrieb; bisherige Benachrichtigungen bleiben | Bisheriger Watch-Kanal `sanftwecker_watch_v3` |
| Lang – kurz – lang | 1.000/250/1.000 ms mit Pausen und Wiederholung | Neues Benachrichtigungskanal-Muster |
| Kurz – kurz – kurz | 300/300/300 ms mit Pausen und Wiederholung | Neues Benachrichtigungskanal-Muster |
| Lang – lang | 1.600/1.600 ms mit Pause und Wiederholung | Neues Benachrichtigungskanal-Muster |
| Fast durchgehend | 4 s Vibration, 0,5 s Pause; höchstens 60 s | Neues Benachrichtigungskanal-Muster, keine Dauer-Garantie |

Die direkten Handy-Muster beginnen am tatsächlichen Alarmzeitpunkt, **unabhängig von der gewählten Verzögerung des Tons**. Sie enden beim Stoppen oder Schlummern, nach automatischem Zeitlimit oder bei „fast durchgehend“ spätestens nach einer Minute. Andere wiederholte Handy-Muster sind aus Schonungsgründen auf maximal zwei Minuten begrenzt. Die Vorschau spielt den gewählten Rhythmus einmal am Handy ab; sie benötigt keine Test-Weckzeit. Android kann Vibrationsanfragen je nach Geräteeinstellung unterdrücken.

**Galaxy-Watch-Einschränkung:** Die Anwendung hat keine direkte Wear-OS-Vibrationssteuerung. Die neuen Muster werden als Vibrationseinstellung eigener Android-Benachrichtigungskanäle an Samsung Galaxy Wearable weitergegeben. Galaxy Wearable bzw. die Uhr können dies durch eigene Haptik ersetzen, mehrere Meldungen bündeln oder bei „Nicht stören“ blockieren. „Dauer-Vibration auf der Uhr“ ist in dieser Architektur **nicht gewährleistbar**. Deshalb bleibt die bekannte wiederholte Benachrichtigungsfolge mit Anzahl (0–12) und Abstand (10–120 s). Ein Knopf setzt optional „12 Impulse alle 10 s“.

**Wichtige Migrationsregel:** Vorhandene Wecker erhalten standardmäßig Modus 0 („bisheriges Muster“) und behalten Namen, Uhrzeit, Ton, Watch-Impulsanzahl etc. Neue Wecker erhalten Modus 1 („lang–kurz–lang“). Kopien behalten den gewählten Modus.

Neue Android-Berechtigung: `android.permission.VIBRATE`. Keine Internetberechtigung, kein Konto oder kostenpflichtiger Dienst. Die neue Beta ist nicht für den alleinigen Nachtwecker freigegeben, bevor reale Samsung-Tests durchgeführt wurden.

**Testplan:** Vibrationsvorschau und alle fünf Muster am Handy probieren; das neue Watch-Benachrichtigungskanal-Muster in Android und Samsung Galaxy Wearable zulassen; regulären Alarm bei ausgeschaltetem Handybildschirm testen; sicherstellen, dass Stoppen, Schlummern und Auto-Stopp die direkte Handy-Vibration beenden. Bei der Watch die tatsächliche Ausgabe messen/fühlen statt die im System angezeigten Kanalmuster als garantierte Uhrhaptik anzunehmen.
