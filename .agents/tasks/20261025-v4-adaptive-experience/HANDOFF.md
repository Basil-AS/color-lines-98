# Handoff

TASK_ID: 20261025-v4-adaptive-experience
STATUS: wip
BRANCH: codex/v4-adaptive-experience
BASE_COMMIT: 1da4f981cb781553a1f8530682138e821208e838
HEAD: 71ef13e9a4b80f91fcc9744655ad6aaaeabd12e0 plus ongoing v4 changes
PR: https://github.com/Basil-AS/color-lines-98/pull/67 (draft)
ACTIVE_SKILLS: subagent-driven-development, frontend-design, systematic-debugging, test-driven-development, verification-before-completion
LAST_VERIFIED_COMMIT: 71ef13e9a4b80f91fcc9744655ad6aaaeabd12e0
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
