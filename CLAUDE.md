# Claude Code — MadhvaNidhi

Follow **[AGENTS.md](./AGENTS.md)** as the source of truth.

## Quick policy

- **Phase:** approved mockup → Android implementation (`MOCKUP_ONLY = false`)
- **Edit:** `app/` (primary), freeze docs, `prototypes/` only for freeze/parity marks
- **Allowed:** Gradle builds, `adb install`, portrait device validation
- **Portrait-first** — main shell is portrait; content readers may use landscape
- **Android visual authority:** Material Design 3 (`Widget.Material3.*` / `md_theme_*`, seed `#BA4E00`)
- **Mockups:** Carbon structure reference (`cds--*`); do not Material-restyle mockups unless asked
- **Frozen:** Panchanga, User, Nitya Sandhya/Japa/Parayana, Puja (rail) — do not redesign layout/IA without a new design pass
- **Restore:** `restore/portrait-mock-pre-m3` before Material3 Android pass

Studio chrome (mockup): portrait screen-size dropdown + live preview only.
