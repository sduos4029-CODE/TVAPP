# Flow TV Prototype 0.1.1 — validation

2026-10-21. This validates the corrected supplied archive, package `com.flowtv.launcher`, not a new build of the full `tv.flow.mobile` app. The full 0.4.2 APK and its latest-APK alias retain SHA-256 `ac3ac6bd3d3582ce587f1054925dad15522027745fbde7bcc9aa36404c665261`.

## Build and package

JDK 21, Gradle 8.10.2, AGP 8.7.3, Kotlin/Compose 2.0.0, Android SDK 35. Executed `assembleDebug assembleDebugAndroidTest lintDebug`: successful. Lint: 0 errors, 2 warnings (320×180 TV vector banner recommendation; legacy fullBackupContent recommendation with allowBackup already false). Neither warning is hidden by a baseline. Android 8/API 26 remains supported.

[Build log](build-results.txt) · [Lint report](lint-results.txt) · [Merged-permission/signature audit](permission-audit.json) · [Original archive fingerprint](input-archive.txt) · [Installed/build/download hash comparison](installed-apk.txt) · [Artifact checksums](../PROTOTYPE-SHA256SUMS.txt).

Only INTERNET, standard READ_EPG_DATA and AndroidX's package-scoped signature receiver guard are requested. No root, privileged provisioning, notification listener, accessibility service, broad package enumeration or microphone access is added. UI automation used for tests runs in the separate instrumentation package, not an accessibility service in the delivered app.

## Emulator checks

Disposable Android 11/API 30 x86_64 phone emulator, 540×960 override, portrait then landscape rotation. APK installation succeeds and the installed APK hash is compared with the delivered build. This emulator has no Play Protect.

- [Phone suite](smoke-results.txt): 14 checks covering ordinary launch/recovery entries, permissions, real deduplicated installed apps/icons, honest unsupported-phone-feed state, synthetic public-cursor parsing, long IDs, unsafe intent/URI filtering, Home-press/crash timestamp helpers, actual touch into All Apps and an installed app, system Back, D-pad Right/OK into Settings, phone Home-candidate refusal, unchanged default Home and recovery entry survival.
- [Next-start recovery](guard-results.txt): 2 checks. The first suite seeds recent crash timestamps; the next actual Application startup enters recovery, disables only its optional Home alias and preserves the recovery screen. This is seeded history, not three induced fatal crashes.
- [Landscape captures](screen-results.txt): two screenshots captured through the same touch navigation. These are visual captures, not two additional functional assertions.

[Actual emulator screenshots](../screenshots-prototype-v0.1.1) contain real installed apps and capability explanations, with no injected movie rows. Portrait Home/All Apps/Settings and landscape Home/All Apps were visually inspected for readable text and usable controls. The recovery capture comes from the next-start guard check.

## Still unverified

Physical realme RMV2004 Home settings/default selection, vendor remote escape/key delivery, actual TV provider access, source artwork retrieval, low-memory TV operation and physical-phone Play Protect acceptance. No YouTube playback, Hide Video, powered-off display or vendor picture-off integration is implemented by this prototype.

The archive's root/remount/default-Home assignment/whole-package-disable instructions were not executed. Disabling this prototype's optional alias during testing does not disable its package or change another app's Home selection. The separate full Flow TV app retains Dashboard and local audio; those features are absent from this prototype.
