# GitHub-Admin-Baustein – noch kein aktiver MCP-Server

Serverseitiger Vorbereitungsstand fuer eine spaetere eigene GitHub-App-Integration.

- github-app-auth.mjs: GitHub-App-RS256-JWT und kurzlebige GitHub Installationstokens, auf Secrets:write und erlaubte Repositories begrenzt.
- github-secrets.mjs: nur erlaubte Repository-/Secret-/Vault-Referenzen, serverseitige Besitzerfreigabe, Public-Key-Abfrage und verschluesselter PUT. Gibt keinen Klartext zurueck.
- seal.mjs: libsodium crypto_box_seal; nur fuer einen geschuetzten Backend-Prozess. Lokale Backend-Abhaengigkeit: libsodium-wrappers (frei verfuegbar).
- ../../tests/github-admin.test.mjs: Tests ohne echte Zugangsdaten.

**Das Modul ist keine laufende Server- oder ChatGPT-Verbindung.** Vor Betrieb fehlen eigener gesicherter Server/Vault, Authentifizierung, tatsaechlicher MCP-Endpunkt, GitHub-App-Installation und ein unterstuetztes ChatGPT-Schreibrecht.

Die Funktion authorizeWrite() muss eine echte vom Host gepruefte Besitzerfreigabe liefern. Der LLM-Text selbst ist kein vertrauenswuerdiges Freigabesignal. Schluessel und Tokens nicht in diesen oeffentlichen Code, eine PWA oder Chat-Texte aufnehmen.

Siehe ../../docs/GITHUB_APP_FACTORY.md.
