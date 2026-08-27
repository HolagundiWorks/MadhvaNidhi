# Acharya OS

Landscape HOME launcher for Acharya OS. Kannada UI. Pages: PANCHANGA → SANDHYAVANDANA → MANTRAS → POOJA → HADUGALU → JAPA → PROFILE → AAYADI.

**Status:** active development flavor in this workspace.

Package: `com.paperdesk.launcher`

## Device

- Samsung SM-J320M (Galaxy J2), ViperOS
- Android 7.1.2, API 25 (`minSdk` 25)
- Landscape 1280×720, density 2.0
- Set this app as Home (always)

AcharyaOS is separate from the Samsung A10s VishwakarmaOS flavor. Do not apply
the A10s lockdown or overlay scripts to this device.

Do not scrape third-party lyrics. Only texts that already ship in `app/src/main/assets` (or that you add yourself) belong in the library.

## Catalogue quality

The runtime catalogue is checked for missing bodies, duplicate titles/content,
unsupported scripts, and extraction noise:

```powershell
node tools\audit-catalogue.mjs
```

See [`docs/CATALOGUE_AUDIT.md`](docs/CATALOGUE_AUDIT.md) for the latest audit,
verified sources, and maintenance commands.

## Build and install

Requires JDK 17 and Android SDK. SDK path stays in `local.properties` (not committed).

```powershell
cd acharya-os
.\gradlew.bat :app:installDebug
adb shell am start -S -n com.paperdesk.launcher/.ui.LauncherActivity
```

USB debugging must be on. After install, choose **Acharya OS** as the Home app.

## Layout

| Path | Role |
| --- | --- |
| `app/` | Android application |
| `tools/` | Optional scripts to rebuild mantra / pooja catalogues into assets |
| `gradle/` | Gradle wrapper |
| `resources/reference/` | Development source documents and catalogues; not packaged automatically |

Pager next/prev wraps from the final page back to Panchanga. Footer shows `|| ಹರಿ ಸರ್ವೋತ್ತಮ ವಾಯು ಜೀವೋತ್ತಮ ||`.

Pooja and Sandhyavandana render a live Sankalpa from the current Panchanga. The active profile supplies name, Gotra, Pravara, Veda Shakha and Nakshatra. Japa targets are optional and profile-specific; no lifetime total is maintained.

Rigveda Sandhyavandana follows the complete Ashvalayana trikala source. Morning,
midday, and evening are rendered as ordered ritual sections with only the
mantrachamana, arghya, and tarpana passages for the selected time.

Profile names accept offline English phonetic typing and are stored in Kannada. The packaged `ದೇವಪೂಜಾ ವಿಧಾನ` contains the complete user-supplied procedure from Nirmalya Visarjana through Samapana.

Panchanga prefers SRS Mantralaya when online, then USB files, then local Chandramana calculation. USB files come from Vishwakarma OS.

## Related

Phone data root: `/sdcard/Vishwakarma` (retained as a compatibility protocol
name). The Windows companion currently lives beside the VishwakarmaOS device
layer; see [`../vishwakarma-os/README.md`](../vishwakarma-os/README.md).
