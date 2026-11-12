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
