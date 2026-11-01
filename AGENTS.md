# Regeln für KI-Entwicklungsagenten

Dieses Repository ist eine wiederverwendbare PWA-Vorlage. Änderungen klein und testbar halten.

1. Relevante Dateien und Tests vor Änderungen lesen.
2. Keine Secrets, Zugangsdaten, persönlichen Daten oder geschäftlichen Informationen im Code, GitHub-Pages-Output oder in Logs speichern.
3. Keine neuen externen Dienste, Tracker, Abhängigkeiten oder kostenpflichtigen APIs ohne ausdrücklichen Auftrag.
4. Keine GitHub-Workflows oder Berechtigungen ohne gesonderte menschliche Prüfung ändern.
5. Namensgebung, Farben, Kurzbeschreibung und Icon-Buchstabe ausschließlich über **app.config.json** konfigurieren. Darin keine Secrets speichern.
6. Der Handy-Assistent in `.github/workflows/setup-app.yml` darf nur in Kopien gestartet werden. Er prüft vor dem Commit und veröffentlicht danach in einem getrennten Job.
7. **site/** ist Quelle. **dist/** wird mit `npm run build` erstellt und nie manuell bearbeitet oder eingecheckt.
8. `npm run check` und `npm test` ausführen. Änderungen sollten passende Tests haben.
9. PWA-Caches müssen nach App-Scope getrennt bleiben: keine Cache-Löschung anderer PWAs derselben Domain.
10. Änderungen als Pull Request vorschlagen und nicht selbstständig mergen.
11. Keine L4L-/SharePoint-Echtdaten oder andere Unternehmensdaten einfügen.

## Projektaufbau

- app.config.json: Branding
- scripts/build.mjs: Auslieferungsartefakte erzeugen
- site/: Statische App-Quelldateien
- dist/: generierte Website (gitignored)
- tests/: Node.js-Tests
- .github/workflows/: Tests, GitHub Pages, optional Codex (Änderungen menschlich prüfen)

## Akzeptanzkriterien

- PWA funktioniert ohne Laufzeit-API-Key.
- Daten bleiben lokal, wenn nicht anders vereinbart.
- Mobile Ansicht, Tastaturbedienung, sinnvolle Fehlermeldungen.
- CI erfolgreich; keine unerwarteten Netzwerkaufrufe.
