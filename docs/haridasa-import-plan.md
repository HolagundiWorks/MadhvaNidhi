# Haridasa / Madhva Sampada — copyright-safe import plan

Status: **Phase 1 pilot started** (2026-11-13). Aligns with `docs/CATALOGUE_AUDIT.md` and `docs/devotional-catalogue.md`.

Pilot shipped: **39** readable `dasara_padagalu` bodies (was 4 → 17 → 39), CC BY-SA Wikisource under `madhwa/texts/haridasa/`. Manifest: `docs/haridasa-import-manifest.csv`. No Sampada scrape.

## Goal

Fill **ಭಕ್ತಿ ಸಂಗೀತ → ಹಾಡುಗಳು** (catalogue groups `dasara_padagalu`, `suladhigalu`, `ugabhogagalu`) with readable Kannada bodies **without shipping third-party digital editions we do not have rights to redistribute**.

## Current gap

| Group | Index titles | Bodies on device |
| --- | ---: | ---: |
| `dasara_padagalu` | 88 | **39** (Phase 1 continued) |
| `suladhigalu` | 13 | **10** |
| `ugabhogagalu` | 2 | **2** |

Empty `file` rows already point at Madhwa Festivals title lists; `CatalogueStore.inGroup` **hides** blank-file items from the reader. Sampada’s [ಹರಿದಾಸ ಸಂಪದ](https://sampada.net/haridasa) (~1,844 padagalu) is the discovery map, **not** a cleared APK source.

## Copyright rules (non-negotiable)

### What is usually free to use

- **Underlying classical compositions** of historical Haridasas (Purandara, Kanaka, Sripadaraya, Vijaya, Jagannatha, Vadiraja, etc.) — public-domain literary works.
- **Factual metadata** we create ourselves: author, form (ಪದ / ಸುಳಾದಿ / ಉಗಾಭೋಗ), deity tags, our own raga/tala notes when independently verified.

### What we must **not** copy into the APK without written permission

| Source | Why |
| --- | --- |
| **sampada.net / Haridasa Sampada** (Saaranga Infotech LLP) | Platform ©; no public reuse license found. Transcription + presentation are theirs. |
| **dasasahithyamahithi.com** | Site-specific lyrics layout + specially recorded **audio**. Discovery OK; scrape not OK. |
| **madhwafestivals.com** lyric pages | Title index already cited; do not mirror page HTML/body. |
| **sanskritdocuments.org** Haridasa/kṛti files | Explicit: personal study/research; **no** copy/repost for a product without permission. |
| Sampada **blog essays**, opinions, modern commentary, podcast audio | Not paddhati; often fully copyrighted. |
| Any **audio / notation / critical apparatus** from modern editions still in copyright | Edition copyright ≠ composition PD. |

### Cleared import tiers

| Tier | Method | Ship in APK? |
| --- | --- | --- |
| **A — Own edition** | Type/proofread from a **public-domain print** (pre-modern / expired-copyright edition; record bibliographic cite). Sanitize like existing madhwa texts. | Yes |
| **B — Written license** | Email Saaranga / other holders; keep grant on file; attribute as required. | Yes, per grant |
| **C — Link-out stub** | Title + author + `url` only; `file: ""` (current pattern). Opens browser / “source” chrome later. | Metadata only |
| **D — Exclude** | Essays, audio, modern prose, Shakta/other-tradition pads, ambiguous modern dasas without clearance. | No |

**Default path for v1: Tier A for bodies + Tier C for the long tail.** Treat Sampada as a **coverage checklist**, not a dump source, until Tier B exists.

## What content to import (priority)

### Phase 1 — Close the empty index (high user value)

Fill bodies for the **57** `dasara_padagalu` rows that already have Kannada titles but empty `file`, using Tier A text only.

Suggested nitya / Madhwa-facing order (subset first if Tier A capacity is limited):

1. Already shipped: ಜಗದೋದ್ಧಾರಣ, ಭಾಗ್ಯದ ಲಕ್ಷ್ಮಿ ಬಾರಮ್ಮ, ಹನುಮ / ಮಧ್ವ pad clusters  
2. Daily-use Purandara / Krishna / Rama / Vittala padagalu matching existing titles  
3. Raghavendra / Madhwa guru-parampara pads  
4. Remaining index titles alphabetically  

Do **not** invent lyrics. If a clear PD print cannot be found for a title, leave Tier C stub.

### Phase 2 — Author pilots (structured growth)

Target **~20–40 bodies each**, Tier A, before scaling:

| Bucket | Form focus | App surface |
| --- | --- | --- |
| ಶ್ರೀಪಾದರಾಯ | ಪದ + ಉಗಾಭೋಗ | Hadugalu |
| ಕನಕದಾಸ | ಪದ | Hadugalu |
| ವಿಜಯದಾಸ | ಪದ + ಉಗಾಭೋಗ | Hadugalu |
| ಜಗನ್ನಾಥದಾಸ | ಪದ (not Harikathamrutasara prose dump) | Hadugalu |
| ಪುರಂದರದಾಸ | Curated nitya set (not all 1,275) | Hadugalu |
| Existing ಸುಳಾದಿ set | Complete missing 3 index rows; no new speculative suladis | Hadugalu |

Use Sampada’s author counts only as a **gap map** (e.g. Purandara 1,275 vs our curated pilot).

### Phase 3 — Optional partnership

Request Tier B from Saaranga Infotech LLP for a **named subset** (e.g. Sripadaraya + Kanaka full lists) with:

- Attribution line in reader footer / About  
- No claim that MadhvaNidhi *is* Sampada  
- No audio unless separately licensed  

Until then, **zero** automated scrape of sampada.net.

### Explicit non-goals

- Importing Sampada magazine posts / Madhva opinion essays into Sandhya, Japa, or Puja  
- Bundling Haridasa audio  
- Redesigning Sangeeta playlists (mockup: later phase)  
- Portrait or non-Carbon reader chrome  

## How to structure it

### 1. Asset layout

```
app/src/main/assets/madhwa/texts/haridasa/
  purandara/
    jagadodharana.txt          # migrate/alias existing where useful
    bhagyada_lakshmi.txt
    …
  kanaka/
  sripadaraya/
  vijaya/
  jagannatha/
  other/
  suladhi/                     # or keep flat suladhi_*.txt at texts/ for parity
  ugabhoga/
```

Keep current flat names working (`madhwa/texts/*.txt`) via stable `file` values in the index; new imports prefer `haridasa/<author>/<slug>.txt`.

### 2. Body file header (machine + human)

Every new body starts with a short YAML-like header stripped by sanitize if needed, or a one-line `SOURCE:` block retained in docs manifest only (prefer **manifest**, clean reader body):

Reader body = lyric text only (matches `dasara_jagadodharana.txt` today).

Provenance lives in the **import manifest** (below), not mixed into chant text.

### 3. Catalogue index fields

Extend each Hadugalu item in `app/src/main/assets/catalogue/index.json`:

| Field | Required | Notes |
| --- | --- | --- |
| `id` | yes | Stable (`dasa_…` / `haridasa_purandara_…`) |
| `kn` / `en` | yes | Display titles |
| `type` | yes | `dasara_pada` \| `suladhi` \| `ugabhoga` |
| `author` | yes for new | e.g. `purandara`, `kanaka` (CatalogueItem already has `author`) |
| `deity` | preferred | Existing field |
| `script` | yes | `kn` for pads |
| `file` | yes if readable | Relative under `madhwa/texts/` |
| `url` | optional | Discovery / print scan cite |
| `form` | new optional | `pada` \| `suladhi` \| `ugabhoga` (redundant with type OK) |
| `raga` / `tala` | optional | Only if Tier A/B verified |
| `license` | new for new rows | `pd-composition+own-edition` \| `licensed-sampada` \| `link-only` |

`CatalogueStore` already reads `author`; wire UI filters later. Unknown JSON keys are ignored today — add Kotlin fields when UI needs them.

### 4. Import manifest (source of truth for rights)

Maintain `docs/haridasa-import-manifest.csv` (or `.md` table) with one row per body:

`id, title_kn, author, form, file, license_tier, print_source, permission_ref, verified_date, notes`

Gate merges: **no `file` in index.json unless manifest row is Tier A or B.**

### 5. Tooling (same pipeline as catalogue audit)

1. Add/edit bodies under `madhwa/texts/…`  
2. Update `catalogue/index.json`  
3. `node tools/sanitize-catalogue.mjs`  
4. `node tools/audit-catalogue.mjs` — zero missing files, orphans, duplicate bodies/titles, extraction noise  
5. `installDebug`; smoke Hadugalu list + open 3 pads  

Do **not** extend `--refresh-online` to Sampada (audit doc already forbids inventing/scraping lyrics).

### 6. App navigation (no redesign)

- **Grantha → ಹಾಡುಗಳು** and **ಭಕ್ತಿ ಸಂಗೀತ → ಹಾಡುಗಳು** continue to use `groupsFor("hadugalu")`.  
- Ship only rows with non-blank `file` into the list (current behaviour).  
- Later (optional): author chips / form filter using `author` + `type`.  
- Playlists remain empty hub until a separate design pass.

## Action checklist

1. **Legal gate** — Decide Tier A print sources for Phase 1 titles; draft Tier B email to Saaranga if bulk Sampada reuse is desired.  
2. **Schema** — Add `license` (and optional `raga`/`tala`) to new index rows; create empty manifest.  
3. **Pilot** — 20 Tier A pads (mix of existing empty titles + 1–2 per major author); run sanitize + audit; install.  
4. **Phase 1 complete** — All current empty `dasara_*` titles either gain a Tier A body or stay explicit link-only with documented reason.  
5. **Phase 2** — Author-bucket expansion to ~100–150 curated pads.  
6. **Docs** — Update `CATALOGUE_AUDIT.md` Sources section with print cites + any license grant; never list Sampada as a copied source without Tier B.

## Success criteria

- Every on-device lyric has a manifest Tier A or B row.  
- Audit remains clean.  
- Hadugalu shows a meaningful Madhwa daily set without implying Sampada ownership.  
- No blog prose, audio, or scraped HTML in `app/src/main/assets`.
