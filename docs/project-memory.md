# MadhvaNidhi project memory

Updated 14 November 2026. Android visual system is Material Design 3 (seed `#BA4E00`); mockups remain Carbon structure reference. Journey patterns: `docs/carbon-android-ux.md`. Restore tag before M3 pass: `restore/portrait-mock-pre-m3`. Verify Git status before changing code.

## Current authority

Read root `AGENTS.md` first. The current phase is approved mockup → Android implementation (`MOCKUP_ONLY = false`). Android implementation, debug builds and connected-device validation are authorized. Publishing and release signing remain out of scope unless asked.

**Android visuals:** Material Design 3 (`Theme.Material3.DayNight.NoActionBar`, `Widget.Material3.*`, `md_theme_*`). Legacy `carbon_*` / `Widget.Carbon.*` names may bridge to M3. **Mockups:** Carbon (`cds--*`) for layout/IA; do not Material-restyle mockups unless asked.

Use the current repository mockups as the layout/IA authority. The earlier external `latest-commit-preview` ZIP/patch based on `5adf7ea` is superseded and must not be applied over the newer source.

## Latest Sandhyavandana flow — 14 November 2026

Portrait Sandhyavandana is now time selection (morning/noon/evening) → Open Sandhyavandana → selected-session day status and actions. Removed entry section summary/progress and Previous/Next buttons. Landscape reader has a Sections chooser (Full session + numbered sections), with previous/next hidden. Skip/Complete and undo require confirmation in both places; navigation never updates completion. See `frozen-nitya-sandhya.md` for the current native contract and verification. This supersedes earlier Sandhya navigation notes.

## Latest native content navigation — 14 November 2026

Stotra, Suktas, Mantras and Namavali now use searchable title lists, opening bodies only in the landscape reader. Bhakti Sangeeta uses Dasara Padagalu / Suladhi / Ugabhoga / Favorites tabs; no Playlist tab or song dropdown. Other Songs appears for additional explicitly classified song groups. Favorites stars are profile-scoped, available in the reader, and included in encrypted profile backups. Legacy playlist links redirect to Favorites. Namavali classification corrected. See `song-library-2026-11-14.md` for implementation and verification; this supersedes older song/playlist navigation notes below. The current web preview has not been changed in this pass.

## Recent commits reviewed

- `9d3b242`: standalone Puja rail destination; Parayana in Nitya tabs; native shell and Panchanga density aligned with the frozen mockup.
- `983acac`: performance/security hardening in stores, readers, navigation and app lifecycle. Preserve the narrowed permissions, bounded work and encrypted profile handling.
- `bd7c678`: Carbon-only tokens/styles and updated agent policy; no raw Material3 layout styles or GhostOutlined chrome.
- `30f56db`: full frozen shell port to Android; shared web studio, vault, Japa session logic and freeze contracts.
- `5adf7ea`: earlier shell, Panchanga and card dashboard foundation. Its older menu/radius/icon assumptions are superseded by the later contracts.

## Navigation and frozen screens

Primary rail, in order: Dashboard → Panchanga → Nitya Karma → Puja → Grantha → Bhakti Sangeeta → Gruha Samskruti → Settings. Native `desk` corresponds to web `panchanga`.

- Nitya Karma tabs: Sandhyavandana (`sandhya`), Japa (`japa`), Nitya Parayana (`nitya`). Puja is not a Nitya tab. Legacy Nitya/Puja links redirect to standalone Puja.
- Grantha tabs: Stotra, Suktas, Mantras, Songs (Hadugalu), Namavali. Leaf ids remap into the Grantha tab host.
- Sangeeta / Samskruti use the same contained-tab hub pattern (`GranthaFragment` groups). Sangeeta child: Playlists. Samskruti: Aayadi, Rangavalli, Recipes/Naivedya.
- Expanded nav panel lists the 8 primary rail destinations only (not a flat child laundry list). Short landscape (`h360dp`) densifies rail rows to 40dp.
- Dashboard is full-width peer tiles; the 30:70 split belongs only to Panchanga.
- Settings contains UI Settings, Users and Backup. Android Backup transfers local profiles using passphrase-protected .mnprofiles files. The web preview retains its .mnbackup format. Users contains the frozen full-width view/edit profile, not the older registration dialog. Preserve fields, Fluid MultiSelect, per-mantra targets, AES-GCM vault and passphrase backup format. Language/theme/text size belong in Settings.
- Japa goal tiles (11/101/1,001) change the daily goal, not completed repetitions. Reset restores the profile goal; the large plus increments completed count. Japa/Puja Skip·Complete are day flags only (no bead auto-fill). System/Escape Back: Settings child tab → UI → hub → Dashboard.

