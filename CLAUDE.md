# Claude Code — MadhvaNidhi

Follow **[AGENTS.md](./AGENTS.md)** as the source of truth.

## Quick policy

- **Phase:** approved mockup → Android implementation (`MOCKUP_ONLY = false`)
- **Edit:** `app/` (primary), freeze docs, `prototypes/` only for freeze/parity marks
- **Allowed:** Gradle builds, `adb install`, landscape device validation
- **Landscape only** — never portrait viewports or layouts
- **Design authority:** IBM Carbon only — `Widget.Carbon.*` / `carbon_*` / `cds--*`; no custom components
- **Frozen:** Panchanga, User, Nitya Sandhya/Japa/Puja — do not redesign without a new design pass

Studio chrome (mockup): screen-size dropdown + live preview only.
