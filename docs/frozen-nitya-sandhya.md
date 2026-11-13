# Frozen Nitya · Sandhyavandana — updated 13 November 2026

Approved web-mockup contract for the **ಸಂಧ್ಯಾವಂದನೆ** tab inside Nitya Karma. Layout changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `#view-nityakarma` → `[data-nitya-panel="sandhya"]` (`data-frozen="nitya-sandhya"`) |
| Parity mirror | `prototypes/menu-pages/pages.html` → same panel |
| Shared chrome | `prototypes/carbon-app.css` (`.sandhya-panel`, `.sandhya-chrome`, `.sandhya-scroll`) |
| Related | [frozen-nitya-japa.md](./frozen-nitya-japa.md); Nitya Parayana is a sibling tab; Puja is a separate rail destination — [frozen-nitya-puja.md](./frozen-nitya-puja.md) |

## Screen structure

1. **Contained tabs** (parent shell): ಸಂಧ್ಯಾವಂದನೆ · ಜಪ · ನಿತ್ಯ ಪಾರಾಯಣ
2. **Single toolbar row**: pathya title (flex) · kala FABs (ಪ್ರಾತಃ · ಮಧ್ಯಾಹ್ನ · ಸಾಯಂ) · Skip · Complete — all on one line
3. **Full-height reading body**: crumb helper + reader body (`pre-wrap`); no bottom pathya nav
4. **Floating progress**: horizontal bar at the bottom of the reading area, overlaid on the text at **50% opacity**; tap → next pathya; long-press → previous

## Behaviour

- Kala change resets pathya index and restores that kala’s persisted day-action state
- No prev/next navigation buttons
- Floating progress hit target is **48px** tall; visual bar stays thin @ 50% opacity
- Advancing onto the last pathya does **not** auto-complete — Complete FAB is explicit
- Skip → index 0 + persisted skip flag; Complete → last pathya + 100% progress + persisted done
- Landscape only; square corners; Carbon FABs / tokens only

## Android mapping notes

`page_nitya_sandhya.xml` + `NityaKarmaFragment.bindSandhya`; Material icon buttons + translucent `ProgressBar` inside a 48dp hit frame. Persist via `PracticeDayStore.setSandhyaDay` / `isSandhyaSkipped`. Reuse `SandhyaStore` for steps.

On narrow landscape phones the five toolbar FABs use Carbon **sm (32dp)** so pathya + kala + skip/complete stay on one row without clipping; mockup reference size remains **md (40dp)**.
