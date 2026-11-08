# Pflichtenheft-Abgleich für Beta 0.4.0

| Anforderung | Status |
| --- | --- |
| UI-1: Dunkle Weckerkarten mit Schalter, Zeit, Chips, Menü und Plus | Implementiert; Samsung-Ansicht ungeprüft |
| DATA-2: Mehr als 20 getrennte Wecker, IDs und Migration | Implementiert; Migration auf Gerät ungeprüft |
| UI-3: Separate Detailansicht mit Ton-/Watch-/Schlummer-Einstellungen | Implementiert; Bedienbarkeit ungeprüft |
| CORE-4: Exakter Alarm, Snooze, Stop, Watch-Benachrichtigung | Implementiert; Nachtzuverlässigkeit ungeprüft |
| QA-5: Unit-Tests, APK-Build, statische Prüfung | CI; Hardwaretests offen |
| MENU-6: Duplizieren, Überspringen, Rückgängig, Löschen | Implementiert |
| MENU-7: Einmalig nächsten Termin ändern, Uhrzeit-/Termin-Sortierung | Implementiert |
| Aktiver Alarmbildschirm / Sperrbildschirm | Implementiert; Full-Screen-Berechtigung und echtes Gerät ungeprüft |
| Alarmton beginnt bei App-Gain 0 | Im Code implementiert und unitgetestet; Lautstärke am Gerät offen |
| Watch vibriert bei gesperrtem Handy | Im früheren Benachrichtigungs-PoC nachgewiesen; neuer Alarmkanal noch nicht getestet |
| Sonnenaufgang, Wetter, Mini-Games, Kamerablitz, Bewegungssensor | Bewusst P2; nicht Teil V1 |

**Nicht freigegeben:** Bis zur echten Samsung-Abnahme und mehreren Nacht-/Neustarttests ersetzt diese Beta keinen vorhandenen zuverlässigen Wecker.