## Visual contract

Portrait-first shell (header + bottom rail; left drawer for labeled nav). Content readers may use landscape. Android dashboard uses the mockup &lt;720dp 2-column stack (week/streak/progress full-width); Japa stacks anga above counter on phone. Dashboard gamification: Sandhya weekly score (X/7) + streak/japa badges via `DashboardBadges` (streak 1→365, weekly 3/5/7, japa cumulative 108→10,00,001).

**Android:** Pure dark/light neutrals (black / white / grey) with orange accent `#BA4E00` only — no warm brown tonal surfaces. `Widget.MadhvaNidhi.*` styles; flat filled cards (no elevation overlays); Material Icons Outlined for shell/common actions. Restore point: git tag `restore/portrait-mock-pre-m3`.

**Mockups / freeze docs:** IBM Carbon remains the structure reference (productive square corners, interactive `#BA4E00`). Use freeze docs and `docs/frozen-design-tokens.json` for density, spacing, type and IA. The old AppleDash/1.5× corner-marker iteration is superseded: frozen Panchanga uses `WatsonHealthStatusPartialFail` for Ekadashi.

Canonical preview: `prototypes/app-shell/index.html` (studio), `shell.html` (screen). Shared assets: `prototypes/carbon-app.css`, `ibm-plex.css`, `studio.js`, `studio.css`, `user-vault.js`, `japa-session.js`. Menu and Panchanga previews are parity/reference surfaces. Preserve freeze markers and do not redesign frozen screens without a new user-directed design pass.

## Preview and verification

From the repository: `python -m http.server 8767 --bind 127.0.0.1 --directory prototypes`. Open `/app-shell/`; presets include 360×640, 390×844 and Galaxy M11 portrait. The mockup is static HTML/CSS/JS and has no frontend dependency-install/build step. A portable build must include the complete tracked `prototypes/` tree, including shared scripts, fonts, themes and relative assets. Do not copy local browser vault data or private app records into artifacts.

For changes, run JavaScript syntax checks, local asset/route checks, then inspect the browser. Do not claim Android or device validation from web-only checks. Preserve untracked user captures; do not add them to a preview package. Current validation results belong in the generated verification report, not in assumed persistent claims.

Rail icon update (after 9d3b242): Nitya Karma uses Carbon TimePlot, Puja uses TransformInstructions, and Grantha uses DocumentMultiple_01 in the shell and menu preview. Child destinations retain their own icons.

Latest mockup audit, 14 November 2026, is recorded in docs/implementation-audit.md. The numeric daily/Sankalpa/cumulative dashboard is the current approved mockup iteration. Android still renders percentage/next-target tiles. Automatic Sankalpa caps at 10,00,001 to match JapaGoals. Dashboard Japa selections open their corresponding counters.

14 November implementation update: Android Settings now has UI Settings, Dashboard, Users, Backup and Legal. The full-width Dashboard tab hosts the Japa target table, five Japa slot configuration rows and three shortcut pickers. Dashboard tile long-press customization/count mutation was removed. Numeric Japa tiles show Daily, Sankalpa and Cumulative. Ekadashi uses the official Carbon Transparency_01 pictogram from @carbon/pictograms 12.88.0 in both Android and the web calendar.

Puja catalogue update: Gruha Paddhati / Recipes now contains its Naivedya introduction and common guidance only. Puja includes all distinct Puja/festival catalogue entries, removing the old three-rite cap. Debug tests/install passed; phone verified full-width Dashboard Settings and Puja selector loading.

Songs navigation update: Hadugalu moved out of Grantha to the Bhakti Sangeeta / Songs menu. Songs now has Hadugalu and Playlists tabs. Grantha contains Stotra, Suktas, Mantras and Namavali. Native and both web previews route existing Hadugalu links into the Songs tab host. Debug build, unit tests and install passed; Galaxy M11 UI hierarchy verified two Songs tabs with the first selected.

