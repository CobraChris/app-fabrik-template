# Sanftwecker: dauerhaft signierte Updates (Stand: 06.11.2026)

## Verifizierter Stand

Die **signierten GitHub-Releases** wurden bereits erfolgreich veröffentlicht:

| Version | Android versionCode | GitHub-Release |
|---|---:|---|
| 0.8.0-beta | 9 | https://github.com/CobraChris/app-fabrik-template/releases/tag/sanftwecker-v9 |
| 0.8.1-beta | 10 | https://github.com/CobraChris/app-fabrik-template/releases/tag/sanftwecker-v10 |

Beide Releases enthalten die signierte Datei `sanftwecker-release.apk`,
`SHA256SUMS.txt` und `RELEASE_INFO.txt`. Der GitHub-Actions-Workflow hat
beim zweiten Release vor Veröffentlichung das **gleiche SHA-256-Signaturzertifikat**
wie beim ersten verifiziert. Außerdem sind die Android-Paket-ID
`com.cobrachris.sanftwecker` und der steigende versionCode geprüft.

**Wichtig:** CI bestätigt Signaturidentität und Builds, aber noch **keinen
erfolgreichen Android-In-place-Updateversuch** und keine zuverlässige Weckfunktion
im Schlaf-/Energiesparmodus. PR #19 bleibt daher im Draft.

## Automatische Release-Fabrik – keine manuellen Secrets und Workflow-Starts

Der aktive, sicherheitsbegrenzte GitHub-Actions-Workflow liegt auf `main` unter
`.github/workflows/sanftwecker-release-candidate.yml`.

1. Die Wecker-App bleibt in `feature/watch-vibration-poc`, damit sie nicht vor
   Samsung-Abnahme in die Haupt-App übernommen wird.
2. Nach erfolgreichen GitHub-Tests wird ein exakter, geprüfter Commit-SHA und
   der höhere Android-versionCode in `requests/sanftwecker-release.json`
   auf `main` eingetragen.
3. Nur ein Push des Inhabers auf `main`, der diese Auftragsdatei ändert,
   löst den Workflow aus. Fremde Pfade, Tags, Commits und fehlerhafte Angaben
   werden abgewiesen.
4. Der unsignierte Gradle-Build führt `:alarm:testDebugUnitTest`,
   `:alarm:lintDebug` und `:alarm:assembleRelease` ohne Secrets aus.
5. Der getrennte Signierjob `android-releases` verwendet die vier bereits
   eingerichteten, vertraulichen Repository-Secrets
   `APP_SIGNING_KEYSTORE_BASE64`, `APP_SIGNING_STORE_PASSWORD`,
   `APP_SIGNING_KEY_ALIAS`, `APP_SIGNING_KEY_PASSWORD`.
6. Vor einem Update wird das zuvor veröffentlichte Signaturzertifikat mit
   dem neuen verglichen. Bei falschem Zertifikat, älterem versionCode oder
   vorhandenem Release-Tag erfolgt **keine Veröffentlichung**.
7. Der fertig signierte APK-Download wird als GitHub-*Prerelease* veröffentlicht;
   das GitHub-Release-Asset ist keine ZIP-Datei.

Eine zweite, ältere manuelle Signierpipeline mit zwei anderen
`SANFTWECKER_*`-Secrets wurde aus dem Entwicklungszweig entfernt; sie ist
überholt und soll nicht mehr für Releases verwendet werden. Die Verteilung
bleibt kostenlos innerhalb der verfügbaren GitHub-Kontingente.

## Samsung-Abnahme – kontrollierter Update-Test

**Wichtig:** Wenn schon eine Debug-Version von „Sanftwecker (Test)“ installiert
ist, lässt sie sich wegen der abweichenden Debug-Signatur wahrscheinlich nicht
direkt mit einer signierten Release-APK ersetzen. Die vorhandene App
**nicht ungeprüft deinstallieren**: Eine Deinstallation löscht Wecker,
Timer und Stoppuhren. Die App hat `android:allowBackup="false"`.

Für einen bewussten Test nach Sicherung beziehungsweise Notieren der Daten:

1. Die bestehende Installation und ihre gespeicherten Einstellungen prüfen.
   Erst bei bewusst gewähltem Wechsel auf die Release-Signatur eventuell die
   Debug-Version deinstallieren.
2. **Build 9** als Ausgangsversion installieren:
   https://github.com/CobraChris/app-fabrik-template/releases/download/sanftwecker-v9/sanftwecker-release.apk
3. Einen Testwecker, Timer und Stoppuhr-Stand speichern. Berechtigungen und
   Watch-Benachrichtigungsspiegelung prüfen.
4. In der App auf **„App aktualisieren“** und **„Nach Update suchen“**
   tippen oder **Build 10** über den externen Browser herunterladen:
   https://github.com/CobraChris/app-fabrik-template/releases/download/sanftwecker-v10/sanftwecker-release.apk
5. Android sollte **„Aktualisieren“ statt „Neu installieren“** anbieten.
   Installationsbestätigung ist normal; stilles Installieren wird nicht
   versucht.
6. Nach dem Update prüfen: App-Version 0.8.1, gespeicherter Testwecker,
   Timer/Stoppuhr, Android-Benachrichtigungen, Alarm nach Neustart,
   gesperrtem Bildschirm, Energiesparmodus und Galaxy Watch.

**Solange diese Tests nicht bestanden sind, den bisher zuverlässigen Wecker
parallel verwenden.** Ein GitHub-Build ersetzt niemals einen echten Test mit
geschütztem Handy und Galaxy Watch.

## Grenzen

Die Updateprüfung nutzt eine begrenzte GitHub-API-Abfrage ausschließlich nach
Nutzerklick und akzeptiert nur eine direkte APK-URL aus dem bekannten
Repository mit zum Release gehörigem numerischem Tag. Kein Login, keine
Analytics, keine kostenpflichtigen Dienste. Android entscheidet selbst, ob
Installation und Update zulässig sind. Browserbasierte APK-Installation
benötigt gegebenenfalls einmalig die passende Android-Berechtigung.
