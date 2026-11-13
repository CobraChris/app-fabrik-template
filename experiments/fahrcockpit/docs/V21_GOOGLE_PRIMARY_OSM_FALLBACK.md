# FahrCockpit v21 – Google zuerst, OpenStreetMap nur als Rückfallebene
Stand 12.11.2026. Freigegebene Nutzerentscheidung: FahrCockpit ist auf Google ausgerichtet; OSM ist KEIN gleichwertiger Standardanbieter. Die Hauptfunktion ist ein fest programmierter 70/30-Cockpit-Bildschirm (Google-Abbiegenavigation + Spotify), KEIN Samsung-Splitscreen.

## Modus A – Google (Standard)
Google Navigation SDK direkt im 70-Prozent-Kartenbereich mit Sprach- und Abbiegeführung. Google-SDK-Library wird nur bei ausdrücklich autorisierter Buildvariante eingebunden. API-Schlüssel ausschließlich geschützt, freigegebene SDK-Nutzung und aktives Billing. Zielanfrage erst nach Zustimmung; lokaler Limitzähler und serverseitige Quoten nachweisbar. 1.000 Ziele pro Monat sind zwar kostenlos, aber aktives Billing bleibt nötig. Der 1-€-Budgetalarm ist keine Kostensperre, eine echte Cloud-Tagesquote war im Trial nicht einstellbar.

## Modus B – OpenStreetMap (nur Ersatz)
Nur wenn Googles Quote erschöpft ist, eine lokal vereinbarte Tagesgrenze erreicht wurde, keine SDK-Autorisierung/Billing mehr besteht (z. B. nach dem 10.02.2027) oder Google nicht erreichbar ist, wird ein **eigenständiger vollwertiger** OSM-Navigator im selben 70/30-Bildschirm angeboten. Das erfordert Kartenanzeige plus vollständiges Routing, GPS-Positionsführung, Neuberechnung und Abbiege-/Sprachanweisungen. Ein OSM-Kartenfenster allein zählt nicht als Navigation. Keine automatische kostenrelevante Google-Wiederholungsanfrage. Nie Google-Navigationsinhalte, Google Places oder aus dem SDK abgeleitete Routen/Koordinaten auf eine OSM-Karte übertragen (Google Maps Platform Terms, Navigation SDK Abschnitt 12.2).

OSM-Daten sind frei, aber die von der OSM Foundation betriebenen Tile-Server verbieten Bulk-/Offline-Kachel-Downloads und bieten keine Verfügbarkeitsgarantie. Für einen dauerhaft kostenlosen mobilen Fallback lokale, rechtmäßig bezogene regionale Kartendaten und eigenen Routing-Code prüfen (z. B. Valhalla offline).

## Unveränderte Notlösung
Die installierte Google-Maps-Android-App bleibt weiterhin per Android-Splitscreen erreichbar, wenn SDK oder OSM-Navigator noch nicht einsatzbereit sind. Sie ist KEIN Ersatz für die eigentliche 70/30-Lösung. Das bereits signierte v20 bleibt unangetastet.

## Realisierungs- und Testgates
1. Google-First-Providerpolicy mit Tests (`NavigationProviderPolicy`) – reine Auswahlregel, **noch kein fertiger OSM-Navigator**.
2. Unabhängige OSM-Karten-/Routing-Engine implementieren, OSM-Lizenz und Offline-Betrieb prüfen.
3. Google-SDK in fester 70/30-Activity priorisiert starten; echte Quote-/Billing-/Fehlerzustände typisiert melden; auf OSM mit nachvollziehbarem Hinweis wechseln. Keine Vermischung von Providerdaten.
4. Nur nach ausdrücklicher Releasefreigabe SDK-fähige APK geschützt mit altem Zertifikat signieren. Noch kein Google-Cloud-Upgrade und keine neuen kostenpflichtigen Dienste.
5. Echter Samsung-Test (GPS/Sprachansagen/Spotify, Route/Reroute, Quote erschöpft, Trial-Ende, Offline, Hoch/Quer, Installation ohne Datenverlust).

Referenzen:
- https://developers.google.com/maps/documentation/navigation/android-sdk/pricing
- https://cloud.google.com/maps-platform/terms/maps-service-terms
- https://operations.osmfoundation.org/policies/tiles/
- https://valhalla.github.io/valhalla/

## Android-SDK-Fehlerpfad
Die `NavigationEngine.Status`-Schnittstelle besitzt jetzt einen typisierten `onGoogleFailure(Failure)`-Callback (Default-Methode, bestehende Lambda-Verwendungen bleiben möglich). `GoogleNavigationEngine` gibt `QUOTA_CHECK_FAILED` als `NAVIGATION_QUOTA_EXHAUSTED`, fehlende SDK-Autorisierung als `BILLING_OR_AUTHORIZATION_UNAVAILABLE`, lokale 20-Anfragen-Sperre als `LOCAL_REQUEST_LIMIT` und Netzwerk-/Laufzeitfehler als `GOOGLE_SERVICE_UNAVAILABLE` weiter. Bei fehlendem OSM-Router zeigt `IntegratedNavigationActivity` ausdrücklich nur die vorhandene externe Google-Maps-Notoption. Keine zusätzliche kostenrelevante automatische Routenanfrage und kein simulierter OSM-Verkehr.

