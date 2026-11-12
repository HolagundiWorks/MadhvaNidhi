# MadhvaNidhi app-shell mockup

Primary interactive UI lab for MadhvaNidhi. Landscape only. Carbon design authority.

## Open

```powershell
cd prototypes
python -m http.server 8767 --bind 127.0.0.1
```

Then open http://127.0.0.1:8767/app-shell/

Studio chrome: **screen size** dropdown + **live preview** of `shell.html`. No design sidebars.

## Files

| File | Role |
| --- | --- |
| `index.html` | Minimal studio (viewport + iframe) |
| `shell.html` / `shell.css` / `shell.js` | Application shell UI |
| `carbon-theme.css` / `theme.js` | Carbon tokens |
| `../studio.css` / `../studio.js` | Shared landscape studio chrome |

Related: [`../panchanga/`](../panchanga/), [`../menu-pages/`](../menu-pages/). Policy: [`../../AGENTS.md`](../../AGENTS.md).

Android stays untouched until explicitly authorized.
