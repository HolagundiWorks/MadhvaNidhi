# Claude Code — MadhvaNidhi

Follow **[AGENTS.md](./AGENTS.md)** as the source of truth.

## Quick policy

- **Phase:** approved mockup → Android implementation (`MOCKUP_ONLY = false`)
- **Edit:** `app/` (primary), freeze docs, `prototypes/` only for freeze/parity marks
- **Allowed:** Gradle builds, `adb install`, portrait device validation
- **Portrait-first** — main shell is portrait; content readers may use landscape
- **Android visual authority:** Material Design 3 only (`Widget.Material3.*` / `md_theme_*`, orange accent `#FF4F18`, black/white/grey neutrals). No Carbon in Android. All icons from Google Fonts Material Symbols with licence attribution.
- **Mockups:** Carbon structure reference (`cds--*`); do not Material-restyle mockups unless asked
- **New knowledge/achievement screens:** portrait; Carbon in the web mockup, Material 3 on Android; wiki facts stay `pending` until cited; Android code for them waits for approval of the spec's Phase 7 (see AGENTS.md)
- **Frozen:** Panchanga, User, Nitya Sandhya/Japa/Parayana, Puja (rail) — do not redesign layout/IA without a new design pass
- **Restore:** `restore/portrait-mock-pre-m3` before Material3 Android pass

Studio chrome (mockup): portrait screen-size dropdown + live preview only.
