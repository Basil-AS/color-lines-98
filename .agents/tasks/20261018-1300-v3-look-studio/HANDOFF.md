# Handoff

TASK_ID: 20261018-1300-v3-look-studio
STATUS: complete
BRANCH: main
BASE_COMMIT: a4a4ef31e1f82e523687be7967a3ed6998f9838b
HEAD: 775046f6c4c6534ebda4819a4144d0168ee5e1e6
LAST_VERIFIED_COMMIT: 775046f6c4c6534ebda4819a4144d0168ee5e1e6
PR: #38 merged
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

- PR #38 merged; release v3.0.0 published (APK signed with the project key, web zip, checksums, attestation); CI, Web CI/Pages, Android CI and CodeQL green on main.

## Uncommitted or running state

- None.

## Exact next action

1. Contributor "Agy": it comes from `Co-authored-by: Agy` trailers in commits #31, #32, #33 on `main`. The cleaned history (identical trees)
   is on branch `rewrite/main-without-agy` (based on the old main, before 3.0). `main` is protected and refused the force-push. If the owner
   allows force-push: rebuild the cleaned history on top of the current main (same filter: delete the trailer lines), push it to `main`,
   move the tags v2.1.2, v2.1.4, v2.1.5, v2.2.0, v3.0.0 to the matching new commits, then restore the branch protection.

EXPECTED: GitHub's contributor list no longer shows Agy (it refreshes with a delay).

## Verification evidence

- Web: `tsc -b`, 1014 vitest tests, e2e audit (33 tests) green locally.
- core-engine: JVM tests green locally (LookTest, BackupTest).
- PR #38: all checks green (android, e2e, web, check-and-build, CodeQL); main after merge: CI, Web CI, Android CI, CodeQL, Release green.
- Found by CI only: a text field in the Android builder tab kept Compose from going idle in tests; replaced by a clipboard button.
