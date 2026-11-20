# MadhvaNidhi project memory

Updated 17 November 2026 (Asia/Calcutta). This is the current implementation summary; older mockup contracts and historical commit messages may describe superseded layouts. Verify Git status and root AGENTS.md before changing files.

## Repository and sync state

- Correct checkout: `D:/Work Development/Repos/MadhvaNidhi`; remote: `https://github.com/HolagundiWorks/MadhvaNidhi.git`. The neighboring PaperDesk repository is not this app's checkout.
- Reviewed `origin/main`: `1484618` (16 November), dashboard image sharing. The Aayadi calculator was removed in `9937ee8`; see "Aayadi moved out" below.
- Sync state at the 17 November refresh is historical; check `git status` and `git log` for the current state.
- `2144f8b` is the earlier broad Material/practice/content update, not the latest revision. Public GitHub search indexing is not needed to fetch commits.
- Neon was connected by mistake and the user disconnected it. Do not use Neon or add a cloud backend for MadhvaNidhi without a new request. Ritual data remains local.

## Authority and work ownership

- Android uses Material Design 3. Root AGENTS.md is the working policy; publishing/release signing is out of scope without explicit authorization.
- Songs (Haridasa source files, song catalogue preview and RAO collection) belong to the songs session. Android UI changes, builds, installation and phone checks belong to the UI worker. Each owner commits only its own paths. Preserve concurrent work and never stage all changes indiscriminately.
- Android source is `app/`; structural prototypes are `prototypes/`; design contracts are `docs/`. Do not apply the old external preview ZIP based on `5adf7ea`.
- The shell is portrait; scripture, mantra, stotra and song bodies open in the sensor-landscape ContentReaderActivity. Reader text sizing stays in the reader; no global text-size setting.

## Design system

- Theme: Theme.Material3.DayNight.NoActionBar via Theme.PaperDesk.Base. Light/Dark only. Accent `#FF4F18`; neutral white/grey and black/grey surfaces; black foregrounds on filled orange controls.
- Three token layers: `app/src/main/res/values/tokens_foundation.xml`, `tokens_semantic.xml`, `tokens_component.xml`. Screen roles use `ds_*` / Material tokens; semantic colours alias `md_theme_*`. Typography has UI, mantra and scripture roles. Do not reintroduce Android Carbon styles/tokens or the old brown palette.
- Icons are offline Google Material Symbols vectors with bundled attribution. Japa uses eda, awards award_star. Icon-only controls are round; labelled controls use the shared rounded Material styles.
- The 15 November cleanup (`85af4d5`) removed unused resources, legacy styles and several stale audit/implementation notes. Do not recreate removed resources merely because older prose references them. `docs/ui-system.md` describes the older AcharyaOS landscape UI and is not current visual authority.
- Web Carbon structure references remain distinct from Android Material visuals. `prototypes/current-app/` contains the Material dashboard preview; `prototypes/app-shell/index.html` redirects there. Other prototypes can lag native implementation: inspect their source before claiming parity.

## Navigation and current screens

- Floating dock, left to right: Panchanga (`desk`), Nitya Karma, Puja, raised centre Dashboard, Grantha, Bhakti Sangeeta, Gruha Samskruti. Current source of truth is ShellHubFragment.RAIL_IDS / FrozenShell; do not restore the older eight-item dock with Settings at the end. Preserve dock clearance beneath scrollable content. Settings remains a supported destination.
- Nitya tabs: Sandhyavandana, Japa, Nitya Parayana. Puja is standalone.
- Grantha tabs: Stotra, Suktas, Mantras, Namavali, Wiki. Songs are outside Grantha. The native Wiki was added in `3b6b8d9`; it is no longer only a planned web page.
- Bhakti Sangeeta: Dasara Padagalu, Suladhi, Ugabhoga, Favorites; additional explicitly classified song groups can use Other Songs. Legacy playlist links redirect to Favorites. Title search opens the landscape reader directly; stars work in lists and reader, persist per profile and participate in encrypted profile backups.
- Samskruti tabs: Rangavalli, Recipes/Naivedya.
- Settings tabs: UI Settings, Dashboard, Users, Backup, Legal. Veda Shakha and Panchanga Parampara are under Users / practice preferences. Dashboard japa targets and shortcuts are configured in Settings; dashboard cards do not mutate practice counts.

