# Frozen User profile — finalized 12 November 2026

This is the **approved web-mockup contract** for the User profile destination (`MOCKUP_ONLY`). Layout and field set changes require a new design iteration.

| Artifact | Path |
| --- | --- |
| Canonical UI | `prototypes/app-shell/shell.html` → `#view-user` (`data-frozen="user-profile"`) |
| Parity mirror | `prototypes/menu-pages/pages.html` → `[data-page="user"]` |
| Vault / crypto | `prototypes/user-vault.js` |
| Shared Carbon chrome | `prototypes/carbon-app.css` |
| Rail icon | Carbon **UserSettings** |
| Related freeze | [frozen-panchanga-shell.md](./frozen-panchanga-shell.md) |
| Carbon audit | [carbon-mockup-audit.md](./carbon-mockup-audit.md) |

Android under `app/` stays **read-only** until explicitly authorized to implement this mockup.

## Screen behaviour

| Mode | UI |
| --- | --- |
| **View (default)** | Plain text: Carbon `cds--label` + body value (no text inputs, selects, or fluid list-box) |
| **Edit** | Carbon Form / Select / Text input / **Fluid MultiSelect**; actions **Save** · **Cancel** |
| **Backup** | Carbon **Accordion** (collapsed by default): passphrase · Create backup · Restore |

Full-width content (no 30:70 split). Landscape masonry: auto-fit columns; **rashi∥nakshatra** and **veda∥matha** share rows.

## Fields (encrypted vault)

Stored AES-GCM on device (`madhvanidhi-user-vault` + device key). Portable `.mnbackup` is PBKDF2 + AES-GCM with user passphrase.

| Field | Control (edit) | Notes |
| --- | --- | --- |
| Name · ಹೆಸರು | `cds--text-input` | |
| Gotra · ಗೋತ್ರ | `cds--select-input` | Illustrative saptarishi list |
| Marital · ವೈವಾಹಿಕ | `cds--select-input` | `married` / `unmarried` |
| Rashi · ರಾಶಿ | `cds--select-input` | 12 rashi |
| Nakshatra · ನಕ್ಷತ್ರ | `cds--select-input` | 27 nakshatra |
| Veda · ವೇದ | `cds--select-input` | Same row as matha |
| Matha · ಮಠ | `cds--select-input` | Same row as veda |
| Japa / diksha mantras | Carbon **Fluid MultiSelect** | Unlocks japa mantra choices |
| Japa targets | Carbon **Data table** + inline **Select** | One row per selected japa · Daily (11 · 101 · 1,001 · 5,001) · Major ascending (5,001 · 10,001 · 50,001 · 1,00,001 · 10,00,001) · Indian comma grouping · vault `japaTargets` |

**Not on this screen:** Language (Settings), theme, text size.

### Design pass · 12 November 2026 (japa targets)

Explicit UI request: after diksha selection, generate a targets table (name · daily · major). Stored in the same encrypted vault as `japaTargets: { [mantraId]: { daily, major } }`. View mode shows a read-only table.

## Carbon-only compliance (freeze audit)

| Check | Result |
| --- | --- |
| Tokens / themes | `--cds-*` only for colour, type, spacing, border, focus |
| Controls | Single outer `cds--form`; `cds--label`; `cds--text-input`; `cds--select-input`; `cds--btn`; Fluid MultiSelect; Accordion |
| Type | View values use `cds--body-compact-01`; intro / vault copy use `cds--form__helper-text` |
| Icons | Carbon paths only (accordion chevron, rail UserSettings) |
| No nested form chrome | Inner grids are layout only (`form-masonry` / `user-profile-grid`) — not nested `cds--form` |
| No custom widget chrome | View mode = plain text (not disabled inputs) |
| No Material / third-party UI kits | Pass |
| Square corners | `--cds-border-radius: 0` |
| Focus | 2px `$focus` outline |

**Intentional app wrappers** (layout only, Carbon tokens underneath): `#user-form.form-masonry`, `.cds--form-row`, `.user-profile-view` / `.user-profile-fields` visibility toggle, `.user-vault-status` live region. Do not replace with non-Carbon component libraries.

## Interaction contract

1. Load decrypts vault → populate form (hidden) + **view** text.
2. **Edit** reveals fields; **Save** encrypts; **Cancel** restores snapshot.
3. Backup / restore blocked while editing; restore reapplies profile and stays in view mode.
4. Legacy backups that still contain `lang` apply language into Settings storage on restore.

## Freeze rule

Do **not** change User profile field set, view/edit behaviour, vault format (`madhvanidhi-user-backup` v1), or Carbon patterns without an explicit new design pass. Shell `#view-user` is canonical; keep `menu-pages` in parity when authorized to touch this screen.
