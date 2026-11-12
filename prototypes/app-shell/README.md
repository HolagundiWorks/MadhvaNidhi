# Carbon shell HTML design studio

Open `index.html` to tune the MadhvaNidhi Carbon application shell before Android work. No build, dependencies, or internet connection are required. For a consistent browser origin and saved settings, serve this folder or the parent `prototypes` tree:

```powershell
# Local only
python -m http.server 8767 --bind 127.0.0.1

# Reachable on the LAN
python -m http.server 8767 --bind 0.0.0.0
```

Open http://127.0.0.1:8767/app-shell/ (or `http://<LAN-IP>:8767/app-shell/` from another device). `shell.html` opens the shell without the studio controls. Allow inbound TCP 8767 in Windows Firewall if needed.

## What this preview covers

- Global header (48px): logo, destination title, date, search, A−/A+, theme cycle
- Side navigation rail (56px, expands to 224px): Dashboard, Panchanga, Nitya Karma, Grantha, Bhakti Sangeeta, Gruha Samskruti, Settings
- Dashboard: Panchanga context pane, Japa, Sandhyavandana sessions, Stotra checklist, separate streak lines, weekly overview, quick actions
- Panchanga destination: embeds the frozen calendar from [`../panchanga/panchanga.html`](../panchanga/panchanga.html) with `?embed=1` so the calendar chrome stays out of the way while tile behavior is unchanged
- Grouped live features and empty states for later content (playlists, rangavalli, recipes)
- Four Carbon themes: Gray 100 (default), Gray 90, Gray 10, White
- Orange MadhvaNidhi accent override; yellow reserved for Ekadashi in the embedded calendar
- Kannada via the shared Noto Sans Kannada fonts from the Panchanga prototype

Practice counts and streaks are labeled **sample data**. PaperDesk leftovers (scheduler, todo, notes, projects, meetings, measure, Gita, tools) are not destinations.

Detailed HTML mockups for the **remaining rail destinations** (Nitya Karma, Grantha, Sangeeta, Samskruti, Settings and their children) live in [`../menu-pages/`](../menu-pages/).

## Design authority

Follows `MadhvaNidhi_Carbon_Technical_Specification.md` Phase 0–2 layout exploration. Carbon icons and theme tokens are Apache-2.0; see [`../panchanga/CARBON-ICONS-LICENSE.txt`](../panchanga/CARBON-ICONS-LICENSE.txt).

Android implementation starts only after this HTML shell is accepted.
