# MadhvaNidhi — Agent Instructions

**Current development phase: APPROVED MOCKUP → ANDROID IMPLEMENTATION.**

`MOCKUP_ONLY = false` (authorized full-shell port)

## Role

You are implementing the **approved web mockups** in the native Android application under `app/`, while keeping mockup freeze contracts accurate under `prototypes/` and `docs/`.

Authorization for this pass: implement the approved mockup in the Android application (full shell; freeze Nitya Sandhya/Japa/Puja first).

## Repository boundaries

| Path | Role | Agent access |
| --- | --- | --- |
| `prototypes/` | Interactive HTML/CSS/JS mockups (design source of truth) | **Read / write** (freeze marks, parity fixes) |
| `docs/` | Design contracts | **Read / write** |
| `app/` | Native Android application | **Read / write** — builds, `gradlew`, `adb install` allowed |
| `playstore/`, `tools/`, `resources/` | Store copy, catalogue tools, reference PDFs | Untouched unless asked |

Primary live preview entry: `prototypes/app-shell/` (studio chrome + `shell.html`).

Related previews / freezes:

- `prototypes/panchanga/` — **frozen** Panchanga (`docs/frozen-panchanga-shell.md`)
- `prototypes/app-shell/` `#view-user` — **frozen** User (`docs/frozen-user-profile.md`)
- Nitya tabs — **frozen** Sandhya / Japa (`docs/frozen-nitya-sandhya.md`, `docs/frozen-nitya-japa.md`) plus **ನಿತ್ಯ ಪಾರಾಯಣ** tab; **Puja** is a standalone rail destination (`docs/frozen-nitya-puja.md`)
- `prototypes/menu-pages/` — hub / child destination mockups (parity)

## Implementation policy

### Allowed

- Edit Android Kotlin, XML layouts/resources, Gradle as needed for mockup parity
- Build/install debug APKs; use `adb` for device validation
- Update freeze docs and mockup only when aligning contracts
- Portrait-first layouts; Carbon visual tokens (`carbon_*` resources)
- Content readers may use `sensorLandscape` (e.g. `ContentReaderActivity`) while the main shell stays portrait

### Still prohibited

- Publishing, signing, or Play Store release unless explicitly asked
- Material restyle that abandons Carbon visuals

## Portrait-first

MadhvaNidhi main shell is portrait on mockup and device (top header + content + bottom destination rail; left drawer for labeled nav). Do not force landscape-only studio presets or lock the launcher to landscape.

## Design system

UX principles and motion checklist: [`docs/carbon-android-ux.md`](docs/carbon-android-ux.md). Keep Carbon language; polish journeys in mockups before Android. Compose design-system pilot is deferred.

**Authority: IBM Carbon Design System only** — colours, spacing, type, hierarchy, and components.

- Use `carbon_*` tokens and `Widget.Carbon.*` / `TextAppearance.Carbon.*` styles only
- Material widgets are allowed **only** as the Android host under Carbon tokens (no Material3 style names in layouts)
- No custom components, Desk chrome, GhostOutlined, or inventing non-Carbon colours / radii / paddings
- Interactive brand maps to Carbon interactive slot (`#BA4E00`); corners stay `0` (productive)
- Mockups: `cds--*` / `--cds-*` only (moon glyph `border-radius: 50%` is the sole domain exception)

## Workflow

1. Prefer frozen mockup contracts as the UI spec
2. Implement in `app/`
3. `installDebug` and validate on connected portrait phone
4. Capture / fix regressions; keep freeze docs in sync

## Validation checklist

- [ ] Portrait shell (header + bottom rail); readers may rotate landscape
- [ ] Nitya tabbed Sandhya / Japa / Parayana match freeze docs; Puja is its own rail item
- [ ] User / Panchanga respect existing freeze contracts
- [ ] Carbon styling preserved; Kannada readable
- [ ] Debug build installs on device

## Project memory

Read [`docs/project-memory.md`](docs/project-memory.md) for the current commit context, navigation, frozen design decisions and preview workflow. Verify it against newer commits; older external preview ZIPs are superseded.
