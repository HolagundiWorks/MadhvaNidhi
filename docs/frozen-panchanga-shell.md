# Frozen Panchanga and application shell — finalized 12 November 2026

This is the **approved web-mockup contract** for the Panchanga calendar. Future screens reuse the shell and Carbon semantic tokens. Panchanga layout changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Live calendar mockup | `prototypes/panchanga/panchanga.html` |
| Studio preview | `prototypes/panchanga/index.html` |
| Shell destination map | `prototypes/app-shell/` |
| Shared chrome | `prototypes/carbon-app.css` |
| Theme tokens | `prototypes/*/carbon-theme.css` (synced copies) |
| Machine tokens | `docs/frozen-design-tokens.json` |
| Carbon audit | `docs/carbon-mockup-audit.md` |

The approved Android port is authorized by root `AGENTS.md` (`MOCKUP_ONLY = false`). Native views implement this frozen contract under Carbon tokens; the mockup remains the visual authority.

Primary rail destinations: Dashboard, Panchanga, Nitya Karma, Puja, Grantha, Bhakti Sangeeta, Gruha Samskruti, Settings (8 items). User profile opens under Settings → Users. Child features open from hub tabs. PaperDesk scheduler/todo/notes/projects/Gita/tools are not destinations.

## Shell and spacing tokens

| Token | Web | Role |
| --- | --- | --- |
| rail width | 56px | Collapsed navigation |
| expanded panel | 224px | Overlay; content does not shift |
| toolbar / header | 48px min control | Header row; no search / theme icons in toolbar |
| action target | 48px (`$button-height`) | Icon buttons and nav items |
| page inset | 24px (`$spacing-06`) | Main content horizontal inset |
| corner | **0** (`--cds-border-radius`) | Square productive surfaces |
| border stroke | 1px (`--cds-border-width`) | Subtle / strong / tile tokens only |
| focus | 2px `$focus` outline | Not a layout border |
| context / calendar | **30:70** (wide) · **stacked** (portrait phone) | Panchanga screen only · On portrait shell Android stacks context above calendar; 30:70 applies when width allows · other destinations are full-width |
| spacing scale | 2–64px | Carbon `$spacing-01`…`$spacing-10` |
| tile columns | 5 | One complete 15-tithi paksha |
| tile gap | `$spacing-03` (8px) | Between day tiles |
| tile padding | `$spacing-05` (16px) | Carbon tile content inset |
| tile marker | 16px (1rem) | Top-right inside tile padding |

## Panchanga screen layout (final)

### Header

- **Left:** screen title `ಪಂಚಾಂಗ`
- **Right:** civil date · tithi (`header-date`)
- No search, theme switcher, or text-size controls in the header. Theme / display prefs live under **Settings** (SettingsAdjust). Profile / tradition fields live under **User** (UserSettings).

### Left column (~30%) — context

1. **Era block** (three width-fitted lines; snap to Carbon heading-compact-01 / -02 / heading-03 only)
   - Line 1: Shaka era (`ಶ್ರೀ ಶಾಲಿವಾಹನ ಗತಶಕೆ-…`) — primary
   - Line 2: Samvatsara — secondary
   - **Masa row** between subtle top/bottom rules: `ಮಾಸ` + ` | ` + Carbon **Sunny** weather glyph
2. **Facts list** (not cards): single-column definition list
   - ರಾಶಿ · ನಕ್ಷತ್ರ · ಭಾವ · ಕರಣ
   - Label left (`$text-secondary` / label-01); value right (`$text-primary` when present, `$text-helper` when pending)
   - Row dividers: 1px `$border-subtle-00`; transparent background (no layer tiles)
3. Subtle rule
4. **ದಿನದ ವಿಶೇಷ** — unboxed event title + summary; vertical scroll with **no visible scrollbar**; Carbon **Launch** opens details dialog

### Right column (~70%) — calendar

1. **Month bar**
   - Lead: paksha moon cycle (`○ → ◐ → ●`) · vertical separator · **Choose date** calendar icon
   - Trailing: Carbon content-switcher paksha toggle (ಶುಕ್ಲಪಕ್ಷ / ಕೃಷ್ಣಪಕ್ಷ)
2. **Day tiles** — borderless Carbon layer tiles
   - Content order: tithi name, civil date number, weekday
   - Selected: `$layer-selected` fill only — **no** selection outline / border / highlight stroke
   - Ekadashi: top **Yellow 30** alert strip (`$support-warning-inverse`); tithi text uses `$support-warning-text` (Yellow 30 dark / Yellow 20 `#684E00` on light); marker = Carbon **WatsonHealthStatusPartialFail**
   - Pournima / Amavasya: filled / outlined moon markers

### Date picker dialog

- Month / year controls + 7-column grid
- Outside-month days: `$text-disabled`
- Selected day: brand interactive fill + on-color text (Carbon date-picker pattern)
- **ಇಂದು** / cancel / confirm actions

