# FahrCockpit – Native Android architecture/prototype

**Status: Architecture + Android test scaffold, NOT approved for everyday driving.**

Goal: leave Google Maps as the user's navigation, display a compact, manually
dismissible media overlay, and later provide an on-demand fuel-prices screen.
This is an **independent Android package**, not a feature of the Japan PWA.

## Stage A · Initial build in this branch

- App ID: `de.cobrachris.fahrcockpit` (separate from Sanftwecker / Reise-App).
- Plain Android Views / Java 17, minSdk 26, compile/target SDK 35.
  No Spotify OAuth, subscription, paid Maps SDK or background location.
- Start/stop only via user action; notification while the overlay is running.
- Two explicit Android settings: overlay permission + Notification Access.
  The latter is broad OS permission; our code uses only Spotify MediaSessions.
- Large, finger-friendly play/pause, previous/next and 15-second seek buttons.
  Seek is guarded by the active Spotify session's `ACTION_SEEK_TO`.
- The overlay can be dragged; its window is not fullscreen and does not
  intercept touches outside the control itself.
- No auto-start, no GPS, no network or API key required for Stage A.
- CI builds a **debug APK for testing only**, not a signed release for updates.

## Planned Stage B · Fuel prices in Germany

Separate Activity: choose E5/E10/Diesel, request location only on a conscious
tap while Activity is visible, query Tankerkönig `list.php` on-demand,
show prices/timestamp/distance/source. Explicit tap to open Maps directions.
**The user's ongoing Google Maps route cannot be extracted reliably**; nearby
stations are not necessarily along that route.

A personal free Tankerkönig API key is required, supplied once by the user,
never committed to GitHub. Do not use the published demo key in production.
Respect usage terms: no background polling, bulk downloading, redistribution
or API key in releases or logs. The free API can change or become unavailable.

## Planned Stage C · Drive mode

Optional per-device overlay position/collapse state, manual commute favorite
and a one-tap shortcut to start Maps. No automatic route interception, no
unattended playback or distracting popups.

## Out of scope

- Blitzer.de/radar-camera warnings in a driver's active Germany configuration.
- A Google Maps embedded view or extraction of Google Maps' current route.
- Accessibility Services, screen capture, reading other apps' private UIs.
- Android Auto/vehicle screen and ongoing GPS-background tracking.
- Claims that Spotify `seekTo()` works for every playlist, ad or podcast.

See `docs/FAHR_COCKPIT_ARCHITECTURE.md` for the detailed plan.

## Reproducible check

The existing App-Fabrik reusable CI runs:
`gradle --no-daemon -p experiments/fahr-cockpit :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

The CI artifact is a debug APK. A stable per-app signing identity and a
real Samsung test are mandatory **before** publishing an installable release.
Do not reuse the Sanftwecker signing key or its package ID.
