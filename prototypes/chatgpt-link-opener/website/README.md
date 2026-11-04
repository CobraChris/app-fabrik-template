# Root-Site für Chat-Link-Öffner

**Noch nicht online.** Diese Dateien sind nur die Vorlage für ein separates öffentliches GitHub-Repository `CobraChris/CobraChris.github.io`.

Kopiere **den Inhalt dieses Verzeichnisses in die Wurzel** des User-Pages-Repositories:
- `.nojekyll` (verhindert das Ausblenden von `.well-known`).
- `.well-known/assetlinks.json` (SHA-256-Fingerprint passt ausschließlich zur APK des GitHub-Actions-Laufs 41818021963).
- `open/index.html` (Fallback, falls Android den Link nicht an unsere App weitergibt).

Die Zieladresse für Android ist `https://cobrachris.github.io/.well-known/assetlinks.json`.
Die testbare App-Link-Adresse lautet:
`https://cobrachris.github.io/open/?url=https%3A%2F%2Fwww.startpage.com%2F`

**Unbedingt beachten:** Die Datei muss am Root-Host liegen, darf keine Weiterleitung verursachen und muss den tatsächlichen Fingerprint der installierten APK nennen. Andere, später neu gebaute Debug-APKs können anders signiert sein. Niemals Debug-Keystore oder private Signaturschlüssel auf GitHub veröffentlichen.

Das Erstellen des Repositorys und die GitHub-Pages-Veröffentlichung sind noch offen; die GitHub-Verbindung kann hier vorhandene Repositories ändern, aber kein neues erzeugen.

Ob die offizielle ChatGPT-Android-App eigene Verified App Links extern öffnet, ist weiterhin unbewiesen und muss auf dem Samsung getestet werden.
