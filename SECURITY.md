# Sicherheit

- Keine persönlichen oder geschäftlichen Daten in öffentlichen Repositories hinterlegen.
- API-Schlüssel ausschließlich als GitHub Actions Secrets speichern; niemals in Quelldateien.
- Die PWA nutzt für Ideen ausschließlich lokalen Browser-Speicher.
- codex-proposal.yml läuft nur auf manuelle Anforderung. Der Codex-Schritt erhält nur Lesezugriff auf GitHub; der Schreibschritt läuft in einem getrennten Job und öffnet einen Pull Request ohne Auto-Merge.
- KI-generierten Code vor dem Merge prüfen, insbesondere JavaScript, Netzwerkaufrufe und Datenzugriffe.
- Die GitHub-Einstellung „Allow GitHub Actions to create and approve pull requests“ wird für automatische PR-Erstellung benötigt und sollte bewusst freigegeben werden.
- Vor echten Projekten Branch Protection oder Rulesets mit erfolgreichen Checks und bei Bedarf Review-Regeln aktivieren.
- GitHub Pages ist kein vertrauliches Hosting; auch die Website eines privaten Projekts kann öffentlich sein.
- GitHub Actions Workflows bei produktivem Einsatz auf unveränderliche Commit-SHAs pinnen.

## Issue-basierte Entwicklungsaufträge

- Issue-Titel und -Text werden als **unvertrauenswürdige Anforderungen** behandelt und über die GitHub-API ohne Shell-Interpolation geladen.
- Ein offenes Issue führt keine Codex-Ausführung selbst aus. Die Aktion ist weiterhin nur über `workflow_dispatch` mit bewusstem Start erreichbar.
- Im lesenden Codex-Job nur `issues: read` und `contents: read`; der Pull-Request-Job läuft getrennt mit Schreibrechten.
- Kein automatischer Merge und kein versehentlicher API-Verbrauch beim Anlegen neuer Issues.
- Öffentliche Issue-Texte sind öffentlich. Keine vertraulichen Projektinformationen in der öffentlichen Vorlage speichern.

## Zentrale Android-Workflows und GitHub-App-Verwaltung

- Android-Release nur nach manuellem Start auf dem geprueften Default-Branch; separate Build- und Signier-Jobs.
- Signier-Secrets niemals in PR-/Debug-Builds; private APK-Signatur pro App getrennt, nicht im Quellcode.
- GitHub-App-Administrationsbibliothek ist **noch nicht deployed**; notwendige Schreibrechte und ein sicherer Secret-Vault fehlen.
- Nur bewusst freigegebene Repositories/Secrets, serverseitige Besitzerfreigabe, keine Klartextausgabe.
- Alle Aenderungen an diesen Workflows benoetigen menschliche Sicherheitspruefung, keinen automatischen Merge. Details in docs/GITHUB_APP_FACTORY.md.

## Automatische Security-Baseline (Phase A)

Bei jedem `npm test` wird zusätzlich `npm run security` ausgeführt. Der
Scanner prüft getrackte Dateien auf hochwahrscheinliche Secrets,
versehentlich eingecheckte private Schlüssel und besonders riskante
GitHub-Actions-Auslöser. Veränderliche GitHub-Actions-Tags werden zunächst
als Hinweis gemeldet (nicht blockiert). Details und Release-Gates:
[docs/SECURITY_BASELINE.md](docs/SECURITY_BASELINE.md).

Diese Prüfung ist **keine** vollständige Code-/Datenschutzprüfung und ersetzt
weder GitHub-App-Rechtekontrolle noch sicherheitskritisches Workflow-Review.
