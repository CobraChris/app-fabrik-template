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
  assert.match(main, /70\/30-Kartenansicht ohne Navigation öffnen/);
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
  // Free real navigation is the permanent app-icon default.
  assert.match(main, /startActivity\(new Intent\(this, SplitCockpitActivity\.class\)\)/);
  assert.doesNotMatch(main, /navigationAuthorizedBuild/);
  assert.match(main, /Google Maps \+ Spotify kostenlos starten/);
  assert.match(main, /new Intent\(this, EmbeddedMapActivity\.class\)/);
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
  assert.match(split,/ui\.postDelayed\(\(\) -> \{/);
  assert.match(split,/if \(isInMultiWindowMode\(\)\)/);
  assert.match(split,/launchMapsAdjacent\(routeTarget\)/);
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
 assert.match(shell,/root.addView\(cockpitPane, controlParams\)/);
 assert.match(shell,/SpotifyMedia\.read\(this\)/);
 assert.match(shell,/SpotifyMedia\.send\(this, action\)/);
 assert.match(shell,/SpotifyMedia\.seekAbsolute/);
 assert.match(shell,/FuelActivity.class/);
 assert.match(shell,/my\.saphelink/);
 assert.match(shell,/SplitCockpitActivity.class/);
 assert.match(shell,/Button route = cockpitButton/);
 assert.match(shell,/Route berechnen/);
 assert.match(shell,/NavigationDestinationResolver\.resolve/);
 assert.match(shell,/engine\.startConfirmedRoute\(lat, lng, readable\)/);
 assert.match(shell,/engine != null && engine\.configured\(\)/);
 assert.match(java('DestinationStore'),/getSharedPreferences\(LEGACY, Context.MODE_PRIVATE\)/);
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
 // The protected release validator is on main, not on this source branch.
 // Check the no-key Gradle gate here; the signed-release gate stays unchanged.
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
 assert.match(gradle,/if \(!withGoogleNavSdk && googleNavKey\)/);
 assert.match(gradle,/throw new GradleException\('Navigation SDK key must not be used in ordinary releases'\)/);
 assert.doesNotMatch(java('MainActivity'),/new GoogleNavigationEngine/);
 assert.doesNotMatch(nav,/AIza[A-Za-z0-9_-]{30}/);
});

