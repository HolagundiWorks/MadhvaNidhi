# Frozen Puja — updated 13 November 2026

Approved web-mockup contract for **ಪೂಜೆ** as a **standalone primary rail destination** (no longer a Nitya Karma tab). Layout still shares Sandhya chrome classes. Changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `#view-puja` (`data-frozen="nitya-puja"`) |
| Parity mirror | `prototypes/menu-pages/pages.html` → `[data-page="puja"]` |
| Shared chrome | Same as Sandhya: `.sandhya-panel.puja-panel` |
| Related | [frozen-nitya-sandhya.md](./frozen-nitya-sandhya.md) |

## Navigation

- Primary rail: Dashboard · Panchanga · Nitya Karma · **Puja** · Grantha · …
- Nitya Karma tabs are **ಸಂಧ್ಯಾವಂದನೆ · ಜಪ · ನಿತ್ಯ ಪಾರಾಯಣ**
- Legacy `nityakarma` + child `puja` routes redirect to `puja`

## Screen structure

Identical to Sandhya toolbar chrome, with **single continuous scroll** (no step prev/next):

1. **Toolbar**: rite `cds--select` (ನಿತ್ಯ · ವಿಷ್ಣು · ಗಣಪತಿ) left · Skip · Complete right
2. **Progress row**: rite title 30% + day progress 70% (0% until Complete → 100%; Skip resets to 0%)
3. **Scroll body** only — crumb + full rite text in one NestedScrollView / `.sandhya-scroll`

Crumb form: `ಪೂಜೆ · {rite}` (no step segment; no Nitya Karma prefix).

## Behaviour

- Full paddhati body for the selected rite scrolls as one document (sankalpa through samarpana in-flow)
- Rite change resets day-action state
- Skip / Complete same day-action semantics as Sandhya (persist day flags; pressed chrome); **no** prev/next step FABs
- Mock + Android share `madhvanidhi-practice-day` / `PracticeDayStore` keys for `puja_complete` / `puja_skipped`

## Android mapping notes

`PujaFragment` + `page_nitya_puja.xml`; rail id `puja` in `ShellHubFragment.RAIL_IDS` / `SettingsStore.PAGE_IDS` (pagesRev ≥ 16). Share Sandhya chrome with rite dropdown instead of kala FABs; bind full `MadhwaStore` / Sankalpa-injected body into `pujaBody` without step paging. Skip/Complete persist via `PracticeDayStore.setPujaDay` with `NityaDayChrome` pressed state.
