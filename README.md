# Basil Lines

[![Web CI & GitHub Pages Deploy](https://github.com/Basil-AS/color-lines-98/actions/workflows/web-ci.yml/badge.svg)](https://github.com/Basil-AS/color-lines-98/actions/workflows/web-ci.yml)
[![Android CI](https://github.com/Basil-AS/color-lines-98/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Basil-AS/color-lines-98/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Basil Lines** is a modern take on the classic puzzle **Color Lines** (Линии 98): a web version that runs in any browser
and a native Android app, sharing one rule set.

- **Play in the browser:** <https://basil-as.github.io/color-lines-98/>
- **Android app:** `BasilLines-<version>.apk` on the [latest release](https://github.com/Basil-AS/color-lines-98/releases/latest)
  (application id `io.github.basil_as.basillines`)

## Features

- **Rules:** 9×9 board, 7 colours, 3 new balls per turn, lines of 5 or more clear (horizontal, vertical, diagonal),
  clearing a line is a free turn, scoring `2L² − 20L + 60`, undo.
- **Spawn preview:** small balls show on the board where the next balls will appear (can be switched off).
- **Four themes** on both platforms, remembered between launches:
  - *Modern dark* and *Modern light*;
  - *Lines 98 (Windows)*: grey window chrome, bevelled cells, black score displays, the original ball sprites;
  - *Color Lines 1992 (DOS)*: the 16-colour EGA/VGA palette, hard bevels, flat-shaded balls and PC-speaker beeps.
- **Progress:** levels with titles, 18 achievements, day streaks, personal records, score trend, active play time and
  the full history of finished games (up to 1000).
- **English and Russian**, picked automatically from the browser or system language (web: also switchable in Settings;
  Android: per-app language in system settings).
- **Everywhere:** responsive layout for phones, tablets and desktops (portrait and landscape), keyboard play and screen
  reader labels on the web, edge-to-edge with system-bar insets on Android, your game resumes after a restart.

> The 1992 and Windows themes recreate the look and feel of the originals from memory and public descriptions; the
> original binaries and artwork are not part of this repository, except for the Lines 98 ball sprites and sounds that were
> already in `assets/`. If you have reference screenshots, the themes can be tuned to match them exactly.

## Repository layout

```
├── src/                 # Web app (React 19 + TypeScript)
│   ├── engine/          # Game rules (board, path finding, lines, engine)
│   ├── progress.ts      # Levels, achievements, streaks
│   ├── stats.ts         # Game history and records
│   ├── i18n.ts          # English/Russian texts (also the source of the Android strings)
│   └── components/      # Dialogs
├── tests/               # Vitest: rules, storage, i18n, progress, UI (Testing Library)
├── android/
│   ├── core-engine/     # Pure Kotlin rules, progress and save format (no Android dependencies)
│   └── app/             # Jetpack Compose UI; Robolectric UI tests in src/test
├── scripts/             # Agent tooling and gen-android-strings.mjs
└── .github/workflows/   # CI, Pages deploy, Android CI, release
```

## Development

### Web
```bash
npm install
npm test            # Vitest (rules, storage, i18n, progress, full UI flows)
npm run lint        # oxlint
npm run typecheck   # TypeScript
npm run build       # production build in dist/
npm run dev         # local dev server
```

### Android
```bash
cd android
./gradlew :core-engine:test    # pure Kotlin unit tests
./gradlew testDebugUnitTest    # engine + Compose UI tests on the JVM (Robolectric, no emulator needed)
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # release APK (signed with the debug key unless you configure your own)
```

Android strings are generated from `src/i18n.ts`; after changing texts run
`node --experimental-strip-types scripts/gen-android-strings.mjs` (CI runs it with `--check`).

## Releases

Push a tag `vX.Y.Z` that matches `package.json` and `versionName` in `android/app/build.gradle.kts`. The release
workflow tests everything and publishes `BasilLines-X.Y.Z.apk` and `BasilLines-X.Y.Z-web.zip`. Every push to `main`
deploys the web app to GitHub Pages.

## Credits

Color Lines (1992) by Gamos: Oleg Demin, Gennady Denisov, Igor Ivkin. Lines 98 (1998) by Dmitry Kivilev.
Research notes are in [`docs/RESEARCH_REPORT.md`](docs/RESEARCH_REPORT.md).

## License

MIT
