# Madhva split: Phase 1 audit

Status: audit only. No source changed. Branch `madhva-split-audit`, created from `main` (`a403e15`). The guideline's Phase 1 deliverable: inventory, ownership matrix, dependency graph, migration plan and risks. Decisions needed before Phase 2 are in section 7.

## 1. Repository facts

| Item | Value |
|---|---|
| Application ID | `in.hcworks.madhvanidhi` (test: `in.hcworks.madhvanidhi.flowtests`) |
| Namespace | `com.paperdesk.launcher` (legacy name; code is under this package) |
| Version | `1.0.0`, versionCode 1 |
| Min / target SDK | 25 / 36 |
| UI toolkit | Android Views + Fragments + XML layouts. **No Jetpack Compose.** |
| Persistence | SharedPreferences (plain and `EncryptedSharedPreferences` via `security-crypto`). **No Room, no DataStore.** |
| Kotlin | 88 files, about 11,800 lines |
| Assets | 15 MB: `library/` (stotra 269, puja 116, namavali 57, mantras 48), `madhwa/texts` (songs 108, texts 79), `knowledge/`, `catalogue/`, `naivedya/`, `rangavalli/`, `licenses/`, `fonts/`, `gita.json` |
| Tests | JVM unit tests in `app/src/test` (14 data, 2 ui); `androidTest` (6 ui, not run in this audit) |
| Dependencies | appcompat, material 1.11, constraintlayout, recyclerview, viewpager2, fragment-ktx, lifecycle, security-crypto, coroutines |
| Shared design system | None. The app has its own tokens: `AcharyaTokens.kt`, `res/values/tokens_*.xml`, `m3_*`, `md_*`. The local `DesignSystem` repo (`../DesignSystem/designsystem`) is wired in `settings.gradle.kts` but **uncommitted** (`settings.gradle.kts` is modified and untracked `debug.keystore` and `tmp/` exist). |
| Installed on test phone | `in.hcworks.madhvanidhi` 1.0.0, first installed 14 Nov 2026, updated 20 Nov 2026. Presumed debug builds from this work; no Play release is known. |

## 2. Persistence inventory

Everything is per-device and local. No database files are created by the app.

| Store (file or prefs name) | Owner data | Owning module | Migration |
|---|---|---|---|
| `madhvanidhi_nitya` | Nitya-karma day state | Dvija | Export/import |
| `paperdesk_japa_ui` | Japa pad UI state | Dvija | Export/import |
| `paperdesk_alarms` (+ `AlarmStore`) | Reminder registrations | Split: festival alarms to Nidhi, personal reminders to Dvija | Re-register in Dvija; retire old alarms to avoid duplicates |
| `madhvanidhi_puja_reader` | Puja reading position | Nidhi | Stay |
| `paperdesk_reader_<pageId>` | Reading position per page | Nidhi (reader) | Stay |
| `paperdesk_reader_hadugalu` | Haadu reading position | Nidhi | Stay |
| `wiki_points` | Wiki pointers | Nidhi | Stay |
| `EncryptedSharedPreferences` (7 uses via `SecurePrefs`) | Profile, Sankalpa, Japa counts, practice days, settings | Mostly Dvija | Export/import (encrypted-at-rest copy is not carried over; export is a plain versioned file, see section 6) |
| Settings (`SettingsStore`) | Panchanga tradition, UI and text settings | Shared | Duplicate per app; each keeps its own copy |
| Profile (`ProfileStore`) | Name, gotra, nakshatra, rashi, Veda, birth details | Dvija (personal) | Export/import |
| Favorites (`SongFavoritesStore`) | Profile-scoped song favourites | Nidhi (favourite songs) | Stay in Nidhi; profile scoping must be decided (section 7) |

Plan note: "Reading history" in the guideline maps to the reader positions above, all of which are in Nidhi.

## 3. Feature ownership matrix

Legend: **N** Madhva Nidhi, **D** Madhva Dvija, **S** shared library, **?** decision needed.

