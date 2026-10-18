# Task

TASK_ID: 20261018-1300-v3-look-studio
STATUS: complete
AGENT: claude
BRANCH: claude/v3-appearance-studio
BASE_REF: refs/remotes/origin/main
BASE_COMMIT: a4a4ef31e1f82e523687be7967a3ed6998f9838b
LAST_CHECKPOINT_COMMIT: -
LAST_VERIFIED_COMMIT: 775046f6c4c6534ebda4819a4144d0168ee5e1e6
PROFILE: balanced
ACTIVE_SKILLS: -
TOOLS_USED: git,playwright,gradle
CREATED_AT: 2026-10-18T13:00:00+03:00
UPDATED_AT: 2026-10-18T16:30:00+03:00
RELATED: 20261006-0230-audit-web-and-android-for-bugs-and-improvements

## Request

Version 3.0: a set of themes with switches inside them (ball depth, ball contrast, markers, grid styles and more), a
powerful builder with all kinds of adaptations; check the analysis functions, styles and buttons, that nothing overlaps.

## Goal

Ship Color Lines 3.0.0 on web and Android: look studio, theme collections, custom theme builder, colour-vision preview,
look codes, backups of looks, layout and analytics audit.

## Definition of done

- [x] Look studio and builder work on web and Android.
- [x] Layout audit (e2e) and analytics audit green; fixes applied.
- [x] CI green on the pull request, merged, release v3.0.0 published, CI/CD green on main.
- [x] `JOURNAL.md` and `HANDOFF.md` are current.

## Non-goals

- Changing the artwork of Lines 98, Lines 98+ and Color Lines 1992.

## Constraints and risks

- The Android app module cannot be built in the cloud session (no Android SDK): CI compiles it. Pure logic lives in `core-engine`, tested on the JVM.
- Removing the contributor "Agy" needs a force-push to the protected `main` (see HANDOFF).
