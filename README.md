# Flow TV 0.5.0 — full Android app

[Download full-app debug APK](releases/Flow-TV-v0.5.0-debug.apk) · [Portable source ZIP](releases/Flow-TV-v0.5.0-source.zip) · [Actual app screenshots](releases/screenshots-v0.5.0) · [Validation](VALIDATION.md)

The complete phone/TV project: native interactive Home, Movies, Shows, New, Kids, My list, Apps, Minimal Glass Dashboard and dot-matrix Audio. Package `tv.flow.mobile`; install and open **Flow TV**. The separately reviewed `com.flowtv.launcher` prototype remains a smaller, independent project.

## Working core

- **Home and apps:** real installed icons and app launches, favourites, touch drag ordering, remote hold-OK ordering, locally recorded apps opened through Flow, and All Apps.
- **Movies / Shows / New / Kids:** public metadata request layer, title search, language filters, right-side details, local My list and explicit loading/offline states. Installed streaming-app shortcuts remain useful when public metadata is unavailable; each is labelled as opening the app home screen. The Kids shortcut does not claim to open a verified child profile.
- **Dashboard:** Option C navy, solid rounded slate widgets, mint focus and lavender accents. Actual installed-device RAM/storage, CPU information, connection state, accessible traffic counters and ports. Unsupported measurement widgets hide. System summary is focusable and opens actual device details. Phone readings are labelled.
- **Audio:** original dot-matrix black/white/red design. First use selects functional local audio; an explicit source choice is preserved. Original bundled tracks and user-selected files support real playback, metadata/artwork where present, search, queue, seek, volume and media notification/background service. No autoplay on app launch.
- **Phone testing:** ordinary app launch, landscape TV Preview, optional on-screen arrows/OK/Back, touch, Android Back and keyboard/D-pad. No replacement of the phone Home launcher.
- **Settings/privacy:** compact right-side panels, supported Android settings intents, appearance settings separate from system controls, local data/history clearing with confirmation. No ads, analytics or tracking SDKs.

0.5.0 adds explicit app-only navigation across browsing tabs, recognizes Airtel Xstream alongside Prime Video/JioHotstar, prefers phone launcher activities on phones and TV activities on TVs, skips disabled/non-exported apps, defaults first-use audio to local playback, adds focusable System details to Dashboard and stops Dashboard polling directly when the display sleeps. Earlier core features are retained.

## Integration limits

**Live TVmaze/Wikidata access is unverified.** The managed cloud proxy returned CONNECT 403 for both public API hosts during this build. No fictional movie/show results are substituted. The phone's own network may differ. Implemented requests and fixture tests do not prove current endpoint availability, coverage or licensing compliance for wider distribution.

Wikidata provides film metadata; TVmaze provides series/search and dated episode schedules. Neither imports private Prime Video, JioHotstar or Airtel Xstream logins, watch histories or subscriptions. India streaming availability, provider-added dates, exact-title provider playback, verified trailers and certified kids ratings are unavailable. Provider cards use installed app icons and open app home screens; they are not movie cards. [Catalogue boundaries](CONNECTED_APPS.md).

YouTube search opens the official app or browser. Flow does not extract streams, request notification/accessibility access, read external media sessions or offer a hidden embedded player. YouTube playback, listening modes and screen-off audio stay under its official app and any applicable membership/content/device restrictions. Flow's dot-matrix controls work with local audio. **YouTube Hide Video and Screen Off are not implemented or verified.** [Feasibility and documentation](YOUTUBE_FEASIBILITY.md).

For local audio, a phone's physical power button can put the display to sleep while the foreground service continues; decoding/state advance was checked on an Android 11 emulator. Physical-phone/TV behavior still needs testing. **Black screen — display stays on** is a separate option with no widgets; it does not power off the panel/backlight. realme RMV2004 picture-off, standby/wake, HDMI control and actual TV feed access remain unverified.

## Install

Android 8/API 26 or newer. Download the APK to the phone, open it and follow Android's per-source installation prompt, then launch **Flow TV**. Keep Play Protect enabled. Physical-phone Play Protect acceptance is unverified; an emulator/adb install is not Google approval. [Installation report](INSTALLATION.md).

VersionCode 7 / versionName 0.5.0, compile/target SDK 35. The existing cloud debug certificate is retained, allowing an update over a matching signed `tv.flow.mobile` build. A different certificate requires an intentional data-aware migration; never silently uninstall the user's app. This is a debug testing build, not a Play Store production release.

## Build and verification

JDK 17+ (tested 21), Gradle 8.10.2, Android SDK/build-tools 35:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

Prepared cloud workspace: `./scripts/build-cloud.sh`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

Disposable Android 11 emulator only:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -r -w tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

Tests reset local state, launch device settings, use explicitly synthetic parser/transport fixtures, import/delete their own audio fixture and sleep/wake the emulator. Do not run on a personal installation. `-e screensOnly true` captures the production UI with actual requests and no successful catalogue fixtures. [Executed results and remaining checks](VALIDATION.md).

Bundled WAVs are original Flow sample audio. Doto is bundled under the SIL Open Font License in `app/src/main/assets/fonts/OFL.txt`. There are no runtime third-party libraries in this native Java app.

[Prototype ZIP review/comparison](reviews/FlowTV-zip-review.md) · [Prototype source](prototypes/launcher-review) · [Earlier supplied-code integration review](IMPORT_REVIEW.md).
