# Aayadi calculator: implementation plan

Source: "Āyādi Calculator — Developer Implementation Guide" (Downloads copy, read 20 November 2026).

## What
Bring the Aayadi screen in line with the guide, keeping the existing View/XML screen.

## Decisions (agreed with the user, 20 November 2026)
- **UI toolkit:** keep Android Views and XML. The guide's Compose request is not adopted.
- **Hasta:** exact rational arithmetic. Totals are integer inches; hasta = inches / 18 as a fraction (40 ft = 80/3). No rounding before a formula. Earlier whole-hasta rounding is withdrawn.
- **Scope:** the full screen now, with the Karnataka rule pack still DRAFT. The viable table therefore stays empty and a configuration error is shown until the pack is reviewed.

## Areas touched
- `domain/measure/`: `FeetInches`, `Rational` (BigInteger), `HastaStandard` (18 in, cited as the conventional standard).
- `domain/aayadi/`: `RulePack` model, `Verdict` (PASS, FAIL, UNDETERMINED), `ParameterResult`, `CandidateResult`, evaluator, pack validation.
- `domain/search/`: 25 x 25 inch-step candidates, inclusive bounds, duplicates excluded, sorting, largest-area option.
- `data/rules/`: Karnataka Shodashavarga pack (Kotlin definition; JSON loading deferred).
- `ui/AayadiFragment.kt`, `res/layout/page_aayadi.xml`: summary, results table, detail sheet, empty and configuration-error states, filter.
- `test/`: unit tests for measurement, search, verdicts, pack gating and sorting.

## Rules
- A pack with missing mandatory formulas, labels or fractional policy, or with `reviewStatus = DRAFT`, produces UNDETERMINED for every parameter. It never produces PASS or FAIL.
- Only PASS candidates enter the viable table. UNDETERMINED keeps its reason in diagnostics.
- Parameters absent from the pack show "Not applicable" and never a value.
- The vyaya < aaya rule is not in the cited book, so it is UNDETERMINED until a source is supplied.

## Acceptance (guide section 10)
1. 40′0″ = 480 in; 30′0″ = 360 in.
2. 480/18 = 80/3 hasta; 360/18 = 20 hasta.
3. 625 candidates when all dimensions are positive.
4. Bounds inclusive: L-12, L+12, B-12, B+12.
5. The input candidate appears exactly once.
6. Zero or negative candidates excluded.
7. No premature rounding.
8. Missing classification gives UNDETERMINED, never PASS.
9. Only PASS candidates in the viable table.
10. Golden fixtures match the cited source. Not possible until the pack is reviewed; reported as open.
11. No PASS candidates gives the empty state.
12. Results stable and independent of locale.
13. Whole-hasta formulas do not accept fractional hasta.
14. Feet/inches round-trip to the integer-inch candidate.

## Out of scope / open
- Compose UI (declined). JSON rule packs (deferred). Review of the Karnataka pack and golden fixtures (needs the source). Yoni, Nakshatra, Vara and Tithi rules (no source in the pack, so Not applicable). Web prototype parity. Kannada wording needs native review.

