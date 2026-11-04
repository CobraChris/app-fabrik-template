# ChatGPT → Startpage: Link-Öffner für Android

Status: Proof-of-Concept-Quellcode, noch nicht als APK gebaut und nicht auf dem Samsung getestet.
Die offizielle ChatGPT-App bleibt unverändert. Keine API, Trackingdienste, Logins oder Gebühren.

## Ziel

Der Link https://cobrachris.github.io/open/?url=https%3A%2F%2Fwww.startpage.com%2F
soll als Android Verified App Link unsere Mini-App starten. Diese liest den URL-Parameter
und startet den tatsächlich eingestellten Android-Standardbrowser, ohne eingebettete Custom Tab.

## Wichtige Voraussetzungen

1. Android prüft zwingend https://cobrachris.github.io/.well-known/assetlinks.json
   über HTTPS ohne Redirect. Die bestehende Projektseite unter /app-fabrik-template/
   kann diese Root-Datei nicht bereitstellen. Ein neues, öffentliches User-Pages-Repo
   CobraChris/CobraChris.github.io oder eine eigene Domain wäre nötig.
2. Die Datei muss den SHA-256-Fingerprint des real verwendeten APK-Signaturzertifikats
   enthalten. Die Vorlage in verification/assetlinks.example.json hat nur einen Platzhalter.
   Niemals den privaten Signaturschlüssel in ein öffentliches Repository legen.
3. Android SDK 35, JDK 17, Gradle 8.9 und Android Gradle Plugin 8.7.3 werden benötigt.
   Build aus diesem Ordner: gradle assembleDebug. APK-Build und Installation fehlen noch.
4. Die ChatGPT-App muss den verifizierten Link an Android durchreichen; das ist nicht
   nachgewiesen und der entscheidende Praxistest auf dem Samsung.

## Unabhängiger Ersttest

Nach dem Installieren die App per Homescreen starten und auf
Startpage testweise öffnen tippen. Dieser Schritt prüft nur die Übergabe an
den Browser, noch nicht den direkten Weg aus ChatGPT.

## Java-Selbsttest (ohne Android-SDK)

javac -d /tmp/chatlinktest app/src/main/java/io/github/cobrachris/chatlinkopener/TargetUrl.java tests/TargetUrlSelfTest.java
java -cp /tmp/chatlinktest io.github.cobrachris.chatlinkopener.TargetUrlSelfTest

## Abnahmekriterien

- Ein Tipp auf einen speziell formatierten ChatGPT-Link startet Startpage extern.
- Zurückwechseln zeigt den bisherigen Chat, nicht einen neuen Chat.
- Wiederholte Links funktionieren konsistent.
- Zieladressen werden nur als HTTP(S) geöffnet, keine unzulässigen Schemes.
- Kein Webhosting oder API mit laufenden Gebühren.

## Nächste Arbeiten

1. Android-Build und Signierung vorbereiten.
2. Separaten Build-Workflow erst nach gesonderter menschlicher Prüfung hinzufügen.
3. Root-Pages-Domain und assetlinks.json einrichten.
4. Auf dem Samsung installieren und die End-to-End-Übergabe aus ChatGPT testen.

Es wird ausdrücklich nicht behauptet, dass der genannte Link jetzt schon funktioniert.
