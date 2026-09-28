# Color Lines 98 (Android Native + Web Standalone)

[![Web CI & GitHub Pages Deploy](https://github.com/Basil-AS/color-lines-98/actions/workflows/web-ci.yml/badge.svg)](https://github.com/Basil-AS/color-lines-98/actions/workflows/web-ci.yml)
[![Android CI](https://github.com/Basil-AS/color-lines-98/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Basil-AS/color-lines-98/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A state-of-the-art, modern recreation of the iconic puzzle game **Color Lines** (Линии 98 / Gamos 1992 / Windows 98). Built natively for Android with **Kotlin 2.x + Jetpack Compose** (120Hz smooth rendering, rich haptics, low-latency audio) and accompanied by a standalone **TypeScript Web client** ready for GitHub Pages.

---

## 🌟 Key Features

* **Authentic Game Mechanics**:
  * 9×9 grid, 7 classic colors, 3 balls spawned per turn;
  * Formation of 5+ matching balls in horizontal, vertical, or diagonal lines clears them and grants a free turn;
  * Canonical Gamos 1992 score formula: $Score(L) = 2L^2 - 20L + 60$;
  * Accurate multi-line cross intersection clearing.
* **Modern Mobile UX (Android)**:
  * **120 FPS Rendering**: GPU-accelerated Compose Canvas;
  * **Pathfinding & Reachability**: BFS wave algorithm (Lee) with real-time highlighted reachable destination cells on touch;
  * **Haptic Feedback**: Rich tactile sensations on tap, move, and line explosions;
  * **Instant Audio Engine**: `SoundPool` with zero-latency playback of classic sounds;
  * **Undo & State Persistence**: Non-blocking session and high score persistence via Jetpack DataStore.
* **Multi-Theme Engine (In-Game Switcher)**:
  * **Modern Theme**: Fluid neon/glassmorphism design, spring physics, and particle FX;
  * **Lines 98 Classic**: Authentic Windows 98 chrome, 3D shiny spheres, and 90s sound effects;
  * **Color Lines 1992**: Nostalgic MS-DOS Gamos wood texture and animated characters.
* **Pure Web Client**:
  * Runs 100% offline in browser (HTML5 Canvas + React 19 + TypeScript);
  * Automatically deployed to GitHub Pages via GitHub Actions.

---

## 🏗️ Architecture

```
color-lines-98/
├── android/                   # Native Android application
│   ├── core-engine/           # Pure Kotlin module (POKO, 0 Android deps, 100% test coverage)
│   │   ├── Board.kt           # 9x9 grid representation
│   │   ├── PathFinder.kt      # Lee wave / BFS shortest path & flood-fill reachability
│   │   ├── LineDetector.kt    # 4-axis line detector & scoring algorithms
│   │   └── GameEngine.kt      # Core state machine, spawns, undo stack
│   └── app/                   # Jetpack Compose UI, Material 3, SoundPool, Haptics
├── src/engine/                # TypeScript algorithmic mirror for Web
├── tests/                     # Vitest unit tests for Web engine
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