| Area | Files (main) | Owner | Notes |
|---|---|---|---|
| Panchanga | `PanchangaStore`, `PanchangaCalendar`, `PanchangaTraditions`, `FrozenPanchangaBinder`, `DeskFragment` | N (primary), S (calculation) | Dvija's dashboard also reads it, so the calculation goes to `panchanga-core` |
| Shell and navigation | `LauncherActivity` (307 lines), `FrozenShell`, `ShellHubFragment`, `DeskWindow`, `DeskLock`, `LockGuard`, `PaperDeskApp` | S (shell pattern), two hosts | `LauncherActivity` imports every practice fragment. It has to be split into two hosts |
| Dashboard | `DashboardFragment` (526), `DashboardBadges`, `DayStatus`, `DashboardDemo`, `DashboardSettingsFragment`, `ShareCards` | D | Required by the guideline. Its shortcut row goes to Dvija |
| Sandhyavandana | `SandhyaStore` (300), `SandhyaDayConfirmation` | D | `ReaderFragment` also imports `SandhyaStore` (see section 4) |
| Nitya Puja / Nitya Karma | `NityaKarmaFragment`, `NityaDayChrome`, `PujaFragment` (practice part) | D (practice); N (general puja info) | `PujaFragment` mixes both. Split it |
| Nitya Parayana | `NityaParayanaStore`, `NityaParayanaBinder`, `NityaParayanaFragment` | D | Uses `PracticeDayStore` |
| Japa / Gayatri | `JapaStore` (357), `JapaCatalog`, `JapaCountMath`, `JapaGoals`, `JapaPadBinder`, `JapaTargetsTable`, `Sankalpa` (174) | D | Gayatri appears in `JapaCatalog`, `JapaStore`, `DemoUser`, `SettingsStore`, `JapaPadBinder`, `SettingsScreenBinder` |
| Practice days and streaks | `PracticeDayStore` (187), `DashboardBadges` | D | Depends on `SandhyaStore` and `PanchangaStore` |
| Profile | `ProfileStore` (317), `ProfileRegisterActivity`, `UserProfileFragment`, `ProfileBackupFragment`, `DemoUser` | D (personal); backup is **?** | `SongFavoritesStore` and `CatalogueListFragment` also depend on it |
| Stotra, mantras, namavali, puja texts | `LibraryStore`, `MadhwaStore`, `MadhwaDays`, `CatalogueStore`, `CatalogueClassification`, `CatalogueListFragment`, `SectionBrowseFragment`, `GranthaFragment`, `PageFactory`, `ContentPagination` | N | Public library |
| Reader | `ReaderFragment` (490), `ContentReaderActivity` (689), `ReaderTextScale`, `TextLineInk`, `AppTextSize` | S (reader) | `ReaderFragment` depends on `SandhyaStore` and `MadhwaStore`. The reader itself is shared; the Sandhya link must go |
| Literature, Wiki | `WikiStore`, `WikiFragment` | N | |
| Songs | `SongLibraryFragment`, `SongFavoritesStore` | N | |
| Haadu / Devaranama | `ReaderFragment` (haadu pages), `madhwa/texts/haridasa` assets | N | |
| Naivedya, Rangavalli | `NaivedyaStore`, `NaivedyaFragment`, `RangavalliStore`, `RangavalliFragment` | N (Samskruti content) | Guideline does not name these; confirm |
| Festivals and notifications | `FestivalStore`, `AlarmScheduler`, `AlarmReceiver`, `AlarmAlertActivity`, `AlarmStore` | Split: festivals N, reminders D | Two channels; old registrations must be retired carefully |
| Settings | `SettingsStore` (271), `SettingsFragment`, `UiSettingsFragment`, `LegalSettingsFragment`, `SettingsScreenBinder` | S (pattern), each app owns its values | |
| Design tokens and components | `AcharyaTokens` (331), `DeskTheme`, `MdMotion`, `MdTabs`, `MaterialLayouts`, `MaterialChoices`, `ListChoices`, `TextLineInk`, `res/values/*tokens*`, `m3_*`, `md_*` | S (move to the design system) | See section 5 |
| Text and script | `IndicScript`, `OsLang`, `RitualText`, `KannadaPhonetic` | S (`localization`) | Used by almost every screen |
| Crypto | `SecurePrefs` | S | |

## 4. Dependency graph: the problems that block a clean split

The table lists, for each file, which other project classes it references (generated from the sources on this branch).

