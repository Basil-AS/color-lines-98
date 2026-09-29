# Task

TASK_ID: 20260930-0120-original-styles-pwa
STATUS: wip
AGENT: Claude Code
BRANCH: agent/feat-original-styles
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: fdd509df0f9252e1464a0d1ff5309426ef2a2cac
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: -
CREATED_AT: 2026-09-30T01:20:27+03:00
UPDATED_AT: 2026-09-30T01:20:27+03:00
RELATED: -

## Request

original-styles-pwa

## Goal

original-styles-pwa

## Definition of done

- [x] Game renamed Color Lines / Цветные линии; Basil only in the package id.
- [x] Original 1992 screen ported (web canvas + Android Compose) from lines.lib artwork.
- [x] Lines 98 look with the original bevelled board and LED digits; sounds per look.
- [x] Mechanics checked against sources (docs/ORIGINALS.md); fresh-game line fix.
- [x] Analysis of games (web + Android); PWA for iOS/Android/Windows/macOS with offline.
- [x] Web 399, Kotlin engine 85, Android UI 34 tests; lint/typecheck/builds green.
- [ ] PR merged, v1.2.0 released, Pages/PWA verified.

## Non-goals

- Unrelated refactoring.
- Dependency or architecture migration without approval.

## Constraints and risks

- Preserve unknown and unrelated work.
- Do not change global tool/plugin configuration without approval.

## Scope

### In scope

- Naming, original artwork/sounds/mechanics, analytics, PWA, tests, docs, release.

### Do not touch

- Signing keys, secrets, the user's agent session databases (not read).

## Assumptions

| Assumption | Verification | Status |
|---|---|---|
| TODO | TODO | unknown |

## Decisions

- TODO

## Plan

1. Reconcile repository and reproduce/understand the task.
2. Implement the smallest correct change.
3. Verify from targeted checks to broader checks.
4. Review diff and prepare delivery/handoff.

## Current state

DONE:
- Implementation and verification complete.

CURRENT:
- PR, CI, merge, tag v1.2.0.

NEXT:
- Optional: Android Lines 98 board artwork, real DOS sound samples, top-ten name entry on game over.

## Verification

| Command | Result | Commit |
|---|---|---|
| `npm test` | 13 files, 399 tests pass | fdd509d |
| `npm run lint` / `typecheck` / `build` | clean / pass / pass | fdd509d |
| `./gradlew :core-engine:test testDebugUnitTest assembleRelease` | 85 + 34 tests pass, APK builds | fdd509d |
| Playwright (Chromium) | DOS screen matches original layout; PWA registers, caches 63 files, reloads offline | fdd509d |
| `gen-android-strings.mjs --check`, `validate_agent_state.py` | pass | fdd509d |

## Delivery

PR: -
MERGE_STATUS: not_started
