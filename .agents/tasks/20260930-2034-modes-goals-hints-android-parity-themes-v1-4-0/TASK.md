# Task

TASK_ID: 20260930-2034-modes-goals-hints-android-parity-themes-v1-4-0
STATUS: wip
AGENT: claude
BRANCH: agent/feat-modes-goals-themes
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: 14260eca78252ede2fdb4699eaf05c69a11c7b04
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: -
CREATED_AT: 2026-09-30T20:34:13+03:00
UPDATED_AT: 2026-09-30T20:34:13+03:00
RELATED: -

## Request

Modes, goals, hints, Android parity, themes (v1.4.0)

## Goal

Modes, goals, hints, Android parity, themes (v1.4.0)

## Definition of done

- [ ] Observable behavior implemented.
- [ ] Relevant verification is green.
- [ ] Documentation/contracts updated if required.
- [ ] `JOURNAL.md` and `HANDOFF.md` are current.

## Non-goals

- Unrelated refactoring.
- Dependency or architecture migration without approval.

## Constraints and risks

- Preserve unknown and unrelated work.
- Do not change global tool/plugin configuration without approval.

## Scope

### In scope

- TODO

### Do not touch

- TODO

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
- Task state created.

CURRENT:
- Understand the request and project.

NEXT:
- Inspect relevant code and define exact verification.

## Verification

| Command | Result | Commit |
|---|---|---|
| TODO | not run | - |

## Delivery

PR: -
MERGE_STATUS: not_started

## Goal / DoD (filled in)
- Modes (classic/easy/blitz/daily), adaptive goals, hint, reliable new-game/reset confirmations on web and Android.
- Android parity: Kotlin seeded engine identical to the web daily game, in-game language switch, wide portrait board,
  Material/Neon/High contrast themes, new-game dialog, Blitz clock, hints.
- DOS look: Russian localisation, animated pretender. Name "Color Lines" everywhere; package keeps basil-as.
- Verified: tsc, oxlint, vitest (557), playwright (41), Android unit tests, gen-android-strings --check.
