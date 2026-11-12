# Frozen Nitya · Puja — finalized 12 November 2026

Approved web-mockup contract for the **ಪೂಜೆ** tab. **Layout parity with Sandhya** (shared chrome classes). Changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `[data-nitya-panel="puja"]` (`data-frozen="nitya-puja"`) |
| Parity mirror | `prototypes/menu-pages/pages.html` |
| Shared chrome | Same as Sandhya: `.sandhya-panel.puja-panel` |
| Related | [frozen-nitya-sandhya.md](./frozen-nitya-sandhya.md) |

## Screen structure

Identical to Sandhya except left toolbar control:

1. **Toolbar**: rite `cds--select` (ನಿತ್ಯ · ವಿಷ್ಣು · ಗಣಪತಿ) left · Skip · Complete right
2. **Progress row**: step title 30% + progress 70%
3. **Scroll body** + crumb · pinned bottom prev/next

## Behaviour

- Sample steps: ಸಂಕಲ್ಪ · ಆವಾಹನ · ಉಪಚಾರ · ಸಮರ್ಪಣ
- Rite change resets step index and day-action state
- Skip / Complete same semantics as Sandhya

## Android mapping notes

Share Sandhya chrome layout with rite dropdown instead of kala FABs; bind to existing puja catalogue / `ReaderFragment.PUJA` data where available.
