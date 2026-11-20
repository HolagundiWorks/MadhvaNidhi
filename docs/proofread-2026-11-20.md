# Text proofread — 20 November 2026

## Scope and method

- Scanned all 679 `.txt` files under `app/src/main/assets` (stotras, songs, mantras, namavali, puja, festivals, Harikathamrutasara, catalogue text).
- Automated checks only: unknown and private-use characters, Kannada and Devanagari sign and virama sequences, mixed-script words, template markers, and LaTeX leftovers.
- **Not done:** word-by-word checking of Sanskrit and Kannada against printed sources. Spelling and meaning still need a human reader.

## Fixed

| Fix | Count | Files |
|---|---|---|
| Private-use glyphs replaced with standard Devanagari marks: U+E003 → visarga (ः); U+E004, U+E007 → ꣳ (U+A8F3); U+F202, U+F175, U+F176 → svarita/anudatta (U+0952); U+F159, U+F1A2 → svarita (U+0951) | 69 | 12 mantra and puja files |
| Doubled vowel signs and viramas collapsed (for example "ೆೆ" → "ೆ", "ुु" → "ु") | 1,755 | 49 files |
| Consonant, space, virama broken apart (for example "ದ ್ರ" → "ದ್ರ") | 3 | 2 files |
| Misspelling "ಗರुಡಾ" → "ಗರುಡಾ" (Garuda) | 1 | festival_naga_panchami.txt |

The placeholder `{{SANKALPA_PURPOSE:...}}` in festival files was left untouched. `Sankalpa.kt` depends on it.

**The private-use mapping is inferred from context, not from a source. Check it against a printed Vedic text.**

## Not fixed: damaged source text

These files still contain damage that cannot be repaired without a clean source. Some words lost their consonant, so vowel signs now start a word.

| File | Damaged sequences (approx.) |
|---|---|
| `madhwa/texts/hks.txt` | 1,706 dangling vowel signs, 440 virama-before-sign, 2,067 doubled signs |
| `hks_02` to `hks_33` (Harikathamrutasara sections) | 30 to 180 each; see the audit for each file |
| `madhwa/texts/puja_mahalakshmi_padagalu.txt` | 2 lines (line 119, line 355) |
| `madhwa/texts/rig_yajur_vaishvadeva.txt` | 7 lines (lines 71, 156, 192, 240, 270) |
| `library/puja/ekadashi-mahatmyam-vrata-raja.txt` (Devanagari) | 1 line, "िख्याता शुचौ ोकादशी" |

**The Harikathamrutasara texts are the most damaged. Users will see broken words in them. Recommended fix: replace them with a clean digital text, then check again.**

## Items not yet added

Ten items from the Adoni Ashtottara parayana scan were not added, because the scan is too garbled to transcribe reliably. They can be added once clean text is available.