### Date details dialog

- Same definition-list pattern as context facts (label | value rows)
- Includes ವಾರ / ಪಕ್ಷ / ತಿಥಿ / ಮಾಸ / ರಾಶಿ / ನಕ್ಷತ್ರ / ಭಾವ / ಕರಣ + event block

## State colours (Carbon)

| State | Token | Use |
| --- | --- | --- |
| Active / selected | `$layer-selected-01` + `$border-interactive` inset | Nav current, paksha / content switcher |
| Active (day tile) | `$layer-selected-01` only | Calendar selection |
| Inactive | `$text-secondary` | Unselected switcher / nav |
| Disabled | `$text-disabled` / `$icon-disabled` | Outside-month, disabled controls |
| Alert strip / icon | `$support-warning` / inverse (Yellow 30 `#F1C21B`) | Ekadashi bar + marker |
| Alert text | `$support-warning-text` | Readable labels (Yellow 30 dark / Yellow 20 light) |
| Brand accent | `$interactive` / `$border-interactive` | Orange brand override of Carbon blue |

Orange is the MadhvaNidhi brand override. Yellow is reserved for Ekadashi alerts. Full interaction roles: `carbon-theme.css` (G100 / G90 / G10 / White). Dark (G100) is the initial default; preference persists.

## Icon contract

| Use | Icon |
| --- | --- |
| Side panel open/close | Carbon `OpenPanelFilledLeft` |
| Ekadashi corner | Carbon `WatsonHealthStatusPartialFail` (Yellow 30) |
| Masa weather | Carbon `Sunny` |
| Event details | Carbon `Launch` |
| Choose date | Carbon calendar glyph |
| Pournima corner | Filled circle |
| Amavasya corner | Outlined circle |
| Paksha selector | Outlined Shukla / filled Krishna circle |
| Name / gotra / marital / rashi / nakshatra / veda / matha / diksha | User destination (frozen · see `docs/frozen-user-profile.md`) |
| Theme / language / text size | Settings destination only (not Panchanga header) |

Carbon SVG paths: Apache-2.0 — see `prototypes/panchanga/CARBON-ICONS-LICENSE.txt`. Markers are decorative; each tile’s accessible name includes tithi and civil date.

## Data and interaction contract

The web fixture is **illustrative**. Pending fields (ರಾಶಿ / ನಕ್ಷತ್ರ / ಭಾವ / ಕರಣ) show `ಜೋಡಣೆ ಬಾಕಿ` with helper colour until data is wired. Do not relabel Yoga as Bhava. Tile press updates context + events; multi-date tiles may prompt for a civil date. Choose-date opens the in-mockup month picker (Today jumps to local today). Standalone preview includes rail + header; shell embed (`?embed=1`) hides them and syncs theme from the parent shell.

## Edge-to-edge display (no app border)

The app has **no device frame / outer border**. It draws edge-to-edge and **merges with the mobile system chrome** (status bar, navigation bar, notch / teardrop / punch-hole cutouts):

| Surface | Colour |
| --- | --- |
| Status / navigation / cutout backdrop | `$background` (`--cds-system-chrome`) |
| App canvas | `$background` |
| Studio preview iframe | No outline / shadow / radius — same theme background |

- Web mockup: `viewport-fit=cover`, `theme-color` synced to `$background`, `env(safe-area-inset-*)` padding on `#app`
- Android (when authorized): edge-to-edge window; status/nav bar colours = Carbon `$background` for the active theme; content uses WindowInsets for cutouts — never a contrasting letterbox border around the app

This is especially important on phones with notches or teardrops in landscape: the cutout region should read as part of the app theme, not a black/white system slab.

## Landscape-only + responsive density

Studio presets are portrait-first (`height > width`). Content readers may use landscape. See `AGENTS.md`.

Reference polish size: **960×540**. On the Panchanga screen, all landscape sizes keep the same **30:70** structure and scale chrome + tiles via:

`--density = clamp(0.55, min(vw/960, vh/540), 1.75)`

| Size | Density | Notes |
| --- | --- | --- |
| 640×360 | ~0.67 | Compact phone |
| 960×540 | 1.00 | Reference polish |
| 1560×720 (SM-M115F) | ~1.33 | Same proportions, scaled up |

| Token | Scales with density |
| --- | --- |
| `--page-inset` / `--pane-gap` | 24px × density |
| `--control-size` / rail | 48px / 56px × density |
| Day tile side | capped at **110px × density** (fills smaller phones; does not balloon on 1560×720) |
| `--tile-pad` | ~14.5% of tile / 16px × density |
| Type (label / body / title) | 12 / 14 / 16px × density |

Do **not** stack context above calendar on narrow landscape widths. Tile grid stays centered in the calendar pane when capped.
