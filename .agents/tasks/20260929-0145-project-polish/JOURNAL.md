# Task journal

Append new entries at the end. Never rewrite old entries except to redact a
secret immediately.

## 2026-09-29T01:45:38+03:00 — task created

AGENT: Claude Code
BRANCH: agent/feat-project-polish
HEAD_BEFORE: ec9d2171d0b57d86651e65b94bb9834768aefff3
HEAD_AFTER: ec9d2171d0b57d86651e65b94bb9834768aefff3
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

## 2026-09-29T01:50:00+03:00 — polish: engine fix, persistence, a11y, CI, docs

AGENT: Claude Code
BRANCH: agent/feat-project-polish
HEAD_BEFORE: 36263ab
HEAD_AFTER: ec9d217
STATUS: DONE
ACTIVE_SKILLS: -
TOOLS_USED: gortex (explore/read), rtk, npm, gradlew --offline, playwright via uv --with
CHANGED: src/engine/*, src/storage.ts, src/App.tsx, src/App.css, tests/*, android core-engine LineDetector + tests, .github/workflows/*, README.md, docs/PLAN_AND_SPECS.md
COMMANDS: see TASK.md verification table
RESULT: All checks green; regression tests were red before the fix (3 lines instead of 1 for a 7-ball "/" line).
TRIED: -
DECISION: Start line runs only at their first cell (drops the visited grid). Persist only board/score/next colors; undo history is not saved. Web-ci.yml left untouched (already valid); ci.yml became a repo-hygiene gate.
SOURCE: code + tests
NEXT: User approval to push branch and open PR.

## 2026-09-29T02:20:00+03:00 — delivered: PR #1/#2 merged, v1.0.0 released

AGENT: Claude Code
BRANCH: main
HEAD_BEFORE: 36263ab
HEAD_AFTER: e3add23
STATUS: DONE
ACTIVE_SKILLS: -
TOOLS_USED: gh, git, rtk
CHANGED: GitHub state only (PRs, tag v1.0.0, release assets, Pages deploy)
COMMANDS: gh pr create/merge, git tag -a v1.0.0, git push origin v1.0.0
RESULT: CI, Android CI, Web CI/Pages deploy and Release Pipeline green; release has app-release.apk (debug-signed) and color-lines-web-standalone.zip; https://basil-as.github.io/color-lines-98/ serves the new bundle.
TRIED: Chained `git switch -c && git commit` was blocked by the workstation guard (branch checked before the chain runs); split into separate calls.
DECISION: Rebase-merge to keep conventional commits; release APK is signed with the debug key (no release keystore exists).
SOURCE: gh run/release output
NEXT: none required.

