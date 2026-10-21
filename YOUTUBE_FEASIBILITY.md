# YouTube listening feasibility — Flow TV 0.4.2

## Official documentation reviewed

Reviewed 21 October 2026 (Asia/Kolkata). The current official pages below were fetched successfully over HTTPS and read during this task. Initial requests were blocked; later requests succeeded. [Retrieval evidence](releases/validation/official-docs-reviewed.json) records URLs, response status and hashes. Documentation establishes supported features and API contracts; it does not prove that a particular account, app installation or TV firmware works.

1. [YouTube Premium benefits](https://support.google.com/youtube/answer/6308116?hl=en): background playback with the screen off is a mobile-app benefit for eligible signed-in Premium users. The Shorts player is excluded. Background-play settings can disable/restrict it. The Android app also documents **On-the-go** listening controls, including its own video/audio toggle, for Premium.
2. [YouTube/YouTube Music supported devices](https://support.google.com/youtube/answer/6308244?hl=en): the TV-connected-devices section limits offline/background playback to mobile apps. TV ad-free playback is a separate benefit. Mobile listening capabilities must not be promised on Android TV.
3. [YouTube Music audio-only/video mode](https://support.google.com/youtubemusic/answer/6313574?hl=en): music Song/Video mode requires YouTube Music Premium or YouTube Premium. Podcast audio-only rules differ and content/region exceptions exist. This task concerns songs; podcast exceptions are not treated as free music entitlement.
4. [YouTube API developer policies](https://developers.google.com/youtube/terms/developer-policies): clients must not separate/isolate audiovisual components or create background API players hidden from the viewed screen. A Data API credential does not authorize audio extraction or hiding an embedded player.
5. [YouTube Data API search](https://developers.google.com/youtube/v3/docs/search/list): search returns resource metadata. It is not an audio stream or playback queue. No API credential is configured in Flow.
6. [Android MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager) and [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService): the documented active-session APIs accept an explicitly enabled notification-listener component instead of a privileged MEDIA_CONTENT_CONTROL grant. Listener access has platform/profile/low-memory restrictions; it is not universal TV support.
7. [Android notification permission exemptions](https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions): media-session notifications are exempt from the Android 13 notification-permission behavior change. Flow posts only its local media notification, without requesting general notification posting permission.
8. [Android PowerManager](https://developer.android.com/reference/android/os/PowerManager): a partial CPU wake lock supports CPU execution, not display/backlight power control. Flow uses no privileged sleep/panel command.
9. [realme support](https://www.realme.com/in/support) and [official manuals](https://www.realme.com/in/support/user-manuals): the reviewed listing did not establish an RMV2004 guide or Picture Off command. No exact RMV2004 picture-off capability was verified. The older `/in/realme-tv/specs` route returned 404 and is not evidence of a missing hardware feature.

## Capability decisions

| Experience | Android mobile | Android TV / realme RMV2004 | Flow behavior |
| --- | --- | --- | --- |
| Normal YouTube video | Official YouTube/YouTube Music player | Official YouTube TV app | Real search/link/app hand-off. Actual YouTube playback was not exercised in this environment. |
| Official listening mode | Eligible YouTube Premium Android users can use video Settings → On-the-go. Eligible Music users can select Song instead of Video inside YouTube Music. | Mobile background/listening benefits do not establish TV support | Flow offers Open YouTube and guidance; it does not remotely toggle private source modes. |
| Dot-matrix YouTube controls | Not offered by Flow 0.4.1 | Not offered by Flow 0.4.1 | Notification-listener and external media-session access were removed. Use the official YouTube app. Flow’s dot-matrix controls work with local audio only. |
| YouTube Hide Video | Hidden embedded/API playback is not a permitted substitute for official audio mode | Same API constraint | Unavailable. No embedded player, extractor or hide-video switch exists. |
| YouTube screen-off audio | Official source can support eligible membership/content/settings; Flow cannot grant or guarantee it | Background playback is not a documented TV benefit. Vendor Picture Off with YouTube still foreground is a separate hardware capability | Unverified with actual YouTube; not labelled working. No YouTube Black screen workaround. |
| Local audio dot-matrix screen | Flow-owned media session, real files, queue/search/seek/volume and media notification | Same implementation; physical TV/decoder/remote testing pending | Implemented with original bundled audio and user-selected content URIs. |
| Actual display off during local audio | User's physical power button; foreground media service/partial CPU wake lock support continued local playback | Vendor Picture Off must be verified. Standby can stop audio | Local display-off and wake are verified in an Android 11 emulator, not a physical phone/TV. Flow does not itself power off a panel. |
| Black screen | Full-screen black view for local audio, dismissible by touch, Back or keyboard/remote | Same view, subject to physical TV input testing | Labelled **display stays on**. Final capture has RGB 0 throughout and no widgets. Black pixels do not prove backlight/panel shutdown. |

## Android media access and privacy

Version 0.4.1 removes the notification-listener service, its manifest binding, Android notification-access settings entry and all external active-media-session queries. This reduces sensitive access following a reported Play Protect installation block; it does not establish that Google has reviewed or approved the new APK. The previous 0.3/0.4 implementation is historical, not a current capability.

Flow cannot read or control YouTube metadata, artwork, playback state, seek or queue. Its official-app search/link hand-off remains. Opening YouTube does not grant Flow a controller. Android’s documented media-session APIs do not grant Premium, API credentials, audio extraction or display-power control. No substitute hidden player is used.

The local source uses Flow's own MediaSession and user-started foreground media service. It requests audio focus, responds to focus loss, and uses MediaPlayer's partial CPU wake lock during playback. It does not hold the display awake. A media notification offers Play/Pause and Stop. Local files are selected through the document picker; no microphone or broad storage access is requested. Flow has no ads or tracking SDKs. YouTube and other opened apps have their own account, advertising and privacy practices.

## realme RMV2004 follow-up

No model-specific Picture Off API or verified manual entry was found in the official material reviewed, and no TV was attached. Flow therefore does not claim a working picture-off command. ACTION_DISPLAY_SETTINGS opens available system controls; it does not prove that Picture Off exists. Privileged sleep APIs and device-admin tricks are not used.

On the actual TV verify the exact firmware, whether supported Picture Off exists, whether it differs from remote power/standby, continued local audio and YouTube audio with its own player still foreground, the remote key that restores the picture, and audio/focus after returning to Flow. Picture Off could leave a foreground video app running; that is distinct from unsupported YouTube background playback. A truly powered-off display cannot show Flow widgets. An emulator power-state test does not prove TV picture-off or backlight shutdown.

## Executed versus pending

[VALIDATION.md](VALIDATION.md) records the current build/signature and emulator checks, real local decoding/queue/seek/import, absence of sensitive services/permissions and local-only media control and local display-off/wake. No official YouTube installation, paid account, physical phone or realme TV was attached. YouTube metadata/actions/source queue are unsupported in Flow 0.4.1. Official-app background audio, vendor picture-off and real-TV wake behavior remain unverified. These are not described as successful simulated integrations.
