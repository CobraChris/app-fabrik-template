# App-Fabrik: Universelle PWA-Vorlage

**Status:** Wiederverwendbares Template mit statischer PWA, Offline-Funktion, automatischen Tests, optionalem Codex-Vorschlagsworkflow und GitHub Pages. Keine Frameworks oder npm-Abhängigkeiten.

## Ideenboard als strukturierter Entwicklungsauftrag

Die Start-App unterstützt jetzt **App-Steckbriefe**: Name, Ziel, gewünschte MVP-Funktionen, Abnahmekriterien, Einschränkungen, Priorität und Status. Das Formular kann klein bleiben (nur ein Name ist erforderlich); weitere Felder liegen unter „Details für den Entwicklungsauftrag“.

- **Bearbeiten** aktualisiert einen Steckbrief, ohne ein zweites Exemplar anzulegen.
- **Für ChatGPT kopieren** stellt einen gegliederten Entwicklungsauftrag zusammen, der im Chat als GitHub Issue angelegt werden kann. Es findet **kein automatischer GitHub-Upload** aus der PWA statt.
- **Backup exportieren/importieren** sichert deine Ideen als JSON-Datei, falls du das Gerät wechselst oder den Browser-Speicher leerst. Der Import überschreibt bestehende Einträge nur nach Bestätigung.
- **Migration:** Auf der ursprünglichen App-Fabrik-Seite werden früher gespeicherte reine Ideentitel automatisch in Steckbriefe übernommen. Der alte Speicher bleibt als Sicherheitskopie erhalten.
- **Privatsphäre:** Jede PWA nutzt einen eigenen lokalen Schlüssel anhand ihres URL-Pfads. Ideen verlassen das Gerät nur durch bewusstes Kopieren oder Exportieren. Keine Kontoverbindung, API-Schlüssel oder Cloud-Synchronisierung.

**Wichtig:** Browserdaten können durch Löschen des Website-Speichers verloren gehen. Regelmäßig ein Backup exportieren. Für vertrauliche Ideen beim Anlegen des GitHub-Issues ein privates Repository verwenden.

## Schnellstart vom Handy (ohne Dateien zu bearbeiten)

