# Frozen Nitya · Japa — finalized 12 November 2026

Approved web-mockup contract for the **ಜಪ** tab inside Nitya Karma. Layout changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `[data-nitya-panel="japa"]` (`data-frozen="nitya-japa"`) |
| Session logic | `prototypes/japa-session.js` |
| Targets source | User vault `japaTargets` ([frozen-user-profile.md](./frozen-user-profile.md)) |
| Shared chrome | `prototypes/carbon-app.css` (`.japa-*`) |

## Screen structure

1. **Toolbar**: mantra `cds--select` (left) · Skip · Complete (right)
2. **50:50 landscape split**
   - **Left:** Carbon **contained** tabs ಪೂರ್ವಾಂಗ / ಉತ್ತರಾಂಗ + anga body
   - **Right:** pad tiles — count `done/target` (completed count **2.5×** target type size, centred); goal tiles **11 · 101 · 1,001** (add to daily goal, not count); reset tile restores **daily goal from profile**; large **+** increments count by 1
3. Fit viewport — **no scroll**; equal `$spacing-05` gaps/padding
4. Indian digit grouping on large numbers where shown

## Behaviour

- Goal tiles mutate daily target only
- Reset restores vault/profile daily goal for the selected mantra (does not zero count)
- Skip / Complete are **day flags only** (pressed chrome); Complete must **not** auto-fill remaining bead count
- Mantra list prefers diksha-selected mantras from User profile

## Android mapping notes

Rewrite `page_japa.xml` / `JapaFragment` to pad layout; Material tiles under Carbon tokens; square corners (`0`). Persist via `PracticeDayStore.setJapaDay`; pressed chrome via `NityaDayChrome`.