## 12.11.2026 – OSM-Abbiege-/Sprachführung im Entwicklungstest

Die optionale OSM-Variante fordert nun `turnInstructionFormat=osmand` bei BRouter an und wertet die **tatsächlich von BRouter berechneten** GeoJSON-`properties.voicehints` aus. Ohne Hinweise werden weder Lenkmanöver erfunden noch eine Sprachnavigation gestartet.

`OsmTurnGuidance` projiziert verlässliche Android-GPS-Positionen auf die lokale BRouter-Routenlinie, verwendet höchstens 35 Meter gemeldete Genauigkeit, unterscheidet Voransage (bis 220 Meter) und Kreuzungsansage (bis 38 Meter) und verhindert die Wiederholung desselben Hinweises. Drei aufeinanderfolgende hinreichend genaue Messungen mehr als 65 Meter von der Route lösen eine lokale BRouter-Neuberechnung aus; ein 60-Sekunden-Cooldown verhindert Dauerschleifen. Bei abgebrochenen Routen verhindert ein Request-Epoch-Zähler verspätete Routenergebnisse.

Android `TextToSpeech` gibt die von BRouter abgeleiteten deutschen Hinweise über den Navigations-Audiokanal aus. Ohne deutsche TTS-Stimme bleiben lediglich visuelle Hinweise erhalten; es gibt weder Google-TTS-Cloudkosten noch eine automatische Sprachpaketinstallation. Der Standort wird nur erfasst, solange die Ansicht im Vordergrund ist. Bei Hintergrundwechsel werden GPS-Abfragen und Ansagen unterbrochen.

**Sicherheitsgate:** Das ist ein automatisiert testbares Entwicklungspaket, noch **kein** in realem Straßenverkehr geprüftes Navigationssystem. BRouter-Profile, Routen und Hinweise brauchen reale regionale Daten und einen Feldtest. OpenFreeMap-Kartenkacheln sind noch nicht offline verfügbar. Google Navigation bleibt erste Wahl; geschützte Release-/Signierungsschritte werden nicht ausgelöst.

## 12.11.2026 – v21 eigener Versionsstand und direkt integrierter Google-Start

Die App verwendet im v21-Entwicklungszweig `versionCode 21` und `versionName 0.9.19-beta`. Die Version v20 (0.9.18-beta) bleibt als signiertes Release unangetastet. Beim Start über das App-Symbol wird im optionalen Google-SDK-Build **nur bei gültigem lokal auf dem Handy gespeichertem eingeschränkten SDK-Schlüssel und vorhandener SDK-Bibliothek** die bestehende feste `IntegratedNavigationActivity` (70/30) geöffnet. Im gewöhnlichen schlüssellosen Build läuft weiterhin die bekannte kostenlose Google-Maps-App im Android-Splitscreen. Die Schlüsselabfrage löst keinerlei neue Routenanfrage aus; eine Google-Routenberechnung benötigt weiter eine ausdrückliche Bestätigung. Bei abgelaufenem Cloud-Billing kann die SDK-Initialisierung später fehlschlagen; die externe kostenlose Google-Maps-Funktion bleibt immer manuell erreichbar. OSM-Turn-by-Turn bleibt bis zum Hardwaretest Entwicklungscode.


## 13.11.2026 – Festes 70/30 mit links/rechts wählbarer Spotify-Steuerung

Im eigenständigen festen 70/30-Cockpit kann der Nutzer **im Querformat** den Spotify-Bedienbereich über einen eigenen Knopf sofort links oder rechts platzieren; die Karte nimmt stets 70 %, Spotify 30 % ein. Die Entscheidung wird in privaten Android SharedPreferences gespeichert (Standard: Karte links, Spotify rechts) und übersteht App-Neustarts und Versionsupdates mit gleichem Paket/Signatur. Beim Umschalten werden die zwei vorhandenen Android-Views umgeordnet, **ohne die Activity oder NavigationEngine neu zu erzeugen**, sodass laufende Musik und Routenstatus nicht unnötig zurückgesetzt werden. Im Hochformat bleibt die bewährte Anordnung Karte oben, Steuerung unten; die Seitenwahl ist dort deaktiviert und für die nächste Querformatnutzung gespeichert.

Die Browser-/In-App-Vorschau hat ebenfalls einen „⇄ Spotify links/rechts“-Umschalter mit CSS-`row-reverse` im Querformat. Im externen Browser wird die Auswahl mit LocalStorage gespeichert; in der abgesicherten lokalen WebView mit deaktiviertem DOM-Storage dient der Knopf ausschließlich der unmittelbar sichtbaren Vorschau. Die **native** FahrCockpit-Einstellung ist davon unabhängig persistent. Keine neuen API-Aufrufe, Bibliotheken, Berechtigungen oder Kosten; geschützte v20 bleibt unverändert.


