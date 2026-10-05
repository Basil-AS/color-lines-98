# Task

TASK_ID: 20261005-1922-v2-1-0-tutorial-mode-android-ux-sound-haptics-up
STATUS: wip
AGENT: claude
BRANCH: agent/feat-v2-1-0-tutorial-ux
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: c0065a87f4f7f231d69a752c6cd05813cd3b5e0e
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: -
CREATED_AT: 2026-10-05T19:22:10+03:00
UPDATED_AT: 2026-10-05T19:22:10+03:00
RELATED: -

## Request

v2.1.0: tutorial mode, Android UX, sound/haptics, update-in-app

## Goal

v2.1.0: tutorial mode, Android UX, sound/haptics, update-in-app

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

Owner request (2026-10-05), 12 items:
1. Tutorial (training) mode.
2. Android DOS theme: sprites must be the same visual size (small sprite is over-stretched).
3. Update straight from the app (Android).
4. Sound: freezes / late start - audit and fix. Haptics: audit, richer/nicer/stronger.
5. Remove debug-looking text from the app ("signed with release key ...").
6. Name entry: justify or remove (no server).
7. King / pretender towers: research original logic (record level vs current score, tower grows) and reproduce.
8. Android landscape: use full height (hide status/notification bar, immersive).
9. After game over: new-game dialog is too intrusive - do not auto-cover the screen.
10. Score panel centre is empty: put a "New game" button there after game over.
11. Mode picker must not be forced on every new game.

Delegation (owner, this session): routine (tests, boilerplate, mass renames, draft surveys) -> agy; I do architecture, hard debugging, verification of agy output.

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
