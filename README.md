# Flow TV 0.2.0 — Android phone / TV preview

A native Java Android app rebuilt around the supplied visual references. Option C uses dark navy, solid rounded slate cards, mint focus rings and lavender accents. Audio uses the bundled Doto dot-matrix font, an ivory/black palette and red playback controls.

**The screenshots in [releases/screenshots](releases/screenshots) are captured from the running APK.** Artwork is separately bundled imagery; navigation, cards, text, progress bars, drawers, keyboard and playback controls are native Android views, not a flattened screenshot.

## Install the mobile test APK

Download [Flow-TV-v0.2.0-debug.apk](releases/Flow-TV-v0.2.0-debug.apk). Android 8+ is required. Open the file on your phone and allow installation from that file manager if Android prompts you. The provided APK keeps the same application ID (`tv.flow.mobile`) and signing certificate as v0.1.0, so you can install it as an update and retain local favourites and watchlist.

The first launch opens landscape TV Preview. Use the grid icon in the top bar for portrait mode and the optional on-screen remote. This is a normal launchable activity; it does not replace your phone's home launcher.

## Implemented

- Home: panoramic feature art, installed-app favourites, clearly labelled illustrative Continue Watching layout and compact sample catalogue rows. No real viewing history or live streaming availability is invented.
- Movies / Shows / New: poster rows and four-column releases, local genre/language/provider/content-type filtering, distinct NEW RELEASE / NEWLY ADDED / NEW EPISODE labels and title details in a right-side panel.
- My list: real local add/remove actions and saved counts. Existing v0.1 sample titles remain available so stored watchlist entries survive updating.
- Apps: discovers actual launchable installed apps, loads their supplied icons and launches them. Favourite ordering is saved locally. Hold/drag a favourite, or long-press OK and move with arrows; OK saves and Back cancels. Missing apps expose a removal option. All Apps shows favourites first, then other installed apps.
- Kids: separate illustrated catalogue, age-group choices that reorder local sample recommendations, and a parental-settings entry. These sample groups are not certified ratings and do not enforce device-wide restrictions.
- Dashboard: native three-column rounded cards and device details. RAM and storage bars use actual API measurements; CPU shows supported hardware/ABI/core information. Wi-Fi, Bluetooth hardware availability, aggregate network counters, accessible inputs/USB devices and system details use the installed device. Phone readings are explicitly labelled. Unsupported GPU/CPU usage and unsupported traffic widgets are omitted. No sample hardware values from the reference pictures are used. Hardware polling stops outside Dashboard and when the app is inactive.
- Settings: compact right-side panels, supported Android settings intents, sample-player volume, saved app appearance brightness, privacy information and confirmation before clearing queue/history or deleting local preferences. App brightness does not change system brightness or TV picture settings.
- Audio: three original 30-second offline instrumental samples; actual MediaPlayer play/pause, seek, ±10 seconds, previous/next, volume, queue and local song/artist/album search. Search and queue browsing keep the current song playing. Results contain artwork, title, artist, bundled channel and duration. The TV keyboard, Play now and Add to queue are native controls.
- Input: touch, Android Back, external keyboard/D-pad, focus rings, landscape TV preview, portrait layouts and an optional remote with physical 48dp touch buttons. Hold the on-screen OK button to invoke the focused tile's long-press action.

## Unavailable integrations

No YouTube, online/live catalogue, trailer streams, cross-app watch history, default-TV-home registration, privileged HDMI switching, device-wide parental enforcement, microphone/voice search, hide-video or screen-off/background playback is integrated. These controls are omitted or explicitly labelled unavailable. Audio pauses when Flow leaves the foreground. A black UI is not a powered-off display.

Provider names and movie artwork are illustrative labels, not statements that a title is available from that provider. App tiles and hardware readings will differ from the pictures according to what is installed and exposed on the actual device. No invented Wi-Fi networks, Bluetooth headsets, CPU load or network speed values are shown.

## Privacy

No ads, tracking SDKs, analytics or runtime network requests. The app has only ACCESS_NETWORK_STATE permission; it does not request a microphone, location, Bluetooth/nearby-device or broad storage permission. Favourites, ordering, watchlist and appearance settings stay locally. Other apps and TV firmware have separate privacy settings.

## Build and checks

JDK 17+ (tested with 21), Android SDK 35 / build-tools 35.0.0, Gradle 8.10.2. Set `ANDROID_HOME` or ignored `local.properties`, then:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

The prepared cloud snapshot can run `./scripts/build-cloud.sh` offline. Main output: `app/build/outputs/apk/debug/app-debug.apk`. Test output: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

Install both APKs in a disposable emulator, then run:

```sh
adb shell am instrument -r -w -e capture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

**Instrumentation resets that test installation's local preferences.** Require the reported pass count and successful instrumentation completion, not just adb's exit status. Optional actual screen captures are written to the test app's internal `files/visuals` folder and can be collected with `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.

See [VALIDATION.md](VALIDATION.md) for the executed checks and physical-device limitations. Native device/playback behavior is in `MainActivity.java`; reference layouts and separate artwork rendering are in `FlowDesign.java`. No third-party Android runtime dependencies or web UI are used.

Audio is generated by [scripts/generate_audio.py](scripts/generate_audio.py), using original synthesized chords. The Doto font is redistributed under its bundled [SIL Open Font License](app/src/main/assets/fonts/OFL.txt). Cinematic and Kids artwork are original generated illustrations used as sample assets.
