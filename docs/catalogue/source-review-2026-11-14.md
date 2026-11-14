# Source catalogue review — 14 November 2026

This update adds bibliographic references only. No external text bodies or software were imported, and no third-party material was relicensed.

| Source | Pinned revision | References | Existing title/ID matches | New candidates | Variants requiring review |
|---|---|---:|---:|---:|---:|
| stotrasamhita/vedamantra-book | 97094b701ba94b13c9dcefb48480a52190f1e4aa | 112 | 46 | 65 | 1 |
| stotrasamhita/puja-vidhanam | 511b0cca219bca01459ee81482f233ed023ca6de | 134 | 113 | 18 | 3 |
| stotrasamhita/stotra-sangrahah | 6a19cc01d18024593baf59b366d5713821c8021a | 282 | 267 | 13 | 2 |
| hrishikeshrt/chanda | 3a9607c6e7a23d60c03d10f839913d01f8bd7ee2 | Tool only | — | — | — |

The 426 existing source-title references link to 429 local records (some titles occur in multiple catalogue collections). The 96 unmatched references are candidates, not newly readable app entries. Six additional references require variant review. Filename/title matches do not prove identical text, recension, provenance or permission. Near matches remain suggestions and are never merged automatically. Long Vedic sections are distinguished from standalone mantras; no Rigveda/Taittiriya taxonomy was inferred from a title match.

## Rights and attribution

The three StotraSamhita collections state personal-use-only and no commercial printing/distribution in their frontmatter. Their public GitHub availability does not supply a general redistribution licence. Source URLs, pinned revisions and original licence evidence are retained in the registry. Adding an open-source licence to this app cannot change those source restrictions. Candidates therefore remain reference-only pending permission or an independently licensed transcription.

Existing bundled entries already overlap these collections by title and path; the Puja library also names puja-vidhanam as its source. Existing bodies were preserved, but this comparison does not clear them for public release. Review their actual provenance and applicable permissions before publication.

Chanda is a Sanskrit metre-analysis Python project under AGPL-3.0-or-later, not a collection of mantras. It is recorded as an optional analysis resource; none of its code is included in the Android app. Its licence has not been applied to this app.

## Files and repeatable checks

- `app/src/main/assets/catalogue/source-references.json`: pinned sources, rights status, title comparisons and review candidates.
- `sourceReferences` metadata on existing matched entries: links only, preserving original titles and content.
- `reference-source-comparison.csv`: complete comparison table, including suggested near matches.
- `python tools/reconcile-catalogue-sources.py`: recompare the saved registry without network access.
- `python tools/reconcile-catalogue-sources.py --write`: regenerate metadata and CSV; no content-body writes.
- `python -m unittest discover -s tools -p test_reconcile_catalogue_sources.py`: focused reconciliation and registry integrity checks.
- `node tools/audit-catalogue.mjs`: existing body-integrity audit.

The source registry is editorial metadata. It does not add unavailable candidates to reader menus or pretend they have bundled text. Review evidence URLs in the registry for each exact upstream revision.

## Verification

Four focused Python tests passed. Regenerating the saved registry produced the same SHA-256. Before/after body audits have identical totals: 576 indexed bodies, zero missing/orphan files, duplicate body/title groups or script/noise findings. No tracked library or Madhwa text bodies were modified. Android assembleDebug passed offline. The full Android unit suite was not rerun for this metadata-only update; the previously recorded Panchanga malformed-input failure remains unresolved.
