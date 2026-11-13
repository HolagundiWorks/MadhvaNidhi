# MadhvaNidhi project memory

Updated 13 November 2026. UX audit pass complete: practice day flags (Sandhya skip, Japa/Puja/Parayana), Sandhya 48dp pathya hit + no auto-complete, backup passphrase confirm, nav toggle 48dp, hierarchical Back. Verify Git status before changing code.

## Current authority

Read root `AGENTS.md` first. The current phase is approved mockup → Android implementation (`MOCKUP_ONLY = false`). Android implementation, debug builds and connected-device validation are authorized. Publishing, release signing, portrait layouts and a Material visual restyle remain out of scope. Material widgets may host Carbon styles; layouts must use `Widget.Carbon.*`, `TextAppearance.Carbon.*` and `carbon_*` tokens.

Use the current repository mockups as the design authority. The earlier external `latest-commit-preview` ZIP/patch based on `5adf7ea` is superseded and must not be applied over the newer source. Earlier chat claims about unresolved native split clipping describe that old intermediate build, not a verified diagnosis of this checkout. No Android build or device test was rerun for this web-preview refresh.

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

Landscape only. IBM Carbon is the sole visual authority. Productive surfaces have square corners (`0`), not the older 4px design. Orange maps to the Carbon interactive slot; domain alert yellow remains separate. Use the exact current freeze docs and `docs/frozen-design-tokens.json` for density, spacing, type, icons and colours. The old AppleDash/1.5× corner-marker iteration is superseded: current frozen Panchanga uses `WatsonHealthStatusPartialFail` for Ekadashi and its selected state is layer-selected only.

Canonical preview: `prototypes/app-shell/index.html` (studio), `shell.html` (screen). Shared assets: `prototypes/carbon-app.css`, `ibm-plex.css`, `studio.js`, `studio.css`, `user-vault.js`, `japa-session.js`. Menu and Panchanga previews are parity/reference surfaces. Preserve freeze markers and do not redesign frozen screens without a new user-directed design pass.

## Preview and verification

From the repository: `python -m http.server 8767 --bind 127.0.0.1 --directory prototypes`. Open `/app-shell/`; presets include 640×360, 960×540 and Galaxy M11 1560×720. The mockup is static HTML/CSS/JS and has no frontend dependency-install/build step. A portable build must include the complete tracked `prototypes/` tree, including shared scripts, fonts, themes and relative assets. Do not copy local browser vault data or private app records into artifacts.

For changes, run JavaScript syntax checks, local asset/route checks, then inspect the browser. Do not claim Android or device validation from web-only checks. Preserve untracked user captures; do not add them to a preview package. Current validation results belong in the generated verification report, not in assumed persistent claims.

Rail icon update (after 9d3b242): Nitya Karma uses Carbon TimePlot, Puja uses TransformInstructions, and Grantha uses DocumentMultiple_01 in the shell and menu preview. Child destinations retain their own icons.
