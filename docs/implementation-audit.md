# Implementation audit — 14 November 2026

This records the earlier comparison. Subsequent changes are summarized below and in project-memory.md.


Compared working-tree Android source with the current web mockups at commit 52a16c6.

| Area | Android source | Mockup |
| --- | --- | --- |
| Primary navigation | Eight rail destinations | Eight destinations, all opened in browser |
| Nitya | Sandhya, Japa, Parayana | Three tabs |
| Grantha | Stotra, Suktas, Mantras, Songs, Namavali | Five tabs |
| Sangeeta | Playlists tab | Present |
| Gruha Samskruti | Aayadi, Rangavalli, Recipes | Three tabs |
| Settings | UI, Users, Backup, Legal | Four tabs |
| Header | Hidden on tabbed hubs | Hidden on hubs and approved dashboard |
| Japa | Per-mantra daily/cumulative records and Sankalpa | Per-mantra daily/cumulative records and Sankalpa |
| Dashboard Japa row | Percentage and next daily target | Approved numeric daily, Sankalpa and cumulative design with four selectable slots |
| Backup | Native profile backup | Web vault backup with a separate format |
| Catalogue and Panchanga | Native catalogue/calculation integrations | Illustrative reader content and calendar fixtures |

## Changes from this audit

- Japa tiles open the selected mantra in the Nitya Japa counter.
- Automatic Sankalpa matches JapaGoals: 5,001 → 10,001 → 50,001 → 1,00,001 → 10,00,001. At or above the final rung, it stays at 10,00,001. A subsequent rung needs a product decision.
- Tightened compact Japa select height.
- Retained the approved mockup-first dashboard design.

## Verification

Browser checks opened all eight routes with no captured runtime exceptions, verified tab counts, selected-mantra routing and target boundaries. Dashboard grid fits 640×360, 960×540 and 1280×720. Visually inspected 960×540. JavaScript syntax checks and Git whitespace checks passed.

Android source was reviewed. Android builds, device behavior, backup round trips and live catalogue/calculation parity were not tested in this audit.

## Measured Galaxy M11 preview

Connected device reports 1560×720 physical landscape pixels, 280 dpi (1.75× density), and 0.9 system font scale. Window bounds indicate 1495×678 usable physical pixels after the 65px horizontal and 42px top insets. Studio now defaults to 854.29×387.43 CSS pixels with 90% system text scaling. Physical-resolution presets are retained for comparison. Browser and native font metrics can still differ.


## Current implementation update

Android now has a full-width Settings Dashboard tab for Japa records, tile selection and shortcuts. Dashboard tiles show daily, Sankalpa and cumulative values. Songs resides under Bhakti Sangeeta. Duplicate Puja procedures were removed from Gruha Paddhati. Ekadashi uses Carbon Transparency_01.

Android Puja now has a selector, a 40:60 breadcrumb/progress row and a full-width section reader. Bottom navigation buttons are removed. Tapping the breadcrumb opens a section picker. Stacked A+/A− controls adjust reading text. Bodies load on selection and section positions persist locally.

Songs collection browsing and Favorites, and shared floating text controls across the other readers, are mockup iterations awaiting Android implementation. Native Settings still contains its existing app-wide text-size control.

Latest Android debug unit tests and install passed. Device checks verified the Puja section picker opens and the footer controls are absent.
