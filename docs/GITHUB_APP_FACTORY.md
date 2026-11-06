# Zentrale GitHub-App-Fabrik – Architektur (06.11.2026)

## Ziel

Eine dauerhafte technische Grundlage fuer mehrere Android-Apps und PWAs, statt immer neuer Installationsanleitungen.

1. ChatGPT entwickelt im eigenen Branch, erstellt PR und prueft CI.
2. Ein *wiederverwendbarer Android-Check* kompiliert, testet und prueft Lint ohne Signier-Secrets.
3. Ein gesonderter, nur manuell auf dem geprueften Default-Branch startbarer Release-Workflow signiert die APK und veroeffentlicht sie als GitHub Release. Private Schluessel sind nur dem Signier-Job zugaenglich.
4. Neue App-Versionen koennen ohne Deinstallation aktualisiert werden, **wenn** Android packageId, Signatur und steigenden versionCode akzeptiert; Android verlangt weiterhin eine Bestaetigung.
5. Spaeter kann ein zusaetzlicher GitHub-App-Admin-Connector die Secrets aus einem geschuetzten Vault nach ausdruecklicher Besitzerfreigabe eintragen.

## Bereits implementierte Komponenten

- .github/workflows/reusable-android-check.yml: project_path, module, read-only, Unit-Tests, Android Lint, Debug-APK.
- .github/workflows/reusable-android-release.yml: project_path, module, release_prefix, package_id; vier explizit uebergebene Signier-Secrets; zwei getrennte Jobs; nur workflow_dispatch auf Default-Branch; Verifizierung der Signatur mit apksigner, SHA256SUMS und eindeutige Release-Tags.
- templates/android-app/.github/workflows/: CI- und Release-Aufrufer als Vorlage fuer neue Apps. Vor produktivem Betrieb die Referenz @main durch einen **geprueften, vollstaendigen Commit-SHA** ersetzen.
- integrations/github-admin/: Node.js-Bausteine fuer GitHub-App-JWTs, kurzlebige Installationstokens und Actions-Secrets per GitHub Public-Key/Libsodium-Sealed-Box.
- tests/github-admin.test.mjs und tests/android-factory-workflows.test.mjs: automatische Regressionstests.

Ein signierter Release-Lauf wurde **noch nicht** ausgefuehrt. Der PR ist Draft. Er darf nicht ohne menschliche Sicherheitspruefung gemerged werden.

## Sicherheit

- Eine GitHub App nur in explizit freigegebenen Repositories installieren; fuer den Admin-Baustein nur **Secrets: write**.
- Nie ein Secret im Chat, in GitHub-Issues, Quellcode oder einer oeffentlichen PWA speichern. Die GitHub-API kann Secret-Klartexte **nicht auslesen**.
- Die Backend-Bibliothek akzeptiert nur Allowlists aus Repository, Secret-Name und Vault-Referenz. *Ein vom Assistenten uebermitteltes boolean ownerApproved ist keine echte Freigabe*: die Freigabe muss der sichere Backend-Host selbst ermitteln.
- Private GitHub-App-Keys und Signier-Schluessel in einem geschuetzten Server/Vault, mit Schluessel-Backup und Rotation.
- Separate Signatur je Android-App statt eines gemeinsamen globalen Schluessels.
- **Kein Zugriff auf Signier-Secrets** in PR-Workflows, kein pull_request_target oder workflow_run. Signieren nur aus vertrauenswuerdigem Default-Branch. Das Deployment-Environment android-releases vor Produktiveinsatz mit Freigaberegeln konfigurieren.
- Workflow-/Security-Aenderungen nur als Draft-PR, kein automatischer Merge; Main-Branch-Schutz empfohlen.
- Kein kostenpflichtiger Dienst eingebaut. GitHub Actions oder ein eigener Server koennen je nach Konto und Nutzung kostenpflichtige Kontingente erfordern.

## Noch offen – NICHT als fertig ansehen

1. Menschliche Sicherheitspruefung der Workflows, danach kontrollierte Aufnahme auf main.
2. Pro App geschuetzte Signatur-Secrets und Environment-Freigaben einrichten.
3. Geschuetzten Vault/Backend bereitstellen; GitHub App registrieren und auf die erlaubten Repos begrenzt installieren.
4. Ein **tatsaechlicher MCP-Server-Endpunkt mit Authentifizierung** und ChatGPT-Schreibzugriff fehlen. Der hier implementierte GitHub-Admin-Baustein ist **keine bereits verbundene ChatGPT-App**.
5. Die aktuelle ChatGPT-Produktdokumentation bietet Custom-MCP-Schreibaktionen nur fuer Business/Enterprise/Edu-Workspaces, nicht fuer Plus. Ein freigeschalteter Zugang bzw. eine weitere offizielle Schreibschnittstelle ist erforderlich; mobile Einrichtung ist derzeit nicht verfuegbar.
6. Zwei aufeinanderfolgende signierte Android-Releases auf einem echten Samsung-Handy installieren, vorhandene Daten und Watch-Berechtigungen pruefen. Erst dann die bestehende Debug-Installation abloesen.

**Quellen:** https://docs.github.com/en/rest/actions/secrets ,
https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows ,
https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt
