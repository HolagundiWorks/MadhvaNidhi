# Frozen Nitya · Sandhyavandana — updated 14 November 2026

Approved web-mockup contract for the **ಸಂಧ್ಯಾವಂದನೆ** tab inside Nitya Karma. Layout changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `#view-nityakarma` → `[data-nitya-panel="sandhya"]` (`data-frozen="nitya-sandhya"`) |
| Parity mirror | `prototypes/menu-pages/pages.html` → same panel |
| Shared chrome | `prototypes/carbon-app.css` (`.sandhya-panel`, `.sandhya-chrome`, `.sandhya-scroll`) |
| Related | [frozen-nitya-japa.md](./frozen-nitya-japa.md); Nitya Parayana is a sibling tab; Puja is a separate rail destination — [frozen-nitya-puja.md](./frozen-nitya-puja.md) |

## Current native screen structure (user-approved 14 November)

1. Contained Nitya tabs: Sandhyavandana / Japa / Nitya Parayana.
2. Portrait entry: Morning / Noon / Evening time selectors, followed immediately by the large Open Sandhyavandana action and a landscape-reader hint.
3. Selected session status, followed by Skipped today / Completed today actions. No current-section card, progress interaction or Previous/Next Section buttons on this screen.
4. The selected time opens ContentReaderActivity in sensorLandscape with the full session initially visible.
5. Inside the reader, Sections opens a Material single-choice dialog containing Full session followed by the numbered sections. Previous/next buttons are hidden for Sandhyavandana. The section label, progress and body update after selection.

## Day-status behaviour

- Both portrait and reader Skip / Complete actions require a Material confirmation dialog naming the selected time. Undoing an existing mark also requires confirmation.
- Cancel, Back and outside dismissal do not change status.
- On confirmation, the captured session and date are updated through PracticeDayStore.setSandhyaDay; complete and skipped remain mutually exclusive and dashboard reads the same records.
- Selecting sections, opening the reader and reaching the final section never changes completion or skip flags. Reading position is separate from explicit completion; uncompleted individual-section progress stays below 100%.
- The legacy web panel below is not the current native layout authority; the former full-height portrait reading/progress navigation contract is superseded by this approved entry/reader split.

## Android mapping and verification

- Entry: page_nitya_sandhya.xml / NityaKarmaFragment.bindSandhya.
- Reader: ContentReaderActivity.bindSandhyaStepBar / paintSandhya.
- Shared dialog: SandhyaDayConfirmation; mutations occur only in the positive-action callback.
- Debug build installed on connected Galaxy M11; offline unit suite 52 tests passed.
- On-device SandhyaFlowInstrumentation passed: entry loaded, removed entry controls absent, cancel/dismiss inert for skip/complete and undo, confirm callbacks execute once. Test uses callbacks without writing practice flags.
- Actual entry, confirmation dialog and landscape section chooser checked on the phone. Full cross-screen smoke suite was not rerun.
