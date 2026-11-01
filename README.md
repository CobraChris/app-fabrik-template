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

## Qualitätssicherung: PWA-Smoke-Tests

Zusätzlich zu den bisherigen Unit-Tests prüft `tests/ui-smoke.test.mjs` nach dem Build ohne neue Abhängigkeiten die wichtigsten Verknüpfungen: JavaScript-Selektoren zu vorhandenen HTML-Elementen, Schnellnavigation, JavaScript-Module, Offline-Cache-Dateien und PWA-Manifest inklusive Icons. Diese Tests laufen mit `npm test` und automatisch in der PR-Qualitätsprüfung.

**Grenze:** Die Smoke-Tests sind statische Integritätsprüfungen und ersetzen keinen echten Android-Browsertest. Für den praktischen Check die veröffentlichte PWA öffnen, einen öffentlichen Repository-Import starten, ein Projekt übernehmen, ein To-do abhaken und nach dem Neuladen den lokalen Speicher kontrollieren.

## Startbereich auf dem Handy: Nächste sinnvolle Aktion statt neuer Checklisten

Ganz oben zeigt die App-Fabrik einen **dynamischen Startbereich** mit einem konkreten nächsten Schritt und einer Schnellnavigation zu *Start*, *Projekte* und *App-Ideen*.

Der Startbereich bewertet **nur vorhandene lokale Daten**. Er benötigt keine zusätzliche Checkliste oder Häkchen:

- **Keine Projekte und Ideen:** Öffentliche GitHub-Repositories importieren oder die erste App-Idee erfassen.
- **App-Ideen, aber noch keine Projekte:** App-Ideen ansehen und die GitHub-Abkürzungen nutzen.
- **Mindestens eine offene Aufgabe bzw. ein nächster Schritt:** Zur projektübergreifenden Fokusliste springen.
- **Aktive Projekte ohne offene Aufgaben:** Ein bestehendes Projekt ansehen und den nächsten Entwicklungsschritt hinterlegen.
- **Alle Projekte fertig:** Neue App-Idee erfassen.

Der direkte Link **Neue App aus Vorlage** öffnet die GitHub-Vorlage und erstellt nichts automatisch. Der Status wird direkt aus den lokal vorhandenen Projekten und Ideen berechnet; es erfolgt keine GitHub-Abfrage und keine Kostenpflicht. Gespeicherte Projektdaten, Checklisten und beide Backup-Formate bleiben unverändert.

## Öffentliche GitHub-Repositories gesammelt importieren

Die Projektübersicht hat die Funktion **Öffentliche GitHub-Repositories gesammelt übernehmen**. Sie erleichtert die Ersteinrichtung auf dem Android-Handy:

1. GitHub-Benutzernamen eintragen (auf deiner github.io-Seite vorausgewählt) und **Repositories suchen** klicken.
2. Die App ruft **erst nach diesem Klick** die maximal 100 zuletzt aktualisierten öffentlichen, nicht archivierten Repositories über GitHubs öffentliche REST-API ab. Ohne GitHub-Login, Token oder zusätzliche KI-API-Kosten.
3. Gewünschte Repositories einzeln abhaken und **Ausgewählte Projekte hinzufügen** wählen.
4. Bereits vorhandene Projekte und ihre Notizen, Checklisten und Status bleiben unverändert. Neue Projekte beginnen mit dem Status **Offen**, ohne künstlich erfundene Aufgaben.
5. Bei privaten Repositories oder GitHub-Abfragelimits weiterhin **+ GitHub-Projekt hinzufügen** nutzen.

Die Importfunktion wird **nicht automatisch beim Start** aufgerufen. Es findet keine Hintergrund-Synchronisierung statt. Der Browser sendet bei der bewussten Suche den gewählten GitHub-Benutzernamen an GitHub und kann bei fehlender Verbindung oder API-Ratenlimit einen Fehler anzeigen. Alle übernommenen Projekte bleiben ausschließlich lokal; vor einem Gerätewechsel Projekt-Backup exportieren.

## Projektübergreifende Fokusliste: „Meine nächsten Aufgaben“

Die Projektübersicht zeigt jetzt ganz oben bis zu acht nächste Arbeitsschritte aus unterschiedlichen Projekten. Dabei gelten folgende Regeln:

- Projekte mit Status **In Arbeit** stehen vor **Offen**, danach **Wartet**. Fertige Projekte werden übersprungen.
- Pro Projekt erscheint die **erste offene Checklisten-Aufgabe**. Hat ein Projekt noch keine Checkliste, verwenden wir als Fallback das bisherige Freitextfeld **Nächster Schritt**.
- Bei bereits vollständig erledigter Checkliste wird der alte Freitext nicht erneut als offene Aufgabe interpretiert. Das verhindert doppelte/stale Meldungen.
- **Erledigt** setzt nur bei echten Checklisten-Einträgen ein lokales Häkchen; die nächste offene Aufgabe dieses Projekts rückt nach. Für Freitext gibt es stattdessen **Bearbeiten**.
- **GitHub** öffnet das passende Repository, ohne dessen Status zu ändern.
- Alle angezeigten Daten kommen ausschließlich aus deinen eigenen lokalen Projekteinträgen; die App behauptet keinen aktuellen GitHub-Status. Weder zusätzliche API-Zugriffe noch Kosten.

