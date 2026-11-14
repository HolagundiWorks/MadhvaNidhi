# MadhvaNidhi — Google Play launch

Package name: `in.hcworks.madhvanidhi`

Account type: Personal / individual

Developer: VISWABHIRAM D HOLAGUNDI

Location: Hosapete, Karnataka, India

Support email: vishwabhiram@gmail.com

Contact phone: +91 9739822277

Website: not provided (optional for this listing)

Public privacy-policy URL: required, not yet provided

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
- Complete individual developer identity verification using the exact legal name and full postal address from the linked Google payments profile. Verify the required contact details.
- Upload the signed AAB to Internal testing first.
- Complete App access, Ads (`No ads`), Content rating, Target audience, Data
  safety, and the privacy-policy URL.
- Host `privacy-policy.md` as a public, non-editable HTTPS webpage with no login or geographical restrictions. Add its real URL to Play Console and the in-app privacy section; no hosting URL has been configured yet.
- Upload `artwork/play-icon-512.png` and
  `artwork/feature-graphic-1024x500.png`; capture final phone/tablet screenshots
  from the signed release build. Do not use Matha emblems without written
  permission.
- Test both Veda choices, all Panchanga tradition labels, offline fallback,
  Kannada rendering, alarms, rotation, and process restoration.
- If the account is a personal developer account created after 13 November
  2023, run a closed test with at least 12 testers opted in continuously for 14 days before applying for production access.

## Source-boundary note

Sri Raghavendra Swamy Matha exposes official daily Panchanga values and is the
only live provider in version 1.0. Uttaradi Math and the Udupi Ashta Mathas are
selectable traditions, but the app clearly labels their displayed values as a
local calculation until each institution provides or approves a daily data feed.
This prevents MadhvaNidhi from presenting inferred values as an official Matha
publication.

## Individual publisher details still needed

- Full legal postal address, including PIN, entered directly into the Google
  payments profile/Play Console. Do not substitute the city-only app contact line.
- A public privacy-policy URL. The local Markdown policy is a draft, not a hosted page.
- Trader status and distribution countries, as requested by Play Console.
- Confirm monetisation (free/paid/in-app purchases) and target audience before
  completing the corresponding Console declarations.

Google generally displays an individual developer’s legal name, country and
public developer email. Full address/phone display can depend on monetisation,
trader status and distribution region. The phone/email supplied for the in-app
Developer section are visible to app users.

Current official references (checked 14 November 2026):
- [Personal-account identity/contact requirements](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en)
- [App-content and privacy-policy requirements](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en)
- [Testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)

Identity metadata does not certify release readiness. Review the Data safety
answers against the exact signed artifact and resolve the outstanding app audit
and test findings before submission.
