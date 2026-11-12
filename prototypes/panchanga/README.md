# Panchanga HTML design studio

Open `index.html` to tune the screen. No build, dependencies, or internet connection are required. For a consistent browser origin and saved settings, serve this folder:

```powershell
python -m http.server 8766 --bind 127.0.0.1
```

Open http://127.0.0.1:8766. `panchanga.html` opens the screen without the design controls.

The studio previews screen dimensions, text scale, page insets, and tile spacing. Changes and iteration notes persist in the browser. Save design settings downloads JSON for the later Android implementation. Corners remain 4px; the accent remains orange.

The landscape screen uses a 30:70 context/calendar split (with a 208px minimum context width on small screens), measured within the area beside the navigation rail. The selected date appears first on the left with a calendar-icon date-picker action, followed by tithi and weekday. A divider separates this from the Shaka year, samvatsara, masa, paksha, Rashi/Nakshatra/Bhava/Karana fields. The unboxed daily-event section follows those details. The right side shows the current paksha in five logical columns, keeping all 15 tithis visible; a two-button Shukla/Krishna selector with empty/filled moon icons sits above the calendar. Square tile size is bounded by both width and height. Shared-date tiles span two columns and remain intact. A tithi with multiple dates retains every date and weekday.

The left navigation rail expands into an overlay drawer with labeled section buttons, closes using the toggle, backdrop or Escape, and keeps Panchanga active. Other sections are explicitly labeled placeholders for later iterations. The old bottom dropdown is removed. The event/importance box sits in the left context section below the selected date. Tapping a calendar tile selects it and opens a date-details dialog with weekday, paksha, tithi, month, event and significance; the selected-date heading opens the same dialog. Portrait previews stack the two areas; the no-scroll fit checks target the requested landscape mode.

**The date fixture is illustrative, not authoritative Panchanga data.** This prototype does not calculate astronomical tithi boundaries. Real data integration and Android implementation follow design review.

The book logo and Kannada fonts are reused from the Android app. Palette, spacing, and hierarchy follow the existing Carbon-inspired direction; this is an HTML prototype using native browser controls, not an Android Material implementation.

## Color roles

`carbon-theme.css` contains exact Gray 100 tokens from `@carbon/themes` 11.85.0, with Orange 40 and its hover value from `@carbon/colors` 11.62.0 as the requested MadhvaNidhi accent override. Carbon's default interactive accent is blue; this preview deliberately retains the requested orange branding. The original logo artwork is unchanged.

The page uses background `#161616`, layer 01 `#262626`, layer hover 01 `#333333`, neutral selected layer `#393939`, primary/secondary text `#f4f4f4`/`#c6c6c6`, border tokens according to surface level, and white focus `#ffffff`. Orange highlights selection outlines and primary actions. Carbon Yellow 30 (`#f1c21b`) separately highlights Ekadashi, including merged tiles, selected-day details. Neutral selected surfaces replace the earlier orange-brown fill. Field 02 is used for dropdowns on layer 01; disabled text, overlay, error and link colors use their Carbon roles.

Reference: https://www.carbondesignsystem.com/building-blocks/foundations/color/tokens

Date details include Rashi, Nakshatra, Bhava and Karana fields. The standalone preview does not have a date-specific Panchanga feed; these new values display a data-pending state rather than invented astronomical assignments.

The separate Gregorian month-range row is removed. The top-toolbar calendar icon opens the date selector; choosing a supported demo date updates the selected date, weekday, tithi, event and current paksha. Clicking date text or a tile still opens details. Any valid civil date can be selected. Dates outside the fixture display a pending-data view instead of inferred Panchanga values; empty or invalid dates show validation feedback.

A neutral Carbon separator precedes the daily-event section. Its content fills the remaining left-panel space and scrolls vertically only when needed; the date, Panchanga details, and calendar stay in place. The scrollbar is hidden while scrolling stays active. The scroll region supports keyboard focus and retains the event-details action.

The Today icon beside the selected date uses the browser’s local calendar date, updates the weekday, and returns to the available calendar when data exists. Otherwise it shows a pending-data state and offers a return to the illustrative sample. Paksha toggles remain usable in this state, and tapping an already-active toggle preserves the selected civil date.

A keyboard-accessible appearance-icon action beside the A−/A+ controls toggles Carbon White and Gray 100. The shared theme script remembers the choice and synchronizes the preview and studio. Light mode uses deeper Carbon orange/yellow palette variants for legible accents; the logo artwork remains unchanged.

Settings lives in the collapsible side navigation and opens the display preferences dialog. The top toolbar retains date selection, text-size controls, and the light/dark switch.

The theme action uses a split-display icon rather than celestial symbols, keeping it distinct from the paksha moon markers. Its tooltip and accessible name describe the next theme action. Panchanga item labels use regular weight; actual values use semibold, while data-pending placeholders stay regular.

Date selection uses a proper month calendar with month/year dropdowns, month navigation, weekday headers, six calendar rows, a Today shortcut, and Cancel/Apply. Arrow keys move day focus; choosing a day changes the draft date, and Apply commits it.


The design is frozen on 12 November 2026. Shell, semantic colours, spacing and icons are specified in `../../docs/frozen-panchanga-shell.md`. ColorSwitch replaces the earlier appearance icon; OpenPanelFilledLeft replaces the side-panel menu icon. Calendar tiles use date → weekday → regular-weight tithi, with 1.5× corner markers.
