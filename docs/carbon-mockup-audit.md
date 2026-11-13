# Carbon mockup audit (web prototypes)

Historical snapshot of the mockup phase. For current implementation policy see [the UI system](ui-system.md).

Date: 2026-11-12 · Scope: `prototypes/` only (`MOCKUP_ONLY`)  
Revision: **panchanga-final + user-profile-final**

## Aligned

| Area | Status |
| --- | --- |
| Color / theme tokens | `@carbon/themes`-style CSS variables in `carbon-theme.css` (G100/G90/G10/White) |
| Brand accent | Orange on `--cds-interactive` / button-primary / link / `$border-interactive` |
| Spacing | Carbon spacing scale (`--cds-spacing-01`…`10`); page inset `$spacing-06` |
| Radius | `--cds-border-radius: 0` (square productive surfaces; `carbon-app.css`) |
| Borders | 1px token strokes; base tiles have no box border |
| Focus | 2px `$focus` outline |
| Studio chrome | Size selector + live iframe only |
| Typography | IBM Plex Sans + Noto Sans Kannada; productive type tokens |
| Shell rail icons | Carbon 32px path icons (Apache-2.0) |
| State colours | Active = layer-selected + border-interactive; inactive = text-secondary; alert = support-warning / warning-text |

## Panchanga final (2026-11-12)

- 30:70 context / calendar; landscape-only
- Header: title + date·tithi; theme/text-size deferred to Settings
- Era: three fitted lines; masa between rules with Sunny glyph
- Facts: definition **list** (ರಾಶಿ / ನಕ್ಷತ್ರ / ಭಾವ / ಕರಣ) — not cards
- Day tiles: layer fill; padding scales with tile (`--tile-pad`, 16px at 960×540)
- Selected = `$layer-selected` only (no outline)
- Ekadashi: Yellow 30 top strip + `WatsonHealthStatusPartialFail`; light themes use Yellow 20 for alert **text**
- Month bar: moon cycle | choose-date · paksha content switcher
- **Responsive density** from 640×360 ↔ 960×540 ↔ 1560×720 (same proportions; density 0.55–1.75; tiles capped at 110px×density)
- **Edge-to-edge**: no preview/app outer border; system chrome / cutouts use `$background` (`theme-color` + safe-area)
- Shared chrome: `prototypes/carbon-app.css`
- Contract: `docs/frozen-panchanga-shell.md` · tokens: `docs/frozen-design-tokens.json`

## User profile final (2026-11-12)

- Full-width landscape masonry; view = Carbon label + body text; edit = Form / Select / Text input / Fluid MultiSelect
- Backup / Restore in Carbon Accordion; status live region outside accordion
- Encrypted vault + `.mnbackup` via `prototypes/user-vault.js` (FORMAT v1 frozen)
- Language not on User — Settings only
- Layout wrappers only: `form-masonry`, `user-profile-view` / `user-profile-fields` (no custom chrome)
- Contract: `docs/frozen-user-profile.md` · markers: `data-frozen="user-profile"`

## Intentional product-specific (keep)

| Item | Why |
| --- | --- |
| Noto Sans Kannada | Script coverage; not in IBM Plex |
| Paksha moon glyphs / day tiles | Domain UI; styled with Carbon tokens |
| Sample / pending panchanga fields | Mock state until data binding |
| Orange brand | MadhvaNidhi accent on Carbon slots |
| User `form-masonry` / profile view toggle | Landscape density; Carbon controls underneath |

## Android Carbon pass (2026-11-12)

Authorized full-shell port under `app/`. Material widgets map through `Widget.Carbon.*` styles (`carbon_components.xml`).

| Check | Result |
| --- | --- |
| Corner radius | `@dimen/carbon_corner_radius` = **0dp** (theme shape + Nitya/User/Japa) |
| Buttons | Primary / Tertiary / Ghost / GhostOutlined / Icon — not raw Material3 chrome |
| Tabs | `Widget.Carbon.TabLayout` (layer + interactive indicator) |
| Selects | `Widget.Carbon.TextInput` / `.ExposedDropdown` square fields |
| Spacing / targets | `$spacing-*` + 48dp (`carbon_layout_size_height_lg`) action targets |
| Icons | Carbon paths; tint via `@color/carbon_icon_*` |

## Remaining gaps (optional later)

- Not using full `@carbon/styles` / Web Components package (static HTML mockup by design)
- Some layout still uses raw `px` equivalents of Carbon spacing (functionally same values)
- Button markup mixes `.primary` / `.ghost` with `cds--btn--*` — prefer `cds--btn` classes going forward
- Full Carbon type classes (`.cds--heading-compact-01`, etc.) not applied as class names everywhere yet (tokens used)
- Legacy hub/reader screens may still mix Material attrs; prefer `Widget.Carbon.*` on touch
