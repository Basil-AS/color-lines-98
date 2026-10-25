# Handoff

TASK_ID: 20261025-v4-adaptive-experience
STATUS: wip
BRANCH: codex/v4-adaptive-experience
BASE_COMMIT: 1da4f981cb781553a1f8530682138e821208e838
HEAD: e3ca4e7 (implementation744c49f plus CI provisioning fix)
PR: https://github.com/Basil-AS/color-lines-98/pull/67 (draft)
ACTIVE_SKILLS: subagent-driven-development, frontend-design, systematic-debugging, test-driven-development, verification-before-completion
LAST_VERIFIED_COMMIT: 744c49f7e7e76d3200bb3c84a3b17c307567736b
PROFILE: balanced
TOOLS_USED: Gradle, npm, Playwright, Artemis AndroidAdbDriver, ADB
UPDATED_AT: 2026-10-25

User approved modern redesign. Explicit constraint: preserve every existing theme and theme builder; preserve DOS/98 identity and functionality, compatible improvements allowed. Full plan: docs/superpowers/plans/2026-10-25-v4-adaptive-experience.md. Task1 committed: invalid core fixture fixed and core tests required in all Android CI/release workflows, 197 tests green. Task2 Android adaptive modern screen in progress, policy 9 tests passed; screen regression fixes undergoing focused verification. Web agent owns App.tsx/App.css/e2e/adaptive.spec.ts, modern geometry and classic compatibility checks ongoing. Toolchain research complete, upgrade not applied. Supporting screens/lifecycle, device matrix and release remain.

Runtime: JAVA_HOME=$PWD/.agent-cache/tools/temurin21/jdk-21.0.13+9/Contents/Home; ANDROID_HOME=$PWD/.agent-cache/android-sdk; ANDROID_USER_HOME=$PWD/.agent-cache/android-user; GRADLE_USER_HOME=$PWD/.agent-cache/gradle-user. Gradle override -Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8 required on macOS. No parallel Gradle jobs. Node bundled runtime and npm in .agent-cache/bin.

Phone USB X6H4C20B16000698, Huawei Android10. Installed release signer differs from debug: never uninstall release or erase progress. Artemis driver verified screenshot; autonomous model credentials absent. Cache contains baseline images and logs. Restore rotation settings after probes (accelerometer_rotation=1,user_rotation=0).

Next: verify AdaptiveGameScreenTest semantics/score/sticky actions fixes, full Android tests, review then checkpoint. Continue plan through signed release/PR. Preserve unknown files; no direct main commits.

## Exact next action

COMMAND: `git status --short --branch`
EXPECTED: v4 implementation changes on codex/v4-adaptive-experience; fresh full Android suite/build/lint green, web180browser/1026unit green. Reconcile/poll live captures before restart. Need final source checkpoint/push and signed CI candidate; physical Huawei currently disconnected.

## Latest evidence
Full source checkpoint744c49f pushed to PR67; CI setup fixe3ca4e7 now provisioning correctly. Signed manual Android CI run40196378487 live (tests finished, lint/build next; candidate job follows). API37tablet840×1280/1280×840 at160dpi also captured by Artemis,81labelledcells in all4poses; sizes/density restored. Fresh whole-branch independent review final_v4_review passes with no actionable defects, conditional release gates remain. Realphone absent in both native and isolated libusb ADB scans; isolated server5038 stopped. No reinstall/uninstall on realphone.

## Huawei and DOS checkpoint (2026-10-25 23:00 Dubai)
Huawei USB authorized; signed142dc8e candidate updated3.2.2 preserving exact board fingerprint. Physical modern + originalWindows98 matrices all4poses81cellsstable; Studio/builder opened; original theme/font/rotation restored. Current PID logcat711lines noFATAL/ANR marker(API29hasnoexit-info). DOS followup RED3/4 baseline then GREEN4/4; full197core/137Android, lintDebug, debug/release builds pass (.agent-cache/v4-dos-full-green.log). API37 DOS Artemis matrix stable, screenshotfont2 fullFkeys+goals, nativeHelpBack manuallyreturnsfield. Independent final_v4_review approved scopedfix. Next commit/push, dispatch new signedcandidate, install overrealphone then repeatclassicDOS/98physical checks, mergePR67 onlyafterallchecks, verifyRelease/Pagesandmain. Do notinstall localdebug onphone.
