# App-Fabrik: privater GitHub-Administrationscontroller

**Status: sichere Vorlage im Draft-PR, noch nicht aktiv.** Die Dateien in diesem öffentlichen
Repository sind ausschließlich **Quellcode ohne Geheimnisse**. Sie werden später in ein
eigenes **privates** Repository übertragen.

## Warum ein privater Controller?

Eine reguläre ChatGPT-GitHub-Verbindung kann derzeit Code und PRs ändern, bietet aber
keine Action zum Anlegen von GitHub-Actions-Secrets. Die GitHub-REST-API unterstützt
das mit einer separaten GitHub App. Anstatt einen kostenpflichtigen Server zu betreiben,
kann ein privates GitHub-Repository einen **manuell ausgelösten GitHub-Actions-Job**
als geschützte Ausführungsumgebung nutzen.

Die einmalige Einrichtung ist Sicherheitsarbeit; der Controller selbst kann danach
für **beliebig viele freigegebene persönliche Android-Repositories** neue
Signatur-Schlüssel erstellen. Für jede weitere App ist nur ein manueller Workflow-Start
und gegebenenfalls die Freigabe dieser App für das Zielrepository erforderlich,
solange ChatGPT keine entsprechende Admin-Schreibaktion anbietet.

## Einmalige Einrichtung (Android/Browser geeignet)

1. Neues **privates** Repository **CobraChris/github-app-fabrik-control** anlegen,
   vorzugsweise leer mit README. Nicht öffentlich machen.
2. Die Dateien aus **diesem Verzeichnis** dort unter denselben Pfaden einspielen:
   \`.github/workflows/provision-android-signing.yml\`,
   \`scripts/provision-core.mjs\`, \`scripts/provision.mjs\`.
   Die vorhandene GitHub-ChatGPT-Verbindung kann das nach Erstellung des Repositories
   übernehmen, sofern sie Zugriff auf das neue Repository hat.
3. Unter **https://github.com/settings/apps/new** eine persönliche GitHub App
   (z.B. *CobraChris App-Fabrik Admin*) registrieren. Webhook deaktivieren,
   ausschließlich Repository-Berechtigung **Secrets: Read and write** gewähren.
   Keine Organisations- oder Account-Berechtigungen und keine Contents/Administration-Rechte
   für diese GitHub App. Installationsbereich **nur dieses Benutzerkonto**.
4. Die App im eigenen Konto installieren und **nur die gewünschten Zielrepositories**
   freigeben. Von GitHub die **App ID** und **Installation ID** notieren.
   Einen privaten GitHub-App-Schlüssel als PEM erzeugen und sicher aufbewahren.
5. In \`CobraChris/github-app-fabrik-control\` einmalig vier
   GitHub Actions Repository-Secrets sicher eintragen:
   - \`GH_APP_ID\` (GitHub App ID)
   - \`GH_APP_INSTALLATION_ID\` (Installation ID)
   - \`GH_APP_PRIVATE_KEY_PEM\` (vollständige PEM-Datei; niemals in Chat posten)
   - \`BACKUP_KEY_BASE64\` (**32 echte Zufallsbytes**, Base64 kodiert;
     sicher auch außerhalb von GitHub sichern, z.B. Passwortmanager)
6. Falls verfügbar ein GitHub-Environment \`signing-administration\` für
   den privaten Controller konfigurieren; nur \`main\` als Deployment-Branch.
   Manueller Trigger und Inhaberprüfung bleiben im Workflow zwingend.
7. GitHub **Actions → Android-App-Signierschluessel einmalig einrichten → Run workflow**.
   Als Eingabe \`CobraChris/meine-neue-android-app\` verwenden.

**Wichtig:** Die Bedienung von GitHub Actions über einen eigenen MCP-Schreibconnector
steht im aktuellen ChatGPT Plus nicht zur Verfügung. Der Benutzer startet diesen
privilegierten Controller bis zur Unterstützung einer geeigneten Verbindung selbst.
Das ist nicht mit einer vollständig automatisierten Integration zu verwechseln.

## Was der Controller pro App macht

- Verlangt den echten Benutzer **CobraChris**, das private Control-Repository
  und einen manuellen Start auf dessen Default-Branch.
- Erstellt einen **eigenen** starken Android-RSA-3072-KeyStore (PKCS12), Passwort
  und Alias für das freigegebene Zielrepository.
- Verschlüsselt alle Werte vor jedem Zielrepo-Schreibzugriff mit **AES-256-GCM**
  und einem separaten 256-Bit-Backup-Schlüssel. Das Backup wird ausschließlich
  verschlüsselt unter \`backups/<repo>.json.enc\` im **privaten** Controller committed.
- Verwendet ein **kurzlebiges GitHub-App-Installationstoken**, begrenzt auf
  ein Zielrepository und **Secrets: write**, und lädt ausschließlich die
  vier standardisierten \`APP_SIGNING_*\`-Secrets zum Zielrepository hoch.
- Falls das Workflow-Senden unterbrochen wird, kann der Vorgang mit **demselben
  verschlüsselten Backup** erneut gestartet werden. Fehlende Secrets werden
  ergänzt; bereits vorhandene werden nicht überschrieben.
- Wenn Secrets ohne passendes Backup existieren, **Abbruch** statt
  unbeabsichtigter Änderung der APK-Signatur.
- Gibt niemals Keystore, Passwörter, private Schlüssel oder Tokens an ChatGPT,
  einen Pull Request, einen Workflow-Log oder eine Website zurück.

## Schutzziele und Grenzen

- Ein Secret kann in GitHub nicht im Klartext gelesen werden; nur Namen und
  Zeitstempel sind abfragbar. Die Verschlüsselung des Backups ist daher kritisch.
- Der private Controller ist hochprivilegiert: Wer dort Workflow-Code und
  GitHub-Secrets verwalten kann, kann Signaturen kompromittieren. Schreibrechte
  strikt begrenzen, Sicherheitsänderungen prüfen. Niemals Pull-Request-Code
  mit Signier- oder Admin-Secrets ausführen.
- Das Backup ist ohne \`BACKUP_KEY_BASE64\` nicht wiederherstellbar. Schlüssel
  **außerhalb von GitHub** sichern. Wer GitHub-App-PEM und Backups verliert,
  verliert möglicherweise alle Update-Schlüssel.
- Der Controller installiert keine APK. Release-Builds bleiben im getrennten
  wiederverwendbaren Android-Release-Workflow. Die installierte Android-App muss
  denselben Package-Namen und dasselbe Signaturzertifikat haben; bei Umstellung
  von einer instabil signierten Debug-APK kann **ein letztes Mal** eine
  Neuinstallation nötig werden.
- GitHub Actions auf privaten Repositories verbrauchen das verfügbare Freikontingent.
  Kein kostenpflichtiger Drittanbieter ist erforderlich; vor Aktivierung die
  GitHub-Abrechnungs-/Limits-Einstellungen prüfen.

## Nächster Abnahmetest

Erst nach Code-Review und Einrichtung mit einem **leeren Testrepository**
provisionieren, dann vier Secret-Namen und verschlüsseltes Backup prüfen.
Anschließend zwei aufeinanderfolgende signierte APK-Releases erstellen und deren
Zertifikatsidentität nachweisen, bevor bestehende Apps migriert werden.
