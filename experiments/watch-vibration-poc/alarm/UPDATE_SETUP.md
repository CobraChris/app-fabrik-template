# Sanftwecker: sichere und einfache App-Updates

Stand: 06.11.2026 – Update-Grundlage implementiert, **noch nicht für Nutzer eingerichtet**.

## Ziel

Eine vorhandene Android-Installation soll **ohne Deinstallation** aktualisiert werden. Gespeicherte Wecker, Timer, Stoppuhren und Freigaben bleiben bei einer normalen Aktualisierung erhalten.

Die Android-Plattform verlangt dafür:
- Exakt dieselbe \`applicationId\` (\`com.cobrachris.sanftwecker\`)
- Exakt dieselbe Signatur bzw. gültige Schlüsselrotation
- Höheren Android-\`versionCode\`
- Meist eine Bestätigung des Anwenders im Systeminstaller

Bisherige Debug-APKs kommen von **ephemeren GitHub-Runnern**, deren \`debug.keystore\` in jedem Runner unterschiedlich sein kann. Die alte private Signatur lässt sich aus einer bereits installierten APK nicht zuverlässig wiederherstellen. Daher ist bei Umstellung auf einen eigenen dauerhaften Schlüssel **voraussichtlich eine letzte Neuinstallation nötig**. Das löscht lokale App-Daten – vorher Wecker-Einstellungen dokumentieren.

## Bereits vorbereitet

1. **Button „App aktualisieren“** auf der Weckerliste öffnet die Update-Seite.
2. **„Nach Update suchen“** ruft nur auf ausdrücklichen Klick öffentlich sichtbare GitHub-Releases ab (kein Hintergrunddienst).
3. Als Update akzeptiert die App ausschließlich ein **direktes APK-Asset \`sanftwecker-release.apk\`** aus Releases dieses Repositories mit einem Tag wie \`sanftwecker-v9\`; ältere Releases werden ignoriert.
4. Über **„APK im Browser herunterladen“** wird der **externe Android-Browser** geöffnet. Android führt die Installation nach Nutzereingabe durch. Es gibt **keine stille Installation**, keine automatische Deinstallation und keine unsignierte APK-Fallback-Installation.
5. Ein eigener manueller GitHub-Workflow \`.github/workflows/sanftwecker-signed-release.yml\` erstellt die Release-APK, signiert sie, prüft die Signatur und lädt die APK **direkt** in ein GitHub-Release (nicht in ein ZIP).
6. Die APK darf nur veröffentlicht werden, wenn die beiden Signier-Secrets vorhanden sind. Private Schlüssel werden **nie** ins Repository eingebaut oder in die App aufgenommen.

**Beachte:** Die Update-Seite benötigt die Android-Berechtigung \`INTERNET\`, die ausschließlich beim manuellen Updatecheck genutzt wird. Ansonsten weiterhin kein Tracking, kein Konto, keine Werbung, keine kostenpflichtigen Dienste.

## Noch einmalige GitHub-Einrichtung erforderlich

Nur Repository-Inhaber bzw. berechtigte Administratoren können GitHub Actions Secrets eintragen. Es sind genau zwei nötig:

- \`SANFTWECKER_KEYSTORE_BASE64\`: Base64-Text einer dauerhaften, privaten PKCS12/JKS-Keystore-Datei
- \`SANFTWECKER_SIGNING_PASSWORD\`: starkes Keystore-Passwort (gleiches Passwort für Store und Schlüssel)

Der Keystore muss **einmalig** mit Alias \`sanftwecker\` erzeugt werden, \`RSA 3072\`, Gültigkeit mindestens 25 Jahre. Die erzeugte Schlüsseldatei und das Passwort unbedingt sicher sichern; falls verloren, werden zukünftige Updates unmöglich. Die Secrets niemals in GitHub-Issues, PRs, öffentlichen Dateien, Chat-Text oder Actions-Logs veröffentlichen.

Einrichten in GitHub im Repository: **Settings → Secrets and variables → Actions → New repository secret**. Weder ein GitHub-Token noch ein kostenpflichtiger Dienst ist notwendig.

Nach Einrichtung: Den Release-Workflow in den **Standardzweig** überführen, damit GitHub \`workflow_dispatch\` anbieten kann, und beim ersten kontrollierten Release einmal manuell auslösen (gewünschten Revisionszweig wählen). Der Workflow baut und veröffentlicht ausschließlich, wenn die Secrets korrekt vorhanden sind. Der PR darf bis zur Samsung-Abnahme Draft bleiben.

## Warum die App Updates nicht selbst installiert

Auf Android dürfen gewöhnliche Drittanbieter-Apps eine neue APK nicht still und unbeaufsichtigt installieren. Eine Nutzerbestätigung und bei Downloads eventuell die Freigabe zum Installieren aus dem jeweiligen Browser sind erforderlich. Ein Update mit gleichem Paketnamen, gleicher Signatur und höherem VersionCode lässt die Daten und die meisten Berechtigungen bestehen. Nach Updates kann Samsung Galaxy Wearable Benachrichtigungen weiterhin anders behandeln; deshalb Watch-Freigabe und Alarm-Tests nach dem ersten signierten Update überprüfen.

## Grenzen und Sicherheitsbewertung

Die bestehende Android-Anwendung zeigt den Update-Button, **aber es gibt noch kein signiertes GitHub Release**. Solange die GitHub-Secrets und der Workflow nicht eingerichtet sind, meldet die App „Noch keine dauerhaft signierte APK veröffentlicht“. Das ist beabsichtigt und verhindert eine falsche Erfolgsmeldung.

Wecker-App bleibt in Beta. Den bisher zuverlässig funktionierenden anderen Wecker bis zur Samsung-Abnahme beibehalten.

## Beta 0.8.1: Signaturkontinuität und Updateprüfung

- Build 10 ersetzt Build 9 mit **identischer applicationId**
  `com.cobrachris.sanftwecker`.
- Die Updateprüfung akzeptiert nur noch ein APK-Asset, dessen tatsächlicher
  GitHub-Release-Tag mit dem im JSON angegebenen Tag übereinstimmt.
- Versionscodes werden auf den Android-Bereich 1 bis 2.100.000.000 begrenzt;
  Versionen mit führenden Nullen werden zurückgewiesen.
- Der GitHub-Release-Workflow prüft beim Übergang von Build 9 zu Build 10
  vor Veröffentlichung den SHA-256-Fingerabdruck des Signaturzertifikats.
- Für die spätere In-place-Installation ist zusätzlich ein praktischer
  Gerätetest nötig. Die automatische CI kann keine echten Alarm- oder
  Galaxy-Watch-Berechtigungen prüfen.
