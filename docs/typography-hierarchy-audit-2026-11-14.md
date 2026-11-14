# Text hierarchy audit — 14 November 2026

## Result

The app has recognisable Material typography, but hierarchy is not consistent enough to freeze. **Three high-priority issues** affect readability or access to text: low-contrast orange labels, permanently truncated descriptions, and calendar text that ignores system font scaling. Eight medium-priority findings address role consistency, emphasis, navigation through headings and mockup parity; one lower-priority risk concerns reader toolbar truncation.

This is an audit and proposed implementation specification. No app or mockup implementation was changed in this pass.

## What was checked

- Source: all **22 screen/activity layout files**, their shared text rows/styles, routing and runtime typography binders. This includes inactive legacy layouts, which are not treated as current device screens.
- Device: **25 captured UI states** across the eight main destinations, Nitya tabs, scripture/song categories, Gruha tools, five Settings tabs, profile edit and the landscape reader. Hubs and their default child can share a rendered layout; 25 is a state count, not 25 distinct designs.
- Galaxy M11: 720 × 1560 pixels, 280 dpi, existing Android **font scale 0.9**. Screenshots were checked after asynchronous loading; initially stale Panchanga/Sandhya captures were replaced. Personal profile values are omitted from this report.
- Web: live Material preview computed typography, including headings, card labels, counts and supporting text.
- Source-only: registration, alarm alert, inactive legacy Puja/hub layouts and dark-theme typography tokens. These flows were not opened as part of this device pass.
- Not run: a 200% font-size device sweep, TalkBack navigation, a complete dark-mode screenshot sweep or automated tests/builds. This pass changed documentation only. No preference values, practice records or profile fields were changed; profile edit was inspected without saving. The phone was returned to Dashboard.

## Prioritised findings

### T01 · High · Orange is too faint for small text in light mode

The current accent `#FF4F18` has calculated contrast **3.290:1 on white** and **3.017:1 on #F5F5F5**. This affects small group headings, selected catalogue titles and orange text actions. Black on the orange fill measures **6.384:1** and is a good existing choice.

