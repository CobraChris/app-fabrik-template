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


## 0.9.5-beta / Build 7 – Samsung-Splitscreen-Fix (09.11.2026)

**Nutzerbefund v6:** Android-Splitscreen **war aktiv**, Google Maps lag unten und FahrCockpit oben (Hochformat), aber zusätzlich lag der alte schwebende Overlay-Service über dem nativen FahrCockpit. Dies war ein konkreter Fehler der vorzeitigen Overlay-Fallback-Aktivierung, **kein** Problem mit Google Maps oder Spotify.

**Korrektur:** Beide Bedienarten schließen sich künftig aus. Beim Start der nativen Splitansicht wird ein etwaiger bestehender Overlay-Service sofort beendet; der Splitstart versucht zuerst die Systemteilung ohne überlagerndes Fenster. Erst wenn diese nach einer Verzögerung nicht bestätigt ist, darf der bislang bewährte Overlay-Fallback gestartet werden. Sobald der Split erkannt wird (einschließlich späterem Samsung-Moduswechsel, Rückkehr und Drehung), entfernt die Activity vorhandene Overlays unabhängig davon, wer sie gestartet hatte. Android-Unit-Tests decken Exklusivität und Fehlerfälle ab.

**Plattformgrenze:** Auf einem Samsung im Hochformat ordnet der System-Splitscreen die zwei Apps häufig übereinander an, im Querformat nebeneinander. Standardmäßig ist das Systemverhältnis nicht 70/30; die Trennlinie wird vom Nutzer auf ca. 70/30 verschoben. Diese Teilung kann nicht durch eine normale Drittanbieter-App erzwungen werden. Google Maps bleibt unverändert. Keine neuen Android-Berechtigungen, API-Schlüssel oder Abrechnung.


## FahrCockpit v8 (0.9.6-beta): ein eigener App-Start statt Samsung-App-Paar

Der App-Launcher `MainActivity` startet bei einem **App-Icon-Tap** direkt einen `DockedCockpitService` mit dem bestehenden Android-Overlayrecht und anschließend Google Maps. Es werden keine Samsung-Systempaneele benutzt und keine weiteren Klicks verlangt. Für Einstellungen über die Cockpit-Schaltfläche wird `MainActivity` mit anderem Intent geöffnet, ohne den Auto-Start auszulösen. Es gibt keinen Hintergrund-Autostart beim Booten.

Im Querformat liegt ein etwa 30 % breites FahrCockpit **rechts** über Google Maps, im Hochformat etwa 30 % hoch **unten**. Die Bedienung enthält Spotify-Titel, Titel-Ziehbalken, Zeiten, Play/Pause, vorherigen/nächsten Titel, ±15 Sekunden, Google Maps, Tankstellen, Saphe, Einstellungen sowie Minimieren/Schließen. Alle Inhalte sind scrollbar. Die vorherige frei verschiebbare Steuerleiste und der native Android-Splitscreen bleiben als **optionale** Modi in den Einstellungen.

**Wichtig:** Dies ist eine optische 70/30-Annäherung, **kein tatsächlicher 70/30-Splitscreen**. Das Overlay **verdeckt** 30 % des Bildschirms; Google Maps rendert weiterhin im gesamten darunterliegenden Fenster. Google Maps wird nicht verkleinert oder umpositioniert, deshalb können Karten- bzw. Routeninformationen unter der Abdeckung liegen. Ein echter 70%-Karten-Viewport kann ohne vom System gesteuerte Split-Quote bzw. eigenes Navigation SDK nicht garantiert werden. Keine Google-Cloud-Kosten, neue Android-Berechtigungen, Tokens oder ungeprüfte Saphe-API. Altversion-/Watch-Signing-Schlüssel unverändert. Einziger Samsung-Endtest nach signiertem Update.


## 0.9.7-beta / Build 9 – linksseitiges FahrCockpit im Querformat