1. **Circular dependency:** `Sankalpa` uses `PanchangaStore`, `ProfileStore`, `SandhyaStore`, `OsLang`, `IndicScript`. `PanchangaStore` uses `Sankalpa`. Sankalpa has to be split: the pure calculation part moves to `panchanga-core`, and the practice part stays in Dvija.
2. **Shell hub:** `LauncherActivity` references 19 project classes, including `DeskFragment`, `NityaKarmaFragment`, `GranthaFragment`, `SongLibraryFragment`, `SettingsFragment`, `ReaderFragment`. Each app needs its own host. Only the navigation pattern is shared.
3. **Dashboard depends on the shell:** `DashboardFragment` imports `FrozenShell` and `LauncherActivity`, plus 13 other project classes.
4. **Reader depends on practice:** `ReaderFragment` imports `SandhyaStore` (and `MadhwaStore`). Madhva Nidhi cannot ship the reader while it references Sandhya data. Decide what the reader needs from Sandhya, probably a reading-mode flag passed in, and remove the import.
5. **Puja mixes public and practice:** `PujaFragment` imports `PracticeDayStore` and `Sankalpa`. The public puja texts stay in Nidhi; the practice tracking moves to Dvija.
6. **Song favourites depend on profile:** `SongFavoritesStore` imports `ProfileStore`, and `CatalogueListFragment` imports `SongFavoritesStore`. Favourites are in Nidhi, so either the profile link goes or profile moves to shared.
7. **Shared hub stores:** `SettingsStore`, `PanchangaStore`, `SecurePrefs`, `IndicScript`, `OsLang`, `RitualText` are used across both apps. These are the candidates for the shared library.
8. **Alarms:** `AlarmReceiver` imports `AlarmScheduler`, `AlarmStore` and `PaperDeskApp`. Festival alarms and personal reminders share this code, so the scheduler is split by purpose.

Shared-library candidates, with no app-specific logic found yet: `DeskTheme`, `MdMotion`, `MdTabs`, `MaterialLayouts`, `MaterialChoices`, `ListChoices`, `AppTextSize`, `ReaderTextScale`, `TextLineInk`, `IndicScript`, `OsLang`, `RitualText`, `KannadaPhonetic`, `SecurePrefs`, `PanchangaCalendar`, `PanchangaTraditions`.

Dvija-only candidates with no reverse dependency from Nidhi: `DashboardBadges`, `DayStatus`, `JapaCatalog`, `JapaCountMath`, `JapaGoals`, `NityaParayanaStore`, `NityaDayChrome`, `DemoUser`.

## 5. Design system and UX

- The guideline requires one authoritative design source for all three apps (Madhva Nidhi, Madhva Dvija, Aaya Calculator). Today:
  - Madhva has its own tokens (`AcharyaTokens`, `tokens_foundation/semantic/component`, `m3_colors`, `md_colors`, `PaperDesk` theme) with primary accent `#FF4F18`.
  - The Aaya Calculator uses `DesignSystem` with an orange brand `#F36C00` (via `ds_brand.xml`).
  - `DesignSystem` (uncommitted, no remote) defines `ds_status_*`, `ds_neutral_*`, `TextAppearance.DS.*`, `Widget.DS.*`.
- The two palettes differ, so moving Madhva onto `DesignSystem` changes its look. Section 7 asks which way to resolve that.
- The guideline's "Material 3 in Jetpack Compose" conflicts with the code, which is Views (see section 7).

## 6. Data migration plan

Principles from the guideline: nothing is deleted until the replacement is verified; the export is explicit, versioned and private; no server.

1. **Export format:** a JSON file, `madhva-practice-export`, with a `schemaVersion` field. It holds the Dvija data from section 2 (Japa counts, Sankalpa, practice days, streaks, profile needed for practice, Dvija settings). Reading history and Nidhi preferences are not exported.
2. **Export path:** Madhva Nidhi (old app) writes the file through Android's share sheet (`FileProvider`, which exists in the app already for `ShareCards`). The user picks the file in Madhva Dvija through `ACTION_OPEN_DOCUMENT`.
3. **Import validation:** check `schemaVersion`, record counts and per-record checksums; reject unknown newer versions; keep a content hash of each imported batch so the same file cannot be imported twice.
4. **Originals:** the old app keeps its data until the user confirms the import and chooses to clear it.
5. **Reminders:** on import, Dvija registers its own alarms. The old app cancels only the alarms it created for practice, so the user does not get the same reminder from both apps. Channel IDs must differ.
6. **Upgrade test:** install the current `in.hcworks.madhvanidhi` 1.0.0 on the phone, create representative data, then upgrade to the new Nidhi build and check that reader positions and favourites survive.

## 7. Decisions needed before Phase 2

These change the plan, so they need your answer:

