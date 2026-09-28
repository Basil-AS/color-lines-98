# Task

TASK_ID: 20260929-0145-project-polish
STATUS: done
AGENT: Claude Code
BRANCH: agent/feat-project-polish
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: ec9d2171d0b57d86651e65b94bb9834768aefff3
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: -
CREATED_AT: 2026-09-29T01:45:38+03:00
UPDATED_AT: 2026-09-29T01:45:38+03:00
RELATED: -

## Request

project-polish

## Goal

project-polish

## Definition of done

- [x] Line-scoring bug fixed in TypeScript and Kotlin engines with regression tests.
- [x] Web game persists, validates stored data, is keyboard/screen-reader usable.
- [x] CI no longer masks failures (`|| true`, skip-if-missing fallbacks removed).
- [x] README and specs match the real feature set and scoring table.
- [x] Lint (0 warnings), typecheck, 33 Vitest tests, web build, Kotlin engine + app unit tests, debug and release APK builds all green.
- [x] `JOURNAL.md` and `HANDOFF.md` are current.
- [x] PR #1 and #2 merged, release v1.0.0 published, GitHub Pages live.

## Non-goals

- Unrelated refactoring.
- Dependency or architecture migration without approval.

## Constraints and risks

- Preserve unknown and unrelated work.
- Do not change global tool/plugin configuration without approval.

## Scope

### In scope

- Engine correctness (TS + Kotlin), web persistence/a11y, CI honesty, documentation accuracy.

### Do not touch

- Reference APK/APKS files in the repo root (gitignored, local only), original sound/sprite assets.

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
- Engine fix, persistence, a11y, CI and docs; all verification green.

CURRENT:
- Nothing pending.

NEXT:
- Optional: move-animation, DataStore cleanup, web UI tests (needs new dev dependencies, approval).

## Verification

| Command | Result | Commit |
|---|---|---|
| `npm run lint` | 0 warnings | ec9d217 |
| `npm run typecheck` | pass | ec9d217 |
| `npm test` | 2 files, 33 tests pass | ec9d217 |
| `npm run build` | pass | ec9d217 |
| `./gradlew :core-engine:test testDebugUnitTest assembleDebug assembleRelease` | BUILD SUCCESSFUL | ec9d217 |
| Playwright smoke (Chromium) | keyboard, persistence, corrupt storage, dialogs, 3 themes OK, 0 console errors | ec9d217 |
| `validate_agent_state.py --soft`, `self_test.py`, `py_compile`, `shellcheck` | pass | ec9d217 |

## Delivery

PR: https://github.com/Basil-AS/color-lines-98/pull/1 (+ #2 version bump)
MERGE_STATUS: merged (rebase)
