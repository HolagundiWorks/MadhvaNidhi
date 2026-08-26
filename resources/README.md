# AcharyaOS development resources

`reference/` contains source documents and curated catalogues used to develop
the bundled AcharyaOS library. These files are development inputs and are not
automatically packaged in the Android application.

Runtime-ready content belongs under `app/src/main/assets/`. Keep generated
extracts and third-party application binaries outside Git in
`../../local-artifacts/`.

Local upstream repositories used by catalogue-generation tools are kept in
`../../local-artifacts/acharya/upstream-sources/` so nested Git metadata does
not pollute this repository.
