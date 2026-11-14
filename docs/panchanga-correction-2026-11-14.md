# Panchanga pāda and Karana correction — 14 November 2026

The field formerly labelled Bhava remains **Nakshatra pāda**, as requested by the user. The label is corrected in both the main Panchanga view and date dialog.

## Corrections

- Local Karana used the wrong repeating index, an unreachable Kimstughna condition, and the non-Karana placeholder NAVAN on Amavasya. The new 60-half-tithi mapping has Kimstughna at slot 0, Bava through Vishti repeated over slots 1–56, then Shakuni, Chatushpada and Naga at 57–59. Each slot spans six degrees of lunar elongation.
- Official provider names are normalized: Garaja/Garija → Gara, Bhadra → Vishti, Naaga → Naga, plus common spelling variants. Unrecognized values remain unavailable rather than being guessed.
- Nakshatra quarter and rashi were derived from tropical longitude while the official displayed nakshatra was sidereal. Local lunar nakshatra, quarter, rashi and Yoga now use a shared approximate sidereal correction. The original truncated orbital model is retained.
- Julian-day conversion now uses the UTC instant, including the actual timezone offset and minutes/seconds. Previously a 06:00 local clock was treated as 06:00 UTC.
- Date parsing is strict, clears residual seconds/milliseconds and safely handles malformed input. This also resolves the previously recorded malformed-date unit failure.
- Provider cache keys are versioned v2 so old unnormalized cached values are not reused. Existing stored keys and user data are preserved.
- Provider refresh compares the full day model, including Karana, so changes to individual facts cannot be missed.

## Provenance and accuracy limits

The SRS daily endpoint provides nakshatra, Yoga and Karana, but its inspected daily output does not supply pāda. The app therefore identifies pāda as a **06:00 device-local estimate**, not an official value. The sidereal correction is a low-precision Lahiri-like mean correction, anchored at the published J2000 ayanamsha. It is not a full ephemeris or a selected matha's sunrise calculation. Pāda can differ near quarter transitions; no transition times are claimed.

A local quarter is withheld when the locally calculated nakshatra disagrees with the provider's nakshatra. The UI displays “ದೃಢಪಟ್ಟಿಲ್ಲ” in that case. Rashi is likewise locally derived; details identify the 06:00 sidereal basis. Official Karana is kept when available; local fallback values remain estimates.

Sources checked:

- SRS daily pages: https://srsmatha.org/panchanga.php?pdate=2026-11-14 (Bava), 2026-11-15 (Kaulava), 2026-11-16 (Garaja), 2026-11-17 (Bhadra), and 2026-04-28 (Bava). The corrected local 06:00 IST samples agree on Karana for these dates. This sample agreement is not a global precision claim.
- Sewell, Indian Chronography, calculation of Yogas and Karanas: https://en.wikisource.org/wiki/Page:Sewell_Indian_chronography.pdf/61
- VedicDateTime authors' methodology: https://cran.r-project.org/web/packages/VedicDateTime/vignettes/VedicDateTime.pdf (sidereal frame, sunrise/timezone basis, half-tithi definitions).
- Astrodienst's published Lahiri/ayanamsha documentation: https://www.astro.com/swisseph/swisseph.htm (J2000 anchor and coordinate convention). No Swiss Ephemeris code or data library is integrated.

## Verification

All **50 Android unit tests passed**, including nine new Karana/pāda regressions and the formerly failing malformed-date test. Coverage includes all 60 half-tithi slots, movable/fixed boundaries, all 108 nakshatra quarters, UTC-instant conversion, official source aliases, provider/local mismatch handling, Kannada fixed-Karana names and five dated SRS Karana samples.

Debug build/install passed. Galaxy M11 screen and hierarchy were inspected: the longer Nakshatra pāda label and estimated value fit within their row, source Karana renders in Kannada, and the corrected local rashi uses the sidereal frame. For 14 November 2026 the provider still reports Bava; it was not arbitrarily replaced. The displayed local pāda is 2 at the explicitly stated 06:00 basis.
