# Flow TV 0.4.0 — rounded widgets and correctly typed app content

[Download APK](releases/Flow-TV-v0.4.0-debug.apk) · [Actual screenshots](releases/screenshots-v0.4) · [Connected-app limits](CONNECTED_APPS.md) · [Validation](VALIDATION.md)

**Signing in to Netflix, Prime Video or another installed app does not give Flow access to that app's full catalogue or private account data.** This build reads only programme cards that Android TV exposes through its documented shared provider, where permission and firmware allow it. A phone normally has no such feed. Unavailable tabs stay empty with an explanation; there are no replacement app grids, generic trailer searches or invented movies.

## What each tab means

| Tab | Actual data used |
| --- | --- |
| Home | Installed favourites, apps opened through Flow, and readable programme cards published by TV apps |
| Movies | Shared records explicitly typed as movies |
| Shows | Shared TV series, seasons and episodes |
| New | Records explicitly marked NEW by the publishing app; never newly installed apps |
| Kids | Records with the source's canonical family/kids genre; no invented age ratings or device-wide enforcement |
| My list | Locally saved shared programme cards; YouTube links remain a separate drawer |
| Dashboard | Actual installed-device readings in round slate widgets and circular RAM/storage gauges |
| All Apps | Real installed packages, their supplied icons, launch actions and saved favourite ordering |

The public sample-design switch was removed, and the old sample preference is cleared on upgrade. Fictional movie artwork is not a fallback for a missing catalogue. Historical sample layouts remain only as test code/assets; bundled original local audio remains intentionally available for lawful playback testing.

## Minimal Glass Dashboard

Large rounded slate cards with thin rims, circular icon wells, mint/lavender accents and matching rounded focus outlines. RAM and storage rings use actual device totals and availability. CPU shows actual supported hardware/ABI/core information, not a fabricated load. Wi-Fi shows permitted transport/validation data; Bluetooth shows hardware availability; network numbers are real OS totals; USB/TV inputs use accessible system APIs. Phone data is labelled. Unsupported metrics are hidden, and polling stops off Dashboard or while inactive. Solid backgrounds and static drawings avoid live blur and decorative animation.

## Reading existing apps

Where available on an Android TV, Flow queries the standard Watch Next and Preview programme tables, honours hidden/non-searchable records, filters by source type, and listens for changes only while the relevant screen is visible. A shared item opens an exposed title link in its source package, or opens that app with an explicit fallback message. The provider controls login, membership and playback. A published card is not proof of entitlement or a full provider catalogue.

**Connected app content** in Settings explains the detected status. Shared artwork uses only supplied HTTPS/content/resource URIs. Missing or inaccessible artwork is labelled. No cookie extraction, database scraping, accessibility harvesting or account-token reuse is implemented. The app does not force itself to become the phone's home launcher or request privileged cross-app access.

Actual TV feed access remains unverified: this environment has only a phone emulator. Some TV firmware may restrict third-party readers to no records. Specific account integrations need an official provider API/partnership and explicit authorization; the names of the user's apps/device are still needed to assess that route. See [CONNECTED_APPS.md](CONNECTED_APPS.md).

## Audio and YouTube

The original Nothing-inspired audio screen uses black/white dot typography and restrained red accents. Local audio has real MediaPlayer decoding, user-selected file imports, metadata/artwork when present, search by song/artist/album, queue, seeking, previous/next, pause/resume and device volume. A user-started foreground media service supports ongoing local playback. Local display-off/wake is tested in an Android 11 emulator. **Black screen · display stays on** is separate and has no widgets; it does not power off a panel/backlight.

YouTube uses its official app/search. Optional Android notification access can expose actual mobile YouTube media-session metadata/actions, when the source supports them. Flow explains the broad access before opening Android settings and does not read/store notification contents. YouTube background playback is documented as mobile-only, so Flow disables that integration on TV. Actual YouTube playback, realme RMV2004 Picture Off and physical TV wake remain unverified. No hidden API player, stream extraction or Premium bypass exists. [Official YouTube feasibility findings](YOUTUBE_FEASIBILITY.md) remain applicable.

## Privacy

No ads, analytics, tracking SDKs or microphone permission in Flow. Favourites, saved titles/links and local-file references stay on the device. Android's TV provider controls what metadata can be read. The app declares READ_EPG_DATA and provider visibility for permitted TV listings; denial is handled as unavailable. INTERNET is used for shared publisher artwork, whose server can observe those requests and has its own privacy practices. Flow does not attach account credentials or cookies. External apps retain their own privacy policies.

Deleting Flow preferences stops local playback, clears its local lists and releases selected document grants. It does not delete original music files, modify other apps' data or revoke Android notification access; revoke that separately in system settings.

## Installation and build

Android 8+. Download the versioned APK and install it over the existing Flow TV app to retain local data. Application ID `tv.flow.mobile` and debug signing certificate are unchanged. The top-bar grid icon controls landscape TV Preview, portrait/rotation and the optional touch remote. Native focus supports external keyboard/D-pad and Android Back.

JDK 17+ (tested 21), Android SDK/build-tools 35, Gradle 8.10.2:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

The prepared environment can use `./scripts/build-cloud.sh`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

Install main/test APKs in a disposable API 30 emulator, then:

```sh
adb shell am instrument -r -w -e liveCapture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

The test resets local preferences, uses clearly synthetic programme records only to check classification/watchlist behavior, temporarily grants/revokes media access, launches Settings, creates/deletes its own audio fixture and sleeps/wakes the emulator. Do not run it on a personal installation. Test-only sample data is removed before production screenshots. Optional captures are in internal `files/visuals`; collect with `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.

Doto is bundled under its SIL Open Font License. Original sample audio is generated by `scripts/generate_audio.py`. No third-party Android runtime libraries or web UI are used.
