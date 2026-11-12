# FahrCockpit v15 – integrierter Google-Kartenbereich (Proof of Concept)

## Anforderung
Nach Build 13 meldete der Nutzer weiterhin eine verdeckte Google-Maps-Navigation. Er verwies auf die vorhandene Live-Karte, in der Google Maps JavaScript in einem begrenzten Kartencontainer mit eigenen Schaltflächen liegt. Diese Architektur wird nun für FahrCockpit adaptiert.

## Stand
- Eigene EmbeddedMapActivity mit echtem Layout: **70 % Kartenfläche + 30 % native Bedienfläche**, Querformat nebeneinander, Hochformat übereinander.
- Kartenfläche ist eine WebView mit fester Größe, kein TYPE_APPLICATION_OVERLAY. Beide bisherigen Overlay-Dienste werden beim Start der Testansicht gestoppt.
- Separate HTTPS-Unter-App vorgesehen: https://cobrachris.github.io/app-fabrik-template/apps/fahrcockpit-map/ (tatsächliche GitHub-Pages-Bereitstellung nach Deployment prüfen).
- Google Maps JavaScript wird erst mit lokal eingegebenem Demo-Schlüssel geladen. Kein Schlüssel im APK/Repository, Speicherung im isolierten Browser-/WebView-Storage, keine native JavaScript-Bridge.
- Google-Karte unterstützt Zoom, Pan und nach ausdrücklicher Standortfreigabe GPS-Zentrierung. Keine Maps-App-UI wird unter Spotify versteckt.
- Spotify nutzt weiter die Android MediaSession: Titel, Play/Pause, Vor/Zurück, ±15 Sekunden, Zeit-Schieberegler. Native Buttons für Tankstellen und Saphe; normale Google Maps kann für echte Navigation separat geöffnet werden.
- Neue Ansicht nur über **„Integrierte Google-Karte testen (Prototyp)“** im Hauptmenü. Standardstart v14 (Android-Systemsplit) bleibt unverändert. Noch keine produktive Navigation.
- HTTPS-Host-Whitelist, kein JavascriptInterface, keine lokalen Dateien, kein Mixed Content. Keine neuen Android-Berechtigungen, neue Google-Cloud-Konten, Signier-/Release-Workflowänderungen oder zusätzliche Kosten.

## Explizite Grenzen
Die eingebettete Google-Karte ist **nicht** die originale Google-Maps-App. Die Demo liefert **keine** Google-Turn-by-Turn-Fahrnavigation, keine dynamische Umleitung, kein Traffic-Rerouting oder verlässliche Sprachausgabe. Vollwertige integrierte Google Navigation SDK braucht einen abrechnungsfähigen Cloud-Account; dieser Prototyp nutzt es nicht.

Maps Demo Key ist **nur für Tests und Prototypen** und kann nach Quotenausschöpfung pausieren. Er darf nicht als garantiert kostenloser produktiver Autonavigator eingesetzt werden. Bei fehlendem Schlüssel bleibt eine klare Einrichtungsansicht.

## Prüf- und Releasegate
1. Kartenunterseite aus einem separaten geprüften main-PR veröffentlichen und den **exakten HTTPS-Pfad** prüfen.
2. Android-CI für die Java-Klassen und Spotify-Funktionen prüfen; PR-Dateien kontrollieren.
3. Erst über den separat geprüften geschützten FahrCockpit-Signierweg eine APK veröffentlichen. Keine Deinstallation.
4. Samsung-Gerätetest nur im Stand: Demo-Key in WebView eingeben, Karte lädt, Rotation, Spotify, Tastatur, Rückkehr, GPS-Freigabe, Netzwerkfehler, keine Überlagerung.
5. Keine Billing-Aktivierung und keine automatische Fahrt-Navigation ohne gesonderte Prüfung.

**Keine Live-Karte-Quelländerung**; nur das Karten-Layoutmuster wurde übertragen.
