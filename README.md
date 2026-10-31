# App-Fabrik: Universelle PWA-Vorlage

Ein schlanker Startpunkt für eigene Web-Apps. Keine Frameworks, keine npm-Abhängigkeiten, keine Laufzeit-APIs. Mobile Oberfläche, lokales Ideenboard, offlinefähige PWA, Tests, GitHub Actions und ein optionaler, bewusst manueller Codex-Workflow.

## Schnell testen

- Node.js ab Version 20 bereitstellen.
- Im Projektordner npm run check und npm test ausführen.
- Zum lokalen Öffnen einen statischen HTTP-Server starten: python -m http.server 8000 -d site und http://localhost:8000 besuchen. Die PWA-Funktionen benötigen localhost oder HTTPS.
- App-Ideen liegen im Browser unter localStorage, nicht in GitHub.

## Einmalige GitHub-Einrichtung

1. Dieses Repository unter **Settings → General → Template repository** als Vorlage markieren. Danach kann für jede neue App über **Use this template → Create a new repository** eine Kopie erstellt werden.
2. Für die PWA-Veröffentlichung **Settings → Pages → Build and deployment → Source: GitHub Actions** auswählen. Danach den Workflow „PWA veröffentlichen“ starten. Nur der Ordner site/ wird veröffentlicht.
3. Optional für Codex: Einen **separat abrechenbaren** OpenAI API-Key als **Settings → Secrets and variables → Actions → Repository secrets → New repository secret** mit dem Namen OPENAI_API_KEY hinterlegen. Nie im Chat oder Code posten.
4. Für automatisierte Pull Requests gegebenenfalls **Settings → Actions → General → Workflow permissions → Allow GitHub Actions to create and approve pull requests** aktivieren.
5. Unter **Actions → Codex Änderung vorschlagen (manuell) → Run workflow** eine konkrete Aufgabe eingeben. Die Automation soll einen Branch und einen PR öffnen. Erst nach Codeprüfung mergen.

## Warum kein Autopilot ohne Freigabe?

Codex erhält im ersten Job Repository-Leserechte. Erst ein zweiter Job darf einen Pull Request erstellen. Keine automatischen Merges. Der Codex-Workflow ist standardmäßig inaktiv, bis er manuell gestartet und der separate API-Key eingerichtet ist.

## Projektstruktur

- site/: Veröffentlichte PWA (index.html, styles.css, app.js, core.mjs, sw.js, Manifest und Symbol)
- tests/: Automatische Node-Tests
- .github/workflows/ci.yml: Tests bei Pull Request und Push
- .github/workflows/deploy-pages.yml: Veröffentlichung über GitHub Pages
- .github/workflows/codex-proposal.yml: Manuelle Codex-Vorschläge als Pull Request
- AGENTS.md: Regeln für Entwicklungsagenten
- SECURITY.md: Schutzmaßnahmen

## Erste eigene App starten

Aus der Vorlage ein neues Repository erstellen und Name, Design und Funktionen anpassen. Keine GitHub-Tokens in Frontend-Dateien ablegen. Für Anwendungen mit Server, Login oder sensiblen Daten ist eine angepasste Architektur erforderlich. Die Vorlage ist für kleine statische PWAs gedacht.

## Nächste Ausbaustufen

- Gemeinsame App-Konfiguration für Namen, Farben und Icon.
- Standardisierter Feature-Backlog aus GitHub Issues.
- Kontrollierte Generierung von PRs aus Issues statt manueller Prompt-Eingabe.
- Optional zentraler CI-Workflow für mehrere Repositories.
