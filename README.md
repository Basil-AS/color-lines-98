# Color Lines 98 (Android Native + Web Standalone)

[![Web CI & GitHub Pages Deploy](https://github.com/Basil-AS/color-lines-98/actions/workflows/web-ci.yml/badge.svg)](https://github.com/Basil-AS/color-lines-98/actions/workflows/web-ci.yml)
[![Android CI](https://github.com/Basil-AS/color-lines-98/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Basil-AS/color-lines-98/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A state-of-the-art, modern recreation of the iconic puzzle game **Color Lines** (Линии 98 / Gamos 1992 / Windows 98). Built natively for Android with **Kotlin 2.x + Jetpack Compose** (haptics and sound effects) and accompanied by a standalone **TypeScript Web client** ready for GitHub Pages.

---

## 🌟 Key Features

* **Authentic Game Mechanics**:
  * 9×9 grid, 7 classic colors, 3 balls spawned per turn;
  * Formation of 5+ matching balls in horizontal, vertical, or diagonal lines clears them and grants a free turn;
  * Canonical Gamos 1992 score formula: $Score(L) = 2L^2 - 20L + 60$;
  * Accurate multi-line cross intersection clearing.
* **Android (Kotlin + Jetpack Compose)**:
  * Compose Canvas board with a Lee-wave (BFS) pathfinder and highlighted reachable cells on touch;
  * Haptic feedback on select, move and line clears;
  * Sound effects through `SoundPool`;
  * Undo, plus persisted theme and best score (`SharedPreferences`);
  * Two themes: **Modern** and **Lines 98 Classic**.
* **Web client (React 19 + TypeScript)**:
  * Runs fully offline in the browser and deploys to GitHub Pages;
  * Three themes: **Modern**, **Win98** (original sprites) and **DOS92**;
  * The current game is saved automatically and resumes after a reload; theme, sound and best score are remembered;
  * Fully keyboard-playable (arrow keys, Enter/Space), labelled for screen readers, respects `prefers-reduced-motion`.

---

## 🏗️ Architecture

```
color-lines-98/
├── android/                   # Native Android application
│   ├── core-engine/           # Pure Kotlin module (no Android dependencies, unit-tested)
│   │   ├── Board.kt           # 9x9 grid representation
│   │   ├── PathFinder.kt      # Lee wave / BFS shortest path & flood-fill reachability
│   │   ├── LineDetector.kt    # 4-axis line detector & scoring algorithms
│   │   └── GameEngine.kt      # Core state machine, spawns, undo stack
│   └── app/                   # Jetpack Compose UI, Material 3, SoundPool, Haptics
├── src/engine/                # TypeScript mirror of the Kotlin engine
├── src/storage.ts             # Validated localStorage persistence (theme, best score, saved game)
├── tests/                     # Vitest tests: engine rules, persistence
├── assets/                    # Preserved authentic sounds and sprites
├── docs/                      # Architectural specifications & research reports
│   ├── PLAN_AND_SPECS.md      # Master roadmap & development guidelines
│   └── RESEARCH_REPORT.md     # In-depth analysis of 1992/1998 binaries & references
└── .github/workflows/         # Automated CI/CD pipelines
    ├── web-ci.yml             # Web test, build & GitHub Pages deployment
    ├── android-ci.yml         # Android unit tests & debug APK build
    └── release.yml            # Tagged release pipeline with APK and web zip
```

---

## 🛠️ Development & Testing

### Web Client
```bash
npm install
npm test            # Run Vitest test suite
npm run typecheck   # TypeScript check
npm run build       # Build standalone web distribution
npm run dev         # Start local dev server with HMR
```

### Android Native
```bash
cd android
./gradlew :core-engine:test    # Run Kotlin core engine unit tests
./gradlew testDebugUnitTest    # Run full Android test suite
./gradlew assembleDebug        # Build debug APK
```

---

## 📜 Historical Research

Original executables from 1992 (*Color Lines* by Gamos: Oleg Demin, Gennady Denisov, Igor Ivkin) and 1998 (*Lines 98* by Dmitry Kivilev) were analyzed:
* Decompressed original LZEXE 0.91 DOS binary (`lines.exe` -> Borland Turbo C/C++, Genus pcxLib);
* Extracted original 1992 PCX archives (`lines.lib`) and Windows 98 sound effects;
* Full findings documented in [`docs/RESEARCH_REPORT.md`](docs/RESEARCH_REPORT.md).

---

## 📄 License
MIT License.
