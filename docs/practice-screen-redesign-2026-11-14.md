# Practice screen redesign — 14 November 2026

Native Material 3 redesign for Sandhyavandana and Japa. Scripture/anga content continues to open in the landscape reader; the portrait screens handle selection and practice tracking.

Sandhyavandana: labelled morning/noon/evening selectors with 24dp icons; current section, practice status and progress grouped in a neutral card; prominent reading action; explicit Previous/Next section controls replace hidden tap/long-press progress-bar gestures; separate labelled Skip/Complete actions. Removed the binder's forced 32dp sizing.

Japa: full mantra name in its dropdown, paired reading actions, central daily count/target/status, a full-width 96dp +1 action and separate 64dp completion controls. Target adjustments are secondary, expandable options. Their labels explain that they change the target, not the recorded count. Existing counts, targets, completion semantics and persistence are preserved.

All principal actions have minimum 64dp heights, visible labels and explicit light/dark foregrounds. Scroll containers accommodate expanded options and larger system fonts. Selected session/day controls expose an accessibility state. Newly revealed tab children are invalidated after layout to avoid missing first-frame display lists observed on the test phone.

Verification: installDebug and assembleDebugAndroidTest passed offline; UI-test APK compiled but instrumentation suite not executed. Galaxy M11 screenshots inspected for both screens. UI hierarchy measurements confirmed six primary controls on each screen are at least 64dp and inside visible bounds; Japa +1 is at least 96dp. Target panel expanded and all four controls were present; no counts, targets or completion flags changed during checks. Japa inspected at 130% system font scale, then the original 0.9 setting restored. Full Android unit suite not rerun; previously recorded unrelated Panchanga malformed-input failure remains unresolved.

This pass updates the native practice screens; the dashboard web mock remains its existing read-only overview.
