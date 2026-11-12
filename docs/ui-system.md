# MadhvaNidhi Carbon shell UI

**Current frozen shell and Panchanga specification:** [frozen-panchanga-shell.md](frozen-panchanga-shell.md). HTML shell preview: [`prototypes/app-shell/`](../prototypes/app-shell/). That contract supersedes the earlier bottom-navigation notes below.

The launcher uses a Carbon header and side rail. Primary destinations are Dashboard, Panchanga, Nitya Karma, Grantha, Bhakti Sangeeta, Gruha Samskruti, and Settings. Screens use Android Material Components with Carbon Gray 100 / White tokens and Orange accents.

## Components

- `MaterialToolbar` exposes settings and A−/A+ icons. One bottom bar combines the destination dropdown with content paging.
- `MaterialCardView` groups Panchanga, reading, japa and Aayadi content.
- `TextInputLayout` with `MaterialAutoCompleteTextView` provides exposed dropdown menus for Veda, Matha, readings, japa and bounded inch measurements. Feet use numeric `TextInputEditText` fields.
- `MaterialButton` and `MaterialButtonToggleGroup` provide actions and reader categories.
- A Material date-entry form selects the Panchanga date; the home screen uses the approved square-tile Panchanga layout with an event box. All 15 tithis stay visible: both pakshas share the screen when space permits, otherwise footer arrows switch complete paksha grids. On short screens with large text, the full header is available through a long press on the month button and in the event-details dialog.
- `MaterialAlertDialogBuilder` provides information and confirmation dialogs.
- Settings exposes a Material text-size slider (100–200% in 25% steps) and an explicit Apply action for keyboard/accessibility use. A−/A+ icons are available in the toolbar. The saved multiplier applies to the whole app in addition to the device font setting. The selected page survives the text-size change.
- `MaterialTextView` uses Material typography; bundled Kannada/Sanskrit fonts remain available for devotional text.

The active screens contain no app-defined View subclasses or custom-drawn backgrounds.
Stock Android/AndroidX layout containers provide structure. Earlier device-ROM
screens remain in the repository for compatibility and are not destinations in the app.

## Responsive behavior

The app stays in landscape in either landscape direction. Page content uses discrete Previous/Next pages;
text and input rows grow with system font size.
The toolbar and pager occupy separate rows, and window insets keep controls outside
system bars and display cutouts. Long readings break between complete text lines.
Selections and stored japa records continue to use the existing stores.

## Device regression check

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w in.hcworks.madhvanidhi.test/com.paperdesk.launcher.ui.ResponsiveUiInstrumentation
```

The device check visits all nine pages, checks toolbar/content separation, text height
and absence of custom views, opens reader selectors and the japa counter, and verifies
the Material date-entry form, its validation, the bottom destination dropdown and settings icon. Screenshots are saved in the app's
external files `ui-audit` directory. Repeat on compact/wide landscape screens and with large system fonts. Pass `-e textScale 2.0` to test the app’s 200% text setting; the audit checks A−/A+ persistence and screen recreation, then restores the saved preference.
Record and restore original display size, density, rotation and font scale after testing.

## Screen-size redesign

Stock GridLayouts adapt to usable window width divided by the active font scale.
Home uses one column on compact/large-text screens, a two-column dashboard on
regular landscape phones, and three
columns on wide screens. Japa places preparation beside the counter when space
allows. Aayadi separates inputs and results, with adaptive fields/result cards.
Settings cards and action rows also reflow. No text is reduced to force a fit.
Side-by-side cards stretch to the same row height. All content retains its configured
text size and is paginated to the available viewport instead of scrolling.

## Home calendar order

The first content line shows the selected date's Shalivahana Shaka year,
Samvatsara, lunar month, Paksha and the Gregorian months covered by the lunar
cycle. It uses Unicode Kannada. Immediately below, two aligned 15-tithi rows
show Shukla Paksha ending in Purnima and Krishna Paksha ending in Amavasya.
The top number in each Material button is the tithi; the bottom number is the
civil date. Repeated tithis retain both dates; skipped tithis are disabled.
Both rows share a paged tithi range when larger text cannot fit all 15 cells.
Previous/Next tithi controls preserve alignment without horizontal scrolling.
At combined font scales above 2.5, each cell uses one line (tithi · civil date)
and wider columns so the full-size text and button fit the viewport.
The cycle uses local calculation; the selected daily data
continues to use the configured Matha source when available.

## Carbon theme

Material components use Carbon Gray 100 (#161616) for the canvas, Gray 90
(#262626) for cards, Gray 80 (#393939) for raised layers, #F4F4F4 for primary
text, and Orange 40 (#FF832B) for interaction/selection. Cards, buttons,
fields, dialogs, selection groups and date-picker cells use a 4dp corner radius.
The design uses Material components throughout; Carbon supplies the palette.
Palette reference: https://www.carbondesignsystem.com/building-blocks/foundations/color/tokens

Jumping to another date uses a standard Material dialog with an ISO date field and strict validation. This avoids fixed-height calendar headers at large font sizes. The primary home Paksha calendar stays in its two-row layout.

## Screen navigation and selection

A single bottom bar contains an exposed Material destination dropdown and the
active screen's content-page controls. Settings has one entry point in the
top-right toolbar, beside A− and A+ icons. The slider in Settings provides exact
100–200% selection. Changing text size recreates the active destination.

Sandhyavandana uses a single-selection Material button group with sunrise,
midday sun and sunset icons. Mantra, puja, song and Nitya readings use exposed
Material dropdowns; selected titles are not repeated as separate headings.

ScreenPages composes stock FrameLayout/LinearLayout and Material controls. It
measures content at its natural height and divides it at shaped glyph boundaries (including Kannada marks) and
control boundaries. A clipped viewport displays one discrete page; there is no
content scroll container in the active view tree. Compact or large-text screens
therefore show additional pages rather than shrinking the user's text.

Dropdown selections use a single line with end ellipsis; opening the stock menu exposes complete labels. This keeps long names within the field at large text sizes.

## Carbon spacing and hierarchy

Reference spacing scale: https://www.carbondesignsystem.com/building-blocks/foundations/spacing/overview
Reference type roles: https://www.carbondesignsystem.com/building-blocks/foundations/typography/type-sets

Spacing tokens map Carbon px to Android dp: 2, 4, 8, 12, 16, 24, 32, 40, 48 and 64.
Dense landscape pages use 8dp outside spacing; regular layouts use 16dp and wide
layouts use 24dp. Card gutters are 16dp; related controls use 8dp. Section breaks
use 24dp. Tokens remain fixed; the layout selects a token at a breakpoint.

Typography uses label-01 (12/16), body-01 (14/20), body-02 (16/24), heading-01
(14/20), heading-02 (16/24), heading-03 (20/28) and numeric emphasis (32/40).
Noto Sans Kannada/Devanagari remain the script fonts. Reading leading is applied
after the Indic font setup; Carbon line heights are nominal minima, with extra
space only when measured glyphs require it. Controls remain Material;
Carbon supplies spacing, type roles, layers and emphasis. Orange identifies the
selected calendar day and the primary action; supporting text uses Gray 30.

Cards that fit stay intact. At large text sizes, oversized card surfaces become
borderless content sections rather than displaying cropped card frames. Japa
puts completion/reset beside the selection and retains one primary increment
action. The jump-to-date dialog uses a standard Material input form with strict
ISO date validation and one confirmation action, avoiding fixed-height headers.
