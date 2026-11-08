# Pflichtenheft – Sanftwecker (Android + Galaxy Watch)

**Fassung:** 1.0 – 05.11.2026  
**Status:** Verbindliche Entwicklungsgrundlage; kein Nachweis einer betriebsbereiten Wecker-App.  
**Referenz:** Vom Auftraggeber gezeigte dunkle Wecker-Listen- und Detailansichten. Visuell inspiriert, nicht pixelgenau kopiert.

## 1. Produktziel und Grenzen

Eine **rein lokale, werbefreie Android-Wecker-App**, die zunächst mit **eigenem Tonausgabe-Gain von 0** beginnt, ungewöhnlich sanft ansteigt und über von Samsung weitergeleitete Benachrichtigungen eine Galaxy Watch vibrieren lässt. Keine eigene Watch-App, kein Konto, kein Internet, keine Abos, keine kostenpflichtigen APIs. Das Verhalten der Uhr wird von Samsung/Galaxy Wearable mitbestimmt; Empfang und Vibration sind über diesen Weg nicht garantierbar.

**Plattformen:** Samsung-Android-Smartphone (primär), Galaxy Watch via Galaxy Wearable. Mobil bedienbar, Deutsch, 24-Stunden-Zeitformat. Keine Integration in die Samsung-Wecker-App erforderlich.

**Bestehender Entwicklungsstand:** Die Beta besitzt drei festgelegte Alarmprofile, einfache Eingabefelder und den Alarmkern. Ziel dieses Pflichtenhefts ist eine richtige **Liste beliebig vieler gespeicherter Wecker (mindestens 20 ohne künstliche Dreiergrenze)** sowie eine separate Detailseite. Die bisherige Beta erfüllt das neue Oberflächenkonzept noch nicht.

## 2. Designsystem

- Dunkler, ruhiger Hintergrund mit etwas helleren, abgerundeten Karten; dezente blau-graue Akzente. Nicht 1:1 das Referenzprodukt nachbauen.
- Große, gut lesbare Uhrzeit (Richtwert 56–72 sp in der Liste, größer in der Detailansicht); sonst 14–18 sp, klare Typografie.
- Touch-Ziele mindestens 48 dp, ausreichender Kontrast, TalkBack-Beschriftungen, skalierbare Systemschrift.
- Hauptfunktionen in höchstens zwei Ebenen; Einstellungen zur Uhr separat und nicht hinter verschachtelten Menüs verstecken.
- Keine Wetterfelder, Werbebanner, Tagesnachrichten, Rätsel oder dekorative Zusatzfunktionen in V1.

## 3. Screen A – Wecker-Liste (Startbildschirm)

**A1 (MUSS):** Titel „Meine Wecker“; rechts optional Sortierung. Bei 0 Weckern Leerseite mit „Ersten Wecker erstellen“.

**A2 (MUSS):** Pro Wecker eine Karte mit Name, sehr großer HH:MM-Uhrzeit, sieben Chips (Mo–So), direkt nutzbarem Ein/Aus-Schalter und Drei-Punkte-Menü. Inaktive Wecker sind visuell gedimmt, aber lesbar.

**A3 (MUSS):** Unterzeile mit wesentlichen Besonderheiten, z. B. „Uhr zuerst · Handyton +30 s · Rampe 10 Min.“; nicht jede technische Einstellung in der Liste zeigen.

**A4 (MUSS):** Antippen der Kartenfläche öffnet Detailansicht; Schalter verändert nur Aktivierung (ohne Detailseite). Aktivierung muss erst nach erfolgreicher Alarmplanung als aktiv erscheinen.

**A5 (MUSS):** Ein klarer schwebender „+“-Button unten rechts erstellt einen neuen deaktivierten Wecker. Keine separate große Mehrzweck-FAB-Leiste.

**A6 (SOLL):** Drei-Punkte-Menü: „Bearbeiten“, „Duplizieren“, „Nächstes Mal überspringen“, „Überspringen rückgängig“, „Löschen“ (mit Bestätigung). „Überspringen“ gilt nur für den *nächsten konkreten Termin*, nicht für alle künftigen Wiederholungen. Temporäre Ausnahmen müssen gespeichert und nach Neustart berücksichtigt werden.

**A7 (SOLL):** Sortierung nach nächster effektiver Auslösung (Standard) oder Uhrzeit; Kennzeichnung „Nächster Alarm“ an genau einer Karte.

## 4. Screen B – Wecker-Detail