## Projekt-To-dos: Mehrere offene Arbeitsschritte im Blick

Für jedes GitHub-Projekt gibt es jetzt eine **lokale To-do-Checkliste** unter dem bisherigen Feld **Nächster Schritt**. Trage mehrere konkrete Aufgaben ein, hake erledigte Punkte ab oder öffne sie erneut. Offene Einträge stehen vor erledigten Aufgaben. Die Projektübersicht zeigt zusätzlich die Anzahl der offenen To-dos.

- Bereits gespeicherte Projekte und der bisherige **Nächster Schritt** bleiben unverändert erhalten.
- Bis zu 50 Aufgaben pro Projekt. Aufgaben und Status werden nur **auf diesem Gerät** gespeichert. Sie ändern keine GitHub-Issues, Pull Requests oder Commits.
- Projekt-Backups enthalten jetzt die Checklisten (\`schemaVersion: 2\`). Ältere Projekt-Backups im Format \`schemaVersion: 1\` bleiben importierbar und erhalten eine leere Checkliste.
- **Status** des Projekts und **erledigt** einer Aufgabe sind bewusst unabhängig: Das Erledigen aller Aufgaben markiert das GitHub-Projekt nicht automatisch als fertig.
- Bei Bedarf weiterhin **Projekt-Backup exportieren**, da Browserdaten verloren gehen können.

## Meine Apps: Projektübersicht und nächste Schritte

Die App-Fabrik enthält eine **lokale Projektübersicht** oberhalb des Ideenboards. Für jedes GitHub-Repository kannst du den **nächsten konkreten Schritt**, kurze Notizen und den manuellen Status (*Offen*, *In Arbeit*, *Wartet*, *Fertig*) notieren. Offene Projekte erscheinen zuerst. Direktlinks führen zu Repository, Pull Requests, Issues und GitHub Actions.

- Zum Hinzufügen **+ GitHub-Projekt hinzufügen** aufklappen und den Pfad \`Benutzer/Repository\` eintragen.
- Über **Bearbeiten** kannst du Status und nächsten Schritt aktualisieren. Ein Klick auf **Entfernen** löscht ausschließlich den lokalen Eintrag, niemals das GitHub-Repository.
- **Projekt-Backup exportieren/importieren** speichert alle Projektangaben als separate JSON-Datei. Das bestehende **Ideen-Backup** bleibt unverändert.
- Der Projektstatus wird **nicht live aus GitHub gelesen**. Besonders private Repositories benötigen GitHub-Authentifizierung; wir speichern keine Tokens in der öffentlichen PWA.
- Die Übersicht ist bewusst **nicht mit einer Cloud verbunden**. Notizen werden je PWA-Pfad lokal gespeichert. Bei Gerätewechsel bitte beide Backups exportieren.

Du kannst deine bereits existierenden Apps, z. B. \`CobraChris/meine-livekarte\`, \`CobraChris/l4l-project-plan-inspector\` und die Vorlage, im Formular eintragen. Diese Namen werden nicht automatisch in die öffentliche Vorlage übernommen.

## Neue App und GitHub-Issue aus einer Idee starten

Jede Idee hat nun im Bereich **Mit GitHub weiterarbeiten** zwei Abkürzungen. **Neue App aus Vorlage** öffnet `https://github.com/new` mit vorausgefülltem Namen, Beschreibung, **der ausdrücklich gewählten Sichtbarkeit** und dem GitHub-Template `CobraChris/app-fabrik-template`. Die neue App wird erst nach eigener Bestätigung auf GitHub erstellt. Danach muss GitHub Pages im neuen Repository noch aktiviert werden.

**Wichtig für den kostenlosen Betrieb:** GitHub Pages funktioniert mit **GitHub Free nur bei öffentlichen Repositories**. Wer **Privat** auswählt, kann den Code privat halten, aber GitHub Pages nicht im Free-Tarif nutzen. Die App fragt daher vor der ersten Repository-Erstellung ausdrücklich nach **Öffentlich** oder **Privat**, merkt sich die Wahl nur lokal und warnt vor dem Öffnen von GitHub nochmals über die Folgen. Vertrauliche Firmen- oder Zugangsdaten gehören niemals in ein öffentliches Repository. Offizielle Dokumentation: https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site.

**GitHub-Issue öffnen** erzeugt einen vorausgefüllten GitHub-Issue-Link für das bestehende Ziel-Repository mit Ziel, Funktionen und Abnahmekriterien. Das Issue wird erst durch den eigenen Klick auf **Submit new issue** angelegt. GitHub-Konto und Ziel-Repository werden unter **GitHub-Verknüpfung** ausschließlich lokal im Browser gespeichert. Weder GitHub-Token noch API-Schlüssel werden benötigt.

**Wichtig für Android:** Die GitHub-Handy-App übernimmt die vorausgefüllten Felder nicht immer. Deshalb erscheint zusätzlich ein lokaler kopierbarer Text und ein erneuter GitHub-Link. Sehr lange Issues öffnen aus Sicherheitsgründen nur die leere Issue-Maske; den Text muss man dann hineinkopieren. Beim Öffnen eines vorausgefüllten Links werden die enthaltenen Daten als URL-Parameter an GitHub übertragen. Kein vertrauliches Material in öffentlichen Issues veröffentlichen.

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
