# Installation block — Flow TV 0.4.1

The user reported “App blocked to protect your device … This app can request access to sensitive data … identity theft or financial fraud” **while installing** the 0.4.0 APK, on a phone using Prime Video, JioHotstar and Airtel Xstream.

Google’s [current Play Protect help](https://support.google.com/googleplay/answer/2812853?hl=en), retrieved 21 October 2026, says it may prevent installation of an unverified app using sensitive device permissions commonly targeted for financial fraud. This is more specific than an ordinary unknown-source prompt. The text alone does not disclose the exact classifier or prove a particular permission caused this block.

## Changes in 0.4.1

The earlier APK declared a NotificationListenerService guarded by BIND_NOTIFICATION_LISTENER_SERVICE for optional YouTube media-session control. That is broad notification access even though the implementation did not read notification contents. It is a plausible trigger, not a confirmed diagnosis.

The service/class, manifest declaration, notification-access settings prompt, listener-grant detection and external active-session acquisition are removed entirely. This is a real reduction in access and functionality, not a package-name/signature change to evade detection. Package and signing certificate remain unchanged.

Flow now opens/searches YouTube in its official app. YouTube metadata/remote control/hide video/screen-off are not offered in Flow. Local audio retains real playback, foreground media notification and its own media session. Minimal Glass Dashboard, device data, installed-app launchers and touch/remote navigation remain.

No SMS, contacts, microphone, accessibility service, notification listener, install-packages or broad-package-list permission is requested. INTERNET supports published artwork; normal device/network/TV-feed permissions and local audio foreground-service/wake-lock permissions remain. No ads, analytics or tracking SDKs are added.

## Testing and installation

Download the new versioned APK linked in README; attempt a normal update with Play Protect enabled. Do not disable Play Protect, rename packages, switch signing keys, use adb to bypass a phone block or force installation of a blocked APK.

The Android 11 test emulator has no Play Protect. Build/signature checks, installed-package manifest inspection and functional tests cannot certify that Google will accept the APK on the user’s phone. No Google approval or malware verdict is claimed. The debug APK is not distributed through a reviewed Play test track.

If the new APK is also blocked, stop installation and retain the exact warning and version. The supported next route is developer review/appeal through the link in Google’s help or distribution through a properly configured Play testing track, with the owner’s developer account. Neither submission has been performed here.
