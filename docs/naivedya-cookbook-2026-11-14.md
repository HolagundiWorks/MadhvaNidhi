# Naivedya cookbook — 14 November 2026

Gruha Samskruti → Pāka / Naivedya now contains five offline Kannada recipe cards: Hayagreeva maddi, avalakki panchakajjaya, hesaru-bele kosambari, bellada panaka and hesaru-bele payasa. The full-width Material selector changes the recipe; a vertically scrollable card contains ingredient/quantity rows, timing, batch information, numbered method, cooking notes and an attributed source action. No Puja procedure was duplicated into Recipes.

## Sources

- Sumana / Udupi Recipes — Hayagreeva: https://udupi-recipes.com/2012/10/hayagreeva-hayagriva-recipe.html
- Smitha Kalluraya / Cook with Smile — Avalakki panchakajjaya: https://smithakalluraya.com/avalakki-panchakajjaya-recipe-sihi-avalakki-sweet-poha-recipe/
- Sailu’s Food — Kosambari: https://www.sailusfood.com/kosambari-recipe-moong-dal-salad
- SwayamPaaka — Bellada panaka: https://swayampaaka.com/juice/bellada-panaka-jaggery-panakam/
- Sumana / Udupi Recipes — Hesaru-bele payasa: https://udupi-recipes.com/2013/04/moong-dal-payasamhesaru-bele-payasa-recipe.html

Ingredient facts were checked against four structured Recipe cards and the manually reviewed panaka page. The app methods are concise original Kannada summaries. Blog photographs, narrative prose, source health claims and ritual legends are not bundled. No open-source licence is asserted for the original blog material. Page title, author/site, URL, checked date and fetched response hash are retained in assets/naivedya/sources.json.

## Editorial choices

- Hayagreeva and payasa sources do not give a serving count; the app states the dry-dal batch size instead of inventing portions.
- Hayagreeva keeps the source ingredient-list ghee total (¼ cup); the summary distributes it between roasting and mixing rather than repeating the source’s inconsistent spoon totals.
- Panchakajjaya’s missing ghee numeral in the structured ingredient list is resolved using the source method’s explicit one teaspoon.
- Kosambari includes salt from the source method and shows the 1–2 hour soak separately from active preparation time. Ginger from the ingredient list is included in the mixing step.
- Panaka uses the source’s one-cup dry-ginger version. Its jaggery base is half the source’s two-cup base: two tablespoons jaggery in one cup water, with ¼ teaspoon dry ginger and ½ teaspoon lemon juice. Its 5–10 minute time is explicitly an editorial estimate.
- Payasa lists the two cups of dal-cooking water found in the source method, plus water for extracting coconut milk. Thick coconut milk is warmed gently.
- These are culinary variants. The app asks users to follow their household/matha ingredient practice; no universal ritual or fasting suitability is claimed.

## Files and checks

Canonical recipe data: app/src/main/assets/naivedya/index.json. Native UI: NaivedyaFragment and page_naivedya.xml. `python tools/fetch-naivedya-sources.py` refreshes bounded source metadata and writes structured recipe review snapshots only to the system temporary directory. `python tools/audit-naivedya.py` verifies unique entries, quantities, steps and reviewed source links.

The catalogue audit passed for all five recipes. Debug build/install passed, and the initial Hayagreeva recipe was visually inspected on Galaxy M11. All five dropdown choices were inspected. Selecting Panaka replaced the recipe and showed its checked quantities; scrolling reached the final preparation step, source author and source action. Dark layout was visually inspected; the Light page was captured and checked, then the original Dark setting restored. Full Android unit suite not run for this content/UI pass; the previously recorded Panchanga malformed-input failure remains unrelated.

Light-mode verification used visual inspection of the captured Hayagreeva page. A strict UI-hierarchy assertion for its English subtitle did not match the accessibility dump, although the subtitle and recipe were visible in the screenshot. Dark mode was restored afterward.