Keep the requested accent for indicators and filled controls. Use `onSurface` for ordinary small text, or another text treatment that reaches 4.5:1. Disabled read-only controls are a separate case and were not counted as active-label failures. The reference threshold is [WCAG contrast minimum](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

Evidence: [catalogue group labels](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/item_select_section.xml:3>); [selected catalogue row styling](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/ReaderFragment.kt:450>).

### T02 · High · Detail prose is permanently cut off

Rangavalli, Recipes and Playlists use a bottom detail body capped at **three lines with end ellipsis**. With the former Open Content action removed, the full description has no visible expansion control in this flow. Important prose is presented as a disposable caption.

Allow the full description to expand/scroll and use BodyLarge for primary guidance; retain BodyMedium only for a genuine short preview with a clear route to the full text.

Evidence: [sectionDetailBody](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/page_section_browse.xml:73>). The cutoff is visible in the device captures.

### T03 · High · Essential calendar labels bypass system font size

The calendar calculates text with screen width and `density`, then applies pixel `AbsoluteSizeSpan` sizes. Weekday/tithi minimums are based on 8 × density; `mockScale()` follows viewport dimensions, not Android `fontScale`. Other screens use SP, so increasing accessibility text size will not enlarge these labels consistently.

Use scale-aware text sizes for date, weekday and tithi, then allow the tile to reflow instead of shrinking essential labels to preserve a fixed square. This is source-verified; a large-font device reproduction remains outstanding.

Evidence: [viewport scaling](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/FrozenPanchangaBinder.kt:31>); [weekday/tithi sizes](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/FrozenPanchangaBinder.kt:128>); [pixel text spans](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/FrozenPanchangaBinder.kt:134>).

### T04 · Medium · Panchanga gives calendar context more weight than the selected day

Shaka year, samvatsara and masa use large titles with runtime semibold treatment. They occupy several strong lines before the facts and calendar. The selected date is comparatively quiet in the shell header. This reverses the reading order: selected day → its facts/event → broader calendar context.

Use a consistent body-sized context block, bolding actual values rather than entire sentences. Keep the selected date as the dominant title. Preserve the now-correct date → weekday → tithi order inside tiles.

Evidence: [context title roles](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/frozen_panchanga.xml:49>); [runtime context emphasis](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/DeskFragment.kt:335>).

### T05 · Medium · Shared roles and Indic weights do not produce a consistent hierarchy

Screen titles vary between 22sp TitleLarge and 24sp HeadlineSmall. Section roles mix those with 16sp TitleMedium, while legacy aliases still force explicit sizes. In `DeskType`, both 400 and 500 resolve to the regular Kannada/Devanagari asset; only 600 uses semibold. A requested medium title can therefore look like ordinary body text.

Assign semantic roles once, map them to existing Material text appearances and use a deliberate supported font weight for each script. A legacy resource name is not itself a defect; inconsistent effective size/weight is.

Evidence: [legacy role overrides](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/values/carbon_layout.xml:19>); [Indic weight selection](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/DeskType.kt:78>).

### T06 · Medium · Selected content names are styled like compact secondary text

`MaterialChoices.bind()` forces 14sp/18sp compact body text and a single line by default. Japa and user practice preferences have wrapping exceptions, but Puja, scripture and song selectors still use this compact primary value. Group headings below them are 12sp labels, weaker than the item list they organise.

Use BodyLarge 16/24 for selected titles and normal form values; allow meaningful long names to wrap. Use a stronger section role plus spacing for catalogue groups.

Evidence: [shared selector binding](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/MaterialChoices.kt:16>); [group heading role](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/item_select_section.xml:3>).

### T07 · Medium · User identity and Backup purpose lack clear section hierarchy

Users gives practice preferences a section heading, but the profile begins with ordinary name/field rows below Edit. The name has the same BodyMedium role as other values. Backup starts immediately with passphrase hints and buttons, with no panel heading or explanatory body.

Add a visible Profile section with the name as a title. Give Backup a purpose heading and short explanatory body before inputs; keep field labels persistent and secondary.

Evidence: [profile name role](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/page_user_profile.xml:108>); [Backup starts with an input](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/page_profile_backup.xml:13>).

### T08 · Medium · Dashboard numbers need clearer semantic labels

The current streak, best streak and next target rely heavily on star/icon notation. The large number is clear, but the visible distinction between current and best is weak. Card supporting labels and cumulative totals use small label roles; the greeting and section headings also share the same size.

Keep count-led cards, but show visible Current streak / Best streak labels, use one secondary-information role, and distinguish the page identity from section titles. The Japa portal’s intentionally large counter is a valid exception, not a reason to enlarge every label.

Evidence: [dashboard section title](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/page_dashboard.xml:13>); [streak text roles](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/page_dashboard.xml:27>); [42sp Japa counter exception](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/page_japa.xml:122>).

### T09 · Medium · Sandhyavandana emphasises a procedure step on the selection screen

The largest in-panel title is `sandhyaPathy`, currently a procedure-step name, while the selected session context is a small breadcrumb. The prominent action opens the complete session in landscape. The title therefore emphasises a detail that does not describe the page’s main action.

Make the selected morning/noon/evening session the main in-panel title; keep step names in the reader, or as secondary context when needed.

Evidence: [step title binding](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/java/com/paperdesk/launcher/ui/NityaKarmaFragment.kt:162>); verified Sandhya device capture.

### T10 · Medium · Visual headings lack explicit accessibility heading semantics

No `accessibilityHeading` or `setAccessibilityHeading` declarations were found in the inspected layouts/UI binders. Larger/bolder text alone does not provide heading navigation to assistive technology.

Mark page and section headings semantically. Do not mark every card label, field value or count as a heading. This is a source finding; TalkBack behaviour was not exercised.

### T11 · Medium · The mockup does not match native text hierarchy

Live web measurements: page title 22px/500, section heading 22px/400, card title 15px/500, overall Japa summary **12px at 65% opacity**, Japa name 16px/700 and Japa count 18px/400. Native uses different roles and weights, including 14sp Japa names and a larger overall count summary. CSS headings also use normal rather than explicit line heights.

Use the same semantic role mapping in the mockup and native app. Match usable viewport and font metrics; equal numeric PX/SP values alone do not guarantee equal rendering.

Evidence: [web typography rules](<D:/Work Development/Repos/MadhvaNidhi/prototypes/current-app/material-dashboard.css:5>); live computed-style inspection.

### T12 · Low · Long landscape reader context can be truncated

Reader content has a useful existing hierarchy: a separate toolbar, body/head scaling and A−/A+ controls. However, toolbar title and breadcrumb are each restricted to a single ellipsized line. Long full Japa/content names can lose context as space or font size changes.

Keep controls stable; allow the title to wrap or provide access to its complete value. The inspected short stotra title fit correctly; long-title/large-font combinations remain a source-backed risk rather than a reproduced failure.

Evidence: [reader title and breadcrumb](<D:/Work Development/Repos/MadhvaNidhi/app/src/main/res/layout/activity_content_reader.xml:53>).

## Screen-by-screen disposition

| Screen/group | Coverage | Hierarchy assessment |
| --- | --- | --- |
| Dashboard + progress details | Device/source; details also checked in preceding implementation verification | Counts are distinct; streak labels and supporting text need T08. Read-only completion behaviour should be preserved. |
| Panchanga + event/date details | Device/source | Highest concentration of hierarchy issues: T01, T03, T04. Full weekday names and event icon are improvements to retain. |
| Nitya → Sandhyavandana | Device/source | Correct landscape reading boundary; selection title/context needs T09. |
| Nitya → Japa | Device/source | Counter is dominant; use T05/T06 for full name and status roles. Keep counts larger than goals. |
| Nitya → Parayana | Device/source | Clear primary reading action; compact selector/breadcrumb needs T06. |
| Puja picker | Device/source | Selected procedure is identifiable, but selector and group/list emphasis need T01/T06. |
| Grantha → Stotra, Suktas, Mantras, Namavali | All four states captured/source | Shared T01/T06. Namavali empty state is visually quiet; use a clear body-sized empty-state message. |
| Songs → Hadugalu, Playlists | Both states captured/source | T01/T06, plus T02 in playlist descriptions. |
| Gruha host → Aayadi | Device/source | Input/result sections are distinguishable. Document why Padam is larger than peer result values; avoid accidental role overrides. |
| Gruha → Rangavalli, Recipes | Both states captured/source | Page/detail titles are distinguishable; T02 is the main problem. |
| Settings → UI | Device/source | Theme/language controls and the corrected two-line developer credit have a clear hierarchy. Keep this footer body-sized. |
| Settings → Dashboard | Device/source | Three section headings exist; dense target table and compact slot values need T05/T06. |
| Settings → Users, edit mode | Both states captured/source | T07; use the same form/value roles as other settings. |
| Settings → Backup | Device/source | T07: field hints are carrying the panel’s explanatory burden. |
| Settings → Legal | Device/source | One of the more consistent screens: page heading → section heading → body → version. Body 14sp is a readability review item for the older audience, not a missing hierarchy. |
| Landscape content reader | Device/source | Existing reading hierarchy and scalable body are sound; T12 and T01 apply. Other reader modes share this surface. |
| Registration | Source only | HeadlineSmall + supporting text + fields is structurally reasonable; verify long Kannada labels and large text during implementation QA. |
| Alarm alert | Source only | 72sp time / 22sp label is an intentional task-specific display hierarchy; do not apply ordinary form-title sizing to it. |
| Legacy Puja and generic hub layouts | Source only, not current routing | Excluded from device conclusions; do not use them as current visual authority. |
| Material Web dashboard | Live computed styles/source | T11; use it as a layout preview until typography parity is corrected. |

## Proposed Material role contract

This is the recommended project mapping, using the existing [Material 3 type scale](https://developer.android.com/develop/ui/compose/designsystems/material3#typography); it is not a proposal to introduce custom UI components.

| Purpose | Material role | Size / line height |
| --- | --- | --- |
| Full page title | HeadlineSmall | 24 / 32sp |
| Major content section | TitleLarge | 22 / 28sp |
| Card/group heading | TitleMedium | 16 / 24sp |
| Body text and selected form value | BodyLarge | 16 / 24sp |
| Secondary prose | BodyMedium | 14 / 20sp |
| Metadata, units, field labels | LabelMedium | 12 / 16sp |
| Important card metric | HeadlineMedium | 28 / 36sp |
| Main Japa counter | DisplaySmall, with any deliberate larger override documented | 36 / 44sp baseline |

Keep reader A−/A+ scaling separate from shell typography. Keep the existing 17sp/26sp reader body preset as an explicit reading choice, then verify it with the actual Indic fonts. Use primary text colour for information; reserve orange for emphasis that remains readable. Preserve full names and the portrait-shell/landscape-reader boundary.

## Recommended implementation order and acceptance checks

1. Fix T01–T03 first: readable accent usage, access to complete descriptions and scale-aware calendar text.
2. Apply the shared role mapping and Indic weights, then refine Panchanga, Sandhya, Dashboard and Users/Backup.
3. Bring the web preview onto the same role mapping; add semantic headings.
4. Recheck at 100%, 150% and 200% system text, in both light/dark, with Kannada, Sanskrit and English examples. Verify complete Japa names, long procedure titles, merged calendar days and landscape reader controls.
5. Confirm that metadata remains subordinate, primary values are readable without colour alone, and no text overlaps or becomes inaccessible. Preserve read-only dashboard completion and all current stored preferences.

The current 90% font-size capture sweep is evidence for this audit, not a pass for those unrun acceptance conditions.
