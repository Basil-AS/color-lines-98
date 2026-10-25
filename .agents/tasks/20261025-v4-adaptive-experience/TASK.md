# Task

TASK_ID: 20261025-v4-adaptive-experience
STATUS: wip
AGENT: codex
BRANCH: codex/v4-adaptive-experience
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: 1da4f981cb781553a1f8530682138e821208e838
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: -
PROFILE: balanced
ACTIVE_SKILLS: cold-start-resume, brainstorming, safe-external-fetch
TOOLS_USED: git, adb, python3, pinned Artemis source
CREATED_AT: 2026-10-25T02:00:16+04:00
UPDATED_AT: 2026-10-25T02:00:16+04:00
RELATED: -

## Request

Major 4.0 redesign using google/artemis and connected ADB phone; fix landscape, board sizing and space distribution across devices

## Goal

Deliver redesigned adaptive Android and web UX, verified on real phone and size/accessibility matrix, with Artemis validation and release 4.0

## Definition of done

- [ ] User-approved design and implementation plan cover the full 4.0 objective.
- [ ] Adaptive game layout maximizes board size for safe viewport constraints, including landscape, narrow windows and tablets.
- [ ] Modern game HUD, controls, settings, appearance studio, statistics, goals, help and results use a coherent accessible visual system.
- [ ] Classic-theme treatment follows the user's design choice; existing themes and customization remain available.
- [ ] Saved games, history, progression, backups, game rules, languages, audio and haptics retain behavior and compatibility.
- [ ] Modern Kotlin/Compose/Android toolchain adopted coherently with compatibility evidence.
- [ ] Web presentation and responsive behavior align with the redesigned product.
- [ ] Artemis real-phone scenarios verify play, controls, settings/looks, statistics, backups and orientation without crashes or overlaps.
- [ ] Size/font/inset/API test matrix and screenshot evidence cover phones/tablets, both orientations, short/narrow windows and accessibility cases.
- [ ] Meaningful regression tests cover layout and discovered faults; full web and Android verification passes.
- [ ] Version 4.0.0 builds, installs and runs on the connected phone; signed release and web assets are published through a green PR and verified.
- [ ] Main synchronized and task/journal/handoff updated with evidence and precise coverage limits.

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

## Reconciled scope and current state

Original 4.0 goal active; preceding goal turn completed PR synchronization work but did not implement 4.0. Current branch codex/v4-adaptive-experience starts at clean main 1da4f98. No other worktrees or hidden redesign found. Phone connected over USB and Wi-Fi (same Huawei Android 10 device). App 3.2.2 baseline screenshots saved; landscape rail and wrapping problems confirmed.

Proposed design: docs/superpowers/specs/2026-10-25-v4-adaptive-experience-design.md. Design-direction question sent asynchronously; approval pending. No application code edited. Artemis pinned at 351ca8422f7b5b54e80a9c1ce03a222e02415b6b; archive SHA256 8ad92b1e4ee293d64767204e73e20cdce1263a3208122e4b5c2645ec7b29ab50, cached outside git. Local isolated pip installation running as exec session 92071, log .agent-cache/artemis-install.log.

Next: inspect live installer session; review Artemis diagnostic entry points; wait for the user's design choice before implementation. Then produce implementation plan and run red/green layout regressions, implement full adaptive UX, verify device/size/accessibility matrix and publish 4.0 via green PR.

## Preparation verified

- Artemis installation completed, dependency check passes; source pin recorded in LOCK.json.
- Artemis AndroidAdbDriver captured a real phone landscape screenshot; full agent doctor is limited by absent model credentials and scrcpy.
- Stable portrait and landscape baselines plus UI hierarchy/metrics saved. Original device rotation preferences restored.
- No application implementation yet; design approval is still pending.

## Goal completion audit (pre-implementation)

Adaptive layout, visual redesign, modern toolchain migration, full screen audits, Artemis user scenarios, size/API matrix, 4.0 version/release and physical-device validation of the new build: **not implemented / not verified**. Current artifacts prove baseline observation and local tooling only. Do not use baseline tests or driver connectivity as evidence that 4.0 is achieved. Saved-data compatibility has not been revalidated against redesign because no redesign exists yet.

## Current state (authoritative)

BLOCKED: user design confirmation required by brainstorming skill; pending through four goal turns. No 4.0 implementation exists. Preparation complete: pinned Artemis driver verified on phone, both orientation baselines captured, local web tests/typecheck/build pass, Android UI tests/lint/debug build pass. Pure-engine suite has one invalid-fixture failure (ring versus persisted contrast); production migration checked separately and behaves correctly. CI omits pure-engine test task. These two test/CI issues remain for implementation.

Next user action: confirm proposed design, provide changes, or explicitly authorize proceeding with design choices without intermediate approvals. After response, retain full scope, write implementation plan, fix fixture/CI gate, then implement/validate/release 4.0.

## Approval and execution supersede earlier blocker

User approved the modern redesign and explicitly required all current themes, full theme builder and DOS/Lines 98 identity to remain intact. Execute full plan docs/superpowers/plans/2026-10-25-v4-adaptive-experience.md without further routine approval gates. Start with pure-engine fixture/CI and adaptive layout regressions.
