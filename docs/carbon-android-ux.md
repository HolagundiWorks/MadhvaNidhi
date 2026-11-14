# Carbon Android UX — MadhvaNidhi

Checklist for Views + `carbon_*` / `Widget.Carbon.*` (no Material 3 restyle, no Compose rewrite in this phase). Design authority remains frozen mockups under `prototypes/`.

## Principles

| Principle | Practice |
| --- | --- |
| Clarity | Dashboard answers: what is done, what remains, how am I progressing — without decoding chrome |
| Consistency | Same day-action FABs, pressed chrome (`NityaDayChrome`), spacing, and hub-tab patterns everywhere |
| Hierarchy | Sandhyavandana week + streak outrank shortcuts; shortcuts outrank decorative chrome |
| Efficiency | One tap to record or open the active practice; no silent irreversible fills |
| Accessibility | ≥48dp action targets (or documented density shrink), TalkBack names, fontScale-safe labels |
| Progressive disclosure | Essentials first; history / detail in dialogs or secondary panels |
| Feedback | Completed / skipped / pending use shared status colours and pressed states |
| Predictability | Rail order, Skip·Complete positions, and Back → parent stay fixed |

## Productive motion

Use `carbon_duration_*` / `CarbonMotion` only. Prefer productive over expressive.

| Token | Duration | Use |
| --- | --- | --- |
| `carbon_duration_fast_01` | 70ms | Button / FAB pressed chrome |
| `carbon_duration_fast_02` | 110ms | Subtle fade |
| `carbon_duration_moderate_01` | 150ms | Expand session detail |
| `carbon_duration_moderate_02` | 240ms | Confirmation / dialog settle |

Respect system reduced-motion: skip non-essential animation when `CarbonMotion.reduced(context)`.

## Dashboard hierarchy

1. Weekly Sandhya matrix (interactive)
2. Quiet streak (does not compete with the week)
3. Japa slots: **daily progress, Sankalpa and cumulative**
4. Shortcuts + daily progress summary

Empty / future days stay pending marks (`–`), never look like errors. Future tiles that are not productized stay hidden or a single placeholder — not fake cards.

## Architecture (current)

- Tokens: `res/values/carbon_*.xml`, `carbon_motion.xml`
- Patterns: `NityaDayChrome`, `CarbonStatus`, `CarbonMotion`
- Screens consume semantic resources — no one-off hex colours or ad-hoc radii

Compose `design-system/` remains a **later pilot** after journeys freeze in the mockup.
