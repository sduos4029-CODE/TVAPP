# Public catalogue versus signed-in apps

The user tests on an Android phone signed into **Prime Video, JioHotstar and Airtel Xstream**. Android isolates those apps' private catalogues, credentials, account sessions and viewing history. Flow cannot read them merely because the apps are installed or signed in.

## New public-metadata route

The supplied code proposes two keyless public sources. Version 0.4.2 wires those sources into the native UI:

| Source | Requests | Meaning / limits |
| --- | --- | --- |
| Wikidata | Public SPARQL film query; selected original language, optional safely quoted title match and children's-film genre | Structured film metadata, source release dates and optional source images. Coverage is incomplete. It supplies no subscription or current India provider availability. |
| TVmaze | `/shows?page=…`, `/search/shows?q=…`, `/schedule/web?date=…` | Global series directory/search and scheduled episode data. A broadcaster/original web channel is not a current regional streaming-availability claim. |
| Wikimedia Commons | MediaWiki `imageinfo` for a Wikidata source image | Only resolved permitted licence/source credits are displayed; missing or unverified art remains unavailable. Images are not necessarily posters. |

**This environment's proxy blocks TVmaze, Wikidata and their documentation endpoints. No successful live catalogue response has been verified here.** Fixture tests are not provider connectivity evidence. The app contains real HTTPS requests and handles errors; the phone's own connectivity and source behavior need testing.

No API key is required for this route; the pasted TMDB BuildConfig key was unused and was not added. A future TMDB or provider integration would need separate implementation, configuration and licensing checks.

## What buttons actually do

A title's source page is an information page, not playback. **Open a streaming app** uses an installed app's real launch entry, explicitly without an exact-title guarantee. That app retains login/subscription checks. No deep link, trailer, entitlement, watch history or provider availability is invented. Movie/show watchlists store selected metadata locally; saved YouTube links remain separate.

Current sources cannot support the requested verified India provider filters. Those controls are not offered. They also cannot verify provider-added dates, personalised recommendations, certified kids age ratings or playback progress. New films uses earliest source release dates within 90 days; New episodes uses source-scheduled dates. Kids is a genre collection, not a safety certification or device-wide lock.

## Optional Android TV route

The earlier standard TvContract Watch Next / Preview reader remains for real TV devices. It reads only OS-permitted public fields, honours browsable/searchable flags and avoids private account data. A readable app-published card remains a limited recommendation, not the full catalogue. Firmware can restrict access; no physical realme TV/provider feed was verified. A phone normally lacks that shared TV feed.

No private API scraping, account-cookie extraction, accessibility harvesting, notification listener or privileged permission bypass is used.
