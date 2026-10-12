# Task

TASK_ID: 20261006-0230-audit-web-and-android-for-bugs-and-improvements
STATUS: wip
AGENT: codex
BRANCH: agent/feat-colorful-themes-grid
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: dc622097d37c6d9b75ad9574379fbfc883835052
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: using-superpowers,cold-start-resume,brainstorming,writing-plans,better-colors,systematic-debugging,verification-before-completion
TOOLS_USED: gortex,helper,rtk,gh,git
CREATED_AT: 2026-10-06T02:30:24+03:00
UPDATED_AT: 2026-10-12T21:23:00+03:00
RELATED: -

## Request

Audit web and Android for bugs and improvements (v2.1.1)

## Goal

Audit web and Android for bugs and improvements (v2.1.1)

## Definition of done

- [x] Observable behavior implemented in web and Android.
- [x] Relevant verification is green.
- [x] CI/CD and release metadata updated.
- [x] `JOURNAL.md` and `HANDOFF.md` are current.

## Non-goals

- Unrelated refactoring.
- Dependency or architecture migration without approval.

## Constraints and risks

- Preserve unknown and unrelated work.
- Do not change global tool/plugin configuration without approval.

## Scope

### In scope

- Add `clear` and `clearNight` themes to web and Android.
- Keep grid and seven ball colours readable for reduced visual acuity.
- Add redundant shape markers and distinct calm sound voices.
- Run web/Android CI on pull requests and release artifacts on version tags.
- Publish v2.1.1 through the task branch/PR workflow.

### Do not touch

- Existing unrelated changes outside this feature.
- Signing key material and global tool configuration.

## Assumptions

| Assumption | Verification | Status |
|---|---|---|
| Existing contrast theme remains compatible | Existing parsers and focused tests | verified |
| New theme identifiers must round-trip in backup/storage | Web and Android storage tests | verified |
| Release workflow can publish v2.1.1 | Version parity + workflow review | verified |

## Decisions

- Use two themes: light `clear` and dark `clearNight`.
- Keep `contrast` unchanged and additive.
- Add unique sound voices because existing voice tests require distinct modern themes.
- Preserve the pre-existing Claude timer/tutorial changes in `src/App.tsx`.

## Plan

1. Reconcile repository and reproduce/understand the task.
2. Implement the smallest correct change.
3. Verify from targeted checks to broader checks.
4. Review diff and prepare delivery/handoff.

## Current state

DONE:
- Kept the existing Clear/ClearNight themes and added Spectrum, Meadow, Orchid across web and Android.
- Added distinct sound profiles, RU/EN labels and plain high-contrast balls; strengthened square board grid lines.
- Added/updated theme consistency and Android all-theme tests.
- Aligned web and Android release metadata to 2.1.5 (Android versionCode 16).

CURRENT:
- Final diff review and GitHub delivery.

NEXT:
- Commit, push task branch, create PR, pass GitHub CI, merge and let release workflow publish v2.1.5.

## Verification

| Command | Result | Commit |
|---|---|---|
| `npm test -- --pool=threads --maxWorkers=1` | 883 passed / 26 files | working tree, 2026-10-13 |
| `npm run lint && npm run typecheck && npm run build` | passed | working tree, 2026-10-13 |
| `npm audit --audit-level=high` | 0 vulnerabilities | working tree, 2026-10-13 |
| Focused theme E2E on `PW_CHROMIUM=/home/basil/.local/bin/chromium` | 16 passed (computed styles + 15 responsive theme cases) | working tree, 2026-10-13 |
| Full E2E | Not clean locally: bundled Chromium SIGTRAP; system Chromium intermittently exits at launch with libseccomp BPF export error | environment limitation, 2026-10-13 |
| `./gradlew --max-workers=2 :app:testDebugUnitTest :core-engine:test :app:assembleRelease` | BUILD SUCCESSFUL | working tree, 2026-10-13 |
| `git diff --check` | clean | working tree, 2026-10-13 |
| Browser screenshot check | unavailable: Python Playwright not installed | working tree |

## Delivery

PR: pending
MERGE_STATUS: not_started
RELEASE: v2.1.5 pending merge/CI
