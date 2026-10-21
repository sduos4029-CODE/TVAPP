# Flow TV 0.4.0 — validation

## Build

The main debug APK, instrumentation APK and Android lint checks passed with JDK 21, Gradle 8.10.2 and Android SDK/build-tools 35. Lint: **0 errors, 28 warnings**, including prototype English strings, RTL/resource presentation and foreground-service disclosure guidance. APK Signature Scheme v2 verification passed.

Package `tv.flow.mobile`, versionCode 4 / versionName 0.4.0, minSdk 26 (Android 8), targetSdk 35. The debug signing certificate matches the earlier published APKs, permitting an update that retains local preferences. The public sample switch was removed and the earlier sample preference is cleared during version migration.

[Raw build/smoke output and documentation retrieval evidence](releases/validation) accompany the release. Tests execute Android 11 / API 30 x86_64 on a software phone emulator; this is not a claim that every supported OS/device was tested.

## 40 phone-emulator checks

The final raw result reports `passed=40`, `40 checks passed`, and `INSTRUMENTATION_CODE: -1`, without FAIL/crash output. Adb exit status alone is insufficient.

- **13 retained test-only sample checks:** earlier navigation/filter/watchlist behavior, actual hardware APIs, drawer focus and Back, real bundled audio playback/search/seek/queue, favourites, injected touch on the on-screen remote and pausing prototype audio/polling in the background. The sample preference is set explicitly by instrumentation; this mode has no public settings entry.
- **24 device/listening checks retained from v0.3:** absence of fabricated content by default, real package icons/launches, real search hand-off URI and remote keyboard, strict YouTube URL validation, saved link persistence, truthful missing-session controls, actual hardware data, scope explanation, recently opened app persistence, media-access grant/revocation with non-YouTube sessions excluded, foreground local audio, actual decoding/duration/metadata/seek/queue/pause, local search, real content-URI import, black pixels and touch/Back dismissal, and local playback through emulator sleep/wake.
- **3 new content regressions:** Movies/Shows/New/Kids never substitute installed-app grids or generic trailer searches; only explicit programme media types/NEW/family genre flags qualify; shared-title My list saves/removes through its actual details controls, updates immediately and remains separate from YouTube links.

The new classification/watchlist checks use clearly synthetic instrumentation records, not a fetched provider catalogue. Those records and their saved state are removed before production capture. They verify type separation and persistence, not account connectivity. The default phone tests correctly observe no shared TV feed. No tests inject a successful Netflix/Prime/Hotstar connection into production screenshots.

Local audio tests use original bundled WAVs and an app-owned MediaStore content-URI fixture that is deleted afterward. Notification access is granted/revoked only by the disposable emulator test shell. During display sleep, PowerManager reports noninteractive, the activity stops polling, MediaPlayer remains playing and position advances; after wake/unlock it remains controllable. This verifies local decoding/state/progress with a virtual audio sink, not acoustic output, YouTube or TV picture-off.

## Visual verification

Actual running-APK captures include rounded landscape and portrait Dashboard layouts, installed apps, Home, Movies/Shows/New/Kids unavailable states, source explanation, saved lists and the preserved dot-matrix audio UI. RAM/storage ring fractions use ActivityManager/StatFs readings. Large rounded slate cards, circular icon wells and matching mint focus outlines replace the previous flatter widgets. Solid backgrounds and static Canvas drawing avoid expensive live blur or decorative animation.

On this phone emulator, content tabs show an explicit unavailable feed rather than invented titles or app cards. All captured app icons and hardware values are actual emulator data. Artwork placeholders describe missing source data. The black-screen capture is RGB zero throughout and has no widgets; this does not establish physical panel/backlight power-off.

## Permissions and data boundaries

READ_EPG_DATA and the Android TV provider visibility declaration support only OS-permitted shared listings. TV queries request a small explicit projection of public programme fields; private internal provider data is not requested. Filtering honours hidden/non-searchable records. Content observation is stopped while inactive. Artwork loading uses source-published HTTPS/content/resource URIs, bounded reads, reduced-size decoding, 24-card pages and a 6 MB memory cache. It does not send account cookies or authorization headers. INTERNET was added for that artwork; the previous no-network-request description no longer applies.

No ads, tracking SDKs, microphone, broad storage or privileged cross-app/account permissions were added. No streaming-app login/session reuse, private database access, scraping, default-home role override or successful provider connection is simulated.

## Unverified / blocked

- **Actual TV feed access is unverified.** No physical realme TV or logged-in streaming provider was attached. Firmware can restrict the standard reader, expose only some records or return none. Permission-denied/unsupported TV-table behavior has not been exercised on that hardware. A public API and manifest declaration do not prove cross-app access.
- Phones generally lack this shared Android TV feed. Existing streaming-app logins do not expose their private catalogues. Full per-provider browsing needs a supported API/partnership; the user's exact apps and device are still needed to assess those options.
- Actual programme posters, provider title deep links, app-published NEW flags and complete publisher coverage remain device checks. No full Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5 or YouTube catalogue was accessed.
- Actual YouTube/YouTube Music playback, membership eligibility, source metadata/actions/queue, mobile background audio and screen-off playback remain unverified. YouTube background integration is disabled on Android TV according to official supported-device documentation.
- realme RMV2004 Picture Off, continued TV audio, remote wake, standby differences, vendor controls, physical remote long-press/touch drag and TalkBack remain physical-device checks. Black pixels do not power off a backlight.

[CONNECTED_APPS.md](CONNECTED_APPS.md) and [YOUTUBE_FEASIBILITY.md](YOUTUBE_FEASIBILITY.md) distinguish official findings, implemented code and executed verification.

## Reproduce

Build with `./scripts/build-cloud.sh` in the prepared environment, or README's SDK instructions. Install both APKs into a disposable API 30 emulator and run:

```sh
adb shell am instrument -r -w -e liveCapture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

The runner resets preferences, uses test-only metadata fixtures, temporarily grants/revokes media access, launches Settings, creates/deletes its own audio file and sleeps/wakes the emulator. Do not run it on a personal installation. Optional screenshots are in internal `files/visuals` and can be collected with `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.
