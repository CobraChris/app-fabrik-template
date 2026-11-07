# FahrCockpit – Blitzer.de-Steuerung (Android-Test, FC-03)

Dies ist die **erste native Android-Komponente** des FahrCockpit-Projekts in der gemeinsamen
GitHub-App-Fabrik. Sie ersetzt **nicht** Google Maps und zeigt **noch kein Spotify-Overlay,
keine Tankstellenpreise und keine Blitzer-Marker in Maps** an.

## Funktioniert bereits im Quellcode (Gerätetest ausstehend)

- Schalter **EIN** sendet per explizitem Android-Broadcast an Blitzer.de PRO
  (Paket de.blitzer.plus, Receiver de.blitzer.service.BlitzerBroadcastReceiver)
  den Befehl de.blitzer.APP_MODE mit String-Extra mode=START.
- Schalter **AUS** sendet de.blitzer.KILL_APP an denselben Receiver.
- **Blitzer.de jetzt beenden** sendet diesen Befehl jederzeit, auch wenn Blitzer.de
  unabhängig vom FahrCockpit-Schalter gestartet wurde.
- Tasten **Hintergrund** und **Mini-App** senden mode=BACKGROUND bzw. mode=WIDGET,
  nachdem ein Startbefehl gesendet wurde.
- Schalter ist bei Erstinstallation AUS. Nur letzter Bedienwunsch wird lokal gespeichert;
  **App-Start/Neustart sendet nie eigenständig einen Startbefehl**.
- Kein Blitzer-Datenabruf, keine eigene Warnquelle, kein Standortzugriff, keine Analytics,
  keine Fahrdaten und keine zusätzlichen Abos/API-Kosten für FahrCockpit.
- Nicht installiertes Blitzer.de PRO und blockierte Befehle führen zu einer Meldung, nicht
  zu einem erfundenen Status. Ein abgesendeter Broadcast garantiert nicht, dass die
  empfangende App die Aktion ausführt.
- Google Maps bleibt eine separate Android-App; ein Button kann sie öffnen.

## Technisches Design

Projekt: experiments/fahrcockpit, Modul: app, Paket: com.cobrachris.fahrcockpit.
Java 17, AGP 8.9.2, compile/target SDK 35, min SDK 30. Keine zusätzlichen App-Libraries.

- BlitzerContract.java: statisch testbarer Intent-Vertrag (ohne Android-Abhängigkeit)
- BlitzerBridge.java: Paketprüfung + explizites Senden, **ohne Erfolgsbestätigung**
- MainActivity.java: bewusst manuelle Steuerung + Anzeige des letzten gesendeten Befehls
- BlitzerContractTest.java: JVM-Regressionstests auf Aktion, Paket, Receiver und Modus

Der schlichte Schalter ist **noch kein Overlay über Google Maps**. Spotify-Overlay und
Tankstellen-Preislisten werden in separaten FahrCockpit-Schritten umgesetzt.

## Android-Build und Tests

Read-only CI ruft den wiederverwendbaren Android-Check der App-Fabrik auf.
Dabei werden Unit-Tests, Android Lint und eine **unsignierte Debug-APK** gebaut
(kein Release, keine Installation, keine Signier-Secrets).

Manuell mit Java 17, Android SDK 35 und Gradle 8.11.1:

    gradle --no-daemon -p experiments/fahrcockpit :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

## Manuelle Samsung-Abnahme vor jeder Release-Auslieferung

1. Blitzer.de PRO prüfen, ggf. installieren und initial die benötigten Berechtigungen
   **direkt in Blitzer.de PRO** konfigurieren (Overlay für Mini-App; Hintergrundbetrieb).
2. FahrCockpit öffnen; standardmäßig AUS. EIN drücken und prüfen, ob Blitzer.de
   tatsächlich startet. **Nicht während des Fahrens testen.**
3. Mini-App / Hintergrund separat anfordern, Google Maps öffnen und kontrollieren,
   dass Blitzer.de weiterhin funktioniert, ohne die Navigation unbeabsichtigt zu ersetzen.
4. AUS drücken; bei Blitzer.de prüfen, ob es tatsächlich beendet wurde.
5. FahrCockpit neu öffnen: gespeicherter Schalterzustand wird angezeigt, es erfolgt
   **kein automatischer Startbefehl**. App ohne installiertes Blitzer.de PRO testen.
6. Bei Android-/Samsung-Abweichungen nur nach realem Test den Broadcast-Ablauf anpassen.

Wichtig: Blitzer.de dokumentiert **keine Rückmeldung des aktiven Zustands**. Deshalb
niemals aus einem gesendeten Intent ein bestätigtes "Blitzer aktiv" ableiten.
Die START- und Hintergrund-Befehle sind getrennt, um nicht mit unbegründeten
Zeitverzögerungen und Race Conditions zu experimentieren.

## Recht und Preise

Deutschland: § 23 Abs. 1c StVO untersagt Fahrzeugführenden die Verwendung einer
aktiven Verkehrsüberwachungs-Warnfunktion während der Fahrt. Der Schalter ist
keine Ausnahmeregel. Nutzung ausschließlich dort, wo sie erlaubt ist.

Blitzer.de PRO ist eine separate kostenpflichtige App, falls noch nicht gekauft;
FahrCockpit selbst verlangt keinen kostenpflichtigen API-Dienst.

Dokumentation des Anbieter-Intent-Vertrags:
https://www.blitzer.de/questions/wie-richte-ich-den-automatischen-start-stopp-per-tasker-ein/

Blitzer.de Hintergrundbetrieb:
https://www.blitzer.de/questions/die-app-warnt-mich-nicht-im-hintergrund-was-muss-ich-einstellen-android/

Projektauftrag: https://github.com/CobraChris/app-fabrik-template/issues/26
