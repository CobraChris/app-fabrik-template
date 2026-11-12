# FahrCockpit v20 – Google Navigation direkt im festen App-Fenster

## Zielarchitektur

Ein einziges `IntegratedNavigationActivity` ohne Multi Window/Overlay, mit:
- Querformat: 70 % Google Navigation SDK `NavigationView` links und 30 % Spotify/Bedienung rechts
- Hochformat: 70 % Navigationsfläche oben und 30 % Spotify/Bedienung unten
- vollständigem nativen Spotify-MediaSession-Transport: Play/Pause, vor/zurück, +/-15 Sekunden, Seekbar
- Tankstellen und Saphe als bereits vorhandene Android-Funktionen
- v18/v19 gespeicherten Zielen, lokaler Zieleingabe und ausdrücklich bestätigter Routenberechnung
- Google Navigation SDK für echte Abbiege- und Sprachnavigation direkt in der Kartenfläche.

**Wichtig: Dies ist ein noch NICHT live mit Google Cloud autorisierter Entwicklungsstand.**
Die auf Samsung erfolgreich abgenommene **v19** bleibt die aktuelle installierbare Testversion.

## Umsetzung und Schutz

Die Version 20 ist zunächst **kein Release**. Standard-Gradle-Build (samt bestehender geschützter APK-Pipeline) nutzt:
- `-PwithGoogleNavSdk=false` als Default
- **keine** Navigation SDK Library, keine SDK-Initialisierung, keine Google-SDK-Routenabrufe
- **keinen** API-Key und **keine** Cloud-Abrechnung
- eine klar als Vorbereitung gekennzeichnete feste 70/30-Vorschau
- den funktionierenden Google Maps Android-Splitscreen aus v19 als Rückfallmöglichkeit.

Das SDK ist technisch in `src/navsdk/java/.../GoogleNavigationEngine.java` separiert. Der optionale Build:
`gradle -p experiments/fahrcockpit -PwithGoogleNavSdk=true :app:testDebugUnitTest :app:assembleDebug`
verwendet `com.google.android.libraries.navigation:navigation:7.6.1`, Android API 36, Java 17, Gradle 8.11.1 und Android Gradle Plugin 8.10.0. **Ohne** die Umgebungsvariable `FAHRCOCKPIT_NAV_SDK_KEY` bleibt selbst ein so gebauter Test ohne echte Nav-SDK-Verbindung. Der Build ist kein Ersatz für eine autorisierte Google-Cloud-Konfiguration.

Eine echte Routenabfrage erfolgt **nur** nach:
1. ausdrücklicher Freigabe von Google-Cloud-Billing, Cloud-Projekt und Navigation SDK durch den Nutzer
2. separatem, Android-App/SHA-1-signing-certificate-restringiertem Google Navigation SDK API-Key; kein Schlüssel im Repository
3. authentifiziertem SDK-Initialisieren und ausdrücklicher Standortfreigabe
4. Eingabe/Geocoding des Ziels sowie einer **zweiten sichtbaren Bestätigung**, dass der Routenabruf gegen die Nav-SDK-Quote zählt
5. `Navigator.setDestination(...)`, und nur nach Erfolg `Navigator.startGuidance()` mit Sprachführung.

Bei Beenden wird `stopGuidance()` und `clearDestinations()` aufgerufen. Reines Umschalten der Bildschirm-Ausrichtung löst **keine** neue Route aus; die Karten-View-Lifecycle-Methoden werden korrekt weitergereicht.

Die Android-Geocoder-Namensauflösung ist eine separate Android-Systemfunktion (nicht Google Places API). Ihr Ergebnis muss vor dem kostenrelevanten Nav-SDK-Aufruf kontrolliert/bestätigt werden. Für höchste Zielführung-Qualität empfiehlt Google Place IDs; die optional kostenpflichtige Places API ist noch nicht vorgesehen.