**B1 – Kopf:** Zurück; Titel „Wecker-Detail“; rechts „Testen“ und „Speichern“. Ungespeicherte Änderungen beim Zurückgehen erkennen und Speichern/Verwerfen anbieten.

**B2 – Hauptkarte (MUSS):** Direkt editierbarer Name, große antippbare HH:MM-Uhrzeit mit Zeitwähler, Ein/Aus-Schalter und sieben einzeln auswählbare Wochentags-Chips. Mindestens ein Tag muss ausgewählt sein. Die aktive Konfiguration wird dauerhaft lokal gespeichert.

**B3 – Ton & Lautstärke (MUSS):**
- Auswahl aus mindestens drei eigenen, nicht urheberrechtlich fremden Wecktönen.
- Master „Ansteigende Lautstärke“ standardmäßig ein; im Aus-Modus kein harter voller Pegel ohne ausdrückliche Nutzerauswahl.
- „Tonstart verzögern“: 0–300 Sekunden, Standard 0.
- „Lautstärkeanstieg“: 1–30 Minuten, Standard 10.
- „Maximaler App-Pegel“: 5–100 % des Android-System-Weckerpegels, Standard 35 %.
- **Akustischer Pegel beginnt bei App-Gain 0**, Anstieg ohne abrupten Mindestschritt. Globale Android-Lautstärke wird nicht verändert.
- „Tonprobe“ und „Sofort stoppen“ unabhängig von normal geplanten Weckzeiten.

**B4 – Galaxy Watch (MUSS):**
- Schalter „Uhr benachrichtigen“, standardmäßig an.
- Anzahl einzelner Impulse 1–12, Standard 6; Intervall 10–120 Sekunden, Standard 20.
- Klarstellung in der UI: „Vibration über Galaxy-Wearable-Benachrichtigungen; bei Schlafmodus/Nicht stören möglicherweise unterdrückt.“
- Link/Anleitung zu der **exakt benannten** App „Sanftwecker (Test)“ in Galaxy Wearable → App-Benachrichtigungen. Keine Verwechslung mit „Watch-Benachrichtigungstest“.
- Getrennte Watch-Testbenachrichtigung mit lokalem Status „gesendet“; **keine** falsche Anzeige „Uhr hat vibriert“.

**B5 – Schlummern & Stoppen (MUSS):** Normale große Stoppen-/Schlummern-Tasten; Schlummerdauer 1–30 Minuten, Standard 5. Stoppen beendet Ton und geplante Watch-Impulse sofort. Schlummern beendet aktuellen Alarm und plant einen neuen Termin. Auto-Stopp nach 20 Minuten (in V1 fest; einstellbar später).

**B6 – Erweitert (SOLL):** Aufklappbare Diagnose: nächster Wecktermin, letzter Auslöseversuch, Benachrichtigungen erlaubt, exakte Alarmberechtigung, Android-Alarm-Lautstärke ≠ 0, Energiesparhinweis, Watch-Zustellung unbekannt.

## 5. Screen C – Aktiver Alarm / Sperrbildschirm

**C1 (MUSS):** Bei Alarmauslösung klar sichtbarer Alarmzustand mit Name/Uhrzeit, großem „Stoppen“, getrenntem „Schlummern“ und erkennbarem derzeitigen Lautstärkeanstieg. Bedienung auch per Android-Benachrichtigungsaktionen, wenn Display gesperrt ist.

**C2 (MUSS):** Ton und Watch-Impulse stoppen bei Stoppen, spätestens nach Auto-Stopp. Keine doppelten Audio-Streams bei mehreren zeitgleichen Alarmauslösungen.

**C3 (SOLL):** Dunkle, reduzierte Sperrbildschirmansicht, gut erreichbare Bedienelemente, Schutz gegen versehentliches Beenden. Stoppen/Schlummern von der Watch sind **nicht** als garantiertes Merkmal versprochen, solange nur Benachrichtigungsspiegelung genutzt wird.

## 6. Fachlogik und Nicht-Funktionales

**F1:** Jeder Wecker besitzt stabile ID, Name, Uhrzeit, Wochentagsmaske, Aktivierung, Ton, Verzögerung, Rampe, Obergrenze, Watch-Einstellungen, Schlummerdauer sowie einzelne Termin-Ausnahmen. Duplizieren erstellt neue ID. Löschen entfernt auch dessen geplante Android-Alarme.

**F2:** Präzises Planen durch Android-Alarmschnittstelle; bei Neustart, Zeitzonenwechsel, Uhrzeitänderung und App-Update erneut planen. Zeitumstellung lokal korrekt behandeln. Aktivieren ohne Berechtigung zeigt deutlichen Fehler statt scheinbar funktionierenden Schalter.

