# Review of the supplied 0.4.2 code

The user supplied three Kotlin snippets (catalogue, telemetry, audio), plus manifest/build/theme/app-list code. They were reviewed against the existing native Java project and adapted rather than copied as disconnected files.

## Adopted and connected

- Public Wikidata/TVmaze catalogue requests connected to Home, Movies, Shows, New, Kids, global search, title details and persistent My List.
- HTTPS system trust, narrow installed-package declarations and optional TV/Bluetooth/USB hardware features.
- Supplied navy/slate/mint/lavender palette and round-widget design.
- Actual elapsed-time traffic rates and cumulative totals, with baseline reset and unsupported handling.
- Existing real installed icons, favourite ordering, app launches, touch/D-pad/remote and local dot-matrix playback.

## Corrected

- Kept compile/target SDK 35; increased versionCode to 6 (5 already belonged to 0.4.1).
- Avoided adding unresolved resource names, a second audio service, a Kotlin plugin or runtime libraries to a working Java build.
- Did not embed the unused TMDB key. The supplied repository actually uses TVmaze/Wikidata.
- Did not add BLUETOOTH_CONNECT: it is a dangerous runtime permission on modern Android, not a normal permission as the pasted manifest comment suggested. Bluetooth device controls are not implemented.
- Preserved separate phone/leanback launch intent filters and the existing actual icon/banner resources.
- Preserved the tested audio service's media-notification actions, audio-focus release, decoder error handling and lifecycle management, rather than replacing it with the less complete sample service.
- A TVmaze page of old shows sorted by premiere is not current new releases. New episodes instead requests the dated global web schedule; new films use a bounded release-date interval.
- Anime does not automatically qualify as kids content. No genre label is presented as a certified age rating.
- Missing source language/date stays missing. Network transport presence does not imply validated Internet. Unsupported traffic counters stay unsupported, not zero.
- Actual app version is read at runtime. TV inputs are not all labelled HDMI.
- Safely escaped SPARQL title search, earliest release aggregation, future-date rejection, deduplication, restricted source/image hosts, bounded reads/caches and honest HTTP/parse/offline failure states.
- Wikidata's general YouTube video property is not proof of an official trailer and is not used as one.
- A Wikidata source page is not a Watch link. An original network/web-channel field is not current India streaming availability.
- Wikidata CC0 metadata does not make every P18 image public domain. Commons images require separately resolved permitted licences/source credits.
- No notification listener, accessibility access, extracted streams or Play Protect bypass was reintroduced.

## External verification limits

Requests to `https://www.tvmaze.com/api`, `https://api.tvmaze.com/search/shows?q=Panchayat` and `https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service` were denied by this environment's CONNECT proxy (403 before HTTPS). No mirror, alternate route or fabricated successful result was substituted.

API shapes and attribution assumptions derive from the supplied code and known public endpoint contracts; current official documentation could not be re-read from this environment. Verify live behavior, terms, CC BY-SA obligations, catalogue coverage, source rate limits and artwork licensing before wider distribution. API endpoints, source information links and user-facing errors are implemented so mobile testing can establish these facts on an allowed network.

The existing YouTube feasibility/Play Protect documentation remains relevant. Neither successful compilation nor synthetic parsing tests establish live catalogue access, actual provider playback, Google review or acceptance on the user's phone.
