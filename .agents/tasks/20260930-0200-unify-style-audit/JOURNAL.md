# Task journal

Append new entries at the end. Never rewrite old entries except to redact a
secret immediately.

## 2026-09-30T02:00:52+03:00 — task created

AGENT: Claude Code
BRANCH: agent/fix-unify-style-audit
HEAD_BEFORE: 5b19825f9b5b08cbe22e7b6541ee8e786dbe015d
HEAD_AFTER: 5b19825f9b5b08cbe22e7b6541ee8e786dbe015d
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

## 2026-09-30T05:00:00+03:00 — consistency and requirements audit

AGENT: Claude Code
BRANCH: agent/fix-unify-style-audit
HEAD_BEFORE: e9f2f01
HEAD_AFTER: 5b19825
STATUS: DONE
ACTIVE_SKILLS: -
TOOLS_USED: WebSearch, unar/7z on the original Lines 98 v5.0 installer, playwright (python + @playwright/test), gradlew --offline
CHANGED: src/*, tests/*, e2e/*, android/*, .github/workflows/*, docs/*, README.md
COMMANDS: see TASK.md
RESULT: Found real gaps: no sound for win/record/start/level-up, several original samples unused, the coronation labels swapped, and a wrong claim about Lines 98 (the original v5.0 does show small balls on the board and uses cyan LEDs). All fixed and covered by tests.
TRIED: Extracting assets from the Lines 98 v5.0 installer: it is a closed Wise package, only splash BMPs inside.
DECISION: Keep the 1992 DOS look without spawn markers, all other looks with markers by default.
SOURCE: archive.org lines98v50 screenshot, previous research
NEXT: PR, CI, merge, tag v1.3.0.

