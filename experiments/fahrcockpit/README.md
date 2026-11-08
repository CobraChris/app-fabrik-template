# FahrCockpit 0.9.3-beta – Android-Test-MVP

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
