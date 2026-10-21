# Flow TV 0.5.0 — validation

## Build and package

JDK 21, Gradle 8.10.2, Android SDK/build-tools 35. Main/debug instrumentation APK builds and Android lint passed: **0 errors, 33 warnings**. Warnings concern English UI strings, RTL/resource presentation, vector/resource configuration and the existing artwork-loader InputStream alias (the stream is closed by try-with-resources). APK Signature Scheme v2 verification passed.

Package `tv.flow.mobile`; versionCode 7 / versionName 0.5.0; minimum Android 8/API 26, target SDK 35. Existing debug signing certificate retained. Network trust uses system certificates and disables cleartext; no user CA or certificate-verification bypass was added.

## Executed: 63 Android 11 emulator checks

[Final build log](releases/validation-v0.5.0/build-results.txt) · [Raw test results](releases/validation-v0.5.0/smoke-results.txt) · [Merged permission/signature audit](releases/validation-v0.5.0/permission-audit.json) · [Network probes](releases/validation-v0.5.0/network-probes.txt).

The final raw result reports **63 checks passed**, `passed=63` and `INSTRUMENTATION_CODE: -1`, without FAIL output. Android 11/API 30 x86_64 software emulator; no physical phone/realme TV or signed-in streaming app is attached.

- **40 existing checks retained/updated:** test-only legacy navigation/filter layouts, real device data, drawer/Back/focus, actual package icons/launches/recent-app persistence, favourites/remote actions, shared-TV metadata classification and saved-state behavior, official YouTube search/link validation and saved links, removal of sensitive listener/access prompts, local-only media controls, actual audio decoding/import/metadata/duration/seek/queue/search/pause, black pixels/dismissal, and local playback through emulator display sleep/wake. Default tab tests now expect public source/offline states, not the earlier unsupported shared-phone catalogue.
- **17 catalogue/telemetry checks:** missing/null field handling; actual source/type identity; anime excluded from children-only classification; episode schedule dates; future/duplicate/unlabelled film rejection; earliest-release/search escaping/language mapping; source image URL validation; separate Commons licence/credit requirements; real byte/time rate calculations and counter-reset/unsupported handling; asynchronous transport; fresh memory/disk cache hits; explicit stale-cache errors and retry cooldown; persistent watchlist through actual details buttons; no fabricated Watch action; provider app-only fallback; search wired through native controls; and networking-off guards for metadata and resolved/queued artwork.

- **6 full-release checks:** recognize the requested video-app package identities without lookalike matches; restrict provider cards to actual installed launchable apps/icons; distinguish app-only navigation across all browsing tabs during source failures; default first-use audio to local while preserving explicit YouTube selection; expose actual System details as a focus target; and activate that production drawer by touching on-screen remote OK, then close it with Back. Provider identity fixtures do not install or authenticate real streaming apps. The emulator has none of the user’s signed-in providers.

Catalogue tests use **clearly synthetic instrumentation fixtures and an injected transport**, not live API responses. Fixture titles, lists and cache files are removed afterward. Their success does not establish a working TVmaze/Wikidata connection, actual streaming availability or entitlement. Production source networking is disabled during deterministic functional tests. Screenshots-only mode subsequently enables actual requests without injecting successful records.

Local audio uses original bundled WAVs and an app-owned MediaStore test file that is deleted afterward. During emulator sleep, PowerManager reports noninteractive while decoding position advances; no Dashboard ticker remains scheduled. The full app listens for screen state and guards the ticker with PowerManager so delayed Activity pause callbacks cannot keep Dashboard polling active; after wake the local session remains controllable. This checks virtual playback state/decoding, not physical acoustic output or YouTube/TV Picture Off.

## Live network boundary

The selected cloud proxy again rejected CONNECT requests to `api.tvmaze.com` search and `query.wikidata.org` SPARQL with 403 before HTTPS on 2026-10-21. Earlier API/docs requests were also blocked. **No successful live catalogue fetch was verified.** No blocked-host mirror, alternate egress or fabricated successful result was substituted. Current API shapes/licensing assumptions come from the supplied snippets and known public contracts; current official source documentation could not be retrieved here. Actual source behavior/coverage/rate limits and licensing obligations still need verification on an allowed network before wider distribution.

Installed streaming-app shortcuts open app home screens and remain useful when metadata is absent. They are explicitly separate from movie/series results, title deep links and children’s profiles. Airtel Xstream recognition is included.

Real request/parsing, timeout/HTTP/JSON-error paths, source information links and native unavailable states are implemented. The source error handling is tested with fixtures; it is not evidence that the endpoint accepted a production query. The phone's own network may differ from the managed build environment.

## Visual verification

Final captures come from the running 0.5.0 APK: supplied Minimal Glass palette, rounded landscape/portrait Dashboard, public catalogue loading/unavailable states, source explanation, actual installed apps, separate saved lists and dot-matrix audio. The capture suite runs after clearing this disposable emulator installation’s app data. Its 17 production captures use no successful catalogue fixtures. No synthetic successful catalogue rows are used in final release captures. [Screenshots](releases/screenshots-v0.5.0) · [Capture log](releases/validation-v0.5.0/screen-results.txt). RAM/storage values are actual emulator readings; traffic rates use elapsed counter differences. Black-screen pixels are RGB zero throughout, without widgets; this does not establish panel/backlight power-off.

## Permissions and privacy

The packaged manifest/Dex audit verifies no notification/accessibility listener binding, external active-session query, microphone/SMS/contacts permission, install-packages permission or QUERY_ALL_PACKAGES declaration. BLUETOOTH_CONNECT was not added because no corresponding device-access feature was implemented. Hardware presence can be shown without claiming paired-device control.

Public metadata uses fixed HTTPS hosts, bounded reads, timeouts and no redirects/account credentials. At most 12 cached responses are retained; freshness is 30 minutes and stale data is explicitly labelled. UI pages hold at most 24 catalogue cards. Shared artwork cache is 6 MB with reduced decoding. Commons images require separately resolved permitted licences/credit before display. Source network controls and cache clearing are implemented. No ads, analytics or tracking SDKs were added.

## Pending or unsupported

- Live TVmaze/Wikidata/Commons retrieval, current source terms/licensing, image coverage and real phone connectivity remain unverified due the environment network block.
- Private Prime Video/JioHotstar/Airtel Xstream catalogue import, subscription/watch-history detection, verified India provider availability, exact-title provider links, provider-added dates, certified kids ratings and trailers are not provided by these integrations. General movie genres beyond the children's collection are not loaded by the current Wikidata projection. TVmaze language filtering applies to its loaded source response; directory slices are not complete recommendations.
- Actual Android TV shared-feed access, HDMI switching, realme RMV2004 Picture Off, physical remote/drag/accessibility behavior and physical-device wake remain real-TV checks. No privileged vendor integration is claimed.
- YouTube playback control/metadata/queue/hide-video are unsupported in Flow after notification access removal. Official-app membership/background/display-off behavior remains unverified.
- Play Protect acceptance is unverified. The test emulator has no Play Protect; build/signature/adb success does not establish Google approval. Keep protection enabled. See INSTALLATION.md.

## Reproduce

Use `./scripts/build-cloud.sh`, or README's SDK instructions. In a disposable emulator install main/test APKs and run:

```sh
adb shell am instrument -r -w -e liveCapture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

Tests reset local state and should not run on a personal installation. Screenshots-only mode: `-e screensOnly true`. Retrieve internal captures with `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.
