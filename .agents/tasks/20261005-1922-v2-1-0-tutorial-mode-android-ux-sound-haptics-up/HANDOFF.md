# Handoff

TASK_ID: 20261005-1922-v2-1-0-tutorial-mode-android-ux-sound-haptics-up
STATUS: wip
BRANCH: agent/feat-v2-1-0-tutorial-ux
BASE_COMMIT: c0065a87f4f7f231d69a752c6cd05813cd3b5e0e
HEAD: c0065a87f4f7f231d69a752c6cd05813cd3b5e0e
LAST_VERIFIED_COMMIT: -
PR: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: scripts/new_task.py
UPDATED_AT: 2026-10-05T19:22:10+03:00

## Read first

1. `AGENTS.md`
2. `.agents/reference/UNIVERSAL_AGENT_BOOTSTRAP.md`
3. This task's `TASK.md`
4. This task's `JOURNAL.md`
5. Relevant git diff, tests and PR/CI

## Done

- Task state initialized.

## Current work

- Request still needs project-specific analysis.

## Uncommitted or running state

- Unknown: inspect `git status` and running processes.

## Verification evidence

- None yet.

## Failed approaches

- None.

## Risks and blockers

- Project context and verification commands may still be unknown.

## Exact next action

COMMAND: `python scripts/agent_context.py --ensure --write-local --write-cache`
EXPECTED: A current repository snapshot, tool capability report and active-task
context without modifying project code.

## Completion path

Implement → targeted verification → full relevant verification → diff review →
update task/journal/handoff → PR or documented local fallback.
