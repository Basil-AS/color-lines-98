# Task

TASK_ID: 20260929-0913-themes-progress-release
STATUS: wip
AGENT: Claude Code
BRANCH: agent/feat-themes-progress
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: 7b914001036d47d37fda55fd1b778be24470ac61
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: -
CREATED_AT: 2026-09-29T09:13:15+03:00
UPDATED_AT: 2026-09-29T09:13:15+03:00
RELATED: -

## Request

themes-progress-release

## Goal

themes-progress-release

## Definition of done

- [x] Four themes (modern dark/light, Lines 98, Color Lines 1992) on web and Android.
- [x] Progress system (levels, achievements, streaks, records, history) on both platforms.
- [x] Spawn preview, ru/en with auto-detection, package io.github.basil_as.basillines, name Basil Lines.
- [x] Web 308 tests, Kotlin engine 57, Android UI 26; lint, typecheck, builds green.
- [x] Release workflow names files BasilLines-<version>.*; README/docs updated.
- [ ] PR merged, tag v1.1.0 published, Pages verified.

## Non-goals

- Unrelated refactoring.
- Dependency or architecture migration without approval.

## Constraints and risks

- Preserve unknown and unrelated work.
- Do not change global tool/plugin configuration without approval.

## Scope

### In scope

- Themes, progress/statistics, spawn preview, i18n, packaging/naming, CI/release, tests.

### Do not touch

- Reference APK/APKS in the repo root (gitignored), signing keys/secrets.

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
- Implementation and verification complete on branch agent/feat-themes-progress.

CURRENT:
- PR, CI, merge, tag v1.1.0.

NEXT:
- Verify Pages and release assets after merge.

## Verification

| Command | Result | Commit |
|---|---|---|
| `npm test` | 8 files, 308 tests pass | 7b91400 |
| `npm run lint` / `typecheck` / `build` | clean / pass / pass | 7b91400 |
| `./gradlew :core-engine:test` | 57 tests pass | 7b91400 |
| `./gradlew testDebugUnitTest assembleRelease` | 26 Robolectric UI tests pass, APK builds | 7b91400 |
| `gen-android-strings.mjs --check`, `validate_agent_state.py` | pass | 7b91400 |

## Delivery

PR: -
MERGE_STATUS: not_started
