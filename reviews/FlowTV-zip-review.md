# FlowTV.zip — review, fixes and comparison

The user requested both **review/fix/build** and **comparison with the existing project**. The archive's README was treated as source material, not authorization to root a device, remount system partitions or select a replacement Home launcher. None of those commands was executed.

## Recommendation

Keep the existing `tv.flow.mobile` Flow TV app as the full-product implementation. This ZIP is a different, smaller Kotlin/Compose launcher, now corrected for ordinary phone testing and optional TV Home selection. Its package remains `com.flowtv.launcher`, so the corrected prototype can be installed beside the existing app. Its different identity comes from the supplied project, not a package rename intended to bypass an installation block.

[Prototype APK](../releases/Flow-TV-Prototype-v0.1.1-debug.apk) · [Portable source ZIP](../releases/Flow-TV-Prototype-v0.1.1-source.zip) · [Source tree](../prototypes/launcher-review) · [Current full Flow TV APK](../releases/Flow-TV-v0.4.2-debug.apk)

## Comparison

| Area | Supplied ZIP | Corrected prototype 0.1.1 | Existing Flow TV 0.4.2 |
| --- | --- | --- | --- |
| Package | `com.flowtv.launcher` | Same as ZIP; separate install | `tv.flow.mobile` |
| Implementation | Kotlin/Compose TV | Kotlin/Compose TV, retained | Native Java/platform views |
| Phone launch | No normal Launcher entry; TV feature required | Normal phone and TV entries; TV feature optional | Supported |
| Home selection | Home alias enabled; README suggests shell selection | Disabled initially; phone UI cannot enable it; explicit TV action opens Android Home settings | Normal app, no forced Home replacement |
| Default-Home label | Claims Flow is Home from a safe-mode boolean | Reports actual Android intent resolution; enabling a candidate does not claim default selection | Does not claim default-Home status |
| App discovery | Leanback apps only, duplicate packages possible | Phone and TV apps, deduplicated; actual icons/banners, launch fallback | Actual apps, favourite ordering and recent launches |
| Programme cards | Privileged setup suggested; only SecurityException handled | Small public TV projection, hidden/non-searchable filtering, stable IDs, restricted/missing feeds handled | Optional OS-permitted TV feed plus public metadata implementation |
| Phone film/show catalogue | None | None; accurately explains unavailable TV feed | Wikidata/TVmaze request layer; live retrieval remains unverified because source routes were blocked in the build environment |
| Dashboard / audio | Absent | Absent | Minimal Glass device Dashboard and functional local dot-matrix audio |
| Remote testing | TV D-pad intended | Touch, external D-pad/OK and Back checked on phone emulator | Touch, D-pad and optional on-screen remote |
| Settings/recovery | TV-only recovery entry; misleading Home status | Phone/TV recovery entries stay enabled when optional Home alias is disabled | Compact settings for the full app |
| Permissions | Broad TV permissions and privileged allowlist instructions | INTERNET + standard READ_EPG_DATA and AndroidX internal receiver guard; no privileged provisioning | Normal network/device/audio/optional TV-feed permissions; no notification listener |
| Build delivery | No APK or Gradle wrapper included | Debug APK, wrapper, source, checks and captures | Versioned APK, source and previous 57-check validation |

Do not replace the full app with this prototype expecting Dashboard, audio, favourites reordering, movie tabs or a phone catalogue connection. They are separate feature sets.

## Findings fixed

1. Added normal phone and leanback activity entries. Home integration is an optional disabled alias, separate from ordinary app/recovery launchers.
2. Removed the privileged permission requests, allowlist file and root/remount/install instructions from the corrected distribution. No permission bypass or private catalogue import is substituted.
3. Added the missing Internet permission needed for source artwork, disabled cleartext traffic, disabled backup/transfer of local guard preferences and added branded icon/banner resources.
4. Preserved package identity; versionCode increased from 1 to 2 / versionName 0.1.1. Updated to SDK 35/AGP 8.7.3 with a Gradle 8.10.2 wrapper. Retained the original Kotlin/Compose implementation and downloaded its public build dependencies through the supported proxy.
5. Fixed phone app discovery and duplicate packages, actual icon fallback without stretching icons into banners, missing/removed app handling and real launch intents.
6. Replaced broad TV projections with a bounded explicit public-field reader. Rejects hidden/non-searchable/blank/own records, preserves 64-bit IDs and catches restricted/unsupported provider behavior. A phone never pretends to have a TV feed.
7. Distinguishes CONTINUE records from other Watch Next cards; duplicate title/image combinations no longer create duplicate Compose item keys.
8. Reconstructs title links as package-bound ACTION_VIEW intents, dropping source extras/components/selectors and URI grants. Rejects dangerous schemes/credential URLs; falls back to explicitly opening the source app.
9. Registers package/feed observation only while visible and cancels pending refresh work on stop. Uses compatibility registration and handles absent providers gracefully.
10. Uses explicit navy/slate/mint/lavender colors, round cards, mint focus indicators, correct light text contrast, phone-aware spacing and scrollable native controls.
11. Crash history records actual failure time, filters stale/future/malformed timestamps, retains only three and forwards errors to the original system handler. Synchronous crash-state writes are intentional because the process may terminate immediately.
12. Recovery disables only this app's optional Home alias. The archive's `disable-user` command would disable its recovery screen too; merely re-enabling an alias would not reverse whole-package disablement. Those instructions were removed.
13. Default-Home status comes from Android resolution, not an internal boolean. TV candidate enabling opens supported settings; it does not silently select a default. The phone path refuses TV candidate enabling.
14. Replaced the inherited restricted `ComponentActivity.dispatchKeyEvent` override with public Activity key callbacks. TV escape logic is gated to actual TV/default-Home use; ordinary phone volume keys are not repurposed.

15. Added pointer tap handling to TV Material buttons/cards, which otherwise handle D-pad/Enter and accessibility actions but not touchscreen gestures. Preserved a single keyboard focus target per control. Touch now has a separate real-input check for app-grid navigation and actual installed-app launching.

## Verification boundaries

The supplied ZIP was reviewed statically; the corrected source was built and tested. Original README recovery claims were not treated as verified behavior. Tests use synthetic cursor records only for parsing/link sanitization, and a seeded recent-crash history to check the next actual Application startup. They do not establish readable provider data or a physical TV crash/remote escape.

Normal phone launch, installed-package data, touch/D-pad navigation, recovery entry and unchanged Android Home resolution are checked in a disposable Android 11 emulator. This emulator has no Google Play Protect. APK/signature/adb installation success does not prove acceptance on the user's phone. Keep Play Protect enabled.

Physical realme RMV2004 Home selection, vendor Home settings, power/volume key delivery, rapid Home-intent delivery, actual TV feed access, artwork fetches and low-memory TV behavior remain unverified. The prototype does not implement YouTube playback, video hiding, screen-off audio or vendor picture-off.

[Build/test validation](../releases/prototype-validation/README.md) separates executed results from these pending checks.
