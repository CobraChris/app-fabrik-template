# Phase A: verbindliche Sicherheitsbasis der App-Fabrik

Stand: 07.11.2026. Die technische GitHub-Verwaltung bleibt in
[CobraChris/github-app-fabrik-control](https://github.com/CobraChris/github-app-fabrik-control).
Die tatsächlichen Entscheidungen und noch offenen Administrator-Freigaben
stehen dort in PROJECT_STATUS.md. Dieses Dokument ist eine
**Qualitäts- und Sicherheitscheckliste**, kein Nachweis der Freischaltung.

## Verbindliche automatische Basiskontrolle

\`npm run security\` startet den lokalen Scanner
\`scripts/security-check.mjs\` ohne externe Pakete, bezahlte Dienste oder
Cloud-Abfragen. Er prüft die von Git getrackten Dateien der PWA-Vorlage
und wird auch über \`npm test\` in der bestehenden GitHub CI gestartet.

**Blockierende Fehler**
- Versehentlich eingecheckte .env-Dateien, private PEM-/PKCS12-/JKS-Dateien,
  Signierschlüssel und ähnliche geheime Dateien (nur zulässige .env-Beispiele
  sind ausgenommen).
- Erkennbare GitHub-, AWS-, OpenAI- und Slack-Tokens sowie PEM-Private-Key-Blöcke
  in Textdateien.
- GitHub Actions mit \`pull_request_target\`, \`workflow_run\`,
  \`permissions: write-all\` oder direktem curl/wget-Pipe-in-Shell.
- Git-getrackte symbolische Links, die unerwartet Inhalte aus anderen
  Verzeichnissen einschleusen könnten.

**Ab dieser Freigabe ebenfalls blockierend**
- Externe GitHub Actions ohne vollständige unveränderliche
  40-stellige Commit-SHA-Referenz. Die initiale Umstellung mit
  geprüfter Tag-/Commit-Auflösung steht in
  [ACTIONS_PINNING.md](ACTIONS_PINNING.md).

Der Scanner meldet **nur Dateinamen und Fehlercodes**, niemals gefundene
Token-Werte. Syntaktische Muster erkennen nicht alle Formen von Geheimnissen:
der Scanner ist eine Mindestanforderung, kein vollständiges Secret Scanning,
kein SAST und kein Penetrationstest.

## Freigaberegeln je Anwendung

| Kategorie | Mindestfreigabe | Zusätzlicher Nachweis |
| --- | --- | --- |
| Öffentliche PWA (ohne Geheimdaten) | Build, Tests, Security Audit und Browser-Smoke | funktionierender Link, keine kaputten Alt-Funktionen |
| Android-Wecker / Watch | Build, Lint, Unit-Tests, Security Audit | manueller echter Handy-/Uhr-Test, Alarm bei gesperrtem Bildschirm, Stummmodus und Standby |
| Fahrassistenz | Build, Regression, Security Audit | sichere Ablenkungs-/Berechtigungsprüfung und Tests unter realen Fahrbedingungen **ohne Bedienung während der Fahrt** |
| L4L-/Firmenprojekt | getrennte autorisierte Firmenumgebung, synthetische Daten | schriftlich erlaubte Schnittstellen, echte Daten nur im genehmigten Unternehmenssystem |

## Release-Gate

1. Pull Request statt Direktänderung an geschütztem Produktionsbranch.
2. Test-, Build- und Sicherheitscheck sind erfolgreich; die betroffenen
   Dateien/Abhängigkeiten wurden überprüft.
3. Kein unbestätigter Administratorzugriff, keine Veröffentlichung von
   privaten Daten; zustimmungspflichtige Controller-/Signing-/Workflow-
   Änderungen immer mit menschlicher Freigabe.
4. Release besitzt eine nachprüfbare Versionsnummer, Build-Quelle,
   passendes Signaturzertifikat (Android) und Wiederherstellungsweg.
5. Bei fehlgeschlagenem Gate wird **nicht** veröffentlicht. Dokumentiere
   den Fehler und die wirksame Korrektur vor dem nächsten Versuch.

## Noch offen für eine professionelle Produktionsumgebung

- GitHub App Berechtigungen und Rulesets müssen tatsächlich eingerichtet,
  verifiziert und getestet werden; Code allein beweist dies nicht.
- Nach dem einmaligen Pinning **regelmäßige, separat geprüfte Aktualisierung**
  auf neue Sicherheitsfixes der Actions; für fremde npm-/Gradle-Abhängigkeiten
  zusätzliche Dependency-/SCA-Prüfung, sobald solche Abhängigkeiten eingesetzt werden.
- Belastbare Recovery-Tests für signierte Android-Releases und
  private Backups. Nicht nur Backup-Dateien prüfen.
- Getrennte Entwicklungs-/Administrator-Vertrauensgrenzen bei externen
  Nutzern oder bezahlten Apps; für die private App-Fabrik ist die
  bestehende gemeinsame GitHub App eine bewusste Komfort-/Risikowahl.
- Erfassung von Datenschutz, Support, Vorfällen und gesetzlichen Pflichten,
  bevor Apps an fremde Nutzer verteilt werden.