## Practice and dashboard

- Sandhyavandana: morning/noon/evening selection, Open Sandhyavandana, selected-session status and Skip/Complete. Sections are selected inside the landscape reader (Full session or a numbered section); no previous/next section buttons. Skip, Complete and undo require confirmation. Reading/navigation must never auto-complete a session. See `docs/frozen-nitya-sandhya.md`.
- Japa: select the full mantra name, Poorvanga reader, counting, Uttaranga reader, explicit finish. Tap/batch/undo update actual counts; targets stay in Settings. Stage is profile/mantra/day scoped. Daily rollover clears daily count/stage while keeping cumulative totals. Completion comes from the practice portal and does not fill target counts automatically.
- Puja keeps its rite selector and section chooser in its reading flow; Skip/Complete and bottom section-navigation buttons remain removed. Reading progress is separate from completion.
- Dashboard now includes Panchanga and Dina Visesha cards, weekly/streak/badge information and read-only japa progress. The newer card hierarchy/shared tabs/list-choice updates supersede the old dashboard row/column mockup contracts.
- Dashboard sharing (`1484618`): user chooses Streak, Badges or Today; ShareCards renders a 1080x1920 PNG locally and opens Android's chooser through FileProvider. Never auto-share or post to WhatsApp. Sharing may disclose the practice facts displayed on the chosen card; keep it user-initiated.

## Panchanga, Wiki and content provenance

- Panchanga now renders the month as equal tithi boxes: five columns, six rows (15 Shukla then 15 Krishna), rather than the older single-paksha screen. Matha context is in the top bar (`232cd31`).
- Nakshatra pada is the user-confirmed field; do not silently replace it with Yoga. Karana uses the corrected 60-slot sequence and normalized provider aliases. Local astronomical estimates must remain labelled as estimates; do not claim provider accuracy for locally calculated values.
- Wiki data lives in `app/src/main/assets/knowledge/wiki.json`, with web data under `prototypes/knowledge/`. Reviewed changes include sourced Madhva/Dvaita articles, Matha lineage, coordinates and paryaya rotation. Preserve citations, pending/partly-sourced markers and traditional-account distinctions. Verify facts before changing their status; do not treat earlier unverified Kaniyooru details as established.
- Songs switched to the RAO Collections blog source; earlier Wikisource/erroneous Haridasa bodies were removed by the songs owner. Do not restore them. `d04d3c7` adds 20 Suladhi songs. Current catalogue has 66 Dasara, 40 Suladhi and 2 Ugabhoga records, all with file references; these counts reflect the inspected JSON, not a fresh text/rights audit.
- Attribution and redistribution restrictions remain in bundled licence notices and `docs/catalogue/source-review-2026-11-14.md`. Bibliographic source references are not blanket permission to redistribute transcriptions. Check provenance before publication.
- Rangavalli: six original offline PNG line studies, editable vectors and source links. Naivedya: five offline Kannada recipe cards with cited ingredient facts and original concise preparation text. External blog photos/article bodies are not bundled. See `docs/rangavalli-artwork-2026-11-14.md` and `docs/naivedya-cookbook-2026-11-14.md`.

## Aayadi moved out

- The Aayadi house-size calculator is no longer part of MadhvaNidhi. It was removed in `9937ee8` and lives in its own app, the Aaya Calculator (repository `AayadiCalculator`, `https://github.com/HolagundiWorks/AayadiCalculator.git`). Do not re-add it here.
- The earlier Aayadi commits (`c7ae0cb` and the commits before `9937ee8`) describe the old in-app screen and are superseded.

## Verification for this memory refresh

- Ran fetch and fast-forward-only pull; reviewed recent commit history and current Kotlin/XML/JSON token, navigation, catalogue, Wiki and sharing sources.
- Changed only this documentation file. No app build, tests, installation, database operation, commit or push was performed in this pass. Prior build/test claims in earlier history are not fresh validation of current HEAD.
