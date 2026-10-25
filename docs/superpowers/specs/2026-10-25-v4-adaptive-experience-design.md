# Color Lines 4.0 — adaptive experience

Status: approved by the user on 2026-10-25. Preserve every current theme and the theme builder; preserve DOS/Lines 98 character while improving their usability and layout.

## Objective

Rework the playing experience, settings, appearance studio and supporting screens into a coherent, attractive 4.0 release. Fix landscape board sizing and space distribution on the connected phone and support phones, tablets, narrow windows, large fonts, both orientations and web. Use the pinned google/artemis source for real-device inspection and repeatable scenario validation. Preserve saved games, progress, backups, rules, sound options, languages and theme customization.

## Evidence

Baseline: main 1da4f98, app 3.2.2. Huawei CDY-NX9A, Android 10, physical 1080x2400, density override 408; ADB USB and network entries represent the same device. Screenshots are in .agent-cache/v4-phone-landscape.png. The existing GameScreen uses orientation-only branching, equal side rails and a 60%-width board cap. In landscape the goal label breaks into lines, side surfaces have oversized empty areas, and controls are not composed as one balanced layout. No 4.0 implementation existed at task start.

## Alternatives

1. Recommended: board-first adaptive shell. One compact supporting panel on sufficiently wide windows, a compact HUD and reachable controls on portrait windows. Size the board from actual safe viewport constraints and measured supporting content. Keeps meaningful controls visible and gives the board the available short dimension.
2. Keep the existing symmetrical rails and improve spacing. Smaller change, but the information hierarchy and large empty surfaces remain.
3. Hide nearly all controls in a full-screen board mode. Maximizes the board but adds navigation steps and hides useful game information; consider an optional focus mode rather than the default.

## Proposed design

### Game

The board is the focal surface. Scores and next balls form a compact HUD; undo and hint remain reachable, with new game, appearance and statistics consistently placed. Landscape uses one intentionally sized side panel; the board occupies the maximum square permitted by safe insets and the remaining width. Narrow/short windows use a compact presentation rather than clipping or forcing a minimum board beyond the viewport. Separate content sizing from orientation: large fonts, multi-window and tablets must select a suitable composition. Retain distinct classic-theme character while applying the same sizing and reachability rules.

### Supporting screens

Use a consistent navigation and surface system for settings, looks, statistics, goals, help and game results. Group choices by purpose, provide clear selected states and local explanations, and keep dismissal and primary actions visible with gesture/navigation insets. Appearance preview and editing controls adapt to short windows; no pinned section should consume the usable editing area. Preserve day/night/automatic behavior and all existing looks and builder capabilities.

### Visual and interaction system

Define semantic spacing, type hierarchy, color roles, contrast and shape rules shared across modern surfaces. Use deliberate depth, restrained motion, responsive touch feedback and persistent selected states. Support screen readers, text scaling, reduced motion, keyboard navigation on web and touch targets of at least 48dp for Android controls. Color and shape cues supplement one another. Preserve the recognizable artwork of classic themes unless the user chooses otherwise.

### Architecture and current platform tools

Extract adaptive sizing into a small testable layout policy and composables with clear responsibilities. Reduce the main activity's presentation responsibilities. Adopt supported modern Kotlin/Compose/Material and adaptive APIs where they improve this design; migrate the Android toolchain coherently and verify dependencies together rather than independently forcing version bumps. Preserve the pure engine and storage contracts. Lifecycle work must cancel delayed callbacks and stop sound work safely on recreation/destruction; investigate the observed intermittent RejectedExecutionException with a deterministic regression before changing it.

### Validation and release

Create regression tests for board sizing and adaptive policy before implementation, plus UI tests for orientation, app recreation, dialogs and persistent data. Device scenarios cover game play, undo/hint/new game, look editing, goals, statistics, backups, day/night, large fonts, system bars and keyboard/gesture behavior. Inspect the connected phone via Artemis and screenshots/logcat. Cover compact widths 320/360/411dp, tablet widths 600/840dp, landscape heights 280/320/400dp and wide web windows; add emulator/API coverage as available and report actual coverage precisely. Require no off-screen board, overlapping actionable controls or inaccessible primary actions. Board size must match the adaptive policy's computed maximum, with screen assertions.

Run web unit/type/lint/build/e2e checks; Android engine/UI tests/lint/debug and release builds; physical-device install and interaction tests; inspect crash/ANR logs. Publish 4.0 through a reviewed green PR, synchronize main and verify release assets and deployment. Record screenshots, scenario results, test evidence and remaining device coverage in the release audit. The goal remains active until implementation, device validation and release evidence meet these requirements.

## Approval

Approved: fully redesign modern UI while retaining classic-theme character. The user explicitly requires preservation of current themes and the complete constructor, and permits compatible improvements to DOS and Lines 98 without breaking them.
