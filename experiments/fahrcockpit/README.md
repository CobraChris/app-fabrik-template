# FahrCockpit 0.9.4-beta – vollständiger Android-Testkandidat

**Status (08.11.2026):** FahrCockpit 0.9.2-beta (Build 4) wurde als dauerhaft signierte APK veröffentlicht und auf einem Samsung Android 16 installiert. Overlay, Spotify-Metadaten, Mediensteuerung und Minimieren sind auf dem Gerät bestätigt. Die signierte Version 0.9.3-beta (Build 5) ist am 08.11.2026 mit geprüftem, zu v4 identischem Zertifikat veröffentlicht; die **In-place-Installation v4→v5 auf dem Gerät steht noch aus**. PR #27 und der nachfolgende Diagnostik-PR bleiben bis zur Geräteabnahme Draft. Der Diagnostik-Zweig ist keine veröffentlichte Version.

## Funktionen

- Schwebende, frei verschiebbare und minimierbare Steuerleiste über Google Maps. Position wird lokal gespeichert und bei Drehung/Größenänderung an den Bildschirmrand angepasst.
- Kontextabhängige Warnsteuerung: Bei ausgewähltem Saphe erscheint **kein** irreführender Blitzer-Stop-Knopf; bei ausgewähltem Blitzer.de PRO sind Start- und Stop-Anforderung getrennt, ausdrücklich ohne bestätigten Aktiv-Status.
- Spotify-Mediensteuerung (ohne zusätzlichen Spotify-Login): Play/Pause, vorheriger/nächster Titel, ±15 Sekunden falls der jeweilige Inhalt Seek unterstützt. Titel und Interpret aus Android MediaSession, Medienzugriff wird einmalig in Android-Einstellungen freigegeben. Im nächsten, unveröffentlichten Entwurf: bei den ±15-Sekunden-Sprüngen die seit dem letzten MediaSession-Zeitstempel vergangene Wiedergabezeit berücksichtigen, um bei älteren Positionsmeldungen korrekt zu springen.
- Warnsystem-Auswahl: Saphe Link (standardmäßig), Blitzer.de PRO (optional), oder kein Warnsystem. Saphe wird nur über seinen offiziellen Android-Paketnamen geöffnet, ohne Geräte- oder API-Zugriff. Das Saphe-Modell wird nach Rückkehr geprüft.
- Blitzer.de PRO: offizielle Befehle START, WIDGET, BACKGROUND und KILL_APP; Start und Beenden auch im schwebenden Cockpit. Kein eigener Blitzer-Datenzugriff oder unbestätigter Aktiv-Status.
- Tankstellen: aktuelle Preise für E5/E10/Diesel in Deutschland über Tankerkönig/MTS-K; bis 25 km. Jeder Treffer bietet Preis, Entfernung, Adresse und Übergabe als Google-Maps-Ziel.
- Der Tankerkönig-API-Key wird nur im privaten Android-App-Speicher gehalten (Backup deaktiviert). Standortfreigabe nur bei manueller Suche; keine Hintergrundabfragen.
- Ohne Tankerkönig-Schlüssel und im Ausland bleibt die Google-Maps-Tankstellensuche ohne Livepreise nutzbar.
- System-Benachrichtigung mit Cockpit-Stopp, Dark-UI, Android-Berechtigungsstatus, Google-Maps- und Spotify-Launcher.
- Keine Boot-Autostarts oder automatischen Blitzer.de-Starts; kein Tracking oder kostenpflichtiger Backend-Dienst.

## Samsung-Test in dieser Reihenfolge

