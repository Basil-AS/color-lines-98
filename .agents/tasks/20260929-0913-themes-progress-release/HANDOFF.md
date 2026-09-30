# Handoff

TASK_ID: 20260929-0913-themes-progress-release
STATUS: wip
BRANCH: agent/feat-themes-progress
BASE_COMMIT: 7b914001036d47d37fda55fd1b778be24470ac61
HEAD: 7b91400
LAST_VERIFIED_COMMIT: 7b91400
PR: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: scripts/new_task.py
UPDATED_AT: 2026-09-29T09:13:15+03:00

## Read first

1. `AGENTS.md`
2. `.agents/reference/UNIVERSAL_AGENT_BOOTSTRAP.md`
3. This task's `TASK.md`
4. This task's `JOURNAL.md`
5. Relevant git diff, tests and PR/CI

## Done

- Themes, progress, spawn preview, ru/en, package rename, tests, CI/release naming.

## Current work

- PR, CI, merge and tag v1.1.0.

## Uncommitted or running state

- Unknown: inspect `git status` and running processes.

## Verification evidence

- See TASK.md.

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
