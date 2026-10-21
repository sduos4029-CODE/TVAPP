# Flow TV Prototype 0.1.1 — corrected archive build

This is the corrected version of the supplied FlowTV.zip, retaining its Kotlin/Compose project and package `com.flowtv.launcher`. It is a **small launcher prototype**, not the full `tv.flow.mobile` Flow TV application. It has no movie catalogue API, Dashboard, audio player or on-screen remote. It supports touch, external keyboard/D-pad, normal phone launch, actual installed app icons/banners and optional OS-permitted TV programme cards.

## Install and open

Android 8/API 26 or newer. Open **Flow TV Prototype** from the phone/TV app list. **Flow Prototype Settings** is a separate recovery entry. Both remain launchable when this app's optional Home alias is disabled.

The prototype installs beside the full Flow TV application. No root, privileged system installation, shell Home assignment or device modification is required. Do not disable Play Protect. The test emulator has no Play Protect; acceptance on a physical phone is unverified.

TV listings do not expose private streaming-app logins/catalogues. A phone reports that the shared TV feed is unavailable. On TV, denied/unsupported/empty feeds are handled without invented programme cards. Source artwork loads only from supported published URI schemes and may remain unavailable.

## Optional TV Home selection

The Home alias is **disabled by default**. On an actual TV, an explicit settings action enables it as a candidate and opens Android Home settings. The user selects the default there; enabling a candidate is not proof of selection. The phone UI does not enable the TV Home alias. Actual TV/firmware behavior is unverified.

Recovery disables only this app's optional Home alias, preserving the normal app/settings entries. Three recent crash timestamps trigger startup recovery. The original system exception handler still receives errors. Long Volume Up / three delivered HOME intents are TV/default-Home-only escape attempts; physical remote/system delivery is not verified. No app can guarantee interception of all system keys or recovery from every failure before its own startup code runs.

## Privacy

The app requests INTERNET for published artwork and standard READ_EPG_DATA for permitted TV metadata. AndroidX also adds a package-scoped signature permission for its internal receiver guard; this grants no access to other apps’ data. It does not request notification-listener, accessibility, microphone, SMS, contact, broad package visibility or privileged TV permissions. No ads/analytics/tracking SDKs are added. App artwork requests follow the publishers' own privacy practices. Guard preferences stay local and are excluded from backup/transfer.

## Build

JDK 17+ (tested 21), Android SDK/build-tools 35 and the included Gradle 8.10.2 wrapper:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug
```

Kotlin/Compose 2.0.0, AGP 8.7.3, Compose BOM 2024.06.00, TV Material 1.0.0 and Coil 2.6.0. First build needs access to Google/Maven public dependencies. The prepared cloud environment may use `python3 scripts/build-cloud.py`; that helper reads inherited proxy settings and system Java trust without embedding credentials.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Package `com.flowtv.launcher`, versionCode 2 / versionName 0.1.1. Another build machine may use a different debug certificate; do not silently uninstall an existing installation and its data to work around a signature mismatch.

## Disposable-emulator checks

Install main/test APKs, then run:

```sh
adb shell am instrument -r -w com.flowtv.launcher.test/com.flowtv.launcher.PrototypeChecks
adb shell am instrument -r -w -e guardRestart true com.flowtv.launcher.test/com.flowtv.launcher.PrototypeChecks
```

The first phase resets this prototype's guard state and seeds three recent crash timestamps at the end; immediately run the second phase to verify startup recovery and clear them. Tests operate only this app's alias/state and do not select a new default Home. Do not run them on a personal installation. Phone fixtures do not establish a real TV provider connection.

Screenshots-only mode: `-e screensOnly true` in a landscape emulator. Captures are stored under internal `files/prototype-visuals`. No synthetic successful catalogue rows are used for screenshots.
