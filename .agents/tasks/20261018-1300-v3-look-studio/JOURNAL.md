# Journal

## 2026-10-18

- Added the look studio (`src/appearance.ts`, `src/colorlab.ts`, `src/applyLook.ts`, `src/components/Look*.tsx`) and the stylesheet layer at the end of `src/App.css` (`body[data-studio]`, `data-volume`, `data-contrast`, `data-shapes`, `data-ballsize`, `data-grid`, `data-cells`).
- Android: `core-engine/.../Look.kt` (model, colour maths, codes, store; JVM tests in `LookTest.kt`), `LookStudio.kt`, `Palette.resolvePalette`, `BoardView` drawing, storage and backup.
- Layout audit `e2e/audit.spec.ts`: found that a strong ring on large balls stuck out of small round cells; fixed by sizing the ball from the ring width in pixels.
- Analytics audit: the best points-per-move now ignores games under 15 moves (as records and goals do); overview figures use thousands separators; figures in cards stay on one line.
- First Android CI run failed on a missing import and on test code (`val engine = <package>` is not Kotlin); fixed.
- 2026-10-18: PR #38 merged (775046f); release v3.0.0 published. Android builder tab: an `OutlinedTextField` made the Compose test never idle; the look code is now applied from the clipboard.
- 2026-10-18: 3.1.0 started from user feedback: Android studio was cluttered and the preview scrolled out of sight; ball depth looked crude. Done: full-screen Android studio (pinned preview, compact choices, colour tiles with one editor), sticky preview on web, checker grid with lines, depth redrawn (tinted highlight/shade, small specular, contact shadow) on web and Android.
- 2026-10-19: 3.2.0: three Meadow looks became one; Modern light and Clear night folded into Modern and Clear; Day / Night / Auto switch for every look (web and Android; custom, Game Boy, terminal, Lines 98, Color Lines 1992 keep their colours); reset-to-theme button in the studio; settings and studio buttons stay reachable on short screens. PRs #41, #43, #46.
- 2026-10-21: 3.1.1: on a phone the studio still did not fit (48 dp chips, buttons under the system bar). Chips 32 dp, smaller preview, Reset/Use/Close moved to the header.
