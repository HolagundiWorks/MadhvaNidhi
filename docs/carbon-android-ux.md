# MadhvaNidhi Android UX — Material Design 3

Android visual authority is **Material Design 3** (`Theme.Material3.DayNight`, `Widget.Material3.*` / `Widget.MadhvaNidhi.*`, `md_theme_*` seeded from brand `#BA4E00`). Mockups remain Carbon (`cds--*`) for layout/IA reference only.

Legacy `carbon_*` / `Widget.Carbon.*` names are bridges remapped onto M3 — do not restore Carbon 0dp productive corners on Android.

## Principles

| Principle | Practice |
| --- | --- |
| Clarity | Dashboard answers: what is done, what remains, how am I progressing |
| Consistency | Shared day-action chrome (`NityaDayChrome`), spacing, hub-tab patterns |
| Hierarchy | Sandhyavandana week + streak outrank shortcuts |
| Efficiency | One tap to record or open the active practice |
| Accessibility | ≥48dp targets, TalkBack names, fontScale-safe labels |
| Feedback | Completed / skipped / pending use tonal containers and pressed states |
| Predictability | Rail order, Skip·Complete positions, and Back → parent stay fixed |

## Material 3 chrome

| Surface | Style |
| --- | --- |
| Theme | `Theme.PaperDesk` → `Theme.Material3.DayNight.NoActionBar` |
| App components | `Widget.MadhvaNidhi.*` in `values/m3_components.xml` |
| Shell header | `MaterialToolbar` |
| Bottom destinations | NavigationBar-style pill indicator (`bg_rail_item_bottom_active`) |
| Panels / tiles | `MaterialCardView` filled (`SurfaceCard`) |
| Tabs | Secondary TabLayout with primary indicator |
| Fields | Outlined TextInput (4dp M3 small corners) |
| Buttons | Filled / Tonal / Outlined / Text with 12dp medium corners |

## Motion

Prefer productive over expressive. `CarbonMotion` / `carbon_duration_*` remain the duration bridges:

| Token | Duration | Use |
| --- | --- | --- |
| `carbon_duration_fast_01` | 70ms | Button pressed chrome |
| `carbon_duration_fast_02` | 110ms | Subtle fade |
| `carbon_duration_moderate_01` | 150ms | Expand session detail |
| `carbon_duration_moderate_02` | 240ms | Dialog settle |

Respect reduced motion via `CarbonMotion.reduced(context)`.

## Dashboard hierarchy

1. Weekly Sandhya matrix
2. Quiet streak
3. Japa slots (daily / Sankalpa / cumulative)
4. Shortcuts + daily progress

## Architecture

- Tokens: `m3_colors.xml`, `m3_components.xml`; legacy `carbon_*.xml` bridges
- Patterns: `NityaDayChrome`, `FrozenShell`, Material cards on portrait surfaces
- Screens consume semantic `md_theme_*` resources — no one-off hex in layouts
