# Task journal

Append new entries at the end. Never rewrite old entries except to redact a
secret immediately.

## 2026-09-29T09:13:15+03:00 — task created

AGENT: Claude Code
BRANCH: agent/feat-themes-progress
HEAD_BEFORE: 7b914001036d47d37fda55fd1b778be24470ac61
HEAD_AFTER: 7b914001036d47d37fda55fd1b778be24470ac61
STATUS: INFO
ACTIVE_SKILLS: -
TOOLS_USED: scripts/new_task.py
CHANGED: task state only
COMMANDS: task creation
RESULT: Task directory initialized.
TRIED: -
DECISION: Use canonical task files for cold-start recovery.
SOURCE: user request + repository state
NEXT: Inspect project and refine TASK.md.

## 2026-09-29T10:30:00+03:00 — themes, progress, spawn preview, i18n, package rename

AGENT: Claude Code
BRANCH: agent/feat-themes-progress
HEAD_BEFORE: a9884ba
HEAD_AFTER: 7b91400
STATUS: DONE
ACTIVE_SKILLS: better-interface (workflow reference)
TOOLS_USED: gortex read, rtk, npm, gradlew --offline, playwright via uv --with, node --experimental-strip-types
CHANGED: src/*, tests/*, android/*, scripts/gen-android-strings.mjs, .github/workflows/*, README.md
COMMANDS: see TASK.md
RESULT: All checks green. Robolectric caught a real crash (AudioTrack.play on an uninitialised track in the DOS theme); fixed.
TRIED: Android emulator (sdkmanager cmdline-tools + system image) - dropped on user request, removed again.
DECISION: Original 1992/Lines 98 binaries are not available offline, so those themes recreate the look from public descriptions and the existing sprites; documented in README.
SOURCE: user request
NEXT: PR, CI, merge, tag v1.1.0.