## Unverzichtbare Freigaben vor einem tatsächlichen SDK-Release

- **Google Cloud Billing explizit zustimmen und selbst einrichten** – auch die ersten 1.000 kostenlosen Ziele/Monat erfordern aktives Billing.
- Google Navigation SDK im dedizierten Projekt einschalten; Eingrenzung auf Navigation SDK und Paketname `com.cobrachris.fahrcockpit` plus SHA-1 des Original-FahrCockpit-Signierzertifikats.
- **Navigation Request-Quote anfangs auf 20 Ziele/Tag begrenzen**, sofern diese Tagesquote in Google Cloud angeboten wird. Damit maximal 620 Ziele in 31 Tagen (nur dieses Projekt). Nutzung und Budgetalarme einstellen. Ein Budgetalarm allein ist keine harte Kostensperre. Auch innerhalb des Freikontingents kann es bei unkontrollierter Nutzung kostenpflichtig werden.
- Navigation SDK-Attributionen und Lizenzhinweise aus `NOTICE.txt` und `LICENSES.txt` im finalen App-UI rechtskonform bereitstellen (noch offen).
- Geschützte Build-/Signierpipeline separat für den **autorisierten SDK-Release** ergänzen, ohne den Originalkeystore zu verändern. Freigabe dafür ausdrücklich einholen.
- Android-Geräteabnahme: Google-Destinationsrouting, Spoken Guidance mit Spotify, Neustart/Drehung, Hoch/Quer 70/30, Start/Stopp, Abbiegekarte und Tastatur im Stand.
- Keine automatische Installation; bestehende FahrCockpit-App bis zum geprüften SDK-Update beibehalten.

## Aktuelle Quellen (10.11.2026)
- Google Navigation SDK – Voraussetzungen: https://developers.google.com/maps/documentation/navigation/android-sdk/setup-overview
- Google Navigation SDK – Android Setup: https://developers.google.com/maps/documentation/navigation/android-sdk/android-studio-setup
- Google Navigation SDK – Einzelziel: https://developers.google.com/maps/documentation/navigation/android-sdk/route
- Google Navigation SDK – Abrechnung: https://developers.google.com/maps/documentation/navigation/android-sdk/pricing
- Google Navigation SDK – NavigationView Lifecycle: https://developers.google.com/maps/documentation/navigation/android-sdk/reference/com/google/android/libraries/navigation/NavigationView

## Google-Cloud-Einrichtung nach Nutzerfreigabe vom 11.11.2026

Der Nutzer hat die Nutzung eines Google-Cloud-Abrechnungskontos fuer die echte Integration grundsaetzlich genehmigt. **Das ist weder eine ausgefuehrte Kontoaktivierung noch eine Freigabe zum Signieren/Veröffentlichen.** Keine Guthaben-, Kontodaten- oder Schluesselwerte im Repository ablegen.

- Eigenes Google-Cloud-Projekt `FahrCockpit Navigation` mit zugeordnetem Billing-Konto.
- Ausschliesslich `Navigation SDK for Android` (`navigationsdk.googleapis.com`) aktivieren, nicht weitere Maps- oder Places-Dienste automatisch einschalten.
- Separater API-Key: **API restrictions** nur Navigation SDK for Android; **Application restrictions** Android apps, Paket `com.cobrachris.fahrcockpit`, **SHA-1** des bisherigen signierten v19-Release-Zertifikats. Achtung: Der SHA-256-Hash der APK selbst ist weder der Android-Signatur-Fingerabdruck noch die benoetigte API-Key-Beschraenkung. Debug-Zertifikat ist nicht das Originalzertifikat.
- Die optionale CI liest zur Orientierung den oeffentlichen SHA-1-Zertifikat-Fingerabdruck der original signierten v19 direkt aus dem GitHub Release (ohne Zugriff auf Keystore oder Signier-Secrets); Ergebnis nur im CI-Protokoll, nicht als API-Key.
- Navigation-Request-Quote nach Moeglichkeit **20 Ziele/Tag** (620/31 Tage) einstellen und kostenloses Monatskontingent sowie Restquote ueberwachen. Eine tatsaechlich durchgesetzte Quote ist zu kontrollieren; bei fehlender oder ungeeigneter Tagesquote darf keine Kostenfreiheit versprochen werden. Cloud-Budgetalarm z.B. bei 1 EUR/Monat als Zusatz, **keine harte Kostensperre**.
- API-Key niemals in Chat, PR, Issue oder git ablegen. Separaten geschuetzten GitHub-Actions-Secret-Kanal fuer einen spaeter ausdrücklich autorisierten Signed-Testbuild verwenden.
- Erst nach verifizierter Cloud-Verbindung, Lizenz/Attribution, signiertem v20 Testbuild mit demselben Originalzertifikat und echtem Samsung-Test die geschuetzte v20 Releasefreigabe einholen. v19 bleibt unveraendert.

