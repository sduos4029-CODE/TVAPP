# Full Flow TV 0.5.0 validation

Build: successful; lint 0 errors / 33 warnings; APK v2 signature verified. Package `tv.flow.mobile`, versionCode 7, API 26+, target 35. Same cloud debug certificate as 0.4.2.

[63 passed emulator checks](smoke-results.txt) · [Build log](build-results.txt) · [Lint report](lint-results.txt) · [Permissions and signature](permission-audit.json) · [Installed/download APK match](installed-apk.txt) · [17 production captures](screen-results.txt) · [Actual screenshots](../screenshots-v0.5.0) · [Network probes](network-probes.txt).

The display-state regression was fixed before this final passing run: Dashboard polling responds to screen-off and checks PowerManager rather than assuming Activity pause arrives immediately. Local playback continued while the emulator display was asleep. The production remote OK/Back test uses the actual System details drawer.

Catalogue fixtures validate parsing and UI behavior, not a live service. Streaming apps/accounts are not installed in this emulator. TVmaze/Wikidata connections were blocked by the cloud proxy; private provider account import and YouTube hide/video-off control are unsupported. Physical realme TV, Play Protect acceptance and actual phone/TV background playback remain unverified. See [full validation and boundaries](../../VALIDATION.md).
