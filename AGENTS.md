# Regeln für KI-Entwicklungsagenten

Dieses Repository enthält eine einfache, lokal nutzbare PWA. Halte Änderungen klein, nachvollziehbar und testbar.

1. Vor Änderungen relevante Dateien und vorhandene Tests lesen.
2. Keine Zugangsdaten, Tokens, Nutzerinformationen oder fremde API-Schlüssel in Code oder Logs schreiben.
3. Keine externen Dienste, Tracker, Abhängigkeiten, Bezahl-APIs oder Benutzerkonten ohne ausdrücklichen Auftrag ergänzen.
4. Sicherheits-Workflows, Berechtigungen und Deployment-Einstellungen nicht eigenständig abschwächen.
5. Keine Änderungen an .github/workflows/ ohne gesonderte Prüfung durch den Eigentümer.
6. npm test und npm run check ausführen und das Ergebnis beschreiben.
7. Produktive Änderungen nur als Pull Request vorschlagen; niemals selbstständig mergen.
8. Für neue Apps: Namen, Zweck und UI-Texte anpassen; generische Tests und CI erhalten.
9. Keine echten L4L-/SharePoint-Projektdaten oder vertrauliche Firmendaten einfügen.

## Projektaufbau

- site/: Statische PWA, kann ohne Build veröffentlicht werden.
- tests/: Automatische Node-Tests.
- .github/workflows/ci.yml: Validierung nach Push/PR.
- .github/workflows/deploy-pages.yml: GitHub-Pages-Veröffentlichung auf main.
- .github/workflows/codex-proposal.yml: Optionaler manueller Codex-Lauf, PR statt Auto-Merge.

## Akzeptanzkriterien

- App funktioniert ohne Laufzeit-API-Key.
- Daten bleiben lokal, sofern nicht ausdrücklich anders beschlossen.
- Vorhandene Tests bestehen; neue Features erhalten sinnvolle Tests.
- Mobile Darstellung, Tastaturbedienung und sinnvolle Fehlermeldungen funktionieren.