1. APK installieren und FahrCockpit öffnen. Die App erscheint als FahrCockpit (Test).
2. Einblendung freigeben. Nach Android-Einstellungen zurück zu FahrCockpit und Cockpit starten.
3. Spotify-Medienzugriff freigeben: in Android-Benachrichtigungszugriff FahrCockpit aktivieren, Spotify öffnen und Musik abspielen.
4. Auf Cockpit über Google Maps starten tippen: Das Cockpit soll über Google Maps sichtbar werden. Am Titel ziehen. Minus minimiert, Kreuz beendet, die Android-Benachrichtigung kann das Cockpit stoppen.
5. Spotify Play/Pause, Titelwechsel, ±15 Sekunden prüfen. Unterstützt Spotify Seek nicht, bleiben diese Tasten deaktiviert.
6. Warnsystem Saphe voreingestellt: Saphe Link öffnen, sofern installiert; das eigentliche Gerät erst nach Rückkehr koppeln oder prüfen. Optional Blitzer.de PRO separat installieren/einrichten, vor Fahrt testen: Ein/Aus, Mini-App, Hintergrundmodus; auch der Beenden-Befehl im Overlay. Die offiziellen Intents haben keine Statusbestätigung.
7. Tankerkönig-Schlüssel persönlich und kostenfrei beantragen unter https://onboarding.tankerkoenig.de/ . In Tankstellenansicht eingeben und auf Speichern tippen. Bei Suche Standort erlauben; Tankerkönig liefert nur Daten für Deutschland.
8. Test-App schließen, wieder öffnen: Berechtigungen und lokal gespeicherte Cockpit-Position sollen erhalten bleiben. Bei Android- oder Samsung-Energiesparregeln Verhalten gesondert kontrollieren.

## Grenzen und Hinweise

- Google Maps wird nicht verändert; die Cockpit-Steuerung ist ein eigenes Android-Overlay.
- Keine in Google Maps eingebetteten Blitzer-Marker. Blitzer.de liefert über die dokumentierten Steuerbefehle keine Blitzerpositionen zum Einzeichnen.
- Spotify hängt vom gerade aktiven MediaSession-Angebot ab; einige Podcasts oder Inhalte unterstützen keine 15-Sekunden-Sprünge.
- Tankerkönig stellt Preise nur in Deutschland bereit, benötigt einen persönlichen Freischalt-Key und gibt keine Garantie für Aktualität.
- Die Android-Overlay-/Foreground-Service-Berechtigung kann herstellerseitig eingeschränkt sein. Auf Samsung Android 16 wurde der reale Betrieb mit Overlay, Spotify und Ein-/Ausklappen am 08.11.2026 bestätigt; andere Geräte und Systemversionen bleiben ungetestet.
- Die Debug-APK verwendet möglicherweise pro GitHub-Runner wechselnde Zertifikate. Nur eine separat signierte Release-APK mit festem Zertifikat ist zuverlässig in-place updatefähig.
- Aktive Blitzerwarnungen während der Fahrt sind in Deutschland für die fahrzeugführende Person nach § 23 Abs. 1c StVO verboten. Ein Ausschalter schafft keine Ausnahme.

## Entwicklung

Java 17, Android Gradle Plugin 8.9.2, SDK 35, min SDK 30, keine zusätzlichen Laufzeitbibliotheken. Die Android-CI führt testDebugUnitTest, lintDebug und assembleDebug aus. Die geschützten Signier-Secrets werden im PR nicht verwendet.

Quellen:
- https://www.blitzer.de/questions/wie-richte-ich-den-automatischen-start-stopp-per-tasker-ein/
- https://developer.android.com/reference/android/media/session/MediaSessionManager
- https://creativecommons.tankerkoenig.de/
- https://onboarding.tankerkoenig.de/

FahrCockpit GitHub: https://github.com/CobraChris/app-fabrik-template/pull/27

## Updates ohne ZIP

Die Hauptansicht enthält \"Auf Updates prüfen\". Der Dialog fragt **nur auf Klick** die öffentlichen GitHub-Releases ab und erlaubt ausschließlich APKs namens `fahrcockpit-release.apk` aus einem passenden `fahrcockpit-v<versionCode>`-Tag. Der Download öffnet den Browser als normalen direkten APK-Link.

**Wichtig:** Eine neue signierte Release-APK kann einen bestehenden Debug-Build mit anderer Signatur nicht direkt ersetzen; beim ersten Wechsel ist daher möglicherweise einmalig eine Neuinstallation erforderlich. Anschließend ermöglichen gleiches Paket, gleiche dauerhafte Signatur und steigender `versionCode` echte Updates ohne Deinstallation. Die signierte **erste** FahrCockpit-Release-APK ist veröffentlicht: https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v4/fahrcockpit-release.apk (Build 4, SHA-256 `72c0f49b366dbb216ac2fd2310bf80e86a3e05fb25e30e94d65af0c74d8fa56b`). Beim ersten Wechsel von der Debug-Signatur war die Neuinstallation notwendig. Die Vollversion wurde nach der gezielten, nicht-globalen Play-Protect-Option auf dem Samsung-Gerät erfolgreich installiert; daraus folgt **keine** allgemeine Google-Freigabe. Ein nächstes Update muss über dieselbe FahrCockpit-Signatur und mit höherem Versionscode erfolgen.


## Nachfolgender Entwurf (noch nicht veröffentlicht)

- [Signiertes v5 / 0.9.3-beta](https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v5/fahrcockpit-release.apk) ist der letzte veröffentlichte Build; der bestehende Signierschlüssel wurde im Release-Lauf mit v4 abgeglichen.
- Separater [Draft PR #35](https://github.com/CobraChris/app-fabrik-template/pull/35): präzisere Spotify-Diagnose, ±15-Sekunden-Seek mit monotoner Positionsfortschreibung aus Androids `PlaybackState`-Zeitstempel, sichere Behandlung von Pause/ungültigen Zeitstempeln/Trackdauer, reichhaltige JVM-Regressionstests und Updateprüfung mit größerem GitHub-Release-Suchfenster (100 statt 40 Einträge, keine Hintergrundabfrage).
- Für Tests der veröffentlichten v5 siehe [Samsung-v5-Abnahme](docs/SAMSUNG_V5_ACCEPTANCE.md). Kein neues APK-Release aus dem Draft-Zweig; der Nutzer installiert v5 selbst und testet später.

## Fahrmodus – Vorentwurf (noch keine signierte APK)

Am 09.11.2026 zeigte der Nutzer FahrCockpit über **Google Maps mit Verkehrslage**; so funktioniert der gewünschte Ein-Karten-Ansatz. Die Saphe-App war zuvor sichtbar, aber **ob Saphe im Hintergrund verbunden bleibt und meldet, ist nicht geprüft**. Eine direkte Übertragung von Saphe-Kartenpunkten in Google Maps ist nicht verfügbar/nicht belegt.

Im nachfolgenden Draft-Zweig wird ein expliziter **Fahrt**-Knopf am Cockpit ergänzt: Statt der großen Vollansicht zeigt er nur eine einzeilige Spotify-Titelanzeige sowie die drei großen Tasten **Play/Pause, nächster Titel und Maps**. Mit **Voll** geht es zur kompletten Steuerung zurück; die bestehende **−**-Taste minimiert weiterhin auf die Kopfzeile und kann den vorherigen Modus wiederherstellen. Die Bedienung wird nicht automatisch umgeschaltet und benötigt keine zusätzlichen Berechtigungen, keine Standortabfragen oder Hintergrunddienste.

Die volle Oberfläche bleibt zum Konfigurieren von Saphe/Blitzer.de und Tanken bestehen. **Keine Blitzer-Marker in Google Maps** und keine automatische Blitzerwarnfunktion hinzugefügt. Auf deutschem Hoheitsgebiet gelten die Vorschriften des § 23 Abs. 1c StVO.

## Spotify-Fortschritt, Kartenwahl und Bildschirmorientierung – nächster Entwurf

- **Zusätzlicher Ziehbalken** für Spotify: exakte Position und Titeldauer aus Androids bestehender MediaSession; Zeitanzeige in `m:ss`, Fortschritt laufend aktualisiert. Freies Spulen nur, wenn Spotify `ACTION_SEEK_TO` anbietet; bei fehlender Trackdauer bleibt der Balken ausgeblendet. Das ergänzt die bestehenden ±15-Sekunden-Knöpfe, ersetzt sie nicht. Die aktive Karte wird nicht verändert.
- **Zwei optionale Schalter in der Hauptansicht:** `Google Maps beim Cockpit-Start öffnen` (standardmäßig EIN) und `Querformat: automatisch kompakter Fahrmodus` (standardmäßig EIN). Wird Maps-Start ausgeschaltet, bleibt die vorhandene andere App im Vordergrund; bereits laufendes Google Maps wird dadurch nicht beendet oder komprimiert.
- **Hoch-/Querformat:** In Querformat wird das schwebende Fenster auf einen verkürzten Fahrmodus umgestellt (sofern eingeschaltet), standardmäßig rechts positioniert; im Hochformat wird die große Bedienoberfläche wiederhergestellt. Positionen können in jeder Ausrichtung separat durch Ziehen gespeichert werden. Die vollständige Bedienoberfläche bleibt auch bei kurzem Querformat-Display über einen Scrollbereich erreichbar. Bestehende gespeicherte Positionen aus v5 bleiben für Hochformat erhalten.
- **Keine vollständige Google-Maps-Einbettung:** Die Google-Maps-App wird separat gestartet und die Navigation bleibt erhalten. Eine echte navigierbare Kartenteilfläche **innerhalb** FahrCockpit erforderte Google Navigation SDK, einen Billing-fähigen Google-Cloud-Account und Preis-/Quota-Freigabe; das ist nicht beauftragt oder eingebaut. Androids systemeigener Split-Screen ist eine zusätzliche, manuell aktivierbare Möglichkeit, Apps tatsächlich nebeneinander anzuordnen, nicht eine garantierte Einbettung.
- Stand: **nur Draft PR #35**, signierte `fahrcockpit-v5` unverändert. Android-Gerätetest für Ziehbalken/Querformat ausstehend; kein neues APK veröffentlicht.

## Echte 70/30-Split-Ansicht (ab Build 6)

- Neue `SplitCockpitActivity` als eigene Android-Multi-Window-Seitenansicht. Sie zeigt den Spotify-Track, dessen Positions-Ziehbalken, Play/Pause, Titelwechsel, ±15 Sekunden, Google Maps, Tankstellen und Saphe-Kurzaufruf. Alle Steuerelemente sind in einer verschiebbaren, scrollbaren Schmalansicht zugänglich.
- Der Button **70/30-Fahransicht starten** schließt das alte schwebende Cockpit und startet die native Seitenansicht. Sie fordert Google Maps mit `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT` im Nachbarfenster an. Die tatsächliche Anordnung ist **Best Effort** des Android-/Samsung-Systems, nicht garantiert. Im Querformat sollten nebeneinanderliegende Fenster möglich sein; in Hochformat werden die Fenster häufig gestapelt.
- **Grenze:** Die App kann *nicht* erzwingen, dass Google Maps genau 70 % erhält; die Samsung-Trennlinie steuert das. Einmalig auf etwa 70/30 verschieben und mit Samsung **App-Paar speichern**. Keine Google-Maps-SDK-Integration, kein Billing, keine Google-Maps-Fenstermanipulation. Die aktive Google-Maps-Navigation bleibt vom externen Maps-Task verwaltet.
- Falls Samsung den Aufruf nicht als Split akzeptiert, wird der fehlende Splitstatus ehrlich angezeigt. Die bisherige Overlay-Ansicht bleibt als Rückfalloption erhalten. **Keine** erweiterte Android-Accessibility-/MediaProjection-Berechtigung oder automatische System-UI-Steuerung.
- Tests erfolgen zuerst automatisiert; die **Samsung-Abnahme der echten 70/30-Fenster und das Anlegen des App-Paars stehen aus**, bis der Nutzer das Endergebnis installiert. Es werden keine Kosten durch Navigations-APIs ausgelöst.