Auf ausdrücklichen Wunsch des Nutzers sitzt das angedockte Spotify-/FahrCockpit-Bedienfeld **im Querformat links**, Google Maps bleibt sichtbar **rechts**. Der Dock-Bereich bleibt bei ca. **30 % Bildschirmbreite**, und beim Minimieren bleibt das Cockpit ebenfalls links. Im Hochformat bleibt der ca. 30 % hohe Dock-Bereich **unten** und die bisherige Bedienung unverändert. Automatischer Ein-Klick-Start über das eigene App-Symbol bleibt erhalten, kein Samsung-Edge-Panel nötig.

Die zugrunde liegende originale Google-Maps-App wird weiterhin **nicht verkleinert**; die seitliche Cockpit-Leiste liegt als Android-Overlay über ca. 30 % der Karte. Keine neuen Berechtigungen, API-Schlüssel, Cloud-Kosten, zusätzlichen App-Dienste oder Änderungen an Signatur/Workflows. Layout- und Kompaktmoduspositionen sind durch JVM-Tests abgedeckt. Endabnahme am Samsung erst nach signiertem Release.


## v10 (0.9.8-beta) – Google Maps ausdrücklich auf der Karte öffnen

**Nutzerbericht:** In der Google-Maps-Querformat-Ansicht blieb rechts neben dem links angedockten Cockpit der Rest der unteren Google-Menüleiste mit **„Beiträge“** sichtbar. Teilweise war Beiträge zuvor sogar aktiv. Das gehört zu Google Maps, nicht FahrCockpit.

**Ursachenhypothese und technische Änderung:** Der bisherige `getLaunchIntentForPackage("com.google.android.apps.maps")`-Aufruf öffnete lediglich Googles App und konnte damit den zuletzt angezeigten Reiter wiederherstellen. FahrCockpit öffnet jetzt über den **dokumentierten Maps-URL-Aufruf** `https://www.google.com/maps/@?api=1&map_action=map&layer=traffic` explizit die *Karte mit Verkehrslage* in der installierten Google-Maps-App. Gilt für Ein-Klick-Start, Dock-/Overlay-Maps-Knopf und optionalen System-Splitscreen. Android-`ACTION_VIEW` mit `setPackage` stellt sicher, dass nicht versehentlich ein Browser gestartet wird. Kein API-Key, keine Zahlung, keine Accessibility-Überwachung und **keine Maskierung** der Google-Maps-Bedienoberfläche. JUnit-Regressionstests schützen die URL-Parameter.

**Grenzen:** Dieser Aufruf kann verhindern, dass Google Maps lediglich einen zuvor aktiven „Beiträge“-Reiter wieder aufnimmt, **garantiert aber nicht die vollständige Ausblendung der fest in Google Maps eingebauten unteren Menüleiste**. Ob die Google-Maps-Version den Kartenaufruf genau wie erwartet umsetzt, erfordert **einen** Samsung-Endtest nach dem signierten Update. Ein erneuter, expliziter Kartenaufruf **kann eine laufende Navigation verlassen**; Google bietet kein offiziell unterstütztes Flag, das gleichzeitig die Karte erzwingt und eine bereits aktive Routenführung garantiert unberührt lässt. Für eine dauerhaft selbst gestaltete Karten-UI wäre eine andere Architektur nötig.


## 0.9.9-beta / Build 11 – Spotify-Rechtsmodus mit wechselbarer Seite

**Nutzer-Screenshot vom 09.11.2026:** Google Maps im Querformat hat seine untere Navigationsleiste („Erkunden“, „Mein Maps“, „Beiträge“) links bis zur Bildschirmmitte. Das zuvor links angedockte 30%-Cockpit verdeckt die ersten zwei Einträge, sodass „Beiträge“ als isolierter schwarzer Rest sichtbar wird. Der Versuch in v10, die Karte explizit mit `ACTION_VIEW` zu öffnen, hat diesen Effekt nicht behoben: Die Menüleiste gehört regulär zu Google Maps.

