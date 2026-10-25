# Color Lines 4.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved board-first modern Android/web experience as a verified 4.0 release while preserving every existing theme, theme builder and recognizable DOS/Lines 98 experiences.

**Architecture:** Keep engine and persistence contracts stable. Put pure viewport sizing in an Android presentation policy, build modern chrome around the existing BoardView, and isolate classic game rendering from modern chrome changes. Improve supporting screens using the existing shared theme palette and semantic controls. Modernize the toolchain as a coordinated tested change after functional layout regressions exist.

**Tech Stack:** Kotlin/Jetpack Compose/Material 3, pure JVM policy tests, Robolectric UI tests, React/TypeScript/CSS, Vitest/Playwright, pinned Artemis AndroidAdbDriver and connected Huawei Android 10 device.

## Global Constraints

- Preserve all current themes, the complete theme constructor, saved overrides, codes and backups.
- DOS and Lines 98 artwork, rules, animation and sound identity must remain recognizable; only compatible layout/accessibility/bug improvements are permitted.
- Preserve saved games, history, progression, languages, audio/haptics and game rules.
- Work only on codex/v4-adaptive-experience or its task worktree; never directly commit to main.
- Preserve installed phone data: local debug signing differs, so install only a matching signed release APK with update semantics.
- The user approved the proposed design on 2026-10-25 and explicitly reinforced theme/classic preservation. No further routine design/execution approval is needed.
- Local baseline: web 1026 tests/typecheck/build pass; Android UI 78 tests/lint/debug build pass; pure engine has one invalid fixture. Build tools and dependency pins are recorded in LOCK.json.

---

### Task 1: Restore meaningful pure-engine verification

**Files:** `android/core-engine/src/test/kotlin/io/github/basil_as/basillines/engine/LookTest.kt`, `.github/workflows/ci.yml`, `.github/workflows/android-ci.yml`, `.github/workflows/release.yml`.

**Interfaces:** consumes LookStore persistence contract (`contrast` is the serialized key); produces a green `:core-engine:test` gate in regular and release verification.

- [x] Reproduce the existing failing test with `:core-engine:test --tests '*LookTest'`; expect the legacy theme fixture to lose its unsupported `ring` key.
- [x] Correct the fixture to `"clearNight" to mapOf("contrast" to "high")`; do not widen or change the production file format.
- [x] Include `:core-engine:test` alongside existing `testDebugUnitTest` commands in all three verification workflows.
- [x] Run `:core-engine:test`; expect 197 tests and zero failures. Review workflow diff and checkpoint.

### Task 2: Testable adaptive game layout and modern Android chrome

**Files:** create `android/app/src/main/kotlin/io/github/basil_as/basillines/AdaptiveGameLayout.kt`, `ModernGameScreen.kt`, `android/app/src/test/kotlin/io/github/basil_as/basillines/AdaptiveGameLayoutTest.kt`; modify `MainActivity.kt`, UI tests and Android strings only when new user-visible wording is necessary.

**Interfaces:** `adaptiveGameLayout(width: Float, height: Float, fontScale: Float): AdaptiveGameLayout`; output arrangement, boardSize, panelWidth, gap and padding in safe-viewport dp. ModernGameScreen consumes the existing snapshot, Palette, callbacks and GameExtras; classic routes retain their existing rendering.

- [ ] Add numeric regressions for 899x423 landscape (board fills short dimension minus padding), 320x640 portrait, 600x280 short landscape, 840x1280 tablet portrait, 1280x840 tablet landscape, font scale 2 and tiny multi-window sizes. Assert finite/nonnegative geometry and no viewport overflow over a width/height/font grid.
- [ ] Run the targeted test before creating the policy; confirm failure for missing policy.
- [ ] Implement policy using available safe constraints, not display orientation or a fixed percentage cap. Reserve a measured minimum supporting panel and favor the larger valid square.
- [ ] Build quiet modern chrome using the selected Palette: strong score hierarchy, next balls, compact mode/goals, primary undo/hint, secondary actions and clearly labelled overflow. Use 48dp control targets, no unbounded text rows and no oversized symmetric cards. Board and controls remain reachable at large fonts.
- [ ] Route modern themes through this shell; retain DOS and Windows classic renderers and all builder overrides. Move only necessary presentation data visibility; do not rewrite engine logic.
- [ ] Add UI assertions for board bounds/size, mode/goals text and primary controls in both orientations; test all themes and builder state survival. Run numeric and Android UI tests, lint and debug build. Review screenshots before checkpoint.

### Task 3: Coherent supporting screens and safe lifecycle

