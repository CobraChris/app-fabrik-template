# FahrCockpit v18 – gebündeltes Navigationspaket ohne neue Kosten

## Bestätigter Ausgangsstand
Samsung v17: Google-Karte, GPS-Punkt, Hoch-/Querformat und Spotify-MediaSession funktionieren. Die vorhandene JavaScript-Karte ist kein Turn-by-Turn-Navigator.

## Entwickelte Funktionen
1. Kompakter Knopf „📍 Ziel / Route wählen“ im nativen FahrCockpit-Bedienbereich; Spotify bleibt unverändert.
2. Scrollbarer Dialog für Adressen, Ortsnamen (auch japanisch) und Koordinaten.
3. „Route in Google Maps ansehen“ über offizielle Maps-URL sowie „Autonavigation in Google Maps starten“ über dokumentierten Android-Navigationsintent.
4. „Ziel merken“ speichert auf ausdrücklichen Fingertipp maximal fünf Ziele lokal in der App, Dubletten werden entfernt. Gespeicherte Ziele können mit einem Knopf vollständig gelöscht werden. Kein stiller Standortverlauf.
5. Im Google-Kartenhost kann ein Punkt angetippt und anschließend mit „Als Ziel übernehmen“ an das native FahrCockpit übertragen werden. Das löst niemals von allein eine Fahrt aus.
6. Validierung und URI-Encoding aller Ziele, Bereichsprüfung für Koordinaten; WebView akzeptiert die interne Kartenpunkt-URL nur vom gepinnten HTTPS-Host, keine JavascriptInterface-Bridge.
7. Aktuelles Ziel überlebt Bildschirmdrehung via savedInstanceState; nur ausdrücklich gemerkte Ziele überleben den App-Neustart.

## Absichtliche Grenze
Die echte Routenberechnung und Sprach-/Abbiegenavigation finden in der separat installierten offiziellen Google Maps Android-App statt. Währenddessen ist die FahrCockpit-Karte nicht dauerhaft sichtbar und das Spotify-Panel nicht in Google Maps eingebettet. Rückkehr zum Cockpit über Android-Appwechsel möglich. Keine Google Navigation SDK Integration oder Cloud Billing freigegeben.

## Freigabe und Abnahme
Android/JUnit und PWA-Node-Tests für Ziele, Intents, Kartenpunkt-Freigabe, Hauptfunktionen. Samsung-Test in beiden Ausrichtungen mit Tastatur, Wegführung/Zurück, GPS und Spotify erst nach signiertem Release.
Keine neue APK, Signierung oder Installation ohne gesonderte ausdrückliche Nutzerfreigabe. Vorhandenes FahrCockpit-Signierzertifikat weiterverwenden.

Quellen: https://developers.google.com/maps/documentation/urls/get-started und https://developer.android.com/guide/components/google-maps-intents