Offizielle Quellen: https://developers.google.com/maps/documentation/navigation/android-sdk/get-api-key , https://developers.google.com/maps/documentation/navigation/android-sdk/pricing

## Zusätzliche lokale Kostenbremse

v20 reserviert vor jedem ausdrücklich bestätigten `Navigator.setDestination`-Aufruf einen lokalen Platz. Maximal 20 Plätze pro UTC-Tag werden im Android-App-Speicher gespeichert; ein nicht speicherbarer Zähler sperrt weitere Anfragen. Ein SDK-Fehler gibt den Platz konservativ nicht zurück. Die Prüfung verbietet auch das einfache Zurückstellen der Systemzeit auf einen früheren UTC-Tag. JUnit- und Node-Vertragstests decken das ab.

**Keine harte Kostengarantie:** Neuinstallation, Löschen der App-Daten und andere Geräte können die lokale Sperre umgehen. Daher ist zusätzlich eine in Google Cloud durchgesetzte Navigation-Request-Tagesquote verpflichtend. Die Freigabe für Billing bestätigt noch nicht, dass Konto, Quote oder API-Schlüssel bereits eingerichtet wurden. Das signierte v19-Release wird nicht verändert.

## Dauerbetrieb nach Ablauf des Cloud-Testzeitraums (10.02.2027)

Google Cloud schließt Testkonten ohne Upgrade nach Ablauf. Trotz 1.000 monatlich kostenloser Navigation-Requests setzt das Navigation SDK weiterhin ein aktives Billing-Konto voraus.

**Deshalb ist die kostenlose, cloudunabhängige Google-Maps-App im Android-Splitscreen jetzt der Standard beim Start über das FahrCockpit-Appsymbol.** Der Weg nutzt kein FahrCockpit-Cloud-Projekt und bleibt auch nach Ende der Google-Cloud-Testphase verfügbar, sofern Google Maps auf dem Handy verfügbar ist. Die Android-Systemfenstergröße bestimmt Samsung, daher sind **feste 70/30 mit Original-Google-Abbiegenavigation ohne SDK nicht garantiert**.

Das feste, eingebettete 70/30-Navigation SDK ist ab jetzt ausschließlich manuell als optionale Funktion aufrufbar. Im 30-Prozent-Cockpit ist immer ein sichtbarer Button „Google Maps dauerhaft kostenlos“ vorhanden, auch bei fehlender SDK-Autorisierung und eingeklapptem Zielmenü. Die ältere 70/30-Google-Web-Kartenansicht ist separat erhalten, jedoch keine vollwertige Navigation und kann ebenfalls von einem Cloud-Schlüssel abhängen.

Kein Upgrade, keine Signierung, keine Veröffentlichung, keine Installation. Die Benutzerfreigabe des bestehenden v19-Updates bleibt unberührt.

Quellen: https://developers.google.com/maps/faq ; https://docs.cloud.google.com/free/docs/free-cloud-features
