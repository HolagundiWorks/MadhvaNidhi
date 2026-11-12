# Remaining menu pages — HTML mockups

Open `index.html` (studio) or `pages.html` (full preview) to review every rail destination **except** Dashboard and Panchanga, which already live under `../app-shell/` and `../panchanga/`.

```powershell
# Local only
python -m http.server 8767 --bind 127.0.0.1

# Reachable on the LAN (phones / other PCs)
python -m http.server 8767 --bind 0.0.0.0
```

Then open:
- Local: http://127.0.0.1:8767/menu-pages/
- Remote (same Wi‑Fi): http://&lt;your-LAN-IP&gt;:8767/menu-pages/

Allow inbound TCP 8767 in Windows Firewall if other devices cannot connect.

## Destinations covered

| Rail hub | Children |
|---|---|
| Nitya Karma | Sandhyavandana, Japa, Pooja |
| Grantha | Nitya Parayana, Stotras, Suktas, Mantras, Namavali |
| Bhakti Sangeeta | Hadugalu, Playlists (empty) |
| Gruha Samskruti | Aayadi, Rangavalli (empty), Recipes (empty) |
| Settings | Appearance, text size, Veda, Matha |

Layouts follow Carbon tokens (`--cds-*` only; brand orange is mapped onto `--cds-button-primary` / `--cds-interactive`). Components use Carbon BEM patterns: `cds--btn`, `cds--tile`, `cds--tile--clickable`, `cds--tag`, `cds--form-item`, `cds--content-switcher`, `cds--empty-state`. Icons are Carbon 32px SVGs only (Apache-2.0; see `../panchanga/CARBON-ICONS-LICENSE.txt`).

Sample copy is illustrative — Android keeps real catalogues and stores.