**Ab v11** wird der Spotify-/Cockpit-Dock im **Querformat standardmäßig RECHTS** dargestellt, damit die Google-Maps-Menüleiste **links vollständig sichtbar** bleibt. Minimierter Zustand ebenfalls rechts. In der FahrCockpit-Hauptansicht kann der Nutzer mit dem Schalter **„Querformat: Spotify rechts (empfohlen)“** zwischen rechts (Standard) und links wechseln. Die Auswahl wird in app-eigenen Preferences gespeichert und beim nächsten App-Start berücksichtigt. Im Hochformat bleibt das Cockpit unabhängig von diesem Schalter **unten**. `DockedLayoutRulesTest` prüft beide Querformatpositionen, minimierten Modus sowie Hochformat und Grenzwerte.

**Architekturentscheidung:** Keine ungeprüfte Einbettung der Google-Maps-App, kein Maskierungs-Overlay über Googles Reiter und keine zusätzliche SDK-/API-/Cloud-Abrechnung. Echte integrierte Google-Karte ohne diese Menüs wäre eine Neuentwicklung mit Maps SDK; turn-by-turn-Navigation in eigener App benötigt Navigation SDK, aktive Cloud-Abrechnung und API-Key. Der vorhandene kostenlose Google-Maps-Betrieb bleibt erhalten. Die systemeigene Google-Maps-Menüleiste bleibt natürlich sichtbar, aber nicht mehr teilweise verdeckt.

**Gerätestatus:** Linksseitiger v10-Start ist vom Nutzer auf Samsung getestet; Rechtsmodus v11 wird erst nach signiertem In-place-Update geprüft. Keine Extra-Minitests.


## 0.9.10-beta / Build 12 – Android-Tastatur bleibt bei Google-Maps-Suche frei

**Samsung-Beobachtung (09.11.2026):** FahrCockpit (Hochformat, unten ca. 30 % Höhe) überlagert bei Eingaben im Google-Maps-Suchfeld die geöffnete Bildschirmtastatur. Die Tastatur kann dadurch nicht bedient werden. Der Fehler ist durch die vorhandene Overlay-Flag-Kombination `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL` erklärbar: Android legt ein nicht fokussierbares Overlay typischerweise **über** die IME.

**Gezielter Fix:** Sowohl der neue `DockedCockpitService` als auch der ältere `OverlayService` verwenden nun die offiziell dokumentierte Flag-Kombination
`FLAG_NOT_FOCUSABLE | FLAG_ALT_FOCUSABLE_IM | FLAG_NOT_TOUCH_MODAL`.
Damit wird das nicht-editierbare Cockpit **unterhalb der Android-Eingabemethode gezeichnet**, ohne selbst Tastaturfokus zu übernehmen. Die Android-Tastatur darf die Leiste beim Tippen vorübergehend überdecken; beim Schließen der Tastatur ist das Cockpit wieder sichtbar, ohne Service-Neustart. **Keine** tastaturüberwachende Accessibility-API, keine fremden Texteingaben auslesen, kein `hideSoftInput` bei Google Maps. Die Bedienbarkeit der Maps-Suche ist wichtiger als eine ständige Sichtbarkeit der Leiste während Texteingabe.

**Automatisiert geprüft:** Einheitsmethode `OverlayKeyboardRules.nonFocusableBehindKeyboardFlags()` und Java-Tests prüfen die benötigten Flags sowie Erhalt von Touch-Ereignissen außerhalb des Fensters. Die tatsächliche Samsung/Google-Maps-IME-Schichtung erfordert die **einmalige Endabnahme des signierten Updates**; ohne Geräteabnahme darf nicht behauptet werden, es funktioniere auf dem Samsung garantiert.

**Unverändert:** Querformat-Dock standardmäßig **rechts** (alternativ links schaltbar), Hochformat Dock unten, Spotify-Ziehbalken, Google-Maps-Start, Tanken/Saphe, eigener App-Icon-Ein-Klick-Start. Gleiche App-ID und signierter Updatekanal wie v11, keine weiteren Manifest-Rechte, SDK-Kosten oder Signing-Workflowänderungen.
