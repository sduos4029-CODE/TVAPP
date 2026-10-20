# Flow TV 0.2.0 — validation

## Build and installation

The final source passed `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` with JDK 21, Gradle 8.10.2 and Android SDK/build-tools 35. Lint reported 0 errors and 24 warnings, including prototype English strings and RTL/resource presentation warnings.

The debug APK is `tv.flow.mobile`, versionCode 2 / versionName 0.2.0, minSdk 26 (Android 8), targetSdk 35. APK Signature Scheme v2 verification passed. Its signing certificate matches the previously uploaded v0.1.0 APK, allowing an update that retains local preferences. Only ACCESS_NETWORK_STATE is requested.

The main and instrumentation APKs installed successfully on an Android 11 / API 30 x86_64 phone emulator using software acceleration. The raw final build and instrumentation output is in [releases/validation](releases/validation).

## Executed checks

All 13 native instrumentation smoke checks passed:

1. Home sample labels and navigation sections.
2. Local catalogue content-type, genre, language and provider filters update displayed results.
3. Kids age selection changes the local sample recommendations.
4. Watchlist changes persist across activity destruction.
5. Watchlist is restored at next launch.
6. Dashboard reads actual device information and handles unavailable phone TV controls.
7. Drawer focus stays within the panel and Back closes it.
8. MediaPlayer plays bundled audio; search, empty results and queue actions work while current playback continues.
9. Sample seeking and pause/resume change actual playback state.
10. Favourite ordering persists, and cancelling a move restores the saved order.
11. Remote target focus is established.
12. Injected touch on the on-screen OK button activates the focused item; Back closes it.
13. Android system Home backgrounds the activity, pauses audio and stops dashboard polling.

The final raw result reports `passed=13`, `13 checks passed`, and `INSTRUMENTATION_CODE: -1`, without FAIL/crash output. An adb exit status alone is not the acceptance check.

## Visual review

[18 screenshots](releases/screenshots) were captured from the final running APK: Home, New, installed Apps, Kids, Quick settings, Wi-Fi, Sound/display, Privacy, Dashboard, Ports, Audio, Song search, Movies, Shows, My list, title details, portrait Home and portrait Audio. Landscape and portrait layouts were inspected against the supplied references. These are actual app captures, not the concept images.

Images are separate bundled artwork. Navigation, text, cards, focus indicators, drawers, keyboard, seek bars and actions are native Android views. Hardware cards display the emulator's actual readings, and app tiles use its installed packages. Captures include a small saved watchlist and favourite selection created by the test fixture; they do not represent imported viewing history. On a phone, readings and app tiles will reflect that phone.

## Reproduce

Run `./scripts/build-cloud.sh` in the prepared environment, or follow the SDK build instructions in README.md. Install the main and instrumentation APKs in a disposable emulator, then run:

```sh
adb shell am instrument -r -w -e capture true tv.flow.mobile.test/tv.flow.mobile.FlowSmoke
```

The test runner clears that installation's local preferences. Optional captures are stored in the app's internal `files/visuals` folder; collect them using `adb exec-out run-as tv.flow.mobile tar -cf - files/visuals`.

## Limits

No physical phone or realme RMV2004 TV was attached. Physical remote long-press, touch drag, vendor settings, TV inputs/USB behavior and TalkBack need device testing. Media decoding, progress and state were verified with the emulator's virtual audio sink; audible speaker output was not acoustically measured. Android 11 was executed; Android 8+ is the installation requirement, not a claim that every supported OS/device has been tested.

The catalogue, artwork and provider labels are illustrative. Live catalogues, YouTube, trailers, cross-app history, privileged HDMI switching, device-wide parental enforcement, voice search, video hiding and screen-off/background audio remain unavailable and are labelled accordingly. Unsupported metrics are omitted; sample CPU, RAM, storage and network values from the reference images are never used as device measurements.
