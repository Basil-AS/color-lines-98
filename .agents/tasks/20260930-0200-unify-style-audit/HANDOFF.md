# Handoff

TASK_ID: 20260930-0200-unify-style-audit
STATUS: wip
BRANCH: agent/fix-unify-style-audit
BASE_COMMIT: 5b19825f9b5b08cbe22e7b6541ee8e786dbe015d
HEAD: 5b19825
LAST_VERIFIED_COMMIT: 5b19825
PR: -
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: scripts/new_task.py
UPDATED_AT: 2026-09-30T02:00:52+03:00

## Read first

1. `AGENTS.md`
2. `.agents/reference/UNIVERSAL_AGENT_BOOTSTRAP.md`
3. This task's `TASK.md`
4. This task's `JOURNAL.md`
5. Relevant git diff, tests and PR/CI

## Done

- Sounds for every event, Lines 98 as the real original, rules suites, e2e in CI, unified style, requirements audit.

## Current work

- PR, CI, merge and tag v1.3.0.

## Uncommitted or running state

- Unknown: inspect `git status` and running processes.

## Verification evidence

- See TASK.md and docs/REQUIREMENTS.md.

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
