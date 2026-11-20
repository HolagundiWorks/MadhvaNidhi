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

## 7a. Decisions taken (owner away; recorded here so they can be reviewed)

The owner asked for decisions to be taken and recorded, and the work to continue without them. Each decision below can be reversed.

| # | Question | Decision | Reason |
|---|---|---|---|
| 1 | UI toolkit | Keep Android Views, Fragments and XML. No Compose. | Reuses working screens; the guideline also asks to avoid redesign. Compose can come later per screen. |
| 2 | Application IDs | Madhva Nidhi keeps `in.hcworks.madhvanidhi`. Madhva Dvija is `in.hcworks.madhvadvija`. | Existing users keep reader positions and favourites in place. |
| 3 | Design system | Madhva adopts `DesignSystem` (`:designsystem`). Its brand override in `ds_brand.xml` keeps the Madhva accent `#FF4F18`, so the colours do not change. Madhva-only token files are removed after the switch. | One design source for all apps; the brand override keeps the look. |
| 4 | Profile | Profile is Dvija's. Favourites in Nidhi are kept per device, without the profile link. | Profile is personal practice data. |
| 5 | Naivedya, Rangavalli | Stay in Madhva Nidhi. | Samskruti content, not practice. |
| 6 | Content classification | Every item defaults to `REQUIRES_REVIEW` until a reviewer changes it. No eligibility is asserted. | Traditional review is needed. |
| 7 | Shared code | Gradle modules in this repository, under `shared/`. `DesignSystem` stays a separate repo, as it is for Aaya. | One repo for the two Madhva apps; the design system is used by all three. |
| 8 | App layout | Nidhi stays as module `:app` (not moved to `apps/`). Dvija is `:apps-dvija` (module `:dvija`). | Smaller diff; keeps the installed app's history and its build. |

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

## 11. Corrections found during Phase 2

- The dependency scan in section 4 matched **file names** only. It missed types declared inside a file and used elsewhere. Example: `CatalogueClassification` uses `CatalogueGroup` and `CatalogueItem`, which are declared in `CatalogueStore.kt`. Those two data classes moved to `shared/core` (`CatalogueModels.kt`). Treat section 4 as a starting point and confirm each edge by compiling.
- `JapaCountMathTest` depends on `JapaGoals`, which depends on `Sankalpa`, so it stays in the app until those move.
- Phase 2b so far: `shared/core` holds `RitualText`, `PanchangaTraditions`, `MadhwaDays`, `KannadaPhonetic`, `JapaCatalog`, `IndicScript`, `DashboardBadges`, `CatalogueClassification`, `CatalogueModels`. Its 13 tests pass; the app's 38 tests pass; `assembleDebug` succeeds. Baseline was 51 tests, so the total is unchanged.

## 12. Status after the autonomous implementation pass

Decisions in section 7a were taken as recommended. Commits are on branch `madhva-split-audit` (not pushed).

