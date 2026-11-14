# Material 3 dashboard preview

The default app-shell studio redirects here. The dashboard is implemented with official Material Web components loaded via the documented esm.run import map. No Carbon components or stylesheets are loaded by this preview.

The reference image is adapted to MadhvaNidhi practice data with a greeting, paired Sandhya/streak cards, circular Japa progress, Japa cards and recent practice.

Demo data is isolated from device records. The preview supports light/dark, read-only practice history and Japa counts and bundled Puja/Songs viewing. It is a dashboard design preview rather than a full Android emulator.

Serve prototypes locally and open /current-app/. Material Web and Google fonts require internet access.

Puja/Songs titles open a landscape reader preview; closing it restores the portrait studio. Text-size controls belong to that reader. The preview exposes only Light/Dark themes.

Dashboard completion is read-only and comes from practice-screen completion flags. The native app exposes count/completion changes only in the Japa/Sandhyavandana screens. The web dashboard uses isolated demo records and exposes no count/completion mutation controls.

Awards use Google Material Symbols Outlined `award_star`, FILL 1, with brown/silver/gold finishes. Japa uses `eda`, FILL 0, weight 100, GRAD 0 and optical size 24. A single combined variable-font request includes these symbols and the other shell glyphs so symbol subsets do not replace one another. Android bundles the matching official SVG outlines as vectors for offline use.

The top dark daily-practice card is removed. Completed Japa summary/tiles use a tonal Material container, accent outline and explicit Completed text instead of checkmarks. The native state comes from the Japa portal completion flag. Preview defaults to demo completion; `shell.html?japa=pending` and `shell.html?japa=skipped` show the other read-only variants without changing counts.

Dashboard cards use neutral surfaces in both themes, including completed Japa cards. Completion keeps its accent outline and explicit status. Weekly streak replaces Recent practice; its rows use session icons with muted incomplete states rather than ticks.

Material shortcut icons: spa for Puja, music_note for Songs and menu_book for Grantha. The weekly-streak section shortcut uses calendar_view_week; Japa retains eda at weight 100.
