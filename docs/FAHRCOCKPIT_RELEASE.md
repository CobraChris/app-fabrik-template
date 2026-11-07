# FahrCockpit – signierte Direkt-APK (Sicherheitsentwurf)

Die Bereitstellung wie beim Sanftwecker ist vorbereitet, aber noch NICHT aktiviert.
Die Workflow-Änderung erfordert eine gesonderte Sicherheitsfreigabe.

Der Workflow reagiert nur auf einen nach Freigabe angelegten und signierten
GitOps-Releaseauftrag unter requests/fahrcockpit-release.json auf main.
Der im Auftrag festgelegte Quell-Commit muss exakt dem geprüften FahrCockpit-
Entwicklungszweig entsprechen. Nur dieser Commit wird ohne Secrets gebaut,
getestet und anschließend durch einen getrennten geschützten Job signiert.

Voraussetzung sind VIER dedizierte FahrCockpit-Signing-Secrets mit den Namen:
FAHRCOCKPIT_SIGNING_KEYSTORE_BASE64, FAHRCOCKPIT_SIGNING_STORE_PASSWORD,
FAHRCOCKPIT_SIGNING_KEY_ALIAS, FAHRCOCKPIT_SIGNING_KEY_PASSWORD.
Der private Controller verwaltet aktuell nur eine Signieridentität je Repo;
sein Ausbau für mehrere Apps ist eine separate Sicherheitsaufgabe.
Kein privater Schlüssel darf in Chat, Quellcode, PR oder Logs erscheinen.

Erst nach bestandenen Signaturtests wird der direkte GitHub-Release-Link
https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v4/fahrcockpit-release.apk
tatsächlich verfügbar. Dieser Link ist NOCH NICHT aktiviert.

Die bereits im FahrCockpit-Code vorbereitete Update-Funktion erkennt später
nur solche signierten GitHub-Release-Assets. Android kann beim einmaligen
Wechsel von einer Debug-Installation auf die dauerhafte Release-Signatur
eine Neuinstallation verlangen. Danach sind In-Place-Updates möglich.