**Files:** `Dialogs.kt`, `LookStudio.kt`, `Dialogs.kt` statistics sections, `SoundManager.kt`, `MainActivity.kt`, their Android UI/unit tests, shared strings.

**Interfaces:** existing dialog callbacks, LookSpec/LookOverrides/LookCode and GameStorage remain compatible.

- [ ] Map each existing setting, look option, stats/goal/help/result/backup action to its redesigned location; add tests for any moved controls before edits.
- [ ] Give dialogs consistent safe-inset-aware dismissal and primary actions; adapt studio preview/editor to short height and large fonts; keep all theme collections, builder switches, custom colors and code/backup round trips.
- [ ] Reproduce delayed sound/executor work after activity destruction deterministically before touching lifecycle code. If reproduced, cancel callbacks and reject post-release work safely; cover recreation and release races with regressions.
- [ ] Audit type scale, wrapping, contrast, focus/semantics and hit areas across modern themes. Retain authentic classic artwork and sounds. Run focused then full Android tests/lint/build and inspect real-device screenshots.

### Task 4: Responsive web parity

**Files:** `src/App.tsx`, `src/App.css`, relevant `src/components/` settings/look/stats files, `tests/app.test.tsx`, `e2e/audit.spec.ts`, `e2e/quality.spec.ts`.

**Interfaces:** existing game/store/appearance formats and theme attributes are preserved; visual sizing follows the Android board-first intent.

- [ ] Add browser regressions for board fit and control reachability at 320/360/411 widths, short landscape heights 280/320/400 and wide/tablet windows; preserve classic pixel/sprite checks.
- [ ] Rework modern HUD/action composition and responsive constraints without changing DOS/Lines 98 selectors or replacing theme assets. Preserve all builder interactions and controls, keyboard navigation, reduced motion and screen-reader labels.
- [ ] Run Vitest, lint, typecheck, build and full Playwright. Inspect representative screenshots, including every classic theme and custom builder output, before checkpoint.

### Task 5: Coordinated modern Android toolchain

**Files:** `android/gradle/libs.versions.toml`, Gradle wrapper and build files, Android manifest/platform integration, Dependabot compatibility bounds, UI tests and project docs.

**Interfaces:** app remains usable on minSdk 26 and the connected Android 10 phone; tests must cover system bars and back behavior on current Android as well.

- [ ] Verify stable Kotlin/Compose/AGP/Gradle/SDK compatibility against official documentation before choosing versions; document the exact compatible set and migration requirements.
- [ ] Migrate the build/plugins and adopt useful adaptive/window/inset/back APIs coherently; remove compatibility bounds only when the new supported set is verified. Do not bump libraries independently into a broken set.
- [ ] Run pure-engine/UI tests, lint, debug/release builds and modern/minimum API or emulator checks. Verify real Android 10 compatibility and preserve storage formats.

### Task 6: Artemis and device/size/accessibility release audit

**Files:** project-local device audit runner and `docs/` evidence report; Android/web regressions for discovered defects. Captures and device dumps stay in ignored cache or deliberate release evidence assets.

**Interfaces:** pinned Artemis AndroidAdbDriver on USB serial X6H4C20B16000698; consume observed hierarchy/coordinates only. Preserve original rotation/density/font settings in cleanup and player data.

- [ ] Install a matching signed candidate update from CI, preserving game data. Run observed scenarios for play/undo/hint/new game, all themes/day-night, custom builder, goals/stats/help/results/backups and orientation/recreation.
- [ ] Check portrait/landscape, narrow/short windows, 600/840dp tablets, large font, safe system insets and gesture/navigation cases. Measure board size against policy; inspect screenshots and app-specific crash/ANR logs. Add regressions for each defect and repeat affected scenarios after fixes.
- [ ] Record exact hardware/API/size coverage and unresolved gaps; do not infer universal correctness from one phone or narrow tests.

### Task 7: Verified 4.0 release

**Files:** version fields in `package.json`, lockfile and Android app build, README/requirements/changelog and task state.

**Interfaces:** existing project-key release pipeline and Pages deployment.

- [ ] Set 4.0.0 with Android versionCode greater than the last published value. Run complete web, pure-engine and Android/UI/lint/build validation and final theme/builder compatibility audit.
- [ ] Request independent spec/code review; resolve findings, create PR with evidence and attach it to this chat. Merge only after green required checks.
- [ ] Verify signed APK, web ZIP, checksums and installation/update on the phone, verify deployment and synchronize clean main. Update task/handoff with final evidence and completion audit against the original objective.
