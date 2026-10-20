# Flow TV 0.3.0 — executed validation

## Final build

`assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed with JDK 21, Gradle 8.10.2 and Android SDK/build-tools 35. Lint: **0 errors, 27 warnings** (including English prototype strings, RTL/resource presentation and foreground-service disclosure guidance). APK Signature Scheme v2 verification passed.

Package `tv.flow.mobile`, versionCode 3 / versionName 0.3.0, minSdk 26 (Android 8), targetSdk 35. The certificate SHA-256 remains `c5129f9e2307d1061497bbd58cd4d7ae97829f1b23980dce6aa4eb50409114b0`, matching the earlier published APKs. Main/test APKs installed successfully over the existing emulator installation.

The emulator was Android 11 / API 30 x86_64 with software acceleration, landscape and portrait. [Raw final build and smoke output](releases/validation) accompanies the APK. Executing Android 11 does not imply that every supported OS/device has been tested.

## 37 passed instrumentation checks

The final raw result reports `passed=37`, `37 checks passed`, and `INSTRUMENTATION_CODE: -1`, without FAIL/crash output. The following groups describe the actual scope:

- **13 retained Design Preview checks:** sample labels and navigation, local sample catalogue filters, Kids grouping, sample watchlist persistence/recreation, actual device Dashboard and unavailable phone TV controls, drawer focus/Back, actual bundled playback/search/seek/queue/pause, favourite reordering/persistence/cancel, injected touch on the on-screen remote, and stopping prototype audio/polling in the background. These are explicitly sample-mode tests, not live streaming tests.
- **10 default-device checks:** no fabricated catalogue/sample playback by default; real package/icon/launch-component discovery; correctly encoded YouTube search URI and remote keyboard; strict video URL/host validation; saved real link persistence; truthful controls without a YouTube session; Minimal Glass hardware readings; permission-scope explanation; launching the actual installed Android Settings app; and restoring its local recent-app entry.
- **2 granted-access checks:** Android notification access was granted temporarily in the disposable emulator using the test shell. Active media sessions were queried successfully, and Flow's own playing local session was correctly rejected as a YouTube source. Revoking access cleared the listener/controller. Production code never grants this permission automatically.
- **12 local-audio checks:** user-started foreground media service; real decoded duration/metadata and supported controls; actual seeking and queue use; consuming queue/next plus uninterrupted song/artist/album search; media-session pause/resume; black view without widgets; an actual screenshot pixel check plus injected touch and Back dismissal; importing a real content URI; playing the imported file; verifying its actual decoded duration; continued playback with the emulator display noninteractive; and waking the display with playback still controllable.

The content-URI fixture was an audio file created through Android MediaStore by this app, copied from an original bundled WAV and deleted by the test afterward. This verifies selected-URI handling and decoding, not every external document provider. The picker itself uses standard ACTION_OPEN_DOCUMENT with audio MIME filtering and no broad storage permission.

For display-off testing, the test sent Android sleep, verified `PowerManager.isInteractive() == false`, verified that the activity was inactive, and verified actual MediaPlayer progress advanced by more than 700 ms while playback continued. It then sent wake/unlock and checked continued playback. **This verifies local audio on this emulator, not YouTube or physical TV picture-off.** The emulator uses a virtual audio sink; decoding/state/progress were verified, not acoustic speaker output.

## Actual captures

[13 v0.3 screenshots](releases/screenshots-v0.3) were captured from this final tested APK: default Home, installed Apps, New/provider hand-offs, Dashboard, YouTube connection state, official-app Search panel, media-access explanation, saved links, portrait Home and YouTube audio, local dot-matrix audio, local Search, and the black screen. Landscape/portrait layouts and missing-artwork states were inspected. Native controls, labels and panels remain independently interactive.

The emulator has no official YouTube/streaming apps, so these captures truthfully show no connected YouTube session or installed providers. Recently opened Settings and the saved YouTube link are test inputs, not fetched recommendations or viewing history. Blank/no-artwork states are labelled; no sample cover is substituted as source artwork. The black capture has no widgets and does not establish panel/backlight shutdown. Historical v0.2 design captures remain separately in `releases/screenshots`.

## Not verified / unsupported

- No actual YouTube/YouTube Music playback, membership eligibility, active YouTube metadata/actions, source queue, source Show video, mobile background playback or screen-off playback was exercised. The official source must expose these capabilities; hand-off and session APIs alone do not prove them.
- No realme RMV2004 or physical phone was attached. Vendor Picture Off, standby differences, remote wake behavior, physical D-pad long-press, touch drag, TV USB/HDMI, vendor settings, acoustic output and TalkBack remain device checks.
- No hidden YouTube player, stream extraction, forced background playback, privileged display-off command or Premium bypass exists. YouTube Hide Video and Screen Off are not labelled working. Black screen is offered for local audio only and is labelled display-stays-on.
- No official API key/catalogue is configured, so in-app live movie or YouTube search results and aggregation of provider suggestions are unavailable. Real search opens the official app/browser, and provider cards open installed apps.
- Current official YouTube/Android/realme documentation was retrieved and reviewed after initial HTTP failures. YouTube limits background playback to mobile apps; the Android TV path is explicitly gated and offers Open YouTube. No RMV2004-specific Picture Off support was established. [YOUTUBE_FEASIBILITY.md](YOUTUBE_FEASIBILITY.md) and the retrieval manifest separate official findings from executed emulator tests.

## Reproduce

Build with `./scripts/build-cloud.sh` in the prepared environment or README's SDK instructions. Install both APKs into a disposable API 30 emulator and run:

```sh
adb shell am instrument -r -w -e liveCapture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

The runner clears this test installation's preferences, temporarily grants/revokes media-session notification access, launches installed Settings, creates/deletes its own MediaStore file, and sleeps/wakes the emulator. **Do not run this test suite on a personal installation.** Require the full pass count and successful instrumentation completion, not adb exit alone. Captures are in the app's internal `files/visuals`; collect with `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.
