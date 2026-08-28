# AcharyaOS catalogue audit

Audit date: 2026-08-28

## Scope

The audit covers the runtime catalogue under `app/src/main/assets/library`, the
Madhwa collection under `app/src/main/assets/madhwa`, and the curated navigation
index under `app/src/main/assets/catalogue`.

Devanagari source text is valid: AcharyaOS converts it to Kannada at runtime via
`IndicScript`. A body is therefore considered Kannada-capable when it contains
either Kannada or Devanagari source characters.

## Corrections

- Removed TeX-only scaffolding, file-path placeholders, duplicate transformed or
  original variants, English blog-index dumps, and duplicate catalogue aliases.
- Replaced the Tamil Unicode Skanda Shashthi Kavacham with the verified
  Devanagari transliteration from Sanskrit Documents. AcharyaOS renders this in
  Kannada at runtime.
- Rebuilt the mixed-script Yajñopavīta entry as a Kannada-only Madhwa sequence
  from the Sumadhwa Seva source, retained its author/collector attribution, and
  separated its procedural cautions from the mantras.
- Corrected ambiguous or incorrect titles for Mrityunjaya Japa, Rama-rahasya
  namavalis, shortened Bhaja Govindam, vrata-katha entries, and
  Vatapuranatha Ashtakam.
- Removed extraction debris including layout dimensions, TeX section labels,
  font names, `shlokacount`, `flushleft`, `multicols`, and attribution metadata.
- Regenerated previews from the sanitized text.

## Result

| Check | Before | After |
| --- | ---: | ---: |
| Unique reachable runtime bodies | 575 | 560 |
| Missing body files | 20 false-positive aliases | 0 |
| Duplicate body groups | 4 direct aliases | 0 |
| Duplicate title groups | 18 | 0 |
| Bodies without Kannada/Devanagari support | 7 | 0 |
| Tamil-only bodies | 1 | 0 |
| Bodies containing known extraction noise | 130 | 0 |

The lower 2026-08-26 body count was intentional: aliases and invalid build
artifacts were removed, not unique devotional texts. Subsequent verified imports
bring the 2026-08-28 catalogue to 576 indexed bodies, with zero missing files,
orphans, duplicate bodies, duplicate titles, non-Indic bodies, or extraction-noise
findings in `tools/audit-catalogue.mjs`.

## Devotional reader collections

The reader catalogue exposes the bundled source bodies without copying them into
new files. The 2026-08-28 navigation audit added dedicated collections for Rama
Raksha, Narasimha stotras, Vishnu Sahasranama, Venkatesha Suprabhata, Krishna
stotras, Ashtottara Shatanama stotras, and all 56 bundled namavalis. Every exposed
entry resolves to a non-empty local body and no source identifier is duplicated.

The Rigveda section is intentionally recension-aware. It contains the four
bundled texts identifiable as core Rigveda or Rigveda-khila material: Purusha,
Shri, Varuna, and Vishnu Suktas. Aghamarshana, Bhagya, Bhu and the other files
whose own headings identify Taittiriya sources remain under general mantras; they
are not relabelled as Rigveda merely because individual verses also occur there.

## Sources and maintenance

- Skanda Shashthi Kavacham:
  <https://sanskritdocuments.org/doc_subrahmanya/skanda-6-kavacha.html>
- Kannada-script source listing:
  <https://sanskritdocuments.org/kannada/kavacha/>
- Vatapuranatha Ashtakam:
  <https://sanskritdocuments.org/doc_vishhnu/vAtapuranAthAShTakam.pdf>
- Kannada Haridasa discovery catalogue:
  <https://dasasahithyamahithi.com/lyrics_and_audio/search/>
- Sumadhwa Seva — Yagnopaveeta Dharana Vidhi:
  <https://www.sumadhwaseva.com/wp-content/uploads/2009/08/yagnopaveeta-dharana-english1.pdf>

Run the maintained checks from `acharya-os`:

```powershell
node tools/audit-catalogue.mjs
node tools/sanitize-catalogue.mjs
node tools/sanitize-catalogue.mjs --refresh-online
```

The online refresh is deliberately limited to the specifically verified Skanda
source. It does not scrape or invent lyrics.
