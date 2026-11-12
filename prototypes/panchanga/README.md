# Panchanga mockup (frozen)

Approved interactive HTML/CSS/JS preview of the MadhvaNidhi Panchanga screen.  
Contract: [`docs/frozen-panchanga-shell.md`](../../docs/frozen-panchanga-shell.md) · tokens: [`docs/frozen-design-tokens.json`](../../docs/frozen-design-tokens.json).

## Layout (final)

- **30:70** context / calendar, landscape only, IBM Carbon (square corners, shared `../carbon-app.css`)
- Reference polish **960×540**; density scales chrome/tiles **down** to 640×360 and **up** to 1560×720 (same proportions; tiles capped at 110px×density)
- Edge-to-edge: no device border; status/cutout areas merge with theme `$background` (notch / teardrop safe)
- Context: three-line era (masa | Sunny) → ರಾಶಿ/ನಕ್ಷತ್ರ/ಭಾವ/ಕರಣ **list** → ದಿನದ ವಿಶೇಷ
- Calendar: moon cycle | choose-date · paksha switcher · 5-column day tiles
- Ekadashi: Yellow 30 top strip + `WatsonHealthStatusPartialFail`
- Selected day: `$layer-selected` fill only (no outline)

## Preview

```powershell
cd prototypes
python -m http.server 8767 --bind 127.0.0.1
```

http://127.0.0.1:8767/panchanga/

Or open **Panchanga** inside the app shell (`../app-shell/shell.html`, embed `?embed=1`). Landscape presets: see [`../../AGENTS.md`](../../AGENTS.md).

## Files

| File | Role |
| --- | --- |
| `panchanga.html` | Screen markup + screen CSS |
| `panchanga.js` | Mock calendar / picker / selection |
| `carbon-theme.css` | Theme tokens (keep synced with app-shell / menu-pages) |
| `theme.js` | Theme persistence |
| `CARBON-ICONS-LICENSE.txt` | Apache-2.0 Carbon icon attribution |
