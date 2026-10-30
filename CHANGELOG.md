# Changelog

All notable changes to this service are documented here.

The format is based on [keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). See [VERSIONING.md](VERSIONING.md) for how versions are chosen and released.

## [Unreleased]

### Added

- OpenAPI spec at `/v3/api-docs` (and `/v3/api-docs.yaml`) and Swagger UI at `/swagger-ui.html`, generated from the controllers. On by default; turn off with `SPRINGDOC_APIDOCS_ENABLED=false`, or set `OPENTUBE_OPENAPI_SERVERURL` to advertise a public base URL

### Removed

- `X-Stream-Type` header on the muxed fallback (deprecated in the previous release)

### Fixed

- Main audio track with no name or language labelled "Unknown" instead of "Original"

## [0.4.0] - 2026-10-18

### Added
- Kiosk stream items now include `uploaderAvatars` (`url`, `width`, `height`, `estimatedResolutionLevel`), matching the avatar shape used in search and related-item responses. (#28)

### Changed
- Kiosk `thumbnails` entries now include `estimatedResolutionLevel`. (#28)

## [0.3.0] - 2026-10-11

### Added
- `DashPlayabilityFilter`: the single gate for streams that can go into an on-demand DASH manifest (#17)

### Changed
- `/dash` describes every playable stream and leaves quality, codec, audio language and subtitle choice to the player (#17)
- Audio AdaptationSets are split by track and codec, with a `Role` and `<Label>` per track
- Manifest is served as `application/dash+xml` (was `application/xml`)
- Muxed fallback is served as `text/uri-list` (was `text/plain`) and is used whenever no playable video or audio remains after filtering
- `mediaPresentationDuration` has millisecond precision
- Audio and subtitle languages use full tags (`pt-BR`, `en-GB`)

### Deprecated
- `X-Stream-Type` header on the muxed fallback; check `Content-Type` instead. Removed in the next release.

### Removed
- `StreamSelectionService`

### Fixed
- Audio AdaptationSets mixing WebM/Opus with MP4/AAC
- Dubbed, descriptive and original tracks in one language merged into a single set
- Manifests emitted without `SegmentBase` when no stream had byte ranges
- Invalid subtitle role (`subtitles` instead of `subtitle`) and srv formats labelled as TTML
- Unescaped manifest attributes
- Subtitle `language` always null
- NPE when a subtitle had an unknown format

## [0.2.0] - 2026-08-16

### Added

 - `StreamInfo` caching keyed on video URL. Repeated requests for the same video now share one extraction instead of re-extracting per endpoint.
 - Configurable cache settings under `opentube.cache.streaminfo`: `max-entries`, `safety-margin`, `fallback-ttl`, `degraded-ttl`, `max-ttl`
 - Request scoped logging context: every log line has a request ID and a resource ID from the request to better identify logs from concurrent requests.
 - `local` Spring profile (`application-local.properties`) enabling DEBUG for the application package during local development.

### Changed

 - Kiosk endpoints moved out of `NewPipeController`/`RestService` into a dedicated `KioskController` and `KioskService`
 - Kiosk responses now return typed DTOs with ISO-8601 timestamps instead of serialised extractor objects
 - `/api/v1/streams/dash` now returns 503 with the standard error body and a `NO_STREAMS_AVAILABLE` code when no streams are found.
 - Log levels overhauled across all services and controllers: entry lines moved to DEBUG, one INFO line per completed operation

### Removed
 
 - Kiosk pagination endpoint (`GET /api/v1/kiosks/{kioskId}/page`) - YouTube kiosk feeds return no continuation token and are single-page by design

### Fixed

 - Granular stream endpoints `/audio`, `/video`, `/subtitles`, `/thumbnails`, `/segments`, `/preview-frames`, `/description`, `/related`) bypassed the incomplete-response retry, so they could return empty or muxed-only results where `/dash` succeeded for the same video.
 - `PlaylistService` logged an error on every successful playlist page request.

## [0.1.1] - 2026-08-09

### Changed

 - Each content type now has its own `AdaptationSet` id range, keeping ids unique within a `Period` now that video can emit more than one set. Video starts at 0, audio at 50, subtitles at 100.

### Fixed

 - Subtitle format preference now runs within each language group rather than across the whole list, so a language is no longer dropped entirely because a different language matched a higher-ranked format.
 - Video `AdaptationSets` are split by mimeType and codec family, so the mimeType declared on a set always matches the `Representations` inside it. Mixed vp9/webm and avc1/mp4 sets previously produced a non-conformant manifest.
 - Video quality selection keys on numeric height instead of resolution labels, matching how the manifest generator sorts `Representations`. Labels such as `1080p60` and `1080p HDR` are now recognised.
 - Audio track preference is driven by codec metadata rather than hardcoded YouTube itag numbers, which are an implementation detail that can change without notice.
 - Streams that are not delivered over progressive HTTP, and streams without both an init and index byte range, are filtered out with a log line rather than being emitted as unplayable `Representations`.
 - Replaced `assert` statements in stream selection and logging with real null checks. Assertions are disabled at runtime unless `-ea` is set, so they provided no protection in production.

[Unreleased]: https://github.com/kl3pt0gr4ph/NewPipeExtractorApi/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/kl3pt0gr4ph/NewPipeExtractorApi/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/kl3pt0gr4ph/NewPipeExtractorApi/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/kl3pt0gr4ph/NewPipeExtractorApi/compare/v1.1.1...v0.2.0
[0.1.1]: https://github.com/kl3pt0gr4ph/NewPipeExtractorApi/compare/v0.1.0...v0.1.1
