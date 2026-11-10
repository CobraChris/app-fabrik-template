import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const base = new URL('../experiments/fahrcockpit/', import.meta.url);
const read = (path) => readFileSync(new URL(path, base), 'utf8');
const java = (name) => read('app/src/main/java/com/cobrachris/fahrcockpit/' + name + '.java');

test('Blitzer.de PRO uses documented receiver and commands', () => {
  const c = java('BlitzerContract');
  const bridge = java('BlitzerBridge');
  for (const word of ['de.blitzer.plus', 'de.blitzer.service.BlitzerBroadcastReceiver',
    'de.blitzer.APP_MODE', 'de.blitzer.KILL_APP', '"START"', '"WIDGET"', '"BACKGROUND"']) {
    assert.ok(c.includes(word), 'missing official protocol: ' + word);
  }
  assert.match(bridge, /intent\.setClassName\(BlitzerContract\.PACKAGE, BlitzerContract\.RECEIVER\)/);
  assert.match(bridge, /context\.sendBroadcast\(intent\)/);
  assert.match(bridge, /SENT_UNCONFIRMED/);
});

test('Overlay requires explicit user gesture, draggable and dismissible', () => {
  const main = java('MainActivity');
  const overlay = java('OverlayService');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(main, /Settings\.canDrawOverlays/);
  assert.match(main, /startForegroundService\(/);
  assert.match(overlay, /TYPE_APPLICATION_OVERLAY/);
  assert.match(overlay, /window\.updateViewLayout\(panel, params\)/);
  assert.match(overlay, /stopSelf\(\)/);
  assert.match(overlay, /BlitzerContract\.Command\.STOP/);
  assert.match(overlay, /BlitzerContract\.Command\.START/);
  assert.match(overlay, /START_NOT_STICKY/);
  assert.match(manifest, /android:foregroundServiceType="specialUse"/);
  assert.match(manifest, /android:exported="false"/);
  assert.match(manifest, /android:allowBackup="false"/);
  assert.doesNotMatch(manifest, /BOOT_COMPLETED/);
});

test('Spotify media uses the Android notification-listener session', () => {
  const media = java('SpotifyMedia');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(media, /getActiveSessions\(listener\)/);
  assert.match(media, /ACTION_SEEK_TO/);
  assert.match(media, /controls\.seekTo\(target\)/);
  assert.match(media, /controls\.skipToNext/);
  assert.match(manifest, /BIND_NOTIFICATION_LISTENER_SERVICE/);
  assert.doesNotMatch(manifest, /MEDIA_CONTENT_CONTROL/);
});

test('Saphe integration uses the official installed app package without fake remote control', () => {
  const warning = java('WarningSource');
  const overlay = java('OverlayService');
  const main = java('MainActivity');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(warning, /my\.saphelink/);
  assert.match(warning, /getLaunchIntentForPackage/);
  assert.match(warning, /getSharedPreferences/);
  assert.match(warning, /"saphe"/);
  assert.match(warning, /"blitzer"/);
  assert.match(warning, /"none"/);
  assert.match(main, /Saphe Link öffnen/);
  assert.match(overlay, /WarningSource\.selected/);
  assert.match(manifest, /<package android:name="my\.saphelink"/);
  assert.doesNotMatch(warning, /BLUETOOTH_SCAN|BluetoothGatt|de\.blitzer\.APP_MODE/);
});

test('Fuel searches on user action, stores key locally and does not poll', () => {
  const fuel = java('FuelActivity');
  const rules = java('FuelRules');
  assert.match(fuel, /search\.setOnClickListener\(v -> search\(\)\)/);
  assert.match(fuel, /getSharedPreferences\(PREFS, MODE_PRIVATE\)/);
  assert.match(fuel, /location\.getLatitude\(\)/);
  assert.match(fuel, /new Thread\(\(\) ->/);
  assert.match(rules, /creativecommons\.tankerkoenig\.de\/json\/list\.php/);
  assert.match(rules, /"e5"/);
  assert.match(rules, /"e10"/);
  assert.match(rules, /"diesel"/);
  assert.doesNotMatch(fuel, /TimerTask|ScheduledExecutor/);
});

test('native Android split is the one-tap default; overlays cannot auto-return', () => {
  const main = java('MainActivity');
  const split = java('SplitCockpitActivity');
  assert.match(main, /scroll\.post\(this::startEmbeddedMap\)/);
  assert.match(main, /\.post\(this::startEmbeddedMap\)/);
  assert.match(main, /FLAG_ACTIVITY_LAUNCH_ADJACENT|SplitCockpitActivity\.class/);
  const mapsLaunch = java('GoogleMapsLaunch');
  assert.match(mapsLaunch, /Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT/);
  assert.match(mapsLaunch, /Intent.FLAG_ACTIVITY_NEW_TASK/);
  assert.match(split, /GoogleMapsLaunch.adjacentDrivingIntent/);
  assert.match(split, /enforceSplitWithoutOverlay\(\)/);
  assert.match(split, /stopService\(new Intent\(this, OverlayService\.class\)\)/);
  assert.doesNotMatch(split, /pendingOverlayFallback|fallbackGraceElapsed|shouldStartFallback/);
});

test('embedded map PoC uses a separate WebView viewport with native Spotify controls', () => {
  const main = java('MainActivity');
  const embedded = java('EmbeddedMapActivity');
  const rules = java('EmbeddedMapRules');
  const manifest = read('app/src/main/AndroidManifest.xml');
  assert.match(main, /FahrCockpit mit Google-Karte starten/);
  assert.match(embedded, /new WebView\(this\)/);
  assert.match(embedded, /LinearLayout\.HORIZONTAL/);
  assert.match(embedded, /LinearLayout\.VERTICAL/);
  assert.match(embedded, /7f/);
  assert.match(embedded, /3f/);
  assert.match(embedded, /SpotifyMedia\.read\(this\)/);
  assert.match(embedded, /setJavaScriptEnabled\(true\)/);
  assert.match(embedded, /setAllowFileAccess\(false\)/);
  assert.doesNotMatch(embedded, /addJavascriptInterface/);
  assert.match(rules, /static final String URL = "https:\/\/" \+ HOST \+ PATH/);
  assert.match(manifest, /android:name="\.EmbeddedMapActivity"/);
});

test('embedded Google-only map keeps native Spotify controls separate', () => {
  const main=java('MainActivity'),embedded=java('EmbeddedMapActivity');
  assert.match(main, /scroll\.post\(this::startEmbeddedMap\)/);
  assert.match(main, /startActivity\(new Intent\(this, EmbeddedMapActivity\.class\)\)/);
  assert.match(embedded, /GoogleKeyTransfer\.isImportStart/);
  assert.match(embedded, /stopService\(new Intent\(this, OverlayService\.class\)\)/);
  assert.match(embedded, /Google-Kartenprototyp ohne OpenStreetMap/);
  assert.doesNotMatch(embedded, /addJavascriptInterface/);
});

test('Google Demo Key handoff is on-device, single-use and uses AES-GCM',()=>{
 const transfer=java('GoogleKeyTransfer'),main=java('MainActivity'),
       embedded=java('EmbeddedMapActivity'),
       manifest=read('app/src/main/AndroidManifest.xml');
 assert.match(transfer,/AES\/GCM\/NoPadding/);
 assert.match(transfer,/new SecureRandom\(\)\.nextBytes/);
 assert.match(transfer,/System\.currentTimeMillis\(\) \+ VALID_MS/);
 assert.match(transfer,/Uri\.parse\(PAGE \+ "#k=" \+ session\)/);
 assert.match(transfer,/setPackage\("com\.android\.chrome"\)/);
 assert.match(transfer,/prefs\.edit\(\)\.clear\(\)\.commit\(\)/);
 assert.match(embedded,/org\.json\.JSONObject\.quote\(key\)/);
 assert.match(embedded,/localStorage\.setItem\('fahrcockpit-map-demo-key-v1'/);
 assert.match(main,/GoogleKeyTransfer\.consume\(this, intent\)/);
 assert.match(manifest,/android:scheme="fahrcockpit" android:host="transfer"/);
 assert.doesNotMatch(embedded,/addJavascriptInterface|L\.tileLayer/);
});

test('embedded driving pane has a single context-aware Spotify repair button',()=>{
 const embedded=java('EmbeddedMapActivity'),rules=java('SpotifyDisplayRules');
 assert.match(embedded,/spotifyAction = Ui\.button\(this, "Spotify öffnen"\)/);
 assert.match(embedded,/spotifyAction\.setOnClickListener\(v -> runSpotifyNextStep\(\)\)/);
 assert.match(embedded,/SpotifyDisplayRules\.nextAction\(listenerGranted, state\.connected\)/);
 assert.match(embedded,/spotifyAction\.setVisibility\(nextAction == null \? View\.GONE : View\.VISIBLE\)/);
 assert.match(embedded,/new Intent\(Settings\.ACTION_NOTIFICATION_LISTENER_SETTINGS\)/);
 assert.match(embedded,/getLaunchIntentForPackage\("com\.spotify\.music"\)/);
 assert.match(embedded,/getPackageManager\(\)/);
 assert.match(embedded,/Benachrichtigungszugriff/);
 assert.match(embedded,/SpotifyDisplayRules\.connectedStatus\(state\.playing\)/);
 assert.match(rules,/hasMediaAccess/);
 assert.match(rules,/Spotify öffnen und Musik starten/);
 assert.doesNotMatch(embedded,/startForegroundService.*Spotify|addJavascriptInterface/);
});

test('driving navigation packet provides native goal dialog, saved destinations, route preview, launch',()=>{
 const embedded=java('EmbeddedMapActivity');
 const maps=java('GoogleMapsLaunch');
 const rules=java('RouteTargetRules');
 assert.match(embedded,/📍 Ziel \/ Route wählen/);
 assert.match(embedded,/new AlertDialog\.Builder\(this\)/);
 assert.match(embedded,/Ziel merken/);
 assert.match(embedded,/Gespeicherte Ziele löschen/);
 assert.match(embedded,/Route in Google Maps ansehen/);
 assert.match(embedded,/Autonavigation in Google Maps starten/);
 assert.match(embedded,/getSharedPreferences\(ROUTES_PREFS, MODE_PRIVATE\)/);
 assert.match(embedded,/if \(slot >= 5\) break/);
 assert.match(embedded,/RouteTargetRules\.coordinates/);
 assert.match(embedded,/EmbeddedMapRules\.allowsPage\(view\.getUrl\(\)\)/);
 assert.match(embedded,/GoogleMapsLaunch\.navigationIntent\(cleaned\)/);
 assert.match(embedded,/GoogleMapsLaunch\.directionsIntent\(cleaned\)/);
 assert.match(embedded,/showDestinationDialog\(chosen\)/);
 assert.match(embedded,/onSaveInstanceState/);
 assert.match(maps,/Uri\.parse\(url\)/);
 assert.match(maps,/setPackage\(GoogleMapsUrlRules\.GOOGLE_MAPS_PACKAGE\)/);
 assert.match(rules,/google\.navigation:q=/);
 assert.match(rules,/www\.google\.com\/maps\/dir\/\?api=1&destination=/);
 assert.match(rules,/travelmode=driving/);
 assert.match(rules,/mode=d/);
 assert.doesNotMatch(embedded,/addJavascriptInterface|NavigationSdk|startForegroundService/);
 assert.doesNotMatch(rules,/maps\.googleapis\.com|apiKey|AIza/);
});

test('v19 split Google-Navigation uses Android window management with no overlays or new API',()=>{
 const embedded=java('EmbeddedMapActivity');
 const split=java('SplitCockpitActivity');
 const maps=java('GoogleMapsLaunch');
 const manifest=read('app/src/main/AndroidManifest.xml');
 const rules=java('RouteTargetRules');
 assert.match(embedded,/Google-Navigation \+ Spotify geteilt starten/);
 assert.match(embedded,/Google-Navigation \+ Spotify im Splitscreen starten/);
 assert.match(embedded,/launchSplitNavigation\(target\)/);
 assert.match(embedded,/SplitCockpitActivity\.EXTRA_NAVIGATION_TARGET/);
 assert.match(split,/static final String EXTRA_NAVIGATION_TARGET/);
 assert.match(split,/RouteTargetRules\.clean\(savedInstanceState != null/);
 assert.match(split,/ui\.postDelayed\(\(\) -> launchMapsAdjacent\(routeTarget\), 450L\)/);
 assert.match(split,/GoogleMapsLaunch\.adjacentDrivingIntent\(target\)/);
 assert.match(split,/showSplitHelp\(\)/);
 assert.match(split,/📍 Ziel für Google-Navigation wählen/);
 assert.match(split,/getSharedPreferences\(ROUTES_PREFS, MODE_PRIVATE\)/);
 assert.match(split,/getSharedPreferences\(ROUTES_PREFS, MODE_PRIVATE\)/);
 assert.match(split,/ScrollView dialogScroll = new ScrollView\(this\)/);
 assert.match(split,/SpotifyDisplayRules\.nextAction\(granted, state\.connected\)/);
 assert.match(split,/SpotifyMedia\.send\(this, action\)/);
 assert.match(split,/state\.putString\(KEY_TARGET, routeTarget\)/);
 assert.match(maps,/Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT/);
 assert.match(maps,/Intent.FLAG_ACTIVITY_NEW_TASK/);
 assert.match(maps,/navigationIntent\(rawTarget\)/);
 assert.match(maps,/setPackage\(GoogleMapsUrlRules\.GOOGLE_MAPS_PACKAGE\)/);
 assert.match(manifest,/<activity android:name="\.SplitCockpitActivity"[\s\S]*?android:resizeableActivity="true"/);
 assert.match(split,/stopService\(new Intent\(this, OverlayService\.class\)\)/);
 assert.match(split,/stopService\(new Intent\(this, DockedCockpitService\.class\)\)/);
 // Old explicit user-initiated overlay mode is retained, but never
 // launched automatically as a fallback if Samsung rejects native split.
 assert.match(split,/overlay.setOnClickListener\(v -> \{/);
 assert.doesNotMatch(split,/TYPE_APPLICATION_OVERLAY|AccessibilityService|pendingOverlayFallback|fallbackGraceElapsed/);
 assert.doesNotMatch(maps,/NavigationSdk|API_KEY|AIza/);
 assert.match(rules,/google\.navigation:q=/);
});


test('v20 integrated native layout is a real fixed 70/30 shell with existing Spotify and fallback',()=>{
 const shell=java('IntegratedNavigationActivity');
 const engine=java('NavigationEngine');
 const rules=java('IntegratedNavigationRules');
 const app=java('MainActivity');
 const manifest=read('app/src/main/AndroidManifest.xml');
 assert.match(shell,/LinearLayout.HORIZONTAL : LinearLayout.VERTICAL/);
 assert.match(shell,/IntegratedNavigationRules.MAP_WEIGHT/);
 assert.match(shell,/IntegratedNavigationRules.CONTROLS_WEIGHT/);
 assert.match(rules,/MAP_WEIGHT = 7/);
 assert.match(rules,/CONTROLS_WEIGHT = 3/);
 assert.match(shell,/FrameLayout mapPane/);
 assert.match(shell,/root.addView\(mapPane, mapParams\)/);
 assert.match(shell,/root.addView\(scroll, controlParams\)/);
 assert.match(shell,/SpotifyMedia\.read\(this\)/);
 assert.match(shell,/SpotifyMedia\.send\(this, action\)/);
 assert.match(shell,/SpotifyMedia\.seekAbsolute/);
 assert.match(shell,/FuelActivity.class/);
 assert.match(shell,/my\.saphelink/);
 assert.match(shell,/SplitCockpitActivity.class/);
 assert.match(shell,/Route in dieser App berechnen/);
 assert.match(shell,/Route berechnen/);
 assert.match(shell,/NavigationDestinationResolver\.resolve/);
 assert.match(shell,/engine\.startConfirmedRoute\(lat, lng, name\)/);
 assert.match(shell,/engine != null && engine\.configured\(\)/);
 assert.match(shell,/getSharedPreferences\(ROUTES_PREFS, MODE_PRIVATE\)/);
 assert.match(shell,/stopGuidanceAndClearRoute\(\)/);
 assert.match(shell,/window|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE/);
 assert.match(shell,/Keine|noch keine|nicht aktiviert/i);
 assert.match(app,/IntegratedNavigationActivity.class/);
 assert.match(manifest,/android:name="\.IntegratedNavigationActivity"/);
 assert.match(engine,/boolean configured\(\)/);
 assert.match(engine,/View createNavigationView/);
 assert.doesNotMatch(shell,/FLAG_ACTIVITY_LAUNCH_ADJACENT|TYPE_APPLICATION_OVERLAY|NavigationApi\.getNavigator/);
});

test('v20 SDK is not in the default signed variant and cannot route without explicit Cloud approval',()=>{
 const gradle=read('app/build.gradle');
 const nav=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
 const resolver=java('NavigationDestinationResolver');
 const script=read('../../scripts/fahrcockpit-release-request.mjs');
 assert.match(gradle,/withGoogleNavSdk/);
 assert.match(gradle,/orElse\('false'\)/);
 assert.match(gradle,/if \(withGoogleNavSdk\) java\.srcDir 'src\/navsdk\/java'/);
 assert.match(gradle,/if \(withGoogleNavSdk\) \{/);
 assert.match(gradle,/System\.getenv\('FAHRCOCKPIT_NAV_SDK_KEY'\)/);
 assert.match(gradle,/navigation:7\.6\.1/);
 assert.match(gradle,/targetSdk withGoogleNavSdk \? 36 : 35/);
 assert.match(nav,/NavigationApi\.setApiKey\(apiKey\)/);
 assert.match(nav,/NavigationApi\.getNavigator\(host/);
 assert.match(nav,/new NavigationView\(host\)/);
 assert.match(nav,/Waypoint\.builder\(\)/);
 assert.match(nav,/navigator\.setDestination\(waypoint, options\)/);
 assert.match(nav,/navigator\.startGuidance\(\)/);
 assert.match(nav,/navigator\.stopGuidance\(\)/);
 assert.match(nav,/navigator\.clearDestinations\(\)/);
 assert.match(nav,/map\.onCreate\(saved\)/);
 assert.match(nav,/map\.onSaveInstanceState/);
 assert.match(resolver,/Geocoder\.isPresent\(\)/);
 assert.match(resolver,/Geocoder\.GeocodeListener/);
 assert.match(script,/refs\/heads\/feature\/fahrcockpit-blitzer-bridge/);
 assert.doesNotMatch(java('MainActivity'),/new GoogleNavigationEngine/);
 assert.doesNotMatch(nav,/AIza[A-Za-z0-9_-]{30}/);
});
