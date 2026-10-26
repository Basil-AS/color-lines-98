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
UPDATED_AT: 2026-10-25T02:00:16+04:00
RELATED: https://github.com/Basil-AS/color-lines-98/pull/67

## Request

Major 4.0 redesign using google/artemis and connected ADB phone; fix landscape, board sizing and space distribution across devices

## Goal

Deliver redesigned adaptive Android and web UX, verified on real phone and size/accessibility matrix, with Artemis validation and release 4.0

## Definition of done

- [x] User-approved design and implementation plan cover the full 4.0 objective.
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

- Full approved v4 plan in docs/superpowers/plans/2026-10-25-v4-adaptive-experience.md.

### Do not touch

- All theme identities, complete builder, storage formats and DOS/98 artwork/rules/sounds must remain compatible.

## Current state (authoritative)

PR67 merged as15e2bb05a43099040bf839b5cb955a7a64a10a4b after all required checks, independent reviews and signed33cebf5physical validation. All18themes and builder preserved; assets unchanged. Final modern/DOS/Windows98matrices passed onHuaweiAndroid10,81cellsstable and same fingerprint as3.2.2. Goals/stats/help/nativeBack and style-code roundtrip checked. Backup save picker opened/cancelled; automated tests cover full export/import and gameplay. Full suite at33cebf5:197core/137Android,1026webunit/180Chromium, lint/builds green.

Additional physical tutorial play found an actual blocker: landscape instructions overlay the required line destination. Release run40225026151 was cancelled before publication; Pages main deployment succeeded but will be refreshed by the correction. A touch-based regression fails atstep5 onAPI29/37. Follow-up branch codex/v4-tutorial-reachability reserves separate bounded scrolling space for the tutorial, leaving the game board unobscured. Full suite, independent review and new signed physical candidate required before merging the fix and restarting release4.0.0. No release4.0published yet.

NEXT_ACTION: Poll .agent-cache/v4-tutorial-full.log, verify corrected tutorial through actual emulator/phone touches, create reviewed follow-upPR, merge only greenCI, verify signed release assets/Pages/main and close task state.

## Verification

- Pure-engine197 +Android137 +lint/debug/release passed at33cebf5.
- Web1026unit/180Chromium plus all required PR67CI passed.
- Signedcandidate33cebf5run40223651992,artifact12610632193, projectcert5D3030E9363804C3F732D1A72BB683E1997F73D0454A1362C0FF5BF463271993.
- ActualArtemis modern/DOS/98matrices: .agent-cache/v4-artemis-huawei-{33,dos33,win9833}/.
- Corrected tutorial gesture test red onAPI29/37, fullruninprogress .agent-cache/v4-tutorial-full.log.

## Delivery

PR: https://github.com/Basil-AS/color-lines-98/pull/67
MERGE_STATUS: merged; tutorial correction and release pending

## Latest user steering (2026-10-26)
Basic actions visible and clearly button-shaped; round modern chrome; preserve full themes/builder; fix landscapeSettingsstatusbarshift; reduce padding/maximizefield. Subagents explicitly requested; simplerwebworkusedgpt-6-luna. Implementation/review and full156Android/182browserchecks pass. SignedcandidatewithlatestUI +physicalArtemisbefore/after remain beforemerge/release. PR73scope now includes these corrections, not only tutorial.
