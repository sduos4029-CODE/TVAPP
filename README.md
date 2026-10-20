# Flow TV 0.3.0

Native Android launcher/test app with real installed-app cards, a Minimal Glass dashboard, and an original Nothing-inspired dot-matrix listening screen. Dark navy, solid rounded slate cards, mint focus and lavender accents remain the Option C design. Audio uses black/white Doto typography and restrained red accents.

[Download the debug APK](releases/Flow-TV-v0.3.0-debug.apk) · [Actual v0.3 screenshots](releases/screenshots-v0.3) · [Feasibility and official references](YOUTUBE_FEASIBILITY.md) · [Validation](VALIDATION.md)

## Install

Android 8+; download the APK, open it on your phone and permit installation from that file manager if asked. Install over v0.1/v0.2 to retain favourites and preferences: the package `tv.flow.mobile` and signing certificate are unchanged. This is a normal activity and does not replace your phone's home launcher. The top-bar grid icon opens TV Preview, portrait/rotation and optional on-screen remote controls.

## Working device experience

- Real installed-app discovery, supplied package icons, launching, local favourites and touch/D-pad reordering. Cards refresh on package changes and on return to Flow. Recently opened means apps launched through Flow, not fabricated watch history or permission-based app surveillance.
- Home and Movies/Shows/New/Kids open actual installed providers and real YouTube search. New apps are sorted by Android installation time. There is no connected live movie recommendation feed; no dummy film suggestions are shown by default. Kids search is not parental enforcement.
- My list saves real user-entered YouTube links locally, validates hosts/video IDs, and opens the source. It does not download videos or invent metadata.
- Minimal Glass dashboard: actual RAM/storage, processor/ABI/core information, permitted network/Bluetooth hardware information, OS traffic totals, exposed inputs/USB and system details. Unsupported metrics are hidden; phone readings are labelled. Polling stops off Dashboard and while inactive. No sample CPU/RAM/storage/network measurements.
- Compact right-side settings, supported Android settings hand-offs, real device media volume where adjustable, app-only appearance brightness, privacy information, and confirmation before clearing history/data.
- Native views and controls support touch, keyboard/D-pad, Android Back and landscape mobile preview. Artwork is not a flattened UI background.

## YouTube listening

The default source is **YouTube**. Search songs/artists/albums through the official app (or an HTTPS browser handler if unavailable). Show video opens the active official app when known. There is no API key configured and no fabricated in-app search results. A search result or saved link is not a YouTube playback queue.

Optional **Connect YouTube controls** explains Android's broad notification access before opening its system settings. Flow does not read/store notification contents. When granted, it reads only active YouTube/YouTube Music media sessions and displays the actual exposed metadata/artwork/state. It shows transport actions only when the session reports support. No active session means no pretend playback buttons or timing. Opening YouTube does not itself grant control, and session access does not establish Premium eligibility.

**YouTube Hide Video and Screen Off are not verified or enabled.** Flow never extracts streams, hides an API player, bypasses Premium or forces background playback. Official app audio/background modes depend on membership, content, device and app support. Official documentation limits background playback to mobile apps, so Flow disables YouTube background controls on Android TV. If YouTube pauses when leaving its app, return to its official player. YouTube has its own ads, account and privacy practices; Flow cannot change them.

Current official YouTube, Android and realme documentation was retrieved and reviewed. YouTube Android Premium documents Settings → On-the-go; YouTube Music documents Song/Video mode for eligible music memberships. These modes remain in the official apps. See the source-backed mobile/TV capability table in [YOUTUBE_FEASIBILITY.md](YOUTUBE_FEASIBILITY.md). No actual YouTube playback or physical realme picture-off was verified.

## Fully functional local audio

Choose **Local audio** in the listening screen. Three original bundled 30-second tracks make decoding/playback testable immediately. **Choose audio files** uses Android's document picker; only selected content URIs are read, with persistent access when the provider permits it. No broad storage or microphone permission is requested.

Local playback has actual metadata and embedded artwork where present, play/pause, previous/next, ±10-second seek, seek bar, device volume, separate queue, and song/artist/album search that leaves current playback running. A user-started foreground media service and media notification support playback while Flow is backgrounded. CPU wake locking is limited to actual MediaPlayer playback; the display is not held on.

For local audio, **Display options** explains using the phone's physical power button and normal wake/unlock method. **Black screen · display stays on** is a real black view with no widgets, dismissed by touch/Back/remote or keyboard key. It does not turn off the backlight or panel. Display-off/wake during local playback is tested on an Android 11 emulator; real-phone and realme TV behavior still needs testing. A truly off display has no widgets. Flow has no model-specific realme picture-off command, and standby may stop audio.

## Samples and privacy

The earlier illustrated catalogue and sample playback UI remain available only through the explicitly labelled **Design preview · samples** setting. This mode is off by default, including on update. It is not live availability or history. Existing sample watchlist data remains separate from real YouTube saved links.

No ads, analytics, tracking SDKs, runtime network requests or microphone access in Flow. Search/link actions hand off to external apps, which have their own policies. Favourites, lists, imported-file references and apps opened through Flow stay local. Optional notification access is managed/revoked in Android settings. Deleting Flow data stops local audio and releases its persisted document grants; it does not revoke Android notification access or delete original music files.

## Build and reproduce

JDK 17+ (tested 21), Android SDK/build-tools 35, Gradle 8.10.2:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

The prepared environment can use `./scripts/build-cloud.sh` offline. APK: `app/build/outputs/apk/debug/app-debug.apk`. Install main and test APKs into a disposable emulator, then run:

```sh
adb shell am instrument -r -w -e liveCapture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

Instrumentation resets that installation's preferences, launches a real installed Settings app, creates/deletes its own MediaStore audio fixture, and sleeps/wakes the emulator display. Do not run it on your personal installation. Check the reported pass count, absence of FAIL/crash, and instrumentation completion; adb exit alone is insufficient. Optional captures are in internal `files/visuals`, collectible with `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.

No Android runtime libraries or web UI are used. MainActivity owns device/lifecycle behavior; FlowDesign owns Option C layouts; LiveFlow owns device-backed screens; YouTubeMedia owns documented media-controller access; LocalAudioService owns local playback. Doto uses its bundled SIL Open Font License. Bundled audio is original synthesis generated by `scripts/generate_audio.py`; illustrative artwork in the optional design preview is generated imagery.
