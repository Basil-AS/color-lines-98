# Task journal

Append new entries at the end. Never rewrite old entries except to redact a
secret immediately.

## 2026-09-30T01:20:27+03:00 — task created

AGENT: Claude Code
BRANCH: agent/feat-original-styles
HEAD_BEFORE: fdd509df0f9252e1464a0d1ff5309426ef2a2cac
HEAD_AFTER: fdd509df0f9252e1464a0d1ff5309426ef2a2cac
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

## 2026-09-30T02:00:00+03:00 — original artwork, mechanics, analysis, PWA

AGENT: Claude Code
BRANCH: agent/feat-original-styles
HEAD_BEFORE: 5185e79
HEAD_AFTER: fdd509d
STATUS: DONE
ACTIVE_SKILLS: better-interface (workflow reference)
TOOLS_USED: WebFetch/WebSearch, Pillow/numpy/scipy/fonttools/UnityPy via uv --with, playwright, gradlew --offline
CHANGED: src/dos/*, src/insights.ts, src/pwa/*, pwa/*, public/*, android/*, docs/ORIGINALS.md, README.md
COMMANDS: see TASK.md
RESULT: Found the real game on archive.org; decoded lines.lib (EGA PCX, custom decoder) and mapped the sprites; the layout and animations match the original screenshots. Service worker bug found and fixed (list built too early; Vary header on module scripts).
TRIED: Reading the agy session notes: not done (agent session databases are off limits without approval).
DECISION: Original artwork is kept in separate folders with a licensing note; spawn-cell markers are off in original looks because the originals only show a Next panel.
SOURCE: archive.org, lines-98.ru, min2win.ru, Wikipedia
NEXT: PR, CI, merge, tag v1.2.0.