Songs browse design pass: mockup menu tabs now follow the actual catalogue groups Dasara Padagalu, Suladigalu and Ugabhogagalu, plus existing Playlists. Search by title/author and author filtering precede a scrollable song list. Selecting a song opens bundled lyrics with Previous/Next and Back preserving list/filter position. Escape returns from reader to list first. Only entries with a bundled readable body are included in prototypes/songs-catalogue.json. Android has not yet received this browse-flow redesign.

Songs Favorites design: replaced the Playlists tab with Favorites in both web previews. Official Carbon Star / StarFilled controls appear in song rows and the lyrics view. Favorites persist in local browser storage and the Favorites collection updates when returning from the reader. Android still needs this approved browse/Favorites flow ported.

Puja Krama simplification: removed Skip/Complete, their progress indicator, and duplicate selected-rite heading/crumb in Android and both mockups. Kept the rite dropdown and procedure body. Switching a rite no longer resets completion flags. User phrase “selected puja” was interpreted as the repeated heading.

Puja Krama redesign, mockup first: Select Puja dropdown, 30:70 section-list/reader split, Section X of Y progress, Previous/Next and per-Puja reading position. Uses 20 bundled catalogue procedures and existing source headings. Deva Puja has eight sections. Texts without structured headings retain a single introduction section. Progress denotes reading position. Skip/Complete controls remain removed. Android still uses the earlier continuous reader until this design is approved for implementation.

Latest Puja implementation: no bottom navigation buttons. Section selection is available by tapping the breadcrumb, with a standard single-choice dialog. Selected Puja content loads on demand. Debug tests/install passed and the phone verified the section picker. Songs collection/Favorites and non-Puja floating reader controls remain web-only.

14 November reader/settings update: Settings exposes only Light/Dark; legacy soft/bright preferences map to those two modes. Global text-size controls were removed; landscape reader A−/A+ controls remain. Catalogue rows/dropdowns (including Puja) open ContentReaderActivity directly, without a second Open Content action. Nitya Sandhya, Parayana and Japa anga scripture no longer renders in the portrait shell; purpose-labelled reading actions open the sensor-landscape reader. Redundant Open Content buttons were removed from section details and Panchanga dialogs. The active Material Web preview mirrors landscape Puja/Songs reading by rotating its studio viewport.
Puja landscape reading keeps the complete source procedure in order, without fabricated steps or Skip/Complete controls. Native device validation confirmed the Light/Dark switch and direct stotra selection opening the landscape reader with reader-only text sizing. The existing Panchanga malformed-date unit test remains failing (32/33 pass).

Orange accent updated by user to #FF4F18 for both Light and Dark in Android Material tokens and the active Material Web preview. Filled orange controls use black foregrounds.

Veda Shakha (Sandhyavandana source) and Panchanga Parampara selectors moved from UI Settings to Users → Practice preferences. They retain the same SettingsStore keys, choices, automatic saving and selected values. The separate profile Veda metadata remains intact for Sankalpa. The active Material Web user dialog mirrors these controls with isolated browser storage.

Dashboard correction: dashboard progress dialogs are read-only; all checkbox/count mutations were removed, as was demo seeding from dashboard binding. Japa checkmarks now use the Japa portal’s explicit completion flag rather than inferring completion from bead targets. Removed media volume from UI Settings; developer credit is body-sized, two-line “Developed by Human Centric Works / hcworks.in”. Panchanga retains only its day-event details icon, with date/weekday/tithi order matching text span offsets. Bottom-nav active pills are centred on icons, use the orange primary surface and expose the selected destination accessibly.

Publisher metadata changed to an individual developer using the name and public support details supplied by the user. Settings footer, Legal developer contact, bundled developer notice and Play Store drafts are aligned. Third-party licence attributions remain. The local privacy-policy draft now describes local Sadhak profile/backup data. A hosted privacy-policy URL and complete Play Console legal-address/identity verification remain publishing tasks; no store release or website publication was performed.

Award/Japa icon update: awards use the official Material Symbols Outlined award_star filled glyph, with lower/middle/upper thirds of each existing milestone ladder mapped to brown/silver/gold. Their thresholds and stored progress are unchanged. Japa uses official eda, outlined FILL 0, weight 100, GRAD 0, optical size 24; Android vectors are bundled offline and the active web preview loads a combined variable-symbol subset. Symbol attribution and Apache-2.0 licence are bundled under assets/licenses.