**F3:** Während eines Alarms darf die App nicht von der offen gehaltenen Oberfläche abhängen. Audio per Android-kompatiblem Foreground-Service und zeitbegrenzter Ressourcennutzung; korrekte Freigabe von Audio/WakeLock beim Stoppen.

**F4:** Benachrichtigungs-Weiterleitung ist ein Zusatzkanal, nicht die einzige Weckgarantie. Sperrbildschirm, DND/Schlafmodus und Bluetooth-/WLAN-Trennung als Risiken dokumentieren.

**F5:** Datenschutz: alle Einstellungen lokal, keine Internet-Berechtigung, keine Nutzeranalyse, kein Account, keine Werbe- oder Cloud-Bibliotheken. Notwendige Berechtigungen transparent erklären.

**F6:** App führt keine unbemerkten Änderungen der globalen Lautstärke oder an Samsung-Einstellungen durch. Batterieoptimierung nur als erklärten manuellen Schritt anbieten.

## 7. Prioritäten / Umsetzungspakete

| Paket | Lieferumfang | Priorität |
|---|---|---|
| UI-1 | Dunkles Designsystem, Startliste, Karten, Chips, Ein/Aus, Plus-Button, Navigation | P0 |
| DATA-2 | Dynamische Weckerliste statt drei fester Slots, lokale Migration, eindeutige IDs | P0 |
| UI-3 | Detailansicht mit Zeit, Tagen, Ton, Rampenkurve, Watch- und Schlummerparametern | P0 |
| CORE-4 | Einzelweckerplaner, Stop/Snooze, Testmodus, Berechtigungsprüfung, Diagnostik | P0 |
| QA-5 | Build, Unit-/UI-Tests, Standby-/Neustart- und Samsung-Abnahmecheckliste | P0 |
| MENU-6 | Duplizieren, nächstes Auslösen überspringen/rückgängig, Löschen | P1 |
| MENU-7 | Nächstes Auslösen einmalig ändern, Sortieroptionen, Komfortdetails | P1 |
| EXTRAS | Wetter, Sonnenaufgang, Bewegungsaufgaben, erweiterte Watch-Steuerung | P2; **nicht** Teil V1 |

**P0 = zwingend vor V1**, **P1 = sinnvolles Folgepaket**, **P2 = bewusst zurückgestellt**.

## 8. Abnahmekriterien

1. In der Liste lassen sich mindestens 20 benannte Wecker anlegen, bearbeiten, einzeln aktivieren, deaktivieren und löschen; Einstellungen bleiben nach Neustart vorhanden.
2. Aktivierungsstatus und nächster Wecktermin stimmen mit tatsächlich vom Android-System geplanter Auslösung überein.
3. Ein 30-Sekunden-Test bei gesperrtem Handy startet mit App-Gain 0, steigt hörbar sehr sanft an und lässt sich zuverlässig stoppen bzw. schlummern.
4. Bei gewählter Tonverzögerung wird das Telefon **nicht vor dem eingestellten Zeitpunkt** hörbar; Watch-Benachrichtigungen können bereits vorher erscheinen.
5. Die Galaxy Watch vibriert beim realen Samsung-Gerätetest mit aktiven Benachrichtigungen; getrennt testen mit eingeschaltetem und gesperrtem Handy, mit und ohne Schlafmodus.
6. Ein Stoppen beendet lokale Wiedergabe und weitere Watch-Benachrichtigungen. Kein Doppelton, kein hängender Vordergrunddienst.
7. Wochentagswechsel, DST, Neustart, Erlaubnisverlust, Energiesparen und Fehlerfälle sind dokumentiert; automatisierte Tests laufen fehlerfrei.
8. App zeigt fehlende Rechte und blockierte Benachrichtigungen deutlich an; behauptet niemals eine bestätigte Watch-Vibration, die nicht technisch gemessen werden kann.
9. Bestehende Test-Apps werden weder überschrieben noch automatisch entfernt. Keine Änderung an `main` vor Codeprüfung und Abnahme.

## 9. Freigaberegel

GitHub-Build und Unit-Tests reichen nicht als Nachweis der Nachtzuverlässigkeit. **Bis zur realen Abnahme den bisherigen zuverlässigen Wecker parallel nutzen.** Die Entwicklung bleibt in einem separaten GitHub-Zweig/Pull Request und kann dort in größeren Paketen fortgeführt werden.
