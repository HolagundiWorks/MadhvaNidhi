# MadhvaNidhi — Agent Instructions

**Current development phase: APPROVED MOCKUP → ANDROID IMPLEMENTATION.**

`MOCKUP_ONLY = false` (authorized full-shell port)

## Role

You are implementing the **approved web mockups** in the native Android application under `app/`, while keeping mockup freeze contracts accurate under `prototypes/` and `docs/`.

Authorization for this pass: implement the approved mockup in the Android application (full shell; freeze Nitya Sandhya/Japa/Puja first). Android visuals use **Material Design 3**; mockups remain Carbon for structure/parity reference.

## Repository boundaries

| Path | Role | Agent access |
| --- | --- | --- |
| `prototypes/` | Interactive HTML/CSS/JS mockups (UX / layout reference) | **Read / write** (freeze marks, parity fixes) |
| `docs/` | Design contracts | **Read / write** |
| `app/` | Native Android application | **Read / write** — builds, `gradlew`, `adb install` allowed |
| `playstore/`, `tools/`, `resources/` | Store copy, catalogue tools, reference PDFs | Untouched unless asked |

Primary live preview entry: `prototypes/app-shell/` (studio chrome + `shell.html`).

Related previews / freezes:

- `prototypes/panchanga/` — **frozen** Panchanga (`docs/frozen-panchanga-shell.md`)
- `prototypes/app-shell/` `#view-user` — **frozen** User (`docs/frozen-user-profile.md`)
- Nitya tabs — **frozen** Sandhya / Japa (`docs/frozen-nitya-sandhya.md`, `docs/frozen-nitya-japa.md`) plus **ನಿತ್ಯ ಪಾರಾಯಣ** tab; **Puja** is a standalone rail destination (`docs/frozen-nitya-puja.md`)
- `prototypes/menu-pages/` — hub / child destination mockups (parity)

## Implementation policy

### Allowed

- Edit Android Kotlin, XML layouts/resources, Gradle as needed for mockup parity
- Build/install debug APKs; use `adb` for device validation
- Update freeze docs and mockup only when aligning contracts
- Portrait-first layouts; **Material Design 3** on Android (`Theme.Material3.*`, `Widget.Material3.*`, `md_theme_*`)
- Content readers may use `sensorLandscape` (e.g. `ContentReaderActivity`) while the main shell stays portrait

### Still prohibited

- Publishing, signing, or Play Store release unless explicitly asked
- Rewriting web mockups to Material without an explicit request

## Portrait-first

MadhvaNidhi main shell is portrait on mockup and device (top header + content + bottom destination rail; left drawer for labeled nav). Do not force landscape-only studio presets or lock the launcher to landscape.

## Design system

**Android visual authority: Material Design 3** — colours, shapes, type, and components.

- Theme: `Theme.PaperDesk` → `Theme.Material3.DayNight.NoActionBar` (`Theme.PaperDesk.Base`) in `app/src/main/res/values/themes.xml`
- Colour: pure white/grey (light) and black/grey (dark) neutrals with the orange accent **`#FF4F18`** (user-updated 14 Nov 2026, `md_theme_primary` in `values/m3_colors.xml` and `values-night/m3_colors.xml`). Do not reintroduce `#BA4E00` or warm brown tonal surfaces on Android. Filled orange controls use black foregrounds.
- Prefer `Widget.Material3.*` / `TextAppearance.Material3.*` and `md_theme_*` colours; surfaces are flat filled cards (no elevation overlays)
- Legacy `Widget.Carbon.*` / `carbon_*` names may remain as bridges remapped onto M3; do not reintroduce Carbon 0dp productive corners on Android
- Icons: offline official Material Symbols Outlined vectors (with attribution under `assets/licenses`) for shell, menu and practice actions; domain glyphs (moon phases, app mark, Japa `eda`) may stay. Ekadashi uses Carbon `Transparency_01` pictogram
- Restore tag before M3 pass: `restore/portrait-mock-pre-m3` (exists locally)

**Mockups:** IBM Carbon remains the web/structure reference (`cds--*` / `--cds-*`; moon glyph circle is the sole domain exception). Do not redesign frozen mockup HTML to Material unless asked. Freeze docs stay layout/IA contracts.

UX notes: [`docs/carbon-android-ux.md`](docs/carbon-android-ux.md) still describes journey patterns; Android chrome follows M3.

## Workflow

1. Prefer frozen mockup contracts as the layout/IA spec
2. Implement in `app/` with Material3 visuals
3. `installDebug` and validate on connected portrait phone
4. Capture / fix regressions; keep freeze docs in sync for structure

## Validation checklist

- [ ] Portrait shell (header + bottom rail); readers may rotate landscape
- [ ] Nitya tabbed Sandhya / Japa / Parayana match freeze docs; Puja is its own rail item
- [ ] User / Panchanga respect existing freeze layout contracts
- [ ] Material3 styling; Kannada readable
- [ ] Debug build installs on device

## Current implemented behaviour (14 Nov 2026)

Verify against the dated notes before changing these; the detailed contracts live in `docs/`.

- **Navigation:** 8 primary rail destinations: Dashboard, Panchanga, Nitya Karma, Puja, Grantha, Bhakti Sangeeta, Gruha Samskruti, Settings. Puja is a standalone rail item, not a Nitya tab.
- **Nitya Karma:** Sandhyavandana (morning/noon/evening selector → open reader; Skip/Complete with confirmation) and Japa (select mantra → Poorvanga reader → counting → Uttaranga reader → explicit finish). Parayana is a Nitya tab. Scripture bodies open only in the sensor-landscape `ContentReaderActivity`.
- **Japa:** target controls live in Settings/Dashboard, not in the counting portal. Undo reverses the last action; daily rollover clears daily count and stage but keeps cumulative total. Stage persistence is per profile/mantra/day.
- **Content lists:** Stotra, Suktas, Mantras, Namavali and Songs are searchable title lists; selecting a title opens the landscape reader directly. Puja keeps its guided procedure selector and section picker.
- **Bhakti Sangeeta:** tabs for Dasara Padagalu, Suladhi, Ugabhoga and Favorites (Playlists removed; old links redirect to Favorites). Favorites are profile-scoped and included in encrypted profile backups.
- **Dashboard:** full-width peer tiles; dashboard progress dialogs are read-only; no tile count mutation. Numeric Japa tiles show Daily / Sankalpa / Cumulative; completion is per mantra.
- **Settings:** UI Settings (Light/Dark only), Dashboard, Users, Backup, Legal. Veda Shakha and Panchanga Parampara live under Users → Practice preferences.
- **Panchanga:** the field labelled "Nakshatra pāda" is intentional (user-confirmed). Local nakshatra/rashi/Yoga/Karana are estimates and must stay labelled as such.
- **Samskruti:** Aayadi, Rangavalli (offline gallery, original PNGs), Naivedya recipes (five offline Kannada cards with source links).
- **Backups:** Android uses passphrase-protected `.mnprofiles`; keep the AES-GCM vault and existing backup formats readable.

## Project memory

Read [`docs/project-memory.md`](docs/project-memory.md) for the current commit context, navigation, frozen design decisions and preview workflow. Verify it against newer commits; older external preview ZIPs are superseded.
