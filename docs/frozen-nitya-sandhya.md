# Frozen Nitya · Sandhyavandana — finalized 12 November 2026

Approved web-mockup contract for the **ಸಂಧ್ಯಾವಂದನೆ** tab inside Nitya Karma. Layout changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `#view-nityakarma` → `[data-nitya-panel="sandhya"]` (`data-frozen="nitya-sandhya"`) |
| Parity mirror | `prototypes/menu-pages/pages.html` → same panel |
| Shared chrome | `prototypes/carbon-app.css` (`.sandhya-panel`, `.sandhya-chrome`, `.sandhya-scroll`) |
| Related | [frozen-nitya-japa.md](./frozen-nitya-japa.md), [frozen-nitya-puja.md](./frozen-nitya-puja.md) |

## Screen structure

1. **Contained tabs** (parent shell): ಸಂಧ್ಯಾವಂದನೆ · ಜಪ · ಪೂಜೆ
2. **Chrome toolbar** (`$spacing-05`): left kala icon FABs (ಪ್ರಾತಃ · ಮಧ್ಯಾಹ್ನ · ಸಾಯಂ); right Skip · Complete day actions
3. **Progress row**: pathya title **30%** + progress bar **70%** on one line
4. **Subtle rule** (`1px` `$border-subtle`)
5. **Scroll body**: crumb helper + reader body (`pre-wrap`)
6. **Pinned bottom nav**: prev / next pathya (does not scroll with body)

## Behaviour

- Kala change resets pathya index and clears day-action pressed state
- Prev disabled on first pathya; next disabled when complete or last pathya
- Skip → index 0; Complete → last pathya + 100% progress
- Landscape only; square corners; Carbon FABs / tokens only

## Android mapping notes

Material icon buttons + ProgressBar under Carbon colours/spacing. Prefer a tabbed `NityaKarmaFragment` hosting this chrome; reuse `SandhyaStore` for steps.
