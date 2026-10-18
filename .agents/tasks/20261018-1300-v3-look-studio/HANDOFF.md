# Handoff

TASK_ID: 20261018-1300-v3-look-studio
STATUS: in_progress
BRANCH: claude/v3-appearance-studio
BASE_COMMIT: a4a4ef31e1f82e523687be7967a3ed6998f9838b
HEAD: -
LAST_VERIFIED_COMMIT: -
PR: #38 open
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: git, playwright, gradle
UPDATED_AT: 2026-10-18T16:30:00+03:00

## Read first

1. `AGENTS.md`
2. `.agents/reference/UNIVERSAL_AGENT_BOOTSTRAP.md`
3. This task's `TASK.md` and `JOURNAL.md`
4. PR #38 diff and CI

## Done

- Look studio, theme collections, custom builder, colour-vision preview, look codes and backup of looks on web and Android.
- e2e layout audit and analytics audit; fixes for ring size, thousands separators, efficiency floor.

## Current work

- Getting PR #38 green (Android compile is only checked by CI), then merge and release v3.0.0.

## Uncommitted or running state

- None.

## Next action (exact)

1. Read the CI result of PR #38 (`get_check_runs`); fix failures, push.
2. When green: squash-merge, confirm release v3.0.0 and CI/CD on main.
3. Contributor "Agy": it comes from `Co-authored-by: Agy` trailers in commits #31, #32, #33 on `main`. The cleaned history (identical trees)
   is on branch `rewrite/main-without-agy`; `main` is protected and refused the force-push. Once the owner allows force-push:
   `git push --force origin rewrite/main-without-agy:main`, move tags v2.1.2, v2.1.4, v2.1.5, v2.2.0 (and v3.0.0 if cut) to the matching new
   commits, rebase open branches. Expected: GitHub's contributor list no longer shows Agy (it refreshes with a delay).

## Verification evidence

- Web: `tsc -b`, 1014 vitest tests, e2e audit (33 tests) green locally.
- core-engine: JVM tests green locally (LookTest, BackupTest).
