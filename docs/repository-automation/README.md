# Neue Repositories über ChatGPT → App-Fabrik → GitHub Actions

## Status: Nicht aktiviert

Der Vorschlag liegt unter docs/repository-automation/create-repository.workflow.proposed.yml
und ist bewusst KEIN aktiver GitHub-Workflow. Änderungen an .github/workflows,
Berechtigungen und Secrets erfordern eine separate menschliche Prüfung und Freigabe
gemäß AGENTS.md. Der Scriptcode unter scripts/create-repository.mjs und dessen
Tests können bereits im bestehenden Pull Request geprüft werden.

## Warum dieser Weg?

Die vorhandene GitHub-Verbindung kann Dateien in vorhandenen Repositories anlegen,
aber weder neue GitHub-Repositories erzeugen noch einen workflow_dispatch auslösen.
Nach einmaliger Einrichtung kann ChatGPT nach ausdrücklicher Nutzerfreigabe
eine neue JSON-Datei auf dem Hauptzweig unter repo-requests/ anlegen.
Dieser Push startet den zuvor geprüften Workflow und erzeugt genau ein Repository.
Es wird kein Token im Chat oder Frontend gespeichert.

## Einmalige Einrichtung (noch nicht durchgeführt)

1. In GitHub einen Fine-grained Personal Access Token für Resource owner CobraChris
   mit Repository creation: Read and write erstellen. Keine unnötigen Rechte,
   ein Ablaufdatum wählen.
2. Diesen Token in den GitHub-Actions-Repository-Secrets von
   CobraChris/app-fabrik-template ausschließlich unter REPO_CREATOR_TOKEN speichern.
   Niemals im Chat, Screenshot, Issue, Quellcode oder Log hinterlegen.
3. Workflow-Vorschlag und Berechtigungen gesondert durch den Nutzer prüfen lassen.
   Erst nach ausdrücklicher Freigabe nach
   .github/workflows/create-repository.yml übernehmen.
4. Eine konkrete Repository-Erstellung muss anschließend ausdrücklich im Chat
   mit Besitzer, Name und öffentlicher/privater Sichtbarkeit beauftragt werden.

## Beispiel für den Chat-Link-Öffner

ChatGPT erstellt dann z. B. auf main diese neue Datei:
repo-requests/2026-11-05-chat-link-pages.json

JSON-Inhalt (Sichtbarkeit wurde ausdrücklich als public bestätigt):
{
  "schemaVersion": 1,
  "owner": "CobraChris",
  "name": "CobraChris.github.io",
  "visibility": "public",
  "description": "Android App Links for Chat-Link-Oeffner",
  "confirmation": "CREATE_PUBLIC"
}

Für private Repositories sind die Werte private und CREATE_PRIVATE notwendig.
Es gibt keinen impliziten Default für die Sichtbarkeit.

## Sicherheitsregeln

- Workflow reagiert NUR auf neue JSON-Dateien im Hauptzweig, niemals auf
  Pull Requests, Issues oder Seitenaufrufe.
- Pro Push maximal eine neu angelegte Auftragsdatei. Modifizierte oder
  gelöschte Auftragsdateien lösen keine erneute Repo-Erstellung aus.
- Validierung der exakten erlaubten JSON-Felder, des Eigentümers CobraChris,
  des Repository-Namens und einer passenden Sichtbarkeitsbestätigung.
- Der API-Aufruf verwendet ausschließlich POST /user/repos, kein Löschen,
  keine Repository-Verwaltung und keine kostenpflichtigen Dienste.
- Das Auftragsfile ist im öffentlichen Repository sichtbar: niemals
  vertrauliche Projektnamen, Geschäftsinfos, Tokens oder Passwörter eintragen.
- Die GitHub-App-Verbindung von ChatGPT hat nicht zwangsläufig Zugriff auf
  neu erzeugte Repositories, wenn die Installation nur ausgewählte Repos
  umfasst. Diesen Zugriff ggf. einmalig in GitHub ergänzen.
- Ein Fine-grained-Token ist ein echtes Sicherheitscredential. Seine
  Ablauffrist, sein Aussteller und die GitHub-Kontoeinstellungen gelten weiter.

## Abgrenzung

Noch KEIN Repository automatisch erstellt, kein Secret hinterlegt,
kein Workflow aktiviert. Für die Android-App-Verifizierung sind danach
die GitHub-Pages-Root-Site und die passende /.well-known/assetlinks.json
zusätzlich einzurichten. Ob ChatGPT Android Verified App Links in die
eigene App weitergibt, muss gesondert auf dem Samsung getestet werden.
