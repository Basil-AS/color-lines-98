# Task

TASK_ID: 20260930-0200-unify-style-audit
STATUS: wip
AGENT: Claude Code
BRANCH: agent/fix-unify-style-audit
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: 5b19825f9b5b08cbe22e7b6541ee8e786dbe015d
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: -
CREATED_AT: 2026-09-30T02:00:52+03:00
UPDATED_AT: 2026-09-30T02:00:52+03:00
RELATED: -

## Request

unify-style-audit

## Goal

unify-style-audit

## Definition of done

- [x] Sounds: every event has a sound in every look; unused original samples wired; tests.
- [x] Lines 98 matched to the real v5.0 (cyan LED, best|next|score panel, markers on).
- [x] Rules conformance suites (TS + Kotlin); coronation labels fixed.
- [x] One style across web and Android (footer in DOS look, Lines 98 header, square retro shapes).
- [x] Browser e2e tests in CI (layout of all looks, offline PWA, 1992 screen).
- [x] docs/REQUIREMENTS.md, README gallery, manifest screenshots, repo metadata.
- [ ] PR merged, v1.3.0 released.

## Non-goals

- Unrelated refactoring.
- Dependency or architecture migration without approval.

## Constraints and risks

- Preserve unknown and unrelated work.
- Do not change global tool/plugin configuration without approval.

## Scope

### In scope

- Consistency, sounds, mechanics check, requirements audit, e2e tests, documentation.

### Do not touch

- Signing keys, secrets, agent session databases.

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
- Audit fixes and verification complete.

CURRENT:
- PR, CI, merge, tag v1.3.0.

NEXT:
- Optional: real DOS sound samples, Android on a device.

## Verification

| Command | Result | Commit |
|---|---|---|
| `npm test` | 15 files, 441 tests pass | 5b19825 |
| `npm run e2e` | 26 browser tests pass | 5b19825 |
| `npm run lint` / `typecheck` / `build` | clean / pass / pass | 5b19825 |
| `./gradlew :core-engine:test testDebugUnitTest assembleRelease` | 95 + 37 tests pass, APK builds | 5b19825 |

## Delivery

PR: -
MERGE_STATUS: not_started