Dashboard Japa state redesign: removed the dark top daily-practice card and Japa tick icons. The overall Japa card and individual tiles use primary-container fill, primary outline and explicit Completed text when the existing portal completion flag is true; pending/skipped remain neutral and labelled. Counts/targets remain factual and are not auto-filled. The mock has matching completed/pending/skipped read-only variants.

External source comparison (14 November): 528 pinned bibliographic references from three StotraSamhita collections, 426 title/ID matches, 96 new candidates and six variant reviews. Chanda is an AGPL Sanskrit metre tool, recorded without integration. No new text bodies imported or relicensed. Existing source overlap requires provenance/redistribution review before publication. See docs/catalogue/source-review-2026-11-14.md.

Dashboard dark-theme correction: primaryContainer is now a muted dark tonal colour (#4A2115 / #FFDAD0), while Japa summary/tiles always use neutral surfaceContainer with onSurface text. Completion is shown by an orange outline and explicit status. Recent practice renamed Weekly streak. Weekly completion check decorations removed; morning/noon/evening icons remain and web rows match. installDebug passed and neutral dark cards inspected on Galaxy M11; all six preview state/theme combinations passed overflow/card-colour/tick checks.

Menu/navigation icons now use offline official Material Symbols Outlined vectors: home, calendar_month, self_improvement (Nitya Karma), spa (Puja), menu_book, music_note, temple_hindu and settings. Dashboard shortcuts delegate to the same FrozenShell icon mapping. Practice tabs now include semantic icons, including the requested eda for Japa (FILL 0, weight 100). Their strip uses natural-width scrollable tabs to accommodate icons and full labels. Material attribution extended. installDebug passed; phone and light/dark preview render inspected.

Native Sandhyavandana/Japa redesign: Material buttons >=64dp; Japa +1 >=96dp and full-width. Labelled session selectors, section/status card, explicit section navigation and separate completion actions. Japa counter hierarchy and expandable clearly-labelled target options. Portrait tracking / landscape content preserved. installDebug + AndroidTest compilation passed; primary control bounds measured on Galaxy M11, target expansion verified without mutations, Japa font scale 1.3 inspected and restored to 0.9. See docs/practice-screen-redesign-2026-11-14.md.

Japa flow correction: select full mantra → Poorvanga → counting → Uttaranga → explicit finish. No target-changing buttons in portal. +1 and positive batch entry record counts; undo reverses last action in both daily/cumulative totals. Per-mantra/day encrypted stage persistence, lifecycle refresh, fixed next/back footer. Finishing preserves totals; below-target finish requires confirmation. Dashboard completion now per mantra; aggregate day complete only for all selected mantras. Five focused unit tests plus isolated device count/persistence/rollover checks passed. See docs/japa-flow-2026-11-14.md.

Rangavalli now routes to an offline Material gallery: six original transparent 1024px PNG line studies with filters, enlarged preview, drawing guides and source links. Three blog pages catalogued (59 image URLs retained); no blog photo/article bodies bundled and no licence grant invented. Editable geometric SVGs and regeneration/audit tools included. See docs/rangavalli-artwork-2026-11-14.md.

Naivedya cookbook: Recipes now routes to NaivedyaFragment with five offline Kannada cards (Hayagreeva, avalakki panchakajjaya, kosambari, panaka, hesaru-bele payasa). Full-width selector, ingredient/quantity rows, explicit soak/prep/cook times, method/notes and source links. Factual recipe cards collected from four blogs; original concise Kannada methods, no source photos/articles/health claims. Audit and installDebug passed. See docs/naivedya-cookbook-2026-11-14.md.

Panchanga correction: user confirmed Bhava should be Nakshatra pada. Both labels corrected. Fixed all 60 Karana slots (off-by-one movable sequence and four fixed names), normalized SRS Karana aliases, versioned provider caches v2. Local nakshatra/pada/rashi/Yoga now share an approximate sidereal frame and proper UTC-instant Julian conversion at 06:00 device-local time. Provider has no pada; UI explicitly labels the local estimate and withholds it if provider/local nakshatras disagree. Strict safe date parsing resolves the old malformed-input failure. FULL unit suite now 50/50 passed; build/install and phone label/layout checks passed. See docs/panchanga-correction-2026-11-14.md.
