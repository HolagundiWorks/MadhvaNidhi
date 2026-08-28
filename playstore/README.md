# MadhvaNidhi — Google Play launch

Package name: `in.hcworks.madhvanidhi`

Developer: Human Centric Works

Website: <https://hcworks.in>

Version: 1.0.0 (`versionCode` 1)

## Release build

The project targets Android 16 / API 36 and produces an Android App Bundle:

```powershell
.\gradlew.bat clean testReleaseUnitTest bundleRelease
```

The bundle is written to `app/build/outputs/bundle/release/app-release.aab`.
Configure a private upload key through local Gradle properties before uploading;
never commit the keystore or its passwords. Enable Play App Signing in Play
Console and retain encrypted backups of the upload key. The exact property names
are provided in `signing.example.properties`. Without all four values, the build
deliberately produces an unsigned release bundle suitable only for validation.

## Console checklist

- Confirm that the Play Console application uses exactly
  `in.hcworks.madhvanidhi`. A Play package name cannot be changed after the first
  artifact is uploaded.
- Complete developer identity and website verification for Human Centric Works.
- Upload the signed AAB to Internal testing first.
- Complete App access, Ads (`No ads`), Content rating, Target audience, Data
  safety, and the privacy-policy URL.
- Publish `privacy-policy.md` at a public HTTPS URL on `hcworks.in`.
- Upload `artwork/play-icon-512.png` and
  `artwork/feature-graphic-1024x500.png`; capture final phone/tablet screenshots
  from the signed release build. Do not use Matha emblems without written
  permission.
- Test both Veda choices, all Panchanga tradition labels, offline fallback,
  Kannada rendering, alarms, rotation, and process restoration.
- If the account is a personal developer account created after 13 November
  2023, run the required closed test before requesting production access.

## Source-boundary note

Sri Raghavendra Swamy Matha exposes official daily Panchanga values and is the
only live provider in version 1.0. Uttaradi Math and the Udupi Ashta Mathas are
selectable traditions, but the app clearly labels their displayed values as a
local calculation until each institution provides or approves a daily data feed.
This prevents MadhvaNidhi from presenting inferred values as an official Matha
publication.
