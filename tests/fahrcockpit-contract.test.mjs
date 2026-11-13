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
 assert.match(shell,/Button toggleRoutes = Ui\.button\(this, "📍 Ziel \/ Route"\)/);
 assert.match(shell,/toggleRoutes\.setOnClickListener/);
 assert.ok(shell.indexOf('panel.addView(media);') < shell.indexOf('panel.addView(routePanel);'));
 assert.ok(shell.indexOf('panel.addView(skip);') < shell.indexOf('panel.addView(routePanel);'));
 assert.match(shell,/scroll\.smoothScrollTo\(0,/);
 assert.match(shell,/SOFT_INPUT_ADJUST_RESIZE/);
 assert.match(shell,/panel\.addView\(fallback\)/);
 assert.doesNotMatch(shell,/routePanel\.addView\(fallback\)/);
 assert.ok(shell.indexOf('panel.addView(media);') < shell.indexOf('panel.addView(fallback);'));
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
 assert.match(screen,/NavigationProviderPolicy\.fallback\(reason,\s*false\)/);
 assert.match(screen,/OSM-Ersatznavigation ist noch nicht installiert/);
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

test('v21 original App icon selects fixed Google 70/30 only with locally restricted SDK key',()=>{
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
 assert.match(gradle,/versionCode 21/);
 assert.match(gradle,/versionName '0\.9\.19-beta'/);
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
  assert.match(screen,/root\.addView\(scroll, controlParams\)/);
  assert.match(screen,/root\.addView\(mapPane, mapParams\)/);
  assert.match(screen,/root\.removeView\(mapPane\)/);
  assert.match(screen,/root\.removeView\(scroll\)/);
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
 assert.match(policy,/PORTRAIT_MAP_WEIGHT = 9/);
 assert.match(policy,/PORTRAIT_CONTROLS_WEIGHT = 11/);
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
 assert.match(shell,/FahrCockpit v21 · OSM-Sichttest/);
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

