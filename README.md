# MadhvaNidhi

MadhvaNidhi is a priest-friendly landscape Android app for Madhva-sampradaya daily practice. It brings Panchanga, Veda-specific Sandhyavandana, mantras, stotras, suktas, pooja procedures, songs, japa and Aayadi into a Carbon-inspired shell with a daily Dashboard.

Developed by **Human Centric Works** — [hcworks.in](https://hcworks.in). Made with love in Hospet.

## Application

- Android application ID: `in.hcworks.madhvanidhi`
- Version: `1.0.0` (`versionCode 1`)
- Minimum Android: 7.1 / API 25
- Target Android API: 36
- Layout: landscape Carbon shell (header + side rail), system font scaling and 100–200% app text-size controls
- Design: Material components with Carbon spacing/type roles, Gray 100 layers, Orange 40 accents and 4dp corners
- Home: Dashboard with Panchanga context and daily practice panels; full Panchanga remains the calendar destination
- Languages/content: Kannada-first devotional library with supported Sanskrit source transliteration
- HTML shell preview: `prototypes/app-shell/`

MadhvaNidhi is a normal launcher application, not a replacement Android ROM or HOME launcher. It does not disable system navigation, radios, accounts or device security.

## Settings

The in-app Settings screen selects:

- Rigveda — Ashvalayana Sandhyavandana
- Krishna Yajurveda — Taittiriya Sandhyavandana
- Sri Raghavendra Swamy Matha (Mantralaya)
- Sri Uttaradi Matha
- all eight Udupi Sri Krishna Mathas: Palimaru, Adamaru, Krishnapura, Puttige, Shirur, Sodhe, Kaniyooru and Pejavara

The selected Veda controls Sandhyavandana content. The selected Matha labels the Panchanga tradition throughout the app. Sri Raghavendra Swamy Matha uses its official daily page when reachable; other choices currently use the bundled local Chandramana calculation and are explicitly labelled as local calculation until an official machine-readable feed is available.

## Catalogue quality

The bundled catalogue is checked for missing bodies, duplicate titles/content, unsupported scripts and extraction noise:

```powershell
node tools\audit-catalogue.mjs
```

See [`docs/CATALOGUE_AUDIT.md`](docs/CATALOGUE_AUDIT.md) for the audit, sources and maintenance commands.

## Build

Requires JDK 17 and Android SDK. The local SDK path belongs in `local.properties` and is not committed.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug bundleRelease
```

Install a development build:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -S -n in.hcworks.madhvanidhi/com.paperdesk.launcher.ui.LauncherActivity
```

## Play Store preparation

The `playstore/` directory contains the store listing, release notes, privacy policy, Data safety answers and launch checklist. A private upload key and final Play Console declarations/assets are intentionally not stored in this repository.

## Repository layout

| Path | Role |
| --- | --- |
| `app/` | Android application |
| `app/src/main/assets/` | bundled devotional catalogue |
| `docs/` | catalogue and interface documentation |
| `playstore/` | Play Store launch copy and checklist |
| `tools/` | catalogue audit and import utilities |
| `resources/reference/` | development reference documents; not packaged automatically |

Pooja and Sandhyavandana generate Sankalpa details from the current Panchanga. No personal profile is required and no user data is sent to Human Centric Works.
