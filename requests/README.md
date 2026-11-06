# Sanftwecker: kontrollierte signierte Beta-Releases

Die Android-Anwendung bleibt bis zur Abnahme in
\`feature/watch-vibration-poc\` (PR #19). Das Release-Build bekommt keinen
Signierschlüssel und verwendet einen exakten, von \`main\` freigegebenen Commit-SHA.

* Ein **Push auf main**, der \`requests/sanftwecker-release.json\` ändert, löst den
  Build aus. Der Besitzer \`CobraChris\` muss der GitHub-Actor sein.
* Die Request-Prüfung akzeptiert nur einen genau definierten JSON-Inhalt.
  Der Commit muss dem aktuellen HEAD des Sanftwecker-Entwicklungszweigs entsprechen;
  bei zwischenzeitlichen Änderungen wird die Freigabe verworfen.
* Gradle baut Tests, Lint und eine **unsignierte** Release-APK ohne Secrets.
* Nur der getrennte GitHub-Environment-Job \`android-releases\` erhält die
  vorhandenen vier \`APP_SIGNING_*\`-Secrets und signiert die geprüfte APK.
* Release-Tag ist \`sanftwecker-v<versionCode>\`. Bestehende Tags und Downgrades
  werden abgewiesen. Der Zertifikats-Fingerabdruck wird vor jedem weiteren
  Update mit dem vorherigen Release verglichen.
* Die APK \`sanftwecker-release.apk\` steht direkt im **GitHub-Prerelease**,
  daneben SHA256SUMS und Build-Informationen. Das ermöglicht den Update-Check
  der App, keine externen Dienste und kein dauerhafter Secrets-Zugriff durch ChatGPT.

**Erster vorbereiteter Release-Auftrag:** Beta 0.8.0, versionCode 9,
GitHub-Commit \`b8191b3626dd368e3a4b59da088b46b94b8ebf3b\`.
Er läuft erst, wenn der zugehörige Workflow per überprüftem Main-Merge aktiviert
wird. Der Benutzer bestätigt anschließend die Installation auf Android.

**Einmaliger Umstieg von Debug auf Release:** Eine bereits installierte
Debug-Version kann wegen der anderen Signatur möglicherweise nicht überinstalliert
werden. Die bestehende Installation **nicht ohne Sicherung ihrer Alarmdaten
deinstallieren**. Danach können Updates mit identischer Signatur und höherem
versionCode regulär eingespielt werden. Bis zur echten Samsung-/Watch-Abnahme
den bisher zuverlässigen Wecker parallel aktiv lassen.

Für weitere Versionen: Sanftwecker-Code und versionCode im Entwicklungszweig
ändern, alle PR-CI-Tests bestehen lassen, anschließend die neue Commit-SHA,
höheren versionCode und neue request_id in dieser Request-Datei auf main
aktualisieren. Dadurch entsteht ohne manuellen "Run workflow"-Knopf
die nächste signierte Version. Die Signaturschlüssel bleiben pro App dauerhaft.