## Result (20 November 2026)
- Built: `domain/measure` (FeetInches, exact Rational, HastaStandard), `domain/aayadi` (RulePack, Verdict, Evaluator with per-parameter working), `domain/search` (625 inclusive candidates, two sort orders), `data/rules/KarnatakaShodashavargaPack` (DRAFT), and the View screen with summary, decision, sort switch, table, empty and configuration-error states, and detail dialog.
- Removed: `data/Aayadi.kt` (replaced by the domain layer).
- Tests: `AayadiTest`, 23 tests, all pass. They cover feet/inches, exact hasta, 625 candidates, inclusive bounds, duplicates, zero exclusion, round trip, sort order, DRAFT gating, Not applicable, fractional refusal, the book table's remainders, and PASS/FAIL logic on a sourced test pack.
- On the phone (SM-M115F): 30′0″ × 40′0″ shows 20 × 80/3 hasta, 625 evaluated, 0 viable, the DRAFT configuration error, and the empty table state. Aaya shows "—" for the entered size because its area is fractional.
- Not reached on the phone: a viable row and its detail dialog, because the pack is DRAFT.
- Follow-up (20 November 2026): the pack is now a JSON asset (`app/src/main/assets/rules/karnataka-shodashavarga.json`), read by `RulePackLoader`; a pack that cannot be read shows a configuration error. `AayadiTest` has 24 tests and reads the same asset. The web prototype (`prototypes/samskruti-tabs.js` and both shell HTML panels) now follows the same exact-hasta, DRAFT and 625-candidate rules; checked in Node with a fake DOM, not in a browser. The info text now describes hasta and DRAFT status.
- Still open (needs input from outside the code): review of the Karnataka pack against the printed source; golden fixtures; a decision on vyaya < aaya and the dhana/runa comparison; rules and sources for Yoni, Nakṣatra, Vāra and Tithi; native Kannada review of the new strings. Compose declined.

## Revision (20 November 2026, pasted specification)
- Pass rows use the application's documented standard: aaya 1,3,5,7; dhana > runa; vyaya < aaya; ayush > 60. The pack status is APPLICATION_STANDARD (not DRAFT). The source is not yet checked against the printed book, and the docs and info text say so.
- Fractional hasta: exact remainders (no whole-hasta refusal). A fractional aaya is not in the whole-number set 1,3,5,7, so it fails. Under a whole-hasta policy the pack refuses the candidate with a reason, which the tests cover.
- Cross-field criteria: dhana > runa and vyaya < aaya are read from the other parameter's remainder. Runa has no criterion of its own, so it is reported and not judged.
- Yoni, Nakṣatra, Vāra and Tithi are removed from the pack and the table, as the specification's table does not include them.
- Screen: Section A (original dimensions, each parameter Pass or Fail, overall) and Section B (recommended table: Length, Breadth, Aaya, Dhana, Runa / Vyaya, Aayushu, Result; closest first; original row in bold when it passes; empty message "No valid combinations found within ±1 foot.").
- Verified: 28 unit tests pass; on the phone, 30′0″ × 40′0″ gives 625 evaluated, 4 Pass, and the original Fails (aaya 8, dhana 20/3, vyaya 10). The web script gives the same counts and decision in Node, not in a browser.
- Hasta standard (fixed, pasted specification): 1 hasta = 18 inches = 24 angula. Now a constant (`domain/measure/Measure.kt`, object `Hasta`); no longer configurable and removed from the rule pack JSON. Unit tests (28) pass and the phone shows the same results.

## UI hierarchy (redesign, requested 20 November 2026)
Principles: one primary action per screen (the inputs), the verdict before the detail, progressive disclosure for working, status shown by text and shape as well as colour, touch targets of 48 dp.

Order, top to bottom:
1. Header: title and info.
2. Input card: Length and Breadth as two rows, each with feet and inches. Changes update the results as the user types.
3. Section A, "ನೀಡಿದ ಅಳತೆ": the entered size in feet and inches (headline), its hasta conversion (caption), one verdict chip with a sentence naming any failing parameters, the parameter list (value, remainder, Pass/Fail chip), and a collapsed "ಲೆಕ್ಕದ ವಿವರ" section for the exact working.
4. Section B, "ಶಿಫಾರಸು ಮಾಡಿದ ಅಳತೆ": a count line, a two-option sort control (closest, largest area), and one card per passing size (feet-and-inches headline, Pass chip, the four values in a 2 x 2 grid, "ನಿಮ್ಮ ಅಳತೆ" tag for the original). Tapping a card opens the derivation. Cards replace the wide table, which needed horizontal scrolling on a phone.
5. Empty state: "No valid combinations found within ±1 foot."

Out of scope for this change: the web prototype (it stays Carbon, per the project rules).
