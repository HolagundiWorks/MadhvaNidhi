# Rangavalli artwork and sources — 14 November 2026

The native Samskruti → Rangavalli tab now has an offline Material gallery rather than the former text-only placeholder. Six original 1024 × 1024 transparent monochrome PNGs cover four-petal flowers, eight-petal lotus studies, eight-point stars, nested diamonds, parallel squares and four lamps. Selecting a card opens a larger, theme-tinted image with an original three-step drawing guide and a reference-blog link. Category filtering covers flowers, geometry and lamps. The grid uses one to four columns according to available width.

## Source collection and rights

Public metadata was collected from these three blog pages:

- Sudha Balaji, Rangoli-sans-dots: https://www.rangoli-sans-dots.com/2013/01/simple-kolam-with-dots-collection.html?m=0
- Sudha Balaji, 2023 archive: https://www.rangoli-sans-dots.com/2023/
- Aishwarya, Rangoli Designs: https://rangolidesigns.net/traditional-kolam-designs-with-dots/

The source registry includes page titles, author names, page URLs, checked dates, response hashes and a bounded list of reference-image URLs. Discovery found 113 non-logo image references; 59 deduplicated-per-page references were retained (maximum 24 per page). No blog image bodies or article bodies were bundled. No general redistribution licence was verified for the blog artwork. Blog photographs and distinctive compositions were not traced or reproduced.

The six bundled illustrations are independent constructions of elementary traditional floral/geometric/lamp motifs. They are simplified practice studies, not exact replicas of individual source photographs, canonical ritual diagrams, dot-count templates or continuous-line sikku patterns. The guides describe these specific app illustrations. No third-party open-source licence has been invented or applied to the referenced blog content.

## Rebuilding and checking

- `python tools/fetch-rangavalli-sources.py`: refresh bounded public source metadata. No image downloads or page-script execution.
- `python tools/generate-rangavalli.py`: regenerate vector paths, SVGs and the app catalogue.
- `tools/render-rangavalli.ps1`: render transparent PNGs using Windows System.Drawing. Source paths are validated before writing.
- `tools/audit-rangavalli.ps1`: check unique IDs, source links, drawing guides, PNG dimensions, transparency and monochrome ink.

Editable SVGs and sampled construction paths live in `resources/rangavalli/`; app PNGs and the catalogue live in `app/src/main/assets/rangavalli/`. Android loads thumbnails off the main thread, with small sampled bitmaps, and tints the alpha mask using the current onSurface colour. External source links open only after the user taps the source action and are restricted to the reviewed HTTPS blog hosts.

## Verification

The artwork audit passed for all six PNGs. A white-background contact sheet and the installed dark gallery were visually inspected. The enlarged preview and drawing guide opened on Galaxy M11. App build and debug installation passed. Light and Dark gallery screenshots were inspected; the original Dark setting was restored. The visible Lamps menu option was selected and the hierarchy confirmed exactly one matching design. The first pair of gallery cards have equal dimensions and remain within screen width. No full unit suite was run for this artwork/gallery pass; the previously recorded Panchanga malformed-input failure remains outside this work.
