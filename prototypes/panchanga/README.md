# Panchanga HTML design studio

Open `index.html` to tune the screen. No build, dependencies, or internet connection are required. For a consistent browser origin and saved settings, serve this folder:

```powershell
python -m http.server 8766 --bind 127.0.0.1
```

Open http://127.0.0.1:8766. `panchanga.html` opens the screen without the design controls.

The studio previews screen dimensions, text scale, Carbon spacing, and minimum tile width. Changes and iteration notes persist in the browser. Save design settings downloads JSON for the later Android implementation. Corners remain 4px; the accent remains orange.

The two paksha rows share aligned tithi ranges. Narrow screens page both rows together without splitting a merged tile. Adjacent tithis assigned to the same civil date render as one actual spanning button; Ekadashi remains marked even inside a merged tile. A tithi with two dates retains both dates. Short screens put the full calendar context and selected-date details in the month disclosure. The settings and section selectors are interactive; other app sections are placeholders.

**The date fixture is illustrative, not authoritative Panchanga data.** This prototype does not calculate astronomical tithi boundaries. Real data integration and Android implementation follow design review.

The book logo and Kannada fonts are reused from the Android app. Palette, spacing, and hierarchy follow the existing Carbon-inspired direction; this is an HTML prototype using native browser controls, not an Android Material implementation.
