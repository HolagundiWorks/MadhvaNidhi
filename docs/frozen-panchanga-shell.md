# Frozen Panchanga and application shell — 12 November 2026

This is the approved visual contract for the Panchanga calendar. Future screens reuse the shell and semantic tokens. Panchanga layout changes require a new design iteration. The HTML calendar reference is `prototypes/panchanga/panchanga.html`; the Carbon shell destination map is `prototypes/app-shell/`. Android uses stock Material Components and stock Android layouts, not a WebView or app-defined View classes.

Primary rail destinations: Dashboard, Panchanga, Nitya Karma, Grantha, Bhakti Sangeeta, Gruha Samskruti, Settings. Child features open from hub pages. PaperDesk scheduler/todo/notes/projects/Gita/tools are not destinations.

Machine-readable export: `frozen-design-tokens.json`. Android dimensions are in `values/panchanga_shell.xml`; both theme palettes are qualified resources.

## Shell and spacing tokens

| Token | Web | Android | Role |
|---|---|---|---|
| rail width | 56px | 56dp | Collapsed navigation |
| expanded panel | 224px | 224dp | Overlay; content does not shift |
| toolbar | 48px | 48dp | Calendar, text size, appearance |
| action target | 48px | 48dp | Toolbar and navigation |
| page inset | 16px | 16dp | Main content |
| corner | 4px | 4dp | Tiles, dialogs, controls |
| context/calendar | 30:70 | 30:70 | Minimum context width 208px/dp where space allows |
| spacing scale | 2,4,8,12,16,24,32,40,48px | Same in dp | Carbon spacing rhythm |
| tile columns | 5 | 5 | One complete 15-tithi paksha; normally three rows |
| tile gap | 4px | 4dp | Shared-date groups span adjacent columns |
| tile marker | 24px / compact 15px | 24dp / compact 15dp | Top right, 1.5× original |

The selected date and Today action lead the left panel, followed by tithi/weekday, separator, Shaka/samvatsara, masa and paksha on one line, then Rashi/Nakshatra/Bhava/Karana. Labels are regular; supplied values are semibold; pending values are regular. A separator introduces unboxed daily events. Events scroll vertically with no visible scrollbar. The right panel has the two-button paksha selector and moon progression, without a repeated paksha heading. Tiles show date number (1.5× base and semibold), weekday, then regular-weight tithi, with separate line spacing. Selected tiles have orange outlines and neutral selected fill. Ekadashi is yellow independently of selection.

## Semantic colour tokens

| Role | Carbon White | Carbon Gray 100 |
|---|---|---|
| background | #FFFFFF | #161616 |
| layer 01 | #F4F4F4 | #262626 |
| selected layer | #E0E0E0 | #393939 |
| text primary | #161616 | #F4F4F4 |
| text secondary | #525252 | #C6C6C6 |
| subtle border on layer | #C6C6C6 | #525252 |
| outline | #8D8D8D | #8D8D8D |
| orange accent | #BA4E00 | #FF832B |
| text on accent | #FFFFFF | #161616 |
| Ekadashi | #684E00 | #F1C21B |
| error text | #DA1E28 | #FF8389 |

Orange is the MadhvaNidhi brand override to Carbon's default blue. Yellow is reserved for Ekadashi. Full web interaction roles remain in `carbon-theme.css`; Android semantic resources are in `values/carbon_colors.xml` and `values-night/carbon_colors.xml`. The appearance preference persists across restarts. Dark is the initial default.

## Icon contract

| Use | Icon |
|---|---|
| Light/dark action | Carbon `ColorSwitch` (`color-switch`) |
| Side panel open/close | Carbon `OpenPanelFilledLeft` (`open-panel--filled--left`) |
| Ekadashi corner | Carbon `AppleDash` (`apple--dash`), yellow |
| Pournima corner | Filled circle |
| Amavasya corner | Outlined circle |
| Paksha selector | Outlined Shukla / filled Krishna circle |
| Top calendar | Material calendar date picker |
| Calendar beside date | Return to actual local Today |
| Text size | A− / A+ |
| Settings | Side navigation only |

Carbon SVG paths come from https://github.com/carbon-design-system/carbon/tree/main/packages/icons/src/svg/32 and are distributed under Apache-2.0; see `prototypes/panchanga/CARBON-ICONS-LICENSE.txt`. Corner markers are decorative; each tile's accessible description names its tithi and civil date. Navigation and theme controls have action descriptions and standard touch targets. The MadhvaNidhi book logo remains unchanged.

## Data and interaction contract

The web fixture is illustrative. Android retains real PanchangaStore and FestivalStore data, date-dependent headers, all supplied civil dates, and explicit shared-date merging. Missing sunrise tithis are retained as disabled tiles with a missing-date state. Rashi and Bhava are pending because the current model does not supply them; Yoga is displayed in details as Yoga, never relabeled as Bhava. A tile opens details for its selected date; multi-date tiles offer a date choice first. A top calendar action opens the standard Material month-calendar picker. Settings and the remaining app sections remain accessible from the side panel. Device system insets remain outside the app shell.
