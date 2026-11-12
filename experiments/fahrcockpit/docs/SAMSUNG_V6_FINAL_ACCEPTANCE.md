# FahrCockpit 0.9.4-beta / Build 6 – eine abschließende Samsung-Geräteabnahme

**Status:** Implementierter, automatisiert geprüfter Release-Kandidat. Noch keine tatsächliche Geräte-Abnahme des nativen Splitscreen-Modus. Release-Signierung und Download-Link erst nach erfolgreicher Veröffentlichung vermerken.

## Umfang dieses kompletten Pakets

- Vollwertiger zusätzlicher **Android-Multi-Window-Splitscreen** mit separater, resizierbarer `SplitCockpitActivity` und Google Maps als **echter, separater App**.
- One-Tap-Start versucht mit `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT` die Teilung. **Das Android-/Samsung-System steuert Position und Aufteilungsquote.** Ein exaktes 70/30-Verhältnis lässt sich aus gewöhnlichem Drittanbieter-Code nicht erzwingen.
- Wenn Samsung den Split nicht annimmt, bleibt nach vorherigem Overlay-Start die ursprüngliche Schwebesteuerung als Fallback nutzbar. Sobald Split aktiv ist, wird das temporäre Overlay wieder geschlossen.
- In der Split-Steuerleiste: Spotify-Titel, Drag-Seeking samt Positions-/Daueranzeige, Play/Pause, vorheriger/nächster Titel, ±15 Sekunden, Maps-/Tankstellen-/Saphe-Aufruf.
- Weiterhin schwebender Modus: Vollansicht/Fahrt/Minimieren, Spotify-Ziehbalken, getrennte Hoch-/Querformatpositionen und autom. kompakter Fahrmodus im Querformat (in den Einstellungen abschaltbar).
- Karte öffnet ohne neue Google-Maps-API, ohne Cloud Billing oder Tokens, ohne Accessibility- oder Medienprojektionserlaubnis.
- Die physische Saphe-Hardware und der Hintergrundwarnstatus werden **nicht** vorgetäuscht; Deutschland: Fahrer-Blitzerwarnungen §23 Abs.1c StVO.

## Ein einziger End-to-End-Gerätetest (nach signiertem Release)

**Nicht das vorhandene FahrCockpit deinstallieren.** Android muss den signierten Build 6 mit demselben Zertifikat als Update über Build 5 annehmen; Settings und Spotify-Recht sollen erhalten bleiben.

1. In FahrCockpit **Auf Updates prüfen** → Build 6 installieren. Version und erhaltene Spotify-/Overlay-Rechte einmal ansehen.
2. Spotify starten und einen seekbaren Titel laufen lassen. FahrCockpit starten und **70/30-Fahransicht starten** wählen.
3. Gerät **quer** halten. Google Maps und FahrCockpit sollen in zwei echten Systemfenstern liegen. Falls die Fensterteilung auf Samsung nicht automatisch erscheint, ist Androids App-Paar-/Multi-Window-Schritt einmalig notwendig: über die letzten Apps Split aktivieren, Google Maps neben FahrCockpit wählen, Teiler auf ca. 70/30 ziehen und als App-Paar speichern. Danach lässt sich dieses Paar direkt starten.
4. Im 30%-Cockpit **Spotify-Play, Titelwechsel, ±15 Sekunden und Zeit-Ziehbalken** einmal prüfen. In Google Maps Route/Verkehrslage unverändert.
5. Handy **hochkant**: die Aufteilung bzw. UI muss sich an die Bildschirmform anpassen und benutzbar bleiben. Alternativ auf **Schwebendes Cockpit** umschalten: Vollansicht, kompakter Fahrmodus, Maps und Spotify prüfen.
6. Saphe-App kann geöffnet werden. Ob sie während des Betriebs im Hintergrund wirklich verbunden bleibt, erst mit dem physischen Modell prüfen. Tankstellen-/Preis-API nur vor Ort in Deutschland mit privatem Schlüssel testen.

## Bekannte Plattformgrenze

Ein Hersteller kann den systemseitigen Split-Start trotz Intent-Flag ablehnen. Das ist **kein** Google-Maps- oder Java-Fehler, der durch einen weiteren API-Key lösbar wäre. Die Möglichkeit, Größenaufteilung oder App-Paar automatisch zu setzen, ist nicht Bestandteil der allgemeinen Android-Drittanbieter-Schnittstellen. Der native Samsung-Multi-Window-Weg bleibt kostenlos, benötigt ggf. den einmaligen Systemschritt.
