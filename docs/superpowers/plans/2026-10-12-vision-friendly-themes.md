# Vision-Friendly Themes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two calm, vision-friendly themes to web and Android, verify them, and ship reproducible GitHub CI/CD release artifacts.

**Architecture:** Extend the existing shared theme identifiers and platform-local palette tables. Keep colour and shape as redundant ball cues, and preserve existing legacy parsing. Reuse the current release workflow and add only the missing web/Android verification and artifact publication steps.

**Tech Stack:** React/TypeScript/CSS, Kotlin/Jetpack Compose, Vitest, Gradle, GitHub Actions, gh CLI.

## Global Constraints

- Preserve the existing unfinished `src/App.tsx` change and unrelated user work.
- Do not change existing theme identifiers or backup formats.
- `clear` and `clearNight` must be available on both platforms.
- Verify actual contrast pairs before claiming completion.
- Use `agy` for routine test/template work; independently inspect and verify its output.

### Task 1: Shared theme contract and web palette

**Files:**
- Modify: `src/themes.ts`
- Modify: `src/App.tsx`
- Modify: `src/App.css`
- Modify: `src/components/SettingsDialog.tsx`
- Test: `tests/app.test.tsx`

**Interfaces:**
- Produces `Theme` members `clear` and `clearNight`, parsed by `parseTheme` and persisted through existing storage.
- Produces `.theme-clear` and `.theme-clearNight` semantic CSS tokens and ball classes.

- [ ] Add failing tests for parsing, rendering selection and persistence of both new identifiers.
- [ ] Run the focused web tests and confirm they fail for the missing identifiers.
- [ ] Add the two identifiers to the existing theme list and voice/sprite helpers without altering legacy aliases.
- [ ] Add semantic CSS tokens for surfaces, grid, focus, controls and text; use a visible board grid and stable shape marker classes for the seven balls.
- [ ] Add swatches and labels to the existing settings dialog using the current component pattern.
- [ ] Run focused tests, TypeScript checks and the web build.

### Task 2: Android theme parity

**Files:**
- Modify: `android/app/src/main/kotlin/io/github/basil_as/basillines/AppTheme.kt`
- Modify: `android/app/src/main/kotlin/io/github/basil_as/basillines/Palette.kt`
- Modify: `android/app/src/main/kotlin/io/github/basil_as/basillines/Dialogs.kt`
- Modify: Android string resources and theme tests found by the graph.

**Interfaces:**
- Produces `AppTheme.CLEAR` and `AppTheme.CLEAR_NIGHT` and corresponding `Palette` values.
- Keeps existing `paletteFor`, backup serialization and shape-coded ball drawing compatible.

- [ ] Add failing tests for enum parsing/backup round-trip and palette properties.
- [ ] Implement both palettes with visible grid contrast, calm surfaces and saturated balls.
- [ ] Add localized display labels and settings swatches.
- [ ] Run the focused Gradle unit tests and Android lint/compile checks.

### Task 3: CI/CD and release artifacts

**Files:**
- Modify: `.github/workflows/ci.yml` or the existing CI workflow.
- Modify: `.github/workflows/release.yml`
- Modify: `README.md` or release documentation only if the repository already documents the workflow there.

**Interfaces:**
- Pull requests run web tests/build and Android tests/build.
- Version tags build web output and Android APK, generate SHA-256 checksums and attach artifacts to a GitHub Release.

- [ ] Inspect current workflow triggers, permissions, Java/Node versions and artifact names.
- [ ] Add only missing matrix steps with cache-safe actions and least-privilege permissions.
- [ ] Validate workflow YAML and run local equivalent commands.
- [ ] Use `gh workflow run`/`gh run watch` only after the branch is pushed; never push to `main`.

### Task 4: Verification and delivery

**Files:**
- Modify: `.agents/tasks/20261006-0230-audit-web-and-android-for-bugs-and-improvements/TASK.md`
- Modify: `.agents/tasks/20261006-0230-audit-web-and-android-for-bugs-and-improvements/JOURNAL.md`
- Modify: `.agents/tasks/20261006-0230-audit-web-and-android-for-bugs-and-improvements/HANDOFF.md`

- [ ] Run the complete relevant web and Android verification commands through `rtk` where available.
- [ ] Inspect the final diff, workflow permissions, generated artifacts and secrets.
- [ ] Push only the task branch over the configured SSH remote and create/update a PR with `gh`.
- [ ] Record exact CI run URLs and release status in task state.