1. In der Vorlage auf **Use this template → Create a new repository** tippen und einen Namen für das neue App-Repository wählen. [Vorlage direkt kopieren](https://github.com/CobraChris/app-fabrik-template/generate).
2. Im neuen Repository unter **Settings → Pages → Build and deployment → Source** einmal **GitHub Actions** auswählen. Das ist pro neuer App notwendig: Die automatische GitHub-Pages-Erstaktivierung mit `GITHUB_TOKEN` allein wird nicht unterstützt.
3. In **Actions** den Workflow **Neue App einrichten (Handy-Assistent)** auswählen und **Run workflow** antippen. App-Name, Kurzname, Beschreibung, Farben und Icon-Buchstaben eintragen.
4. Der Workflow prüft die Eingaben, baut und testet die App, speichert `app.config.json` und veröffentlicht die PWA auf GitHub Pages. **Kein API-Key erforderlich.** In der Workflow-Zusammenfassung steht der Veröffentlichungslink.

**Wichtig:** Den Assistenten nur in einer **Kopie** starten, nicht in `CobraChris/app-fabrik-template`. Die Originalvorlage ist im Workflow vor versehentlicher Umkonfiguration geschützt. Änderungen werden erst nach bewusstem Start des Workflows auf `main` gespeichert. Da `GITHUB_TOKEN`-Commits keine neuen Push-Workflows auslösen, führt dieser Assistent seine Tests und die Veröffentlichung selbst aus. Wenn Pages noch nicht aktiviert ist, kann die Veröffentlichung trotz erfolgreich gespeicherter Konfiguration scheitern; dann Pages aktivieren und den normalen Workflow **PWA veröffentlichen** starten.

## Alternative: Konfiguration direkt bearbeiten

1. Auf GitHub **Use this template → Create a new repository** wählen und ein neues Repository benennen.
2. In **app.config.json** den App-Namen, Kurz-Namen, Beschreibung, die Farben und den Icon-Buchstaben ändern. Nur diese eine Datei ist für das Branding erforderlich.
3. Im neuen Repository **Settings → Pages → Build and deployment → GitHub Actions** einstellen.
4. Mit dem vorhandenen Workflow wird bei Änderungen an main die App aus **app.config.json** gebaut und unter GitHub Pages veröffentlicht. Die beiden bereits eingebauten Qualitätsprüfungen kontrollieren den JavaScript-Code und die Konfiguration.
5. Funktionen nach und nach unter **site/** hinzufügen. Das enthaltene Ideenboard ist ein Beispiel; der Konfigurationswechsel erzeugt allein noch keine individuelle Fach-App.

Beispiel:

```json
{
  "name": "Mein Fitness-Tracker",
  "shortName": "Fitness",
  "description": "Meine Übungen und Trainingseinheiten",
  "themeColor": "#101622",
  "accentColor": "#b1e0ff",
  "iconLetter": "F"
}
```

**Sicherheit:** Farben dürfen ausschließlich sechsstellige Hex-Codes sein, der Icon-Buchstabe A–Z oder 0–9. Texte werden für HTML escaped. Erforderliche Felder werden beim Build geprüft.

## Lokal starten

Node.js >=20 verwenden:

```bash
npm run check
npm test
npm run build
python -m http.server 8000 -d dist
```

Danach http://localhost:8000 öffnen. Nur **dist/** enthält das fertige, konfiguriert gebaute Ergebnis. **site/** bleibt der editierbare Quellcode, **dist/** wird generiert und nicht ins Repository eingecheckt. Auf GitHub veröffentlicht der Workflow ebenfalls ausschließlich **dist/**. Die PWA kann auf HTTPS/localhost offline gecacht werden.

Für GitHub-Pages-Projektpfade erhalten PWAs jetzt jeweils ihren **eigenen Cache-Namensraum**; neue Apps löschen keine Cache-Daten anderer Apps auf derselben github.io-Domain.

## Optional: Codex integriert Änderungen als Vorschlag

Der Workflow **Codex Änderung vorschlagen (manuell)** führt erst nach manueller Auslösung Codex aus und versucht, Änderungen in einem separaten Branch mit Pull Request vorzuschlagen. Dafür wird der separat abrechenbare API-Key als GitHub Actions Repository Secret **OPENAI_API_KEY** benötigt. Der Codex-Workflow ist noch nicht mit einem echten API-Lauf verifiziert.

In **Settings → Actions → General → Workflow permissions** muss ggf. die Erstellung von Pull Requests durch Actions aktiviert werden. Automatisches Mergen ist deaktiviert. Kein Key, keine Laufzeit-Abrechnung.

## Aufgaben direkt aus ChatGPT als GitHub Issues

ChatGPT kann über deinen bereits verbundenen GitHub-Zugang eine strukturierte Entwicklungsaufgabe (**Issue**) für ein freigegebenes App-Repository anlegen. Du musst dafür keinen GitHub-Dialog ausfüllen: Beschreibe im Chat die neue Funktion, bestätige bei Bedarf das Ziel-Repository, und die Aufgabe wird mit Ziel, Umfang, Abnahmekriterien und Einschränkungen in GitHub erfasst.

**Ein neues Issue löst noch keine kostenpflichtige KI-Ausführung aus.** Eine spätere Codex-Ausführung ist bewusst manuell:

1. Öffne im *betroffenen App-Repository* **Actions → Codex Änderung vorschlagen (manuell) → Run workflow**.
2. Trage **entweder** den Auftrag als Freitext im Feld `task` **oder** die Issue-Nummer im Feld `issue_number` ein. Das andere Feld bleibt leer.
3. Nach bewusster Ausführung liest der Workflow das offene Issue über die GitHub-API, lässt Codex nur im lokalen Workspace arbeiten und erstellt einen überprüfbaren Pull Request. Kein Auto-Merge.
4. Prüfe Änderungen, Tests und Sicherheitsauswirkungen vor einer Freigabe.

Dafür ist weiterhin ein separat abgerechneter `OPENAI_API_KEY` im jeweiligen App-Repository notwendig. Codex wurde in dieser Vorlage noch **nicht** mit einem echten API-Lauf getestet. Auch die Erstellung eines neuen Repositories aus dem Template wird nicht durch einen Issue-Workflow übernommen.

**Datenschutz:** `app-fabrik-template` ist öffentlich. Keine persönlichen Ideen, Firmeninformationen oder Zugangsdaten in öffentliche Issues schreiben. Private Ideen gehören in ein separates **privates** App-Repository.

## Dateien

- `app.config.json`: Name, Farben, Kurzbeschreibung und Icon-Buchstabe
- `scripts/build.mjs`: Validierung, HTML/Manifest/Icon/CSS-Branding
- `site/`: App-Quellcode mit Beispiel-Ideenboard und Offline-Service-Worker
- `dist/`: generierter, von GitHub Pages veröffentlichter App-Stand (gitignored)
- `tests/`: Node-Tests für Ideenboard und Konfiguration
- `.github/workflows/ci.yml`: Qualitätsprüfung bei PR und Push
- `.github/workflows/deploy-pages.yml`: Deployment aus dist nach main-Push
- `.github/workflows/codex-proposal.yml`: optionaler, manuell aufgerufener Codex-PR-Vorschlag aus Freitext **oder Issue**
- `scripts/prepare-codex-task.mjs`: prüft Eingaben, lädt ein offenes Issue und erstellt Codex-Prompt-Datei

**Achtung:** Die Vorlage ist für kleine öffentliche statische PWAs gedacht. Keine vertraulichen Firmeninformationen, Nutzerdaten oder API-Schlüssel in site oder app.config.json speichern. Für Authentifizierung, Datenbanken und private Hosting-Umgebungen ist eine andere Architektur erforderlich.
