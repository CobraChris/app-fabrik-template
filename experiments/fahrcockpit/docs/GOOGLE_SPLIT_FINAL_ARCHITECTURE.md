# FahrCockpit – Zielarchitektur Google-Turn-by-Turn und feste Spotify-Fläche

## Produktziel
Eine Ansicht in FahrCockpit mit echter Google-Abbiege-/Sprachnavigation im Kartenteil und dauerhaft zugänglichen Spotify-/Tankstellen-/Saphe-Bedienflächen. Hochkant Karte oben/Bedienung unten, quer Karte links/Bedienung rechts. Keine Overlay-Überdeckung, kein Verdecken der Tastatur, kein Funktionsverlust bereits abgenommener v18-Features.

## Technische Weggabelung (offizielle Plattformgrenzen)

**Stufe A – native Android-Splitscreen-Probe, v19:** Die installierte Google Maps App übernimmt Navigation mit den dokumentierten `google.navigation:q=…&mode=d` Intents. Android `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT` bittet um Platzierung neben `SplitCockpitActivity`, die Spotify-Tasten/Seekbar und weitere Cockpit-Funktionen zeigt. Samsung/Android bestimmt Fensterposition und Trennungsverhältnis; bei fehlender Unterstützung kann Google Maps im Vollbild erscheinen. Dann steht eine direkte Split-Hilfe in der FahrCockpit-App bereit, kein Overlay-Fallback und keine versprochene 70/30-Garantie. Kein Cloud-Billing oder Schlüssel erforderlich. **Dies sind weiterhin zwei Android-App-Fenster, nicht Googles Navigationsansicht innerhalb unserer eigenen Activity.**

**Stufe B – wirklich eingebettet (eigentliches Endziel):** Offizielles Google Navigation SDK für Android kann Google-Navigation in der App-Kartenfläche rendern und Navigations-Callbacks liefern, z. B. für eine schmale Karte mit danebenliegenden Spotify-Controls. Der Navigator ist nicht die normale Google Maps App; Google-Cloud-Projekt, Navigation SDK und API-Key/OAuth/Billing sind erforderlich. Offizielle Preise (Stand 10.11.2026): 1.000 Destinationen/Monat kostenfreie Nutzung; danach verbrauchsabhängige kostenpflichtige Navigations-Requests. Auch bei ausschließlich kostenloser Nutzung muss Billing **aktiviert** sein. Quoten/Alarme minimieren, verhindern aber mögliche Kosten nicht automatisch. Kein SDK-Key, keine Abrechnung, kein SDK-Call, keine Abos ohne **neue ausdrückliche Nutzerfreigabe**.

Offizielle Dokumentation:
- https://developer.android.com/develop/ui/views/layout/support-multi-window-mode
- https://developers.google.com/maps/documentation/navigation/android-sdk/pricing
- https://developers.google.com/maps/billing-and-pricing/pricing

## V19 als zusammenhängendes kostenloses Paket

1. V18-Kartenansicht, gespeicherte fünf Ziele, Google-Key-Übernahme, Spotify, GPS, Verkehrsschalter, Saphe und Tankstellen unverändert.
2. V18-Zieldialog erhält `Google-Navigation + Spotify im Splitscreen starten`; Haupt-Cockpit erhält eine direkte Split-Schaltfläche, wenn ein Ziel gewählt ist.
3. `SplitCockpitActivity` empfängt ein validiertes Ziel, startet die echte Google-Maps-Navigation über dokumentierten package-spezifischen Intent im benachbarten Android-Fenster, zeigt Status und bietet erneute Route/Zielwahl.
4. Im Split-Cockpit sind Spotify-MediaSession-Controller, +/-15s und die in v17 erprobte Spotify-Freigabediagnose sichtbar, plus Tankstellen, Saphe und alte alternative Modi.
5. Wenn Android den automatischen Splitscreen ablehnt, keine unbefugte Bildschirmmanipulation, keine neue Overlay-Automatik. Benutzer kann über die Android-Appübersicht manuell den geteilten Bildschirm einstellen. Nur im Stand testen.
6. Drehung, Rückkehr aus Google Maps/Spotify, letzter Zieltext via savedInstanceState; keine unfreiwillige Änderung gespeicherter Ziele, kein stiller Ortsverlauf.

## Abnahme und Abbruchkriterien

- CI (Node-Contracttests, Java-JUnit, Android lint/build) muss erfolgreich sein, bevor v19 gemergt wird.
- Geschützter Release ausschließlich nach gesonderter v19-/PR-Freigabe mit Originalsignatur. Nutzer installiert selbst über v18.
- Samsung vor Ort im Stand: Ziel wählen → Split starten → Google Maps navigiert in einem Fenster → Spotify-Bedienelemente bleiben im anderen → beide Ausrichtungen, Tastatur, Appwechsel, Aufteilen-/Zurückkehren testen.
- Falls Google Maps nur Vollbild bleibt und Samsung Multi Window nicht zuverlässig zulässt, **diese Beschränkung offen dokumentieren**, nicht über Overlays umgehen. Ein echtes Single-App-Fenster als Endziel erfordert Stufe B und eine getrennte Billing-Entscheidung.
