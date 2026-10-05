# Task journal

Append new entries at the end. Never rewrite old entries except to redact a
secret immediately.

## 2026-10-05T19:22:10+03:00 — task created

AGENT: claude
BRANCH: agent/feat-v2-1-0-tutorial-ux
HEAD_BEFORE: c0065a87f4f7f231d69a752c6cd05813cd3b5e0e
HEAD_AFTER: c0065a87f4f7f231d69a752c6cd05813cd3b5e0e
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

- 2026-10-05T19:30+03:00 Branch agent/feat-v2-1-0-tutorial-ux created from main (c0065a8). TASK.md scope written (12 items). Delegated read-only survey of all 12 items to agy (agy:agy-rescue); waiting for report. Owner rule: routine -> agy; architecture/debug/verification -> Claude.

- 2026-10-05T19:55+03:00 Baseline green (web 813 tests, Android unit exit 0). Delegated to agy (own worktrees, results applied by me after review): (1) debug-text + name-entry UI removal (applied, reviewed diff), (2) haptics patterns + HapticsTest (applied, reviewed), (3) Kotlin tower port, (4) Android test updates + Updates helper tests, (5) web game-over flow port, (6) Kotlin tutorial model port. Note: agy jobs 1+2 shared a single worktree (harness symlink) - files were disjoint, no conflict.
- Done by me: Android game-over flow without auto dialog (GameOverActions, showResult, folded mode picker), web pretender tower (towerRise, stretched layout strip; checked visually by rendering draws), in-app update (UpdateInstaller: checksum from SHA256SUMS.txt + signature/version check + FileProvider + dialog stages), SoundManager rework (loading off the UI thread, stale-pool listener guard, warm-up, per-version cache), landscape immersive, tutorial model (src/tutorial.ts + tests).
- Verified facts: agy survey claims checked against code; the 1992 original has a tall king pillar and a short pretender pedestal in layout.png; 'pretender tower grows with score' is NOT verifiable offline from the assets - implemented as owner described (rise = maxRise * score / kingScore).
- Open: Android tower/portrait layout equal-scale heroes, tutorial UI (web + Android), device checks (sound, vibration) cannot be run here.

- 2026-10-05T21:10+03:00 All 12 requests implemented on agent/feat-v2-1-0-tutorial-ux (version 2.1.0). Fresh verification in the main tree: vitest 850 passed, tsc clean, oxlint clean, playwright e2e 67 passed, Android testDebugUnitTest + core-engine test 241 tests 0 failures, lintDebug OK, assembleDebug OK. Tutorial driven in a real browser (Playwright). agy findings that were real: isAllowedDownloadUrl accepted https://github.com:pw@evil.com (fixed with java.net.URI), DOS theme had no Result action (added). Not verifiable here: sound/vibration feel on a device, TalkBack, in-app install flow on a phone, landscape immersive on a real phone.