| Phase | State | Evidence |
|---|---|---|
| 1 Audit | Done | `6e6b7a9`, this file |
| 2a Baseline | Done: 51 Nidhi unit tests pass; debug build succeeds | Gradle runs in this pass |
| 2b Shared code | Partly: `shared/core` holds pure logic (calendar traditions, Kannada text helpers, catalogue classification and models, Japa catalogue, dashboard badges, practice transfer codec, passphrase crypto). 28 core tests pass. The reader, the design system and the navigation chrome are still in each app | `c98fba0`, `e37151c`, `3f09120` |
| 3a Madhva Dvija app | Done: `:dvija`, application ID `in.hcworks.madhvadvija`, own host, copied practice screens and stores, bottom navigation (Home, Sandhya, Japa, Nitya Puja, profile, settings). 14 practice tests pass. Dashboard, Sandhya, Japa and Nitya Puja checked on the phone | `5de418d`, `787e474`, `e107180` |
| 3b Data transfer | Done: export from Madhva Nidhi, encrypted with a passphrase; import into Madhva Dvija with validation, duplicate refusal and a no-history rule. Checked on the phone (350 records) with the encrypted file, and the duplicate refusal | `e37151c`, `3f09120` |
| 4 Refocus Nidhi | Partly: home is now a content-discovery screen (today's Panchanga, library entries, settings entry). Demo practice seeding and the demo data classes are removed. **Not done:** removing Nitya Karma from the dock and the page list, the Dashboard and Users settings tabs (they still show Japa configuration and profile), and the profile part of the Backup tab | `b6c09c0`, `4b95134` |
| 5 Notifications and migration verification | **Not done:** Nidhi's practice reminders (`paperdesk_alarms`) are not cancelled when Dvija takes over, and Dvija's reminder screens are not verified. The upgrade test from the installed Nidhi build has not been run | — |
| 6 Release | Dvija release AAB and APK built, signed with its own upload key, and checked on the phone (dashboard). Madhva Nidhi release signing is unchanged. Dvija still uses the Madhva launcher icon and a copy of all Nidhi assets (APK about 6.4 MB) | `dist/madhva-dvija-1.0.0.*` (ignored folder) |

### Decisions taken autonomously, and their effect
- The practice export is encrypted (PBKDF2-SHA256, AES-256-GCM). The earlier plain-text export is not used by any release.
- Import is refused when the device already has practice history. Defaults written at first launch do not count.
- Dvija does not request the internet permission, so Panchanga uses local calculation only.
- Dvija does not seed demo data.

### Known issues
- Import and export messages (some passphrase and refusal texts) are in English, or mixed with Kannada. Native review is pending.
- Dialog OK buttons use the default accent colour, not the brand colour.
- Dvija's dashboard menu button does nothing yet, because the side panel is not ported. The dashboard shortcut for Panchanga falls back to the dashboard.
- Dvija keeps every Nidhi asset. Unused assets should be pruned by usage after the Nidhi removals in Phase 4.
- The debug and release builds of Madhva Nidhi were installed on the phone over the same package during these checks; the upgrade path from the released package has not been tested.

### Next steps, in order
1. Remove Nitya Karma from Nidhi's dock and page list; remove the Dashboard and Users settings tabs; remove the profile part of Nidhi's Backup tab, keeping favourites with the owner's decision.
2. Cancel Nidhi's practice reminders on Dvija's first launch, using separate channels, and verify no duplicates.
3. Prune Dvija's assets and adopt the shared design system (`DesignSystem`) in both Madhva apps, keeping the brand override.
4. Run the upgrade test from the installed Nidhi build, with real data, and write the verification report.

## 13. Phase 4 and 5 progress (branch madhva-split-phase4)

| Item | State | Evidence |
|---|---|---|
| Nidhi page list | Done: Nitya Karma and the profile page are removed from the page list and the dock; Wiki is on the dock; `pagesRev` 18 rewrites saved layouts. Old page ids map to home or settings | `SettingsStore`, `ShellHubFragment`, `PageFactory` |
| Nidhi settings tabs | Done: UI Settings, Backup, Legal. The Backup tab holds the practice export and a favourites-only backup | Phone check of the Backup tab |
| Nidhi profile code | Removed: profile registration screen, profile settings page, the profile part of the backup (older backups still restore their favourites) | Build |
| Nidhi reminders | Stopped: Nidhi no longer schedules alarms at boot or launch; pending ones are cancelled. Stored items are kept, not deleted. Nidhi has no alarms registered with the system after launch | `dumpsys alarm` |
| Dvija reminders | Done: list, add (time picker or typed time) and remove, in settings. Alarms are exact when the system allows it (`SCHEDULE_EXACT_ALARM`), otherwise a window | A reminder for 21:33 fired at 21:33:00.012 with a heads-up notification on the Madhva Dvija alarm channel |
| Dvija AlarmScheduler | Changed from a 10-minute window to an exact alarm: the window let Samsung delay a reminder | The fix was needed after the first 21:27 test was still pending at 21:27:48 |
| Still open | Nidhi's practice stores and the reader's Sandhya mode are unreachable from the UI but still in the code (kept because the reader's confirmation flow is shared). Dvija reminder strings need native review. Dvija's asset pruning and the shared design system are not done. The upgrade test from the released Nidhi package has not been run | — |
