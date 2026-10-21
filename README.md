# Flow TV 0.4.2 — public catalogue integration

[Debug APK](releases/Flow-TV-v0.4.2-debug.apk) · [Screenshots](releases/screenshots-v0.4.2) · [Validation](VALIDATION.md) · [Imported-code review](IMPORT_REVIEW.md)

Native Android phone/TV application. Rounded Option C Dashboard, actual installed apps, touch/D-pad/landscape preview and original dot-matrix local audio are preserved.

## What changed

The supplied Kotlin catalogue was adapted to the existing dependency-free Java app. Movies now requests public **Wikidata** film metadata; Shows uses the **TVmaze** directory and title search. Home combines actual favourite/recent apps with public film discovery and today's global episode schedule. New distinguishes films released within 90 days from today's scheduled episodes. Kids filters explicit children/family metadata; anime alone does not qualify. My List stores selected real source metadata locally, independently of saved YouTube links and earlier shared TV cards.

**Live TVmaze/Wikidata retrieval is not verified in this build environment.** Its network proxy rejects their domains. Implemented requests are not proof of endpoint availability or catalogue completeness. Empty/error states never substitute fictional movies. Test fixtures establish parsing/control behavior only and are removed before final production captures. See [source boundaries](CONNECTED_APPS.md).

The user is testing a phone signed into Prime Video, JioHotstar and Airtel Xstream. Public metadata does not import those private logins, catalogues, histories or subscriptions. These sources do not establish regional streaming availability in India. No provider availability filter or fake Watch action is offered. Details distinguish **Open metadata source** from **Open a streaming app**; the latter explicitly opens the app, not a verified title. Missing apps are explained. A general Wikidata YouTube video statement is not assumed to be a trailer.

## Catalogue controls and privacy

- No catalogue API key is needed for these public endpoints. No unused TMDB secret is embedded in BuildConfig.
- Search by title using actual native buttons and the remote keyboard. Typing does not send requests; Search submits them.
- Original-language filtering for Wikidata; source-language filtering within TVmaze's loaded response. Default films: Hindi; series: all languages.
- TVmaze directory slices are ordered by source ID, not popularity or personal recommendation. Up to 40 matching records per response; 24 cards per UI page.
- Source fetches run off the main thread, with HTTPS, fixed public hosts, no redirects/credentials/cookies, timeouts and a 2 MB response cap. No periodic catalogue polling.
- Up to 12 cached responses with 30-minute freshness. Refresh failures retain available records with an explicit cached/stale error and timestamp. A short failure cooldown prevents immediate retry loops.
- Sources opens a compact right-side panel with networking on/off, cache removal, source links, attribution and optional TV shared-feed explanation.
- Artwork memory cache is 6 MB with reduced-size decoding. TVmaze medium images come from the source. Wikidata P18 is a source image, not necessarily a poster. Commons images require a resolved permitted CC BY/CC BY-SA/public-domain licence and source attribution before display. Image licences are separate from Wikidata CC0 structured metadata.
- Public API/image servers receive ordinary connection data and submitted search terms; their own privacy policies apply. Flow sends no streaming-app credentials.

Current source documentation retrieval was blocked here; linked API/licensing references and the assumptions inherited from the supplied snippets are documented in IMPORT_REVIEW.md. Source availability, licensing obligations and coverage should be verified before wider distribution.

## Minimal Glass Dashboard

Uses the supplied navy/slate/mint/lavender palette, large rounded widgets, circular icon wells, actual RAM/storage usage rings and matching focus outlines. RAM, storage, CPU information, transport/validation, accessible USB/TV inputs and cumulative traffic come from the installed device. Network rates use actual counter deltas and elapsed time; unsupported counters and first/reset samples are not converted to fabricated zeros. Baselines reset off Dashboard or while inactive. No fake CPU/GPU load or sample realme specifications are shown. Phone data is labelled; TV-only controls are unavailable on a phone. Actual app version is read from PackageManager.

## Audio and YouTube

The existing tested local playback service remains, rather than introducing a second service. It provides genuine decoding, file-picker import, metadata/artwork, queue/search/seek, volume, pause/resume, audio-focus handling and a media notification. Display sleep/wake is checked on an Android 11 emulator, not physical TV hardware. **Black screen — display stays on** remains separate from actual panel power-off and contains no widgets.

YouTube opens/searches in its official app. Notification access and external media-session acquisition remain removed. No YouTube metadata, remote controls, hidden player, extracted streams, Premium bypass or successful screen-off integration is simulated. [YouTube feasibility](YOUTUBE_FEASIBILITY.md).

## Installation and permissions

Android 8+; package `tv.flow.mobile`; versionCode 6 / versionName 0.4.2; target/compile SDK 35. Install over 0.4.1 when the signing certificate matches. The generated APK preserves this environment's previous debug certificate. Another build environment may use a different certificate; do not silently uninstall a user's app/data to work around that.

No notification listener, accessibility service, microphone, SMS, contacts, Bluetooth runtime grant, broad storage, QUERY_ALL_PACKAGES or install-packages permission. Actual Bluetooth connection/device access is not implemented; the card reports hardware presence. Network/audio/TV-feed permissions support implemented features. No ads, analytics or tracking SDKs. Cleartext is disabled and network trust uses system certificates.

Keep Play Protect enabled. The earlier installation warning's exact cause and new-build acceptance remain unverified. An emulator/adb install does not establish Google approval. [Installation report](INSTALLATION.md).

## Build and checks

JDK 17+ (tested 21), Gradle 8.10.2, Android SDK/build-tools 35:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

Prepared cloud environment: `./scripts/build-cloud.sh`. Main APK: `app/build/outputs/apk/debug/app-debug.apk`.

Dependency-free instrumentation in a disposable API 30 emulator:

```sh
adb shell am instrument -r -w -e liveCapture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

Tests reset local data, use explicitly synthetic catalogue transport/parser fixtures, launch Settings, import/delete their own audio fixture and sleep/wake the emulator. Do not run them on a personal installation. Optional screenshots-only mode uses `-e screensOnly true`; it enables real source requests and does not inject successful results. Captures are stored in internal `files/visuals`.

Doto is bundled under the SIL Open Font License. The three bundled sample WAVs are original Flow audio. No Kotlin, AppCompat, coroutines or other runtime dependency migration was needed.