## 13.11.2026 – Separat installierbare visuelle Android-Probe

Die OSM-CI erzeugt nun mit `-PwithOsmFallback=true -PvisualPreview=true` eine unabhängige **Debug-Sichttest-App** unter Paket `com.cobrachris.fahrcockpit.preview` und dem Namen „FahrCockpit v21 Vorschau“. Sie kann **neben** der normal signierten v20 stehen, ohne Paket oder Einstellungen der v20 zu überschreiben. Nach App-Start öffnet sich sofort die **native feste 70/30-Oberfläche** mit MapLibre-OSM-Karte, Spotify-Steuerung sowie dem links/rechts-Umschalter. Über „Zurück“ gelangt man zu den Vorschau-Optionen. Es wird **kein Google Navigation SDK und kein Google API-Key** verwendet, weil der ursprüngliche Google-Schlüssel auf den Originalpaketnamen eingeschränkt ist. Der experimentelle OSM-Navigator bleibt ausdrücklich nicht für Fahrten freigegeben.

GitHub Actions lädt eine kurzlebige, Android-typisch debug-signierte APK nur als **CI-Artefakt** hoch (7 Tage); kein GitHub-Release, keine Originalsignierung, kein Upgrade von v20 und keine Installation. Die Debug-Signatur kann bei erneuten CI-Builds wechseln: Folgeversionen dieses unabhängigen Sichttests sind daher nicht als reguläre Updates zugesichert. Für eine dauerhaft updatefähige v21 mit Originalpaket und Google SDK muss die bestehende geschützte Releasepipeline gesondert freigegeben und korrekt auf beide Module konfiguriert werden.


## 13.11.2026 – Bildschirmtest: Weltkarte und verdecktes Spotify behoben

Hardware-Screenshots der unabhängig installierten v21-Vorschau zeigten die OSM-Karte beim ersten Start in der Weltansicht und zu wenig Raum für Spotify im Hochformat. Im **Querformat bleibt der native 70/30-Split unverändert**, einschließlich persistentem Links-/Rechts-Umschalter. Im **Hochformat wird die Kartenhöhe auf 45 % und der separat scrollbar bedienbare Spotify-/Einstellungsbereich auf 55 %** gesetzt. Der deaktivierte Links-/Rechts-Umschalter wird im Hochformat ausgeblendet.

Die kostenlose MapLibre-OSM-Engine zentriert bei vorhandener Feinstandortberechtigung einmalig auf einen zeitlich frischen Android-GPS-/Netzwerkfix (max. 3 min alt, Genauigkeit <=250 m). Der neue gut sichtbare **„◎ Meine Position“**-Knopf rechts oben erlaubt die jederzeitige manuelle Neuzentrierung. Ein blauer, weiß umrandeter Positionspunkt zeigt nur den unabhängig ermittelten Android-Standort. Fehlende Berechtigungen, deaktivierter Standort oder ungenaue/stale Fixes werden nicht als gültige Position vorgetäuscht. Ausstehende Standortabfragen werden bei Hintergrundwechsel/Schließen abgebrochen. Es werden keine Google-Maps-SDK-Daten an OSM weitergegeben und keine neuen kostenpflichtigen Dienste eingebunden.


## 13.11.2026 – Klarstellung nach Hardware-Sichttest: Google bleibt das Produkt

Die separate installierbare Vorschau ist **ausschließlich ein OSM-Sichttest**, weil ihr Paketname `com.cobrachris.fahrcockpit.preview` und das Debug-Zertifikat nicht zur eingeschränkten Google-SDK-Android-Credential passen. Die Nutzerentscheidung bleibt unverändert: **Google Navigation SDK im fest integrierten 70/30-Fenster ist die Hauptnavigation**; OSM ist ausschließlich die technische Rückfallebene. Die native OSM-Sichttestoberfläche zeigt nun sichtbar „FahrCockpit v21 · OSM-Sichttest“ und erklärt, dass die echte Google-Navigation erst in der mit Originalzertifikat und dem richtigen Paketnamen signierten v21 enthalten sein kann. Die optionale externe Google-Maps-App heißt ausdrücklich „Google Maps separat öffnen (Samsung-Splitscreen)“, statt den Android-Splitscreen als feste Google-SDK-Navigation auszugeben. Der Vertragstest schützt diese Beschriftung.

**Keine Änderung des Cloud-Kontos, keine Originalsignierung, keine Veröffentlichung.** Die neue Beschriftung erscheint erst in einer erneut vom Nutzer manuell installierten Test-APK, nicht rückwirkend in der bereits installierten Vorschau. Der gesonderte geschützte Releasepipeline-Entwurf PR #71 bleibt DRAFT.