test('v20 keeps primary Spotify controls visible before optional route form in 30 percent pane',()=>{
 const shell=java('IntegratedNavigationActivity');
 assert.match(shell,/routePanel\.setVisibility\(View\.GONE\)/);
 assert.match(shell,/toggleRoutes = cockpitButton\("Ziel \/ Route", true\)/);
 assert.match(shell,/toggleRoutes\.setOnClickListener/);
 assert.ok(shell.indexOf('panel.addView(mediaControls);') < shell.indexOf('panel.addView(routePanel);'));
 assert.ok(shell.indexOf('panel.addView(mediaSkip);') < shell.indexOf('panel.addView(routePanel);'));
 assert.match(shell,/scroll\.smoothScrollTo\(0,/);
 assert.match(shell,/SOFT_INPUT_ADJUST_RESIZE/);
 assert.match(shell,/panel\.addView\(fallback\)/);
 assert.doesNotMatch(shell,/routePanel\.addView\(fallback\)/);
 assert.ok(shell.indexOf('panel.addView(mediaControls);') < shell.indexOf('panel.addView(fallback);'));
});


test('v20 local 20-per-UTC-day spending guard reserves before Google requests',()=>{
 const budget=java('NavigationRequestBudget');
 const engine=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
 assert.match(budget,/MAX_DAILY_REQUESTS = 20/);
 assert.match(budget,/Math\.floorDiv\(System\.currentTimeMillis\(\), MILLIS_PER_DAY\)/);
 assert.match(budget,/currentUtcDay < storedUtcDay/);
 assert.match(budget,/\.putInt\(COUNT, nextCount\)\.commit\(\)/);
 assert.match(budget,/catch \(RuntimeException cannotPersist\)/);
 assert.match(engine,/if \(!NavigationRequestBudget\.reserve\(host\)\)/);
 assert.ok(engine.indexOf('NavigationRequestBudget.reserve(host)') <
    engine.indexOf('navigator.setDestination(waypoint, options)'));
});

test('v20 free-first navigation does not depend on 90-day Cloud trial',()=>{
 const main=java('MainActivity'),split=java('SplitCockpitActivity');
 const shell=java('IntegratedNavigationActivity');
 const start=main.indexOf('private void startEmbeddedMap()');
 const end=main.indexOf('private void startSplitAndMaps()',start);
 assert.ok(start>0 && end>start);
 const launcher=main.slice(start,end);
 assert.match(launcher,/restrictedGoogleSdkAvailable\(\)/);
 assert.match(launcher,/sdkCompiled\s*\?\s*IntegratedNavigationActivity\.class/);
 assert.match(launcher,/: SplitCockpitActivity\.class/);
 assert.doesNotMatch(launcher,/BuildConfig\.NAVSDK_API_KEY|EmbeddedMapActivity\.class/);
 assert.match(split,/GoogleMapsLaunch\.adjacentDrivingIntent\(target\)/);
 assert.match(shell,/Google Maps separat öffnen \(Samsung-Splitscreen\)/);
 assert.match(shell,/panel\.addView\(fallback\)/);
});

test('v20 Samsung App Pair guidance is available without Cloud billing or overlays', () => {
  const split = java('SplitCockpitActivity');
  const rules = java('SplitPaneRules');
  assert.match(split, /70\/30 einmalig als Samsung-App-Paar speichern/);
  assert.match(split, /Trennlinie antippen/);
  assert.match(split, /Startbildschirm oder/);
  assert.match(split, /App-Paar zum gemeinsamen Start/);
  assert.match(split, /showSplitHelp\(\)/);
  assert.match(rules, /als App-Paar speichern/);
  assert.match(split, /GoogleMapsLaunch\.adjacentDrivingIntent\(target\)/);
  assert.doesNotMatch(split, /pendingOverlayFallback|fallbackGraceElapsed/);
});

test('Samsung App Pair does not reopen Google Maps inside an existing split', () => {
  const split = java('SplitCockpitActivity');
  const start = split.indexOf('if (!requested) {');
  const end = split.indexOf('// Early status refresh', start);
  assert.ok(start > 0 && end > start);
  const launch = split.slice(start, end);
  assert.match(launch,/if \(isInMultiWindowMode\(\)\)/);
  assert.match(launch,/updateSplitStatus\(\)/);
  assert.match(launch,/return;/);
  assert.ok(launch.indexOf('if (isInMultiWindowMode())') <
    launch.indexOf('launchMapsAdjacent(routeTarget)'));
  assert.doesNotMatch(launch,/startForegroundService|OverlayService/);
});

test('v21 Google navigation is the primary provider; OSM requires a real ready engine',()=>{
 const policy=java('NavigationProviderPolicy');
 const tests=read('app/src/test/java/com/cobrachris/fahrcockpit/NavigationProviderPolicyTest.java');
 assert.match(policy,/if \(googleSdkConfigured\) return Provider\.GOOGLE_NAVIGATION_SDK/);
 assert.match(policy,/osmNavigatorReady/);
 assert.match(policy,/NAVIGATION_QUOTA_EXHAUSTED/);
 assert.match(policy,/BILLING_OR_AUTHORIZATION_UNAVAILABLE/);
 assert.match(policy,/routeRequiresExplicitApproval/);
 assert.match(tests,/googleIsAlwaysFirstWhenConfigured/);
 assert.match(tests,/osmIsOnlyChosenIfACompleteOsmNavigatorExists/);
 assert.match(tests,/fallbackAfterQuotaOrBillingLossDoesNotReturnToGoogleSdk/);
});

test('v21 interactive fixed 70/30 HTML preview is transparent, interactive and cloud-free',()=>{
 const html=read('demo/fahrcockpit-v21-preview.html');
 assert.match(html,/\.map-pane\{[^}]*flex:7 1 0/);
 assert.match(html,/\.cockpit\{[^}]*flex:3 1 0/);
 assert.match(html,/GOOGLE · STANDARD/);
 assert.match(html,/OPENSTREETMAP · NOTFALL/);
 assert.match(html,/INTERAKTIVE KONZEPTDEMO/);
 assert.match(html,/Keine echte Google- oder OSM-Navigation/);
 assert.match(html,/id="quota"/);
 assert.match(html,/id="billing"/);
 assert.match(html,/id="reset"/);
 assert.match(html,/state\.provider='osm'/);
 assert.match(html,/state\.provider='google'/);
 assert.match(html,/routeLine/);
 assert.doesNotMatch(html,/<script[^>]+src=|api\.google|maps\.googleapis|tile\.openstreetmap|https:\/\/[^\s]*\.js/);
});

test('v21 embedded interactive design preview is local and clearly non-navigational',()=>{
 const main=java('MainActivity');
 const preview=java('NavigationPreviewActivity');
 const manifest=read('app/src/main/AndroidManifest.xml');
 const html=read('app/src/main/assets/fahrcockpit-v21-preview.html');
 assert.match(main,/Feste 70\/30-Vorschau öffnen/);
 assert.match(main,/new Intent\(this, NavigationPreviewActivity\.class\)/);
 assert.match(manifest,/android:name="\.NavigationPreviewActivity"/);
 assert.match(manifest,/android:exported="false"/);
 assert.match(preview,/setAllowFileAccess\(false\)/);
 assert.match(preview,/setAllowContentAccess\(false\)/);
 assert.match(preview,/setDomStorageEnabled\(false\)/);
 assert.match(preview,/setJavaScriptCanOpenWindowsAutomatically\(false\)/);
 assert.match(preview,/loadDataWithBaseURL\("about:blank"/);
 assert.doesNotMatch(preview,/GoogleNavigationEngine|setDestination|startGuidance/);
 assert.match(html,/Keine echte Google- oder OSM-Navigation/);
 assert.doesNotMatch(html,/<script[^>]+src=|maps\.googleapis|tile\.openstreetmap/);
});

test('v21 Google SDK quota/auth failures are typed and shown without fake OSM navigation',()=>{
 const contract=java('NavigationEngine');
 const google=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
 const screen=java('IntegratedNavigationActivity');
 assert.match(contract,/default void onGoogleFailure\(NavigationProviderPolicy\.Failure reason\)/);
 assert.match(google,/NavigationApi\.ErrorCode\.NOT_AUTHORIZED/);
 assert.match(google,/Navigator\.RouteStatus\.QUOTA_CHECK_FAILED/);
 assert.match(google,/NavigationProviderPolicy\.Failure\.NAVIGATION_QUOTA_EXHAUSTED/);
 assert.match(google,/NavigationProviderPolicy\.Failure\.BILLING_OR_AUTHORIZATION_UNAVAILABLE/);
 assert.match(google,/NavigationProviderPolicy\.Failure\.LOCAL_REQUEST_LIMIT/);
 assert.match(screen,/new NavigationEngine\.Status\(\)/);
 assert.match(screen,/showProviderFailure\(reason\)/);
  assert.match(screen,/if \(BuildConfig\.GOOGLE_ONLY\) \{/);
  assert.match(screen,/Es wird keine Ersatzkarte oder neue kostenpflichtige/);
});

test('v21 independent BRouter offline routing bridge validates GeoJSON and never uses Google SDK data',()=>{
 const src=read('app/src/osm/java/com/cobrachris/fahrcockpit/BRouterOfflineRouteClient.java');
 const parser=read('app/src/osm/java/com/cobrachris/fahrcockpit/BRouterRouteGeometry.java');
 const aidl=read('app/src/osm/aidl/btools/routingapp/IBRouterService.aidl');
 const gradle=read('app/build.gradle');
 const manifest=read('app/src/main/AndroidManifest.xml');
 assert.match(src,/getTrackFromParams\(params\)/);
 assert.match(src,/params\.putString\("v", "motorcar"\)/);
 assert.match(src,/params\.putString\("trackFormat", "json"\)/);
 assert.match(src,/btools\.routingapp\.BRouterService/);
 assert.match(src,/new Thread\(\(\) ->/);
 assert.match(src,/Offline-Route nicht verfügbar/);
 assert.match(parser,/FeatureCollection/);
 assert.match(parser,/LineString/);
 assert.match(parser,/Double\.isFinite/);
 assert.match(aidl,/String getTrackFromParams/);
 assert.match(gradle,/withOsmFallback/);
 assert.match(gradle,/org\.maplibre\.gl:android-sdk:/);
 assert.match(manifest,/android:name="btools\.routingapp"/);
 assert.doesNotMatch(src,/GoogleNavigationEngine|NAVSDK_API_KEY|maps\.googleapis/);
});

test('v21 native MapLibre OSM road map preserves Google priority and warns about missing guidance',()=>{
 const screen=java('IntegratedNavigationActivity');
 const map=read('app/src/osm/java/com/cobrachris/fahrcockpit/OsmRoutingPreviewEngine.java');
 assert.match(screen,/optionalEngine\(IntegratedNavigationRules\.ENGINE_CLASS\)/);
 assert.match(screen,/OsmRoutingPreviewEngine/);
 assert.match(screen,/isOsmPreview\(\)/);
 assert.match(screen,/OSM-Abbiegeführung \(nicht für Fahrten freigegeben\)/);
 assert.match(map,/MapLibre\.getInstance\(host\)/);
 assert.match(map,/new MapView\(host\)/);
 assert.match(map,/tiles\.openfreemap\.org\/styles\/liberty/);
 assert.match(map,/BRouterOfflineRouteClient\.route\(host/);
 assert.match(map,/new GeoJsonSource\(SOURCE,\s*route\.geoJson\)/);
 assert.match(map,/OSM-Abbiegeführung im Entwicklungstest aktiv/);
 assert.doesNotMatch(map,/GoogleNavigationEngine|NAVSDK_API_KEY|setDestination\(/);
});

test('v21 restricted Google Navigation SDK key is stored on Android, not checked in',()=>{
 const store=java('NavigationSdkKeyStore');
 const main=java('MainActivity');
 const google=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
 const manifest=read('app/src/main/AndroidManifest.xml');
 assert.match(store,/AndroidKeyStore/);
 assert.match(store,/AES\/GCM\/NoPadding/);
 assert.match(store,/setRandomizedEncryptionRequired\(true\)/);
 assert.match(store,/validKey\(String value\)/);
 assert.match(store,/Arrays\.fill\(plain/);
 assert.match(main,/Class\.forName\(IntegratedNavigationRules\.ENGINE_CLASS\)/);
 assert.match(main,/Google-SDK-Schlüssel lokal hinterlegen/);
 assert.match(main,/NavigationSdkKeyStore\.save\(this,key\)/);
 assert.match(main,/NavigationSdkKeyStore\.clear\(this\)/);
 assert.match(google,/NavigationSdkKeyStore\.read\(host\)/);
 assert.doesNotMatch(store,/Log\.|System\.out|new Intent\(Intent\.ACTION_SEND/);
 assert.match(manifest,/android:allowBackup="false"/);
});

test('v21 BRouter guidance uses official voice hints, validated GPS and local German TTS',()=>{
 const route=read('app/src/osm/java/com/cobrachris/fahrcockpit/BRouterRouteGeometry.java');
 const backend=read('app/src/osm/java/com/cobrachris/fahrcockpit/BRouterOfflineRouteClient.java');
 const guide=read('app/src/osm/java/com/cobrachris/fahrcockpit/OsmTurnGuidance.java');
 const voice=read('app/src/osm/java/com/cobrachris/fahrcockpit/OsmSpeechGuide.java');
 const engine=read('app/src/osm/java/com/cobrachris/fahrcockpit/OsmRoutingPreviewEngine.java');
 assert.match(backend,/putString\("turnInstructionFormat", "osmand"\)/);
 assert.match(route,/getAsJsonArray\("voicehints"\)/);
 assert.match(route,/germanInstruction\(\)/);
 assert.match(guide,/accuracyMeters > 35f/);
 assert.match(guide,/offRouteCount>=3/);
 assert.match(guide,/earlyAnnounced/);
 assert.match(guide,/nearAnnounced/);
 assert.match(voice,/new TextToSpeech\(activity,this\)/);
 assert.match(voice,/Locale\.GERMANY/);
 assert.match(voice,/USAGE_ASSISTANCE_NAVIGATION_GUIDANCE/);
 assert.match(engine,/requestLocationUpdates\(LocationManager\.GPS_PROVIDER,1000L,3f/);
 assert.match(engine,/onGpsLocation\(Location loc\)/);
 assert.match(engine,/calculateFrom\(loc,epoch,true\)/);
 assert.match(engine,/REROUTE_COOLDOWN_MS = 60000L/);
 assert.match(engine,/stopGpsTracking\(\)/);
 assert.match(engine,/voice\.close\(\)/);
 assert.doesNotMatch(guide,/GoogleNavigationEngine|NAVSDK_API_KEY/);
});

test('v22 original App icon selects fixed Google 70/30 only with locally restricted SDK key',()=>{
 const main=java('MainActivity');
 const gradle=read('app/build.gradle');
 const start=main.indexOf('private boolean restrictedGoogleSdkAvailable()');
 const end=main.indexOf('private void startSplitAndMaps()',start);
 assert.ok(start>0&&end>start);
 const selection=main.slice(start,end);
 assert.match(selection,/NavigationSdkKeyStore\.read\(this\)/);
 assert.match(selection,/NavigationSdkKeyStore\.validKey\(NavigationSdkKeyStore\.read\(this\)\)/);
 assert.match(selection,/Class\.forName\(IntegratedNavigationRules\.ENGINE_CLASS\)/);
 assert.match(main,/sdkCompiled\s*\?\s*IntegratedNavigationActivity\.class/);
 assert.match(main,/: SplitCockpitActivity\.class/);
 assert.match(gradle,/versionCode 35/);
 assert.match(gradle,/versionName '0\.9\.33-beta'/);
});

test('v21 OSM turn guidance can be opened for safe manual field tests in a single fixed cockpit',()=>{
 const main=java('MainActivity');
 const screen=java('IntegratedNavigationActivity');
 assert.match(main,/OSM-Abbiegeführung im festen 70\/30 testen/);
 assert.match(main,/IntegratedNavigationActivity\.EXTRA_FORCE_OSM_TEST,true/);
 assert.match(main,/Class\.forName\("com\.cobrachris\.fahrcockpit\.OsmRoutingPreviewEngine"\)/);
 assert.match(screen,/getIntent\(\)\.getBooleanExtra\(EXTRA_FORCE_OSM_TEST,false\)/);
 assert.match(screen,/optionalEngine\(IntegratedNavigationRules\.ENGINE_CLASS\)/);
 assert.match(screen,/OSM-Abbiegeführung \(nicht für Fahrten freigegeben\)/);
});

test('v21 SDK-capable launch stays on local key setup rather than opening unwanted Android split',()=>{
 const main=java('MainActivity');
 assert.match(main,/if \(sdkCompiled && !restrictedGoogleSdkAvailable\(\)\)/);
 assert.match(main,/startActivity\(new Intent\(this, sdkCompiled/);
 assert.match(main,/embedded\.setOnClickListener\(v -> startSplitAndMaps\(\)\)/);
 assert.match(main,/Google Navigation SDK vorhanden/);
});


test('v21 permanent fixed 70/30 has persisted Spotify left-right switching',()=>{
  const screen=java('IntegratedNavigationActivity');
  const prefs=java('CockpitPanePreferences');
  const html=read('demo/fahrcockpit-v21-preview.html');
  const asset=read('app/src/main/assets/fahrcockpit-v21-preview.html');
  assert.match(screen,/CockpitPanePreferences\.spotifyLeft\(this\)/);
  assert.match(screen,/CockpitPanePreferences\.saveSpotifyLeft\(this, newLeft\)/);
  assert.match(screen,/if \(landscape && spotifyLeft\)/);
  assert.match(screen,/root\.addView\(cockpitPane, controlParams\)/);
  assert.match(screen,/root\.addView\(mapPane, mapParams\)/);
  assert.match(screen,/root\.removeView\(mapPane\)/);
  assert.match(screen,/root\.removeView\(cockpitPane\)/);
  assert.doesNotMatch(screen,/sideToggle\.setOnClickListener\(v -> recreate\(\)/);
  assert.match(prefs,/getSharedPreferences\(PREFS, Context\.MODE_PRIVATE\)/);
  assert.match(prefs,/putBoolean\(KEY_SPOTIFY_LEFT, left\)\.commit\(\)/);
  assert.match(html,/classList\.toggle\('spotify-left',state\.spotifyLeft\)/);
  assert.match(html,/SIDE_KEY='fahrcockpit-demo-spotify-left-v1'/);
  assert.match(html,/localStorage\.setItem\(SIDE_KEY/);
  assert.match(html,/\.app\.spotify-left\{flex-direction:row-reverse\}/);
  assert.match(html,/sideSwitch/);
  assert.strictEqual(html,asset);
});

test('v21 signed Google-only release rejects legacy visual preview or OSM compilation',()=>{
 const gradle=read('app/build.gradle');
 const ci=read('../../.github/workflows/fahrcockpit-navsdk-compile.yml');
 assert.match(gradle,/if \(googleOnly && \(withOsmFallback \|\| visualPreview\)\)/);
 assert.match(ci,/-PwithGoogleNavSdk=true -PgoogleOnly=true/);
 assert.doesNotMatch(ci,/-PwithOsmFallback=true|-PvisualPreview=true/);
});


test('standalone v21 preview never exposes Samsung split controls and survives activity restoration',()=>{
 const main=java('MainActivity');
 const start=main.indexOf('if (BuildConfig.VISUAL_PREVIEW) {', main.indexOf('setContentView(scroll)'));
 const legacy=main.indexOf('Ui.section(this, root, "Dauerhaft kostenlose Navigation"',start);
 assert.ok(start>0 && legacy>start);
 const separate=main.slice(start,legacy);
 assert.match(separate,/Festes 70\/30-Cockpit · kein Samsung-Splitscreen/);
 assert.match(separate,/Festes 70\/30-Cockpit öffnen/);
 assert.match(separate,/return;\s*\}\s*root\.addView\(Ui\.text/);
 assert.match(separate,/IntegratedNavigationActivity\.EXTRA_FORCE_OSM_TEST,true/);
 assert.doesNotMatch(separate,/Google Maps \+ Spotify kostenlos starten|startSplitAndMaps\(\)/);
 assert.doesNotMatch(main,/BuildConfig\.VISUAL_PREVIEW && savedInstanceState == null/);
});

test('v21 landscape still 70/30 while portrait keeps Spotify usable; OSM centers on Android GPS',()=>{
 const screen=java('IntegratedNavigationActivity');
 const policy=java('CockpitLayoutRules');
 const center=java('MapCenterRules');
 const osm=read('app/src/osm/java/com/cobrachris/fahrcockpit/OsmRoutingPreviewEngine.java');
 assert.match(policy,/PORTRAIT_MAP_WEIGHT = 12/);
 assert.match(policy,/PORTRAIT_CONTROLS_WEIGHT = 8/);
 assert.match(policy,/IntegratedNavigationRules.MAP_WEIGHT/);
 assert.match(policy,/IntegratedNavigationRules.CONTROLS_WEIGHT/);
 assert.match(screen,/CockpitLayoutRules.mapWeight\(false\)/);
 assert.match(screen,/CockpitLayoutRules.controlsWeight\(false\)/);
 assert.match(screen,/sideToggle.setVisibility\(landscape \? View.VISIBLE : View.GONE\)/);
 assert.match(osm,/◎ Meine Position/);
 assert.match(osm,/centerOnAndroidPosition\(false\)/);
 assert.match(osm,/centerOnAndroidPosition\(true\)/);
 assert.match(osm,/getLastKnownLocation\(provider\)/);
 assert.match(osm,/getCurrentLocation\(provider, centerCancellation/);
 assert.match(osm,/new CircleLayer\(USER_LAYER,USER_SOURCE\)/);
 assert.match(osm,/cancelCenterRequest\(\)/);
 assert.match(osm,/MapCenterRules.acceptable\(/);
 assert.match(center,/MAX_AGE_MS = 180_000L/);
 assert.match(center,/MAX_ACCURACY_METERS = 250f/);
 assert.doesNotMatch(osm,/GoogleNavigationEngine|NAVSDK_API_KEY/);
});
test('v21 OSM preview cannot appear to be the Google SDK product',()=>{
 const shell=java('IntegratedNavigationActivity');
 const manifest=read('app/src/main/AndroidManifest.xml');
 const gradle=read('app/build.gradle');
 assert.match(shell,/FahrCockpit · OSM-Test/);
 assert.match(shell,/Die fest integrierte Google-Navigation bleibt das Hauptziel/);
 assert.match(shell,/Hier ist kein Google-SDK enthalten/);
 assert.match(shell,/Google Maps separat öffnen \(Samsung-Splitscreen\)/);
 assert.match(gradle,/if \(visualPreview && \(!withOsmFallback \|\| withGoogleNavSdk\)\)/);
 assert.match(manifest,/\$\{appDisplayName\}/);
});


test('v21 release configuration fails closed to native Google SDK only',()=>{
 const gradle=read('app/build.gradle');
 const main=java('MainActivity');
 const nav=java('IntegratedNavigationActivity');
 const setup=java('GoogleSetupActivity');
 const manifest=read('app/src/main/AndroidManifest.xml');
 const ci=read('../../.github/workflows/fahrcockpit-navsdk-compile.yml');
 assert.match(gradle,/def googleOnly = providers.gradleProperty\('googleOnly'\)/);
 assert.match(gradle,/if \(googleOnly && \(withOsmFallback \|\| visualPreview\)\)/);
 assert.match(gradle,/buildConfigField 'boolean', 'GOOGLE_ONLY'/);
 assert.match(ci,/-PwithGoogleNavSdk=true -PgoogleOnly=true/);
 assert.doesNotMatch(ci,/-PwithOsmFallback=true/);
 assert.match(main,/if \(BuildConfig.GOOGLE_ONLY\) \{/);
 assert.match(main,/\? IntegratedNavigationActivity.class/);
 assert.match(main,/: GoogleSetupActivity.class/);
 assert.match(nav,/if \(BuildConfig.GOOGLE_ONLY\) \{/);
 assert.match(nav,/return optionalEngine\(IntegratedNavigationRules.ENGINE_CLASS\)/);
 assert.match(nav,/if \(!BuildConfig.GOOGLE_ONLY\) \{/);
 assert.match(nav,/startActivity\(new Intent\(this, GoogleSetupActivity.class\)\)/);
 assert.match(setup,/NavigationSdkKeyStore.save\(this,value\)/);
 assert.match(setup,/NavigationSdkKeyStore.clear\(this\)/);
 assert.match(setup,/Festes Google-70\/30-Cockpit öffnen/);
 assert.doesNotMatch(setup,/OsmRoutingPreviewEngine|SplitCockpitActivity|withOsmFallback/);
 assert.match(manifest,/android:name=".GoogleSetupActivity"/);
});
test('v21 Google SDK errors and transferred deep links never expose alternate maps',()=>{
 const main=java('MainActivity');
 const nav=java('IntegratedNavigationActivity');
 const setup=java('GoogleSetupActivity');
 assert.match(main,/if \(BuildConfig\.GOOGLE_ONLY\) \{/);
 const start=main.indexOf('if (BuildConfig.GOOGLE_ONLY) {');
 const legacy=main.indexOf('getWindow().setStatusBarColor',start);
 assert.ok(start>0&&legacy>start);
 const nativeGate=main.slice(start,legacy);
 assert.match(nativeGate,/\? IntegratedNavigationActivity.class/);
 assert.match(nativeGate,/: GoogleSetupActivity.class/);
 assert.doesNotMatch(nativeGate,/GoogleKeyTransfer\.isTransfer|SplitCockpitActivity|OsmRoutingPreviewEngine/);
 assert.match(nav,/if \(BuildConfig\.GOOGLE_ONLY\) \{\s*\/\/ Never fall back/);
 assert.match(nav,/Es wird keine Ersatzkarte oder neue kostenpflichtige/);
 assert.match(nav,/Google Navigation SDK noch nicht eingerichtet/);
 assert.doesNotMatch(setup,/OsmRoutingPreviewEngine|SplitCockpitActivity/);
});



test('v22 Google-only candidate preserves package and build identity',()=>{
 const gradle=read('app/build.gradle');
 assert.match(gradle,/applicationId 'com\.cobrachris\.fahrcockpit'/);
 assert.match(gradle,/versionCode 35/);
 assert.match(gradle,/versionName '0\.9\.33-beta'/);
 assert.match(gradle,/if \(googleOnly && \(withOsmFallback \|\| visualPreview\)\)/);
 const ci=read('../../.github/workflows/fahrcockpit-navsdk-compile.yml');
 assert.match(ci,/-PwithGoogleNavSdk=true -PgoogleOnly=true/);
 assert.match(ci,/versionCode'\]==35/);
 assert.doesNotMatch(ci,/pull_request_target/);
});

test('v22 only holds the screen awake during active Google guidance',()=>{
 const activity=java('IntegratedNavigationActivity');
 const engine=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
 assert.match(activity,/onGuidanceStateChanged\(boolean active\)/);
 assert.match(activity,/FLAG_KEEP_SCREEN_ON/);
 assert.match(activity,/if \(routeActive\)/);
 assert.match(activity,/getWindow\(\)\.clearFlags\(WindowManager\.LayoutParams\.FLAG_KEEP_SCREEN_ON\)/);
 assert.match(engine,/navigator\.isGuidanceRunning\(\)/);
 assert.match(engine,/status\.onGuidanceStateChanged\(false\)/);
});

test('v22 mute affects Google guidance speech, not Spotify or routing',()=>{
 const engine=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
 const activity=java('IntegratedNavigationActivity');
 const voice=java('NavigationVoicePreferences');
 assert.doesNotMatch(engine,/Navigator\\.AudioGuidance\\.VIBRATION/);
 assert.match(engine,/navigator\.setAudioGuidance\(mode\)/);
 assert.match(engine,/NavigationVoicePreferences\.isMuted\(host\)/);
 assert.match(activity,/voiceButton\.setOnClickListener/);
 assert.match(activity,/engine\.setVoiceMuted\(muted\)/);
 assert.match(voice,/buttonLabel\(boolean muted\)/);
 assert.doesNotMatch(voice,/SpotifyMedia/);
});

test('v22 fuel handoff requires confirmation before Google route request',()=>{
 const activity=java('IntegratedNavigationActivity');
 const fuel=java('FuelActivity');
 assert.match(activity,/new Intent\(this, FuelActivity\.class\)/);
 assert.match(activity,/FUEL_PICK_REQUEST/);
 assert.match(activity,/setPositiveButton\("Route berechnen"/);
 assert.match(activity,/engine\.startConfirmedRoute\(lat, lng, chosenName\)/);
 assert.match(fuel,/setResult\(RESULT_OK, result\)/);
 assert.match(fuel,/String coordinates = FuelRules\.stationCoordinates\(lat, lng\)/);
 assert.match(fuel,/if \(returnToCockpit\)/);
});

test('v23 favorites and local address prediction retain v22 navigation without paid Places',()=>{
  const shell=java('IntegratedNavigationActivity');
  const store=java('DestinationStore');
  const bridge=java('PlacesAutocompleteBridge');
  const gradle=read('app/build.gradle');
  const rules=java('DestinationMatchRules');
  const geo=java('AddressSuggestionResolver');
  const nav=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  assert.match(gradle,/versionCode 35/);
  assert.match(gradle,/versionName '0\.9\.33-beta'/);
  assert.match(gradle,/GOOGLE_PLACES_ACTIVATED', 'false'/);
  assert.match(gradle,/WITH_GOOGLE_PLACES/);
  assert.match(bridge,/if \(!BuildConfig\.GOOGLE_PLACES_ACTIVATED \|\| !BuildConfig\.WITH_GOOGLE_PLACES\)/);
  assert.match(shell,/tools\.addView\(voiceButton, new LinearLayout\.LayoutParams\(/);
  assert.match(shell,/panel\.addView\(tools\)/);
  assert.doesNotMatch(shell,/mapPane\.addView\(voiceButton/);
  assert.match(shell,/DestinationStore\.recordRecent/);
  assert.match(shell,/DestinationStore\.saveFavorite/);
  assert.match(shell,/DestinationStore\.removeManaged/);
  assert.match(shell,/AddressSuggestionResolver\.suggest/);
  assert.match(shell,/postDelayed\(pendingSuggestions, 650L\)/);
  assert.match(geo,/Geocoder\.GeocodeListener/);
  assert.match(store,/fahrcockpit_manual_routes_v1/);
  assert.match(store,/fahrcockpit_destinations_v23/);
  assert.match(store,/static boolean removeFavorite/);
  assert.match(store,/static boolean removeManaged/);
  assert.match(rules,/maySuggestOnline/);
  assert.match(nav,/NavigationRequestBudget\.reserve\(host\)/);
  assert.doesNotMatch(shell,/Places\.initializeWithNewPlacesApiEnabled/);
});


test('v24 speaker is monochrome icon-only, never a Samsung emoji or text button',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const icon=java('NavigationAudioIcon');
  const voices=java('NavigationVoicePreferences');
  assert.match(cockpit,/new ImageButton\(this\)/);
  assert.match(cockpit,/voiceButton\.setImageDrawable\(new NavigationAudioIcon\(muted\)\)/);
  assert.match(cockpit,/voiceButton\.setOnClickListener\(v ->/);
  assert.match(cockpit,/engine\.setVoiceMuted\(muted\)/);
  assert.match(cockpit,/tools\.addView\(voiceButton, new LinearLayout\.LayoutParams\(/);
  assert.match(cockpit,/panel\.addView\(tools\)/);
  assert.doesNotMatch(cockpit,/mapPane\.addView\(voiceButton/);
  assert.doesNotMatch(cockpit,/voiceButton\.setText\(/);
  assert.doesNotMatch(icon,/\uD83D\uDD0A|\uD83D\uDD07|Emoji/);
  assert.match(icon,/if \(muted\)/);
  assert.match(icon,/waves\.lineTo\(20\.8f, 20\.8f\)/);
  assert.match(icon,/canvas\.drawPath\(waves, paint\)/);
  assert.doesNotMatch(voices,/SpotifyMedia/);
});

test('v24 custom favorite editor keeps a nickname separate from actual route target',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const store=java('DestinationStore');
  const match=java('DestinationMatchRules');
  assert.match(cockpit,/showFavoriteEditor\(null, "", target\)/);
  assert.match(cockpit,/Name des Ziels/);
  assert.match(cockpit,/Adresse oder Koordinaten/);
  assert.match(cockpit,/z\. B\. Tante Emma oder Onkel Klaus/);
  assert.match(cockpit,/DestinationStore\.updateFavorite\(this, previousTarget, label, target\)/);
  assert.match(cockpit,/applyDestinationSuggestion\(label, target\)/);
  assert.match(cockpit,/selectedNavigationTarget\(\)/);
  assert.match(store,/static boolean updateFavorite\(/);
  assert.match(store,/if \(original == -1\) return false/);
  assert.match(store,/return write\(context, "fav_", FAVORITES, entries\)/);
  assert.match(match,/sameTarget\(/);
});

test('v24 route menu collapses during active guidance while remaining reversible',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(cockpit,/onGuidanceStateChanged\(boolean active\)/);
  assert.match(cockpit,/if \(active\) \{\s*stopSpeedUpdates\(\);\s*collapseDestinationMenu\(\);/);
  assert.match(cockpit,/private void collapseDestinationMenu\(\)/);
  assert.match(cockpit,/routePanel\.setVisibility\(View\.GONE\)/);
  assert.match(cockpit,/toggleRoutes\.setOnClickListener/);
  assert.match(cockpit,/destinationActions\.addView\(remembered/);
  assert.match(cockpit,/destinationActions\.addView\(saveDestination/);
  assert.match(cockpit,/\.setPositiveButton\("Straßenroute planen"/);
  assert.doesNotMatch(cockpit,/Nur nach Freigabe der Abrechnung fortfahren|Google Navigation: Route bestätigen/);
});

test('v24 portrait gives Google ETA extra room, landscape remains 70/30',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const layout=java('CockpitLayoutRules');
  const split=java('IntegratedNavigationRules');
  assert.match(layout,/PORTRAIT_MAP_WEIGHT = 12/);
  assert.match(layout,/PORTRAIT_CONTROLS_WEIGHT = 8/);
  assert.match(split,/MAP_WEIGHT = 7/);
  assert.match(split,/CONTROLS_WEIGHT = 3/);
  assert.match(cockpit,/CockpitLayoutRules\.mapWeight\(false\)/);
  assert.match(cockpit,/CockpitLayoutRules\.controlsWeight\(false\)/);
  assert.match(cockpit,/CockpitLayoutRules\.safeBottomInset\(bars\.bottom, keyboard\.bottom\)/);
  assert.doesNotMatch(cockpit,/Google: Das SDK benötigt aktives Cloud Billing/);
});


test('v25 Saphe probe is opt-in and reads only the exact Android package name',()=>{
  const listener=java('SpotifyNotificationListener');
  const rules=java('SapheNotificationRules');
  const probe=java('SapheNotificationProbe');
  const ui=java('IntegratedNavigationActivity');
  const manifest=read('app/src/main/AndroidManifest.xml');
  const gradle=read('app/build.gradle');
  assert.match(listener,/onNotificationPosted\(StatusBarNotification notification\)/);
  assert.match(listener,/notification\.getPackageName\(\)/);
  assert.match(listener,/SapheNotificationProbe\.onNotificationPosted/);
  assert.doesNotMatch(listener,/getNotification\(\)|getKey\(\)|getId\(\)|getTag\(\)/);
  assert.match(rules,/WarningSource\.SAPHE_PACKAGE\.equals\(packageName\)/);
  assert.match(rules,/optedIn && WarningSource/);
  assert.match(probe,/if \(!SapheNotificationRules\.shouldRecord/);
  assert.match(probe,/putLong\(KEY_LAST_EVENT, System\.currentTimeMillis\(\)\)/);
  assert.match(probe,/return prefs\(ctx\)\.getBoolean\(KEY_ENABLED, false\)/);
  assert.doesNotMatch(probe,/BluetoothGatt|Network|HttpURLConnection|Notification\.EXTRA|getExtras\(\)/);
  assert.match(ui,/sapheCheck\.setOnClickListener\(v -> showSapheProbeDialog\(\)\)/);
  assert.match(ui,/showSapheBanner\(true\)/);
  assert.match(ui,/SapheNotificationProbe\.lastObservedAt\(this\)/);
  assert.match(ui,/Safety-Ereignis · Typ nicht verifiziert/);
  assert.match(ui,/keine echte Warnung/);
  assert.match(ui,/Settings\.ACTION_NOTIFICATION_LISTENER_SETTINGS/);
  assert.match(ui,/engine\.startConfirmedRoute\(lat, lng, readable\)/);
  assert.match(manifest,/FahrCockpit Spotify &amp; Saphe-Test/);
  assert.match(gradle,/versionCode 35/);
  assert.match(gradle,/versionName '0\.9\.33-beta'/);
  assert.match(gradle,/GOOGLE_PLACES_ACTIVATED', 'false'/);
});


test('v27 compact cockpit keeps navigation voice accessible without masking Google map',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const layout=java('CockpitLayoutRules');
  const weights=java('IntegratedNavigationRules');
  assert.match(cockpit,/tools\.addView\(voiceButton, new LinearLayout\.LayoutParams\(/);
  assert.match(cockpit,/panel\.addView\(tools\)/);
  assert.doesNotMatch(cockpit,/mapPane\.addView\(voiceButton/);
  assert.match(cockpit,/voiceButton\.setOnClickListener\(v ->/);
  assert.match(cockpit,/engine\.setVoiceMuted\(muted\)/);
  assert.match(cockpit,/sideToggle\.setContentDescription\(/);
  assert.match(cockpit,/CockpitPanePreferences\.saveSpotifyLeft\(this, newLeft\)/);
  assert.match(cockpit,/status\.setVisibility\(routine \? View\.GONE : View\.VISIBLE\)/);
  assert.match(cockpit,/CockpitLayoutRules\.mapWeight\(false\)/);
  assert.match(cockpit,/CockpitLayoutRules\.controlsWeight\(false\)/);
  assert.match(layout,/PORTRAIT_MAP_WEIGHT = 12/);
  assert.match(weights,/MAP_WEIGHT = 7/);
  assert.match(weights,/CONTROLS_WEIGHT = 3/);
});


test('v28 Tankerkoenig map prices require explicit search and never start billable routes',()=>{
  const activity=java('IntegratedNavigationActivity');
  const fuel=java('FuelActivity');
  const cache=java('FuelMapSnapshot');
  const bridge=java('NavigationEngine');
  const engine=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  const marker=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleFuelPriceMarkers.java');
  const gradle=read('app/build.gradle');
  assert.match(gradle,/versionCode 35/);
  assert.match(gradle,/versionName '0\.9\.33-beta'/);
  assert.match(fuel,/FuelMapSnapshot\.save\(this, stations, selectedFuelType\)/);
  assert.match(fuel,/String\.format\(Locale\.GERMANY, "%.2f €\/l", price\)/);
  assert.match(cache,/MAX_AGE_MS = 15 \* 60 \* 1000L/);
  assert.match(cache,/MAX_STATIONS = 40/);
  assert.match(cache,/context\.getSharedPreferences\(PREFS, Context\.MODE_PRIVATE\)/);
  assert.doesNotMatch(cache,/apiKey|tankerkoenig_local_key|requestUrl|HttpURLConnection/);
  assert.match(activity,/engine\.showFuelPrices\(displayed,/);
  assert.match(activity,/station -> acceptFuelStation\(station\.lat, station\.lng, station\.name\)/);
  assert.match(activity,/setPositiveButton\("Route berechnen"/);
  assert.match(activity,/engine\.startConfirmedRoute\(lat, lng, chosenName\)/);
  assert.match(bridge,/default void showFuelPrices\(FuelMapSnapshot\.Snapshot snapshot/);
  assert.match(engine,/map\.getMapAsync\(googleMap ->/);
  assert.match(marker,/active\.addMarker\(new MarkerOptions\(\)/);
  assert.match(marker,/active\.setOnInfoWindowClickListener\(marker ->/);
  assert.match(marker,/marker\.setTag\(s\)/);
  assert.match(cache,/String\.format\(Locale\.GERMANY, "%.2f €", price\)/);
  assert.doesNotMatch(marker,/setDestination\(|startGuidance\(|Places\.|HttpURLConnection|new NavigationView|new MapView/);
});


test('v29 nearby fuel search recovers from unavailable GPS through network provider',()=>{
  const fuel=java('FuelActivity');
  assert.match(fuel,/manager\.isProviderEnabled\(\s*LocationManager\.NETWORK_PROVIDER\)/);
  assert.match(fuel,/manager\.isProviderEnabled\(\s*LocationManager\.GPS_PROVIDER\)/);
  assert.match(fuel,/recentLocation\(manager\.getLastKnownLocation\(\s*LocationManager\.NETWORK_PROVIDER\)\)/);
  assert.match(fuel,/recentLocation\(manager\.getLastKnownLocation\(\s*LocationManager\.GPS_PROVIDER\)\)/);
  assert.match(fuel,/getCurrentLocation\(LocationManager\.NETWORK_PROVIDER/);
  assert.match(fuel,/getCurrentLocation\(LocationManager\.GPS_PROVIDER/);
  assert.match(fuel,/onLocationCandidate\(generation, location, key\)/);
  assert.match(fuel,/if \(generation != locationGeneration/);
  assert.match(fuel,/pendingLocationProviders <= 0/);
  assert.match(fuel,/stopLocationRequests\(\)/);
  assert.doesNotMatch(fuel,/locationSignal = new CancellationSignal/);
  assert.match(fuel,/FuelMapSnapshot\.save\(this, stations, selectedFuelType\)/);
  assert.match(fuel,/String\.format\(Locale\.GERMANY, "%.2f €\/l", price\)/);
  // The key stays in private app storage; only a manual search can call API.
  assert.match(fuel,/search\.setOnClickListener\(v -> search\(\)\)/);
  assert.doesNotMatch(fuel,/requestLocationUpdates\(/);
});


test('v29 direct fuel overlay is one tap, explicitly refreshed, and reuses saved settings',()=>{
  const activity=java('IntegratedNavigationActivity');
  const quick=java('FuelQuickSearch');
  const settings=java('FuelQuickSettings');
  const fuel=java('FuelActivity');
  assert.match(activity,/fuelButton = cockpitButton\("Preise anzeigen", false\)/);
  assert.match(activity,/fuelButton\.setOnClickListener\(v ->/);
  assert.match(activity,/fuelRefreshButton\.setOnClickListener\(v ->/);
  assert.match(activity,/startQuickFuelSearch\(\)/);
  assert.match(activity,/FuelMapSnapshot\.read\(this\)/);
  assert.match(activity,/engine\.showFuelPrices\(displayed,/);
  assert.match(activity,/fuelMarkersVisible = false/);
  assert.match(activity,/FuelMapSnapshot\.MAX_AGE_MS/);
  assert.match(activity,/new Intent\(this, FuelActivity\.class\)/);
  assert.match(quick,/getString\("tankerkoenig_local_key"/);
  assert.match(quick,/FuelRules\.validKey\(key\)/);
  assert.match(quick,/FuelRules\.requestUrl\(key/);
  assert.match(quick,/getCurrentLocation\(LocationManager\.NETWORK_PROVIDER/);
  assert.match(quick,/getCurrentLocation\(LocationManager\.GPS_PROVIDER/);
  assert.match(quick,/locationResolved = true/);
  assert.match(quick,/if \(token != generation \|\| !searching \|\| locationResolved\) return/);
  assert.match(quick,/FuelMapSnapshot\.save\(host, stations, type\)/);
  assert.doesNotMatch(quick,/startGuidance\(|setDestination\(|Places\.|requestLocationUpdates\(/);
  assert.match(settings,/fuel_type_index/);
  assert.match(settings,/fuel_radius_index/);
  assert.match(settings,/fuel_sort_index/);
  assert.match(fuel,/FuelQuickSettings\.saveType\(FuelActivity\.this, position\)/);
  assert.match(fuel,/FuelQuickSettings\.saveRadius\(FuelActivity\.this, position\)/);
  assert.match(fuel,/FuelQuickSettings\.saveSort\(FuelActivity\.this, position\)/);
});

test('v29 idle Spotify panel keeps launch action while hiding disabled controls',()=>{
  const activity=java('IntegratedNavigationActivity');
  assert.match(activity,/boolean showMedia = state\.connected/);
  assert.match(activity,/mediaControls\.setVisibility\(showMedia \? View\.VISIBLE : View\.GONE\)/);
  assert.match(activity,/mediaSkip\.setVisibility\(showMedia \? View\.VISIBLE : View\.GONE\)/);
  assert.match(activity,/spotifyHeader\.setVisibility\(showMedia \? View\.VISIBLE : View\.GONE\)/);
  assert.match(activity,/spotifySetup\.setVisibility\(nextAction == null \? View\.GONE : View\.VISIBLE\)/);
  assert.match(activity,/spotifySetup\.setOnClickListener\(v -> openSpotifyOrPermissions\(\)\)/);
});


test('v31 Saphe diagnostic is ten-minute opt-in and never reads payload or Bluetooth',()=>{
  const state=java('SapheDiagnosticSession');
  const listener=java('SpotifyNotificationListener');
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(state,/DURATION_MS = 10L \* 60 \* 1000L/);
  assert.match(state,/now >= start && now < ends/);
  assert.match(state,/WarningSource\.SAPHE_PACKAGE\.equals\(packageName\)/);
  assert.match(state,/c\.getPackageName\(\)\.equals\(packageName\)/);
  assert.match(state,/Math\.min\(9999,/);
  assert.match(state,/putLong\(LAST_SAPHE, now\)/);
  assert.match(state,/putLong\(TEST_SEEN, now\)/);
  assert.doesNotMatch(state,/getNotification\(|getExtras\(|BluetoothGatt|HttpURLConnection/);
  assert.match(listener,/onListenerConnected\(\)/);
  assert.match(listener,/SapheDiagnosticSession\.onNotification\(this, notification\.getPackageName\(\)/);
  assert.doesNotMatch(listener,/getNotification\(|getKey\(|getId\(|getTag\(/);
  assert.match(cockpit,/SapheDiagnosticSession\.start\(this, System\.currentTimeMillis\(\)\)/);
  assert.match(cockpit,/SapheDiagnosticSession\.stop\(this\)/);
  assert.match(cockpit,/SapheDiagnosticSession\.sapheCount\(this\)/);
  assert.match(cockpit,/SapheDiagnosticSession\.lastSapheAt\(this\)/);
  assert.match(cockpit,/SapheDiagnosticSession\.selfTestSeen\(this\)/);
  assert.match(cockpit,/manager\.notify\(31031, notification\)/);
  assert.match(cockpit,/setDefaults\(0\)/);
  assert.match(cockpit,/10 Minuten starten/);
  assert.match(cockpit,/Testanzeige/);
  assert.match(cockpit,/keine echte Warnung/);
});

test('v31 Google voice defaults off and routine guidance haptics are omitted',()=>{
  const prefs=java('NavigationVoicePreferences');
  const engine=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(prefs,/getBoolean\(KEY_MUTED, true\)/);
  assert.match(prefs,/putBoolean\(KEY_MUTED, muted\)/);
  assert.match(engine,/NavigationVoicePreferences\.isMuted\(host\)/);
  assert.match(engine,/Navigator\.AudioGuidance\.VOICE_ALERTS_AND_GUIDANCE/);
  assert.match(engine,/Navigator\.AudioGuidance\.BLUETOOTH_AUDIO/);
  assert.doesNotMatch(engine,/Navigator\.AudioGuidance\.VIBRATION/);
  assert.match(engine,/navigator\.setAudioGuidance\(mode\)/);
  assert.match(cockpit,/engine\.setVoiceMuted\(muted\)/);
});

test('v33 neutral safety prototype retains Saphe events with visible timed map banner',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const gradle=read('app/build.gradle');
  assert.match(gradle,/versionCode 35/);
  assert.match(gradle,/versionName '0\.9\.33-beta'/);
  assert.match(cockpit,/SAFETY_COOLDOWN_MS = 10000L/);
  assert.match(cockpit,/SapheDiagnosticSession\.lastSapheAt\(this\)/);
  assert.match(cockpit,/SapheNotificationProbe\.lastObservedAt\(this\)/);
  assert.match(cockpit,/Math\.max\(observedAt, diagnosticAt\)/);
  assert.match(cockpit,/title = simulated \? "TEST · ACHTUNG" : "ACHTUNG"/);
  assert.match(cockpit,/Safety-Ereignis · Typ nicht verifiziert/);
  assert.match(cockpit,/handler\.postDelayed\(hideSapheBanner, 3200L\)/);
  assert.match(cockpit,/safetyEventCount\+\+/);
  assert.match(cockpit,/showSapheBanner\(true\)/);
  assert.doesNotMatch(cockpit,/sapheBanner\.setText\("Blitzer"/);
});

test('v33 Google-only navigation enables native speed and speed limits without extra APIs',()=>{
  const engine=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  const manifest=read('app/src/main/AndroidManifest.xml');
  assert.match(engine,/map\.setSpeedometerEnabled\(true\)/);
  assert.match(engine,/map\.setSpeedLimitIconEnabled\(true\)/);
  assert.match(engine,/map\.onCreate\(saved\)/);
  assert.ok(engine.indexOf('map.onCreate(saved)') < engine.indexOf('map.setSpeedometerEnabled(true)'));
  assert.doesNotMatch(engine,/SpeedLimitsApi|RoadsApi|SpeedLimitLookup|requestSpeedLimit/);
  assert.doesNotMatch(manifest,/ACCESS_BACKGROUND_LOCATION/);
});

test('v33 sticky cockpit speed clears stale GPS readings and stops location on pause',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(cockpit,/cockpitPane\.addView\(currentSpeedView/);
  assert.match(cockpit,/cockpitPane\.addView\(scroll/);
  assert.match(cockpit,/root\.addView\(cockpitPane, controlParams\)/);
  assert.match(cockpit,/currentSpeedView\.setText\("GPS — km\/h"\)/);
  assert.match(cockpit,/location\.hasSpeed\(\)/);
  assert.match(cockpit,/location\.hasAccuracy\(\)/);
  assert.match(cockpit,/location\.getAccuracy\(\) > 80f/);
  assert.match(cockpit,/location\.getElapsedRealtimeNanos\(\)/);
  assert.match(cockpit,/location\.getSpeedAccuracyMetersPerSecond\(\) > 8f/);
  assert.match(cockpit,/Math\.round\(metersPerSecond \* 3\.6f\)/);
  assert.match(cockpit,/handler\.postDelayed\(expireSpeedReading, 8000L\)/);
  assert.match(cockpit,/manager\.requestLocationUpdates\(LocationManager\.GPS_PROVIDER/);
  assert.match(cockpit,/manager\.requestLocationUpdates\(LocationManager\.NETWORK_PROVIDER/);
  assert.match(cockpit,/speedLocationManager\.removeUpdates\(speedLocationListener\)/);
  assert.match(cockpit,/onPause\(\) \{\s*stopSpeedUpdates\(\)/);
  assert.doesNotMatch(cockpit,/getLastKnownLocation\(/);
});

test('v33 large red Safety alert lasts 3.2 seconds without flashing or claiming blitzer identity',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(cockpit,/sapheBanner = Ui\.text\(this, "", 28, 0xffffffff, true\)/);
  assert.match(cockpit,/sapheBanner\.setBackground\(Ui\.background\(this, 0xffc91921, 12\)\)/);
  assert.match(cockpit,/handler\.postDelayed\(hideSapheBanner, 3200L\)/);
  assert.match(cockpit,/Safety-Ereignis · Typ nicht verifiziert/);
  assert.match(cockpit,/SAFETY_COOLDOWN_MS = 10000L/);
  assert.doesNotMatch(cockpit,/ObjectAnimator|ValueAnimator|AnimationUtils/);
});


test('v34 only shows native Google speedometer during guidance and GPS fallback otherwise',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const google=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  assert.match(google,/map\.setSpeedometerEnabled\(true\)/);
  assert.match(google,/map\.setSpeedLimitIconEnabled\(true\)/);
  assert.match(cockpit,/navigationGuidanceActive = active/);
  assert.match(cockpit,/currentSpeedView\.setVisibility\(\s*active \? View\.GONE : View\.VISIBLE\)/);
  assert.match(cockpit,/if \(active\) \{\s*stopSpeedUpdates\(\);\s*collapseDestinationMenu\(\);\s*\} else \{\s*startSpeedUpdates\(\);/);
  assert.match(cockpit,/if \(navigationGuidanceActive \|\| currentSpeedView == null/);
  assert.match(cockpit,/stopSpeedUpdates\(\)/);
  assert.match(cockpit,/showUnknownSpeed\(\)/);
});

test('v34 removes SDK billing dialog while retaining user-triggered bounded routes',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const google=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  assert.match(cockpit,/route\.setOnClickListener\(v -> resolveAndStartRoute\(\)\)/);
  assert.match(cockpit,/private void resolveAndStartRoute\(\)/);
  assert.match(cockpit,/NavigationDestinationResolver\.resolve\(this, target/);
  assert.match(cockpit,/!target\.equals\(selectedNavigationTarget\(\)\)/);
  assert.match(cockpit,/engine\.startConfirmedRoute\(lat, lng, readable\)/);
  assert.doesNotMatch(cockpit,/Google Navigation: Route bestätigen|Nur nach Freigabe der Abrechnung fortfahren|Google-SDK-Kontingent|Navigation-SDK-Quote/);
  assert.match(cockpit,/\.setTitle\("Google Navigation: Tankstelle anfahren"\)/);
  assert.match(cockpit,/\.setPositiveButton\("Route berechnen", \(dialog, which\) ->/);
  assert.match(google,/NavigationRequestBudget\.reserve\(host\)/);
  assert.match(google,/if \(calculating\)/);
});


test('v35 compact unified cockpit preserves Google 70/30, portrait split, and accessibility',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const layout=java('CockpitLayoutRules');
  assert.match(cockpit,/IntegratedNavigationRules\.MAP_WEIGHT/);
  assert.match(cockpit,/IntegratedNavigationRules\.CONTROLS_WEIGHT/);
  assert.match(cockpit,/CockpitLayoutRules\.mapWeight\(false\)/);
  assert.match(cockpit,/CockpitLayoutRules\.controlsWeight\(false\)/);
  assert.match(cockpit,/root\.addView\(mapPane, mapParams\)/);
  assert.match(cockpit,/root\.addView\(cockpitPane, controlParams\)/);
  assert.match(cockpit,/tools\.addView\(cockpitTitle, new LinearLayout\.LayoutParams\(0, -2, 1f\)\)/);
  assert.match(cockpit,/cockpitTitle\.setSingleLine\(true\)/);
  assert.match(cockpit,/sideToggle\.setContentDescription\(/);
  assert.match(cockpit,/voiceButton\.setContentDescription\(/);
  assert.match(cockpit,/new android\.graphics\.drawable\.RippleDrawable\(/);
  assert.match(cockpit,/cockpitButton\("Ziel \/ Route", true\)/);
  assert.match(cockpit,/getResources\(\)\.getConfiguration\(\)\.orientation/);
  assert.match(layout,/static int mapWeight\(boolean/);
});

test('v35 Spotify has compact header, single progress row and retains all seek actions',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(cockpit,/spotifyHeaderRow = Ui\.row\(this\)/);
  assert.match(cockpit,/spotifyHeaderRow\.addView\(spotifyState/);
  assert.match(cockpit,/mediaProgressRow = Ui\.row\(this\)/);
  assert.match(cockpit,/mediaProgressRow\.addView\(elapsed/);
  assert.match(cockpit,/mediaProgressRow\.addView\(seek/);
  assert.match(cockpit,/mediaProgressRow\.addView\(duration/);
  assert.match(cockpit,/mediaProgressRow\.setVisibility\(showProgress \? View\.VISIBLE : View\.GONE\)/);
  assert.match(cockpit,/spotifyHeaderRow\.setVisibility\(showMedia \? View\.VISIBLE : View\.GONE\)/);
  assert.match(cockpit,/send\(SpotifyMedia\.Action\.PREVIOUS\)/);
  assert.match(cockpit,/send\(SpotifyMedia\.Action\.NEXT\)/);
  assert.match(cockpit,/send\(SpotifyMedia\.Action\.SEEK_BACK\)/);
  assert.match(cockpit,/send\(SpotifyMedia\.Action\.SEEK_FORWARD\)/);
  assert.match(cockpit,/SpotifyMedia\.seekAbsolute\(/);
  assert.match(cockpit,/spotifySetup\.setOnClickListener\(v -> openSpotifyOrPermissions\(\)\)/);
  assert.match(cockpit,/play\.setContentDescription\("Wiedergabe starten oder pausieren"\)/);
});

test('v35 secondary fuel tools are collapsible without losing a manual refresh or station handoff',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(cockpit,/fuelActions\.addView\(fuelButton, cockpitActionParams\(1f, true\)\)/);
  assert.match(cockpit,/Button fuelMore = cockpitButton\("⋯", false\)/);
  assert.match(cockpit,/fuelExtraActions\.setVisibility\(View\.GONE\)/);
  assert.match(cockpit,/fuelMore\.setOnClickListener\(v ->/);
  assert.match(cockpit,/fuelExtraActions\.setVisibility\(open \? View\.VISIBLE : View\.GONE\)/);
  assert.match(cockpit,/fuelRefreshButton\.setOnClickListener\(v ->/);
  assert.match(cockpit,/fuelSettings\.setOnClickListener\(v -> startActivityForResult\(/);
  assert.match(cockpit,/FuelActivity\.class/);
  assert.match(cockpit,/fuelButton\.setOnClickListener\(v ->/);
  assert.match(cockpit,/engine\.showFuelPrices\(displayed,/);
});

test('v35 Saphe status is truthful and warnings and diagnostic remain available',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  assert.match(cockpit,/sapheHeader = Ui\.row\(this\)/);
  assert.match(cockpit,/sapheHeader\.addView\(safetyEventStatus/);
  assert.match(cockpit,/refreshSapheStatus\(\)/);
  assert.match(cockpit,/SapheNotificationProbe\.enabled\(this\)/);
  assert.match(cockpit,/String summary = enabled \? "Erkennung an" : "Erkennung aus"/);
  assert.match(cockpit,/sapheCheck\.setContentDescription\("Saphe-Diagnose und Warnanzeige testen"\)/);
  assert.match(cockpit,/sapheCheck\.setOnClickListener\(v -> showSapheProbeDialog\(\)\)/);
  assert.match(cockpit,/sapheBanner\.setBackground\(Ui\.background\(this, 0xffc91921, 12\)\)/);
  assert.match(cockpit,/handler\.postDelayed\(hideSapheBanner, 3200L\)/);
  assert.match(cockpit,/if \(!simulated\) refreshSapheStatus\(\)/);
  assert.match(cockpit,/SAFETY_COOLDOWN_MS = 10000L/);
  assert.doesNotMatch(cockpit,/Saphe verbunden|Blitzer erkannt/);
});

test('v35 removes routine success clutter and keeps route and Google-only cost guards',()=>{
  const cockpit=java('IntegratedNavigationActivity');
  const google=read('app/src/navsdk/java/com/cobrachris/fahrcockpit/GoogleNavigationEngine.java');
  assert.match(cockpit,/message\.startsWith\("Google Navigation bereit"\)/);
  assert.match(cockpit,/status\.setVisibility\(routine \? View\.GONE : View\.VISIBLE\)/);
  assert.match(cockpit,/currentSpeedView\.setVisibility\(\s*active \? View\.GONE : View\.VISIBLE\)/);
  assert.match(cockpit,/route\.setOnClickListener\(v -> resolveAndStartRoute\(\)\)/);
  assert.match(cockpit,/NavigationDestinationResolver\.resolve\(this, target/);
  assert.match(cockpit,/engine\.startConfirmedRoute\(lat, lng, readable\)/);
  assert.match(google,/NavigationRequestBudget\.reserve\(host\)/);
  assert.doesNotMatch(cockpit,/Nur nach Freigabe der Abrechnung fortfahren|Google Navigation: Route bestätigen/);
});