1. **UI toolkit.** The guideline says Jetpack Compose. The code is Views and Fragments (about 11,800 lines). A rewrite to Compose would be a large redesign, which the guideline itself forbids. Recommendation: keep Views for this split and reuse the existing screens. Confirm?
2. **Application IDs.**
   - Madhva Nidhi keeps `in.hcworks.madhvanidhi`, so the installed user keeps the reader positions and favourites in place.
   - Madhva Dvija gets a new ID, for example `in.hcworks.madhvadvija`.
   Confirm, or give other IDs.
3. **Design system.** Should Madhva move onto `DesignSystem` (its brand and components, so its colours change to match the shared design), or should Madhva keep its own palette and `DesignSystem` be extended to match it? The guideline asks for one source, so the first option fits it better. Confirm.
4. **Profile.** Is the profile personal to Dvija (recommended), or does Nidhi need it for favourites and backups? Favourites currently depend on profile scoping.
5. **Naivedya and Rangavalli.** The guideline does not name them. Recommendation: Nidhi (Samskruti content). Confirm.
6. **Content classification.** The guideline asks for `ContentAccess` metadata and traditional review of initiation-dependent material. This needs a tradition reviewer's decisions; I can add the structure and mark items "REQUIRES_REVIEW", but the classification of each mantra, sukta and practice must come from a reviewer.
7. **Shared code location.** Shared modules inside this repo (`shared/*` as Gradle modules), or a separate shared repository like `DesignSystem`? The guideline allows both. Recommendation: Gradle modules in one repo for the two apps, and keep `DesignSystem` separate for Aaya as it already is.

## 8. Phased plan (after decisions)

| Phase | Work | Verification |
|---|---|---|
| 1 | This audit (done) | Reviewed by you |
| 2a | Make the baseline build and tests pass on the branch (confirm `settings.gradle.kts` / DesignSystem path works from a clean checkout) | `testDebugUnitTest`, `assembleDebug` |
| 2b | Extract shared Gradle modules: `shared/reader`, `shared/localization`, `shared/panchanga-core`, `shared/design-system` (ds tokens and components) | Build and tests; screens unchanged on the phone |
| 3a | Create the Madhva Dvija app (new ID, launcher, manifest, release config, own settings and data) with the dashboard, Sandhya, Japa, Nitya Puja, Nitya Karma, reminders | Install alongside Nidhi; tests migrated; practice flows checked on the phone |
| 3b | Export and import of practice data (section 6) | Unit tests for schema validation and duplicate imports; upgrade test from the installed Nidhi build |
| 4 | Refocus Madhva Nidhi: content-discovery home, remove practice routes only after Dvija parity | Phone checks of the reader, Panchanga, library, audio/TTS |
| 5 | Upgrade and migration verification report | Installed-together test; notification duplicates check |
| 6 | Release builds for both apps, documentation | Release APKs installed and smoke-tested |

Phase 3 comes before Phase 4, as the guideline asks: Dvija is built and verified before any practice feature leaves Nidhi.

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Circular dependency `Sankalpa` ↔ `PanchangaStore` | Splitting modules fails to compile | Split Sankalpa first (step 2b) |
| `LauncherActivity` hosts everything | Nidhi would ship practice screens | Two hosts; remove practice imports from Nidhi last |
| Reader references Sandhya | Nidhi cannot be separated cleanly | Pass a flag, remove the import |
| Palette change when moving to DesignSystem | Visible change to Nidhi look | Decision 3; screenshot comparison before and after |
| Encrypted data cannot be read by another app | Practice history lost if not exported | Export before removing; keep the old app's data (section 6) |
| Duplicate reminders during the transition | User annoyance | Dvija alarms only after old alarms are cancelled, with separate channels |
| Content classification needs human review | Wrong access labels in the app | Mark `REQUIRES_REVIEW` by default; do not assert eligibility |
| No automated tests cover the UI | Regressions go unnoticed | Phone checks per phase; androidTest run where possible |
| Repo state: `settings.gradle.kts` modified, untracked `debug.keystore` and `tmp/` | Accidental commit of secrets or temp files | Do not commit those; commit only named paths |
| Installed `1.0.0` may be debug-signed | Upgrade with the release key will fail | Confirm signing before the upgrade test; migrate via export if signatures differ |

## 10. What was verified in this phase

- Repository state, Gradle configuration, source inventory, persistence names, installed package (commands run on this branch and on the connected phone).
- The dependency list was generated by a script that searches each file for other project class names. A reference inside a comment or string also counts, so it can overstate a dependency. Each flagged edge in section 4 should be confirmed by reading the file before the matching change is made.
- Not run in this phase: builds, unit tests, UI checks. The baseline is part of Phase 2a.
