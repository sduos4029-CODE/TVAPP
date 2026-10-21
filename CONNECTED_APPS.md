# Existing logged-in apps — what Flow can actually read

Flow cannot reuse a streaming app's sign-in merely because it is installed. Android isolates app storage, account/session tokens and private catalogues. No password, cookie, private database, accessibility scraping or hidden provider endpoint is used.

## Implemented route

Android TV defines public programme metadata tables:

- [TvContract.WatchNextPrograms](https://developer.android.com/reference/android/media/tv/TvContract.WatchNextPrograms)
- [TvContract.PreviewPrograms](https://developer.android.com/reference/android/media/tv/TvContract.PreviewPrograms)

The current WatchNextPrograms reference was retrieved and reviewed on 21 October 2026. It documents programme types, NEW/CONTINUE flags, canonical genres, poster/title-link fields and browsable/searchable constraints. It explicitly requires proper permission for reading searchable records; this is not unrestricted access to every app's data.

Flow declares the standard READ_EPG_DATA permission and provider visibility, then queries these OS tables only where present. The TV firmware/OS can deny access or filter records. Flow honours non-browsable/non-searchable records, uses only records from other packages, and does not request a privileged bypass or force a home-launcher role. Queries run off the main thread. Change observation is active only while Flow is visible on a content page.

Movies uses only TYPE_MOVIE. Shows uses TV series/season/episode types. New uses only explicit WATCH_NEXT_TYPE_NEW. Kids requires the canonical FAMILY_KIDS genre and does not claim certified ratings. Missing fields are not invented. My list stores the published metadata locally after a user saves a title. A programme link is narrowed to ACTION_VIEW in the source package; if it cannot resolve, Flow opens that app and explains that no supported title link was available.

Artwork is loaded only from source-published HTTPS/content/resource URIs, with size limits, 24-card pages, reduced-size decoding and a bounded memory cache. Android enforces content-URI access. No account cookies or authentication headers are attached. Missing artwork is labelled, not replaced with fictional movie art.

## Limits

- This is a limited feed of app-published recommendations/Watch Next cards, not a full streaming catalogue, subscription check or import of private viewing history.
- Android phones generally do not expose this Android TV feed. Signing in to an installed phone app does not populate these tables.
- Some TV firmware may restrict third-party readers or expose no records. A declared normal permission does not override provider restrictions.
- No physical realme TV or logged-in streaming app was attached here. Actual readable records, publisher coverage, poster access and title deep links still need device testing.
- The exact logged-in app names and whether they are on the phone or TV remain unspecified. That information is needed before claiming a provider-specific account integration is possible.
- A separate authorized metadata catalogue could supply real titles/posters, but it would not inherit the installed app's login, watch history or entitlement. It has not been substituted for the requested existing-app source.

The Movies/Shows/New/Kids screens therefore show an honest unavailable/empty state when the OS supplies no matching data. App-launch tiles remain in Home/All Apps; they are not passed off as movies, shows or new releases. The public fictional Design Preview switch was removed and its previous preference is cleared on upgrade.

## Verification boundary

Phone unavailability, tab separation, local watchlist operations and typed filtering are tested. Typed filtering uses synthetic instrumentation fixtures that are not shipped as catalogue data and are removed before screenshot capture. There is no claim that a full Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5 or YouTube catalogue was accessed. Actual provider integration requires its documented API or permitted TV publishing route and device verification.
