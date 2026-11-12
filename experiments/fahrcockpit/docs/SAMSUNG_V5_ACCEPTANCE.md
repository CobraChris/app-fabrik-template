# FahrCockpit – Samsung-Abnahme des signierten Updates v4 → v5

**Stand 08.11.2026.** Dieses Dokument ist eine Abnahme-Checkliste, **keine Installationsfreigabe** und kein automatischer Android-Eingriff.

## Bereits nachgewiesen
- Signierte Android-v4-/0.9.2-beta-APK auf Samsung Android 16 installiert.
- FahrCockpit als Overlay über Google Maps und Spotify sichtbar; Wiedergabe-Metadaten und Medientasten, ±15 s und minimieren/ausklappen vom Nutzer bestätigt.
- Signierte v5-/0.9.3-beta-APK erfolgreich veröffentlicht; [Run 42483843251](https://github.com/CobraChris/app-fabrik-template/actions/runs/42483843251) mit Test, Lint, Versionscode-Prüfung und identischem Zertifikatsfingerprint zu v4 vollständig erfolgreich.
- V5-APK SHA-256 laut GitHub-Release-Metadaten `ab6bc9fbc626e33fbdc5649533ab7bf75c65871914f9257c84a2dfeb6fb5ab00`, 82511 Bytes.

## Gerätetest v5 – erst wenn der Nutzer bereit ist

**Direkter Download:** https://github.com/CobraChris/app-fabrik-template/releases/download/fahrcockpit-v5/fahrcockpit-release.apk

1. In der installierten FahrCockpit-v4-App unten **Auf Updates prüfen** wählen und Build 5 als neueres Release bestätigen. Alternativ den direkten GitHub-APK-Link **in Chrome** herunterladen.
2. Die vorhandene App **nicht deinstallieren**. Android soll die APK als **Update** installieren. Sollte Android erneut eine Entfernung der bestehenden App fordern oder einen Signaturfehler melden, **abbrechen** und Ursache prüfen.
3. Hauptansicht öffnen, Overlay-/Spotify-Berechtigungen müssen **erhalten** sein; Overlay über Google Maps starten.
4. Bei ausgewähltem **Saphe Link**: nur **Saphe öffnen** sichtbar, **kein** Blitzer-Stop-Knopf. In den App-Einstellungen Blitzer.de PRO wählen: im Cockpit getrennte **Start-/Stop-Anforderungen** und kein als verifiziert dargestellter Blitzer-Status.
5. Spotify starten, Titelanzeige, Play/Pause, nächster/vorheriger Titel und ±15 s erneut prüfen; Cockpit einklappen, ausklappen, verschieben.
6. Falls praktisch: Displaydrehung und erneutes Öffnen prüfen; das Cockpit soll innerhalb der Bildschirmgrenzen bleiben.

**Spätere, getrennte Tests in Deutschland:** physisches Saphe-Gerätemodell/koppeln; Tankerkönig-Livepreise erst mit eigenem kostenlosen API-Key und Standortfreigabe auf Nutzerklick. Während der Fahrt keine Bedienung und keine gesetzlich unzulässigen Blitzerwarnungen (Deutschland: § 23 Abs. 1c StVO).

## Nächster Entwicklungsentwurf (noch kein Release)

Separater, ungemergter Code-Entwurf `feature/fahrcockpit-next-diagnostics`: Spotify-Berechtigung fehlt vs. Spotify ohne aktive Sitzung unterscheiden. Keine neuen Manifest-Berechtigungen, Secrets, externen Dienste oder GitHub-Workflows. Dieser Code gehört **nicht** zum veröffentlichten Build 5.
