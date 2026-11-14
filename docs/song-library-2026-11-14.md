# Content lists and song collections — 14 November 2026

## Native implementation

Portrait library screens show searchable, full-width Material 3 title lists. Selecting a title opens the existing sensor-landscape ContentReaderActivity directly. No content-selection dropdown or intermediate Open Content button is used for Stotra, Suktas, Mantras, Namavali or Songs. Puja keeps its separate guided procedure selector; this change does not alter practice flows.

Bhakti Sangeeta has contained tabs: ದಾಸರ ಪದಗಳು, ಸುಳಾದಿಗಳು, ಉಗಾಭೋಗಗಳು, ಮೆಚ್ಚಿನವು. Playlists is removed from displayed navigation; existing playlist links redirect to Favorites. Songs remain outside Grantha. An ಇತರೆ ಹಾಡುಗಳು tab appears when an additional catalogue group is marked collection=songs or contains explicitly typed song records. Mere title keywords do not classify scripture as songs.

Only catalogue entries with local readable files are listed. Current bundled native Songs lists contain 4 Dasara, 10 Suladhi and 2 Ugabhoga entries. Title-only catalogue references are not presented as readable songs, and no source texts were imported by this change.

Song stars toggle Favorites from either the list or the landscape reader. An outlined star means not saved; a filled orange star means saved. Favorites use stable catalogue IDs, are scoped to the active profile and are included in the existing encrypted profile backup envelope. Older backup files without songFavorites remain supported. List and reader actions have accessibility labels and favorite-state descriptions. Search supports Kannada, Sanskrit, English and author fields already present in the catalogue.

Namavali classification is evaluated before the generic mantra fallback, so the Namavali tab receives its own readable entries.

## Verification

- Offline unit suite: 52 tests, no failures or errors, including known/future song groups and scripture classification.
- Isolated SongFavoritesInstrumentation on connected Galaxy M11: add/remove, idempotency, persistence, profile isolation, copy isolation and catalogue grouping passed. Test keys cleaned afterward.
- Debug build installed on Galaxy M11.
- Device interaction: favorite a song, see it in Favorites, open landscape reader, remove favorite in reader and return. Temporary favorite removed.
- Inspected actual light/dark Songs screenshots and dark Namavali/landscape reader screenshots. Suladhi tab shows its own 10 entries. Cold navigation to Namavali opens the correct tab.
- Full encrypted-backup roundtrip was not exercised in this pass. Web preview is not changed by this native-app request.
