# MadhvaNidhi Carbon shell UI

**Canonical contract:** [frozen-panchanga-shell.md](frozen-panchanga-shell.md)
**User profile (frozen):** [frozen-user-profile.md](frozen-user-profile.md)
**Tokens:** [frozen-design-tokens.json](frozen-design-tokens.json)
**Carbon audit:** [carbon-mockup-audit.md](carbon-mockup-audit.md)  
**HTML preview:** [`prototypes/app-shell/`](../prototypes/app-shell/) · Panchanga: [`prototypes/panchanga/`](../prototypes/panchanga/)

## Phase

`MOCKUP_ONLY = false` — the approved full-shell Android port is authorized. Keep native implementation and frozen web contracts aligned. See root `AGENTS.md` and [project-memory.md](project-memory.md).

## Destinations

Primary rail: Dashboard, Panchanga, Nitya Karma, Puja, Grantha, Bhakti Sangeeta, Gruha Samskruti, User, Settings. Child features open from hub pages. **User** is frozen (`docs/frozen-user-profile.md`): view/edit profile + Fluid MultiSelect diksha/japa mantras + AES-GCM vault + passphrase `.mnbackup`; **Settings** holds appearance (theme, language, text size). Vault helper: `prototypes/user-vault.js`.

- **Dashboard** — peer Carbon tiles (selected date, Japa, Sandhya, Stotra, quick actions). Full-width content; rail icon is Carbon **Template**. No 30:70 split.
- **Single-tile pages** — if a destination has only one content tile/panel, that layer is removed and the content sits directly on the screen background (multi-tile pages keep cards).
- **Panchanga** — **frozen** 30:70 context / calendar only on this screen (see contract). Facts are a definition list; day tiles are borderless Carbon tiles with Yellow 30 ekadashi alert. Other destinations stay full-width.
- **Nitya Karma** — Carbon **Tabs** for children (ಸಂಧ್ಯಾವಂದನೆ · ಜಪ · ನಿತ್ಯ ಪಾರಾಯಣ) in shell and menu-pages; Japa uses `prototypes/japa-session.js` (count / mala / goal).
- **Puja** — standalone rail destination (`docs/frozen-nitya-puja.md`); Sandhya-parity step chrome with rite select.
- **Grantha** — Stotra, Sukta, Mantra, Namavali (Nitya Parayana lives under Nitya Karma).
- Reading, Aayadi, and section browse screens: mockups in `prototypes/menu-pages/`; Android mapping follows the approved freeze contracts.

## Preview

```powershell
cd prototypes
python -m http.server 8767 --bind 127.0.0.1
```

- Studio + shell: http://127.0.0.1:8767/app-shell/
- Panchanga studio: http://127.0.0.1:8767/panchanga/

Landscape screenshots (reference): `captures/carbon-app/`.

Tabbed destinations use the tab strip as the first row and hide the shell header and divider. The side navigation stays available. Gruha Samskruti tabs are Aayadi, Rangavalli and Recipes / Naivedya.
