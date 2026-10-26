# Task

TASK_ID: 20261025-v4-adaptive-experience
STATUS: wip
AGENT: codex
BRANCH: codex/v4-tutorial-reachability
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: 1da4f981cb781553a1f8530682138e821208e838
LAST_CHECKPOINT_COMMIT: 15e2bb05
LAST_VERIFIED_COMMIT: 33cebf5
PROFILE: balanced
ACTIVE_SKILLS: cold-start-resume, brainstorming, safe-external-fetch
TOOLS_USED: git, adb, python3, pinned Artemis source
CREATED_AT: 2026-10-25T02:00:16+04:00
UPDATED_AT: 2026-10-27
RELATED: https://github.com/Basil-AS/color-lines-98/pull/67

## Request

Major 4.0 redesign using google/artemis and connected ADB phone; fix landscape, board sizing and space distribution across devices

## Goal

Deliver redesigned adaptive Android and web UX, verified on real phone and size/accessibility matrix, with Artemis validation and release 4.0

## Definition of done

- [x] User-approved design and implementation plan cover the full 4.0 objective.
- [x] Adaptive game layout maximizes board size for safe viewport constraints, including landscape, narrow windows and tablets.
- [ ] Modern game HUD, controls, settings, appearance studio, statistics, goals, help and results use a coherent accessible visual system.
- [x] Classic-theme treatment follows the user's design choice; existing themes and customization remain available.
- [x] Saved games, history, progression, backups, game rules, languages, audio and haptics retain behavior and compatibility.
- [x] Modern Kotlin/Compose/Android toolchain adopted coherently with compatibility evidence.
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

- Full approved v4 plan in docs/superpowers/plans/2026-10-25-v4-adaptive-experience.md.

### Do not touch

- All theme identities, complete builder, storage formats and DOS/98 artwork/rules/sounds must remain compatible.

## Current state (authoritative)

PR67 merged as15e2bb05. PR73 draft on codex/v4-tutorial-reachability contains corrective layout/dialog work and the latest requested strategy coach/swipe tabs. All18themes and fullconstructor preserved, originalart/audioassets unchanged. Current source fullAndroid202core+165app tests/lint/debug/release green;185realChromium tests green. Finalweb unit isolated rerun after tiny horizontal-only tabrail fix is running. Six real-engine bilingual coach puzzles accept equivalent open-end/diagonal/corridor solutions; all6completion, wrong/hint, savedparty/history restoration and training-time isolation tested.

Physicalphone currently installed053e27c project-signed4.0candidate, which lacks latestswipes/coach. User continuesplaying; baselineparty must be captured afresh and preserved, never assume oldscore42fingerprint. LatestphysicalArtemis captures .agent-cache/v4-artemis-ux-actions. FinalcandidateCI required beforemerge73. Latestphysicaltests remainpendingwhileHuaweiabsent; userexplicitlyinstructedcommit/push/PR/mergeon2026-10-27. Release40225026151cancelled beforepublication; no4.0releaseyet. WorkflowonlyPR68–72green, mergeafter73.

NEXT_ACTION: Finish isolatedwebunit, checkpoint/push source, dispatchsignedcandidate; verify certificate+Huawei Artemis coach/swipes/insets/partyrestoration, then green PR73/remainingPRs/release/Pages/main.

## Delivery

PR: https://github.com/Basil-AS/color-lines-98/pull/67
MERGE_STATUS: merged; tutorial correction and release pending

## Latest user steering (2026-10-26)
Basic actions visible and clearly button-shaped; round modern chrome; preserve full themes/builder; fix landscapeSettingsstatusbarshift; reduce padding/maximizefield. Subagents explicitly requested; simplerwebworkusedgpt-6-luna. Implementation/review and full156Android/182browserchecks pass. SignedcandidatewithlatestUI +physicalArtemisbefore/after remain beforemerge/release. PR73scope now includes these corrections, not only tutorial.
