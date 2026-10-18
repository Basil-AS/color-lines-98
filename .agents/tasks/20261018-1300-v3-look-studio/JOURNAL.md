# Journal

## 2026-10-18

- Added the look studio (`src/appearance.ts`, `src/colorlab.ts`, `src/applyLook.ts`, `src/components/Look*.tsx`) and the stylesheet layer at the end of `src/App.css` (`body[data-studio]`, `data-volume`, `data-contrast`, `data-shapes`, `data-ballsize`, `data-grid`, `data-cells`).
- Android: `core-engine/.../Look.kt` (model, colour maths, codes, store; JVM tests in `LookTest.kt`), `LookStudio.kt`, `Palette.resolvePalette`, `BoardView` drawing, storage and backup.
- Layout audit `e2e/audit.spec.ts`: found that a strong ring on large balls stuck out of small round cells; fixed by sizing the ball from the ring width in pixels.
- Analytics audit: the best points-per-move now ignores games under 15 moves (as records and goals do); overview figures use thousands separators; figures in cards stay on one line.
- First Android CI run failed on a missing import and on test code (`val engine = <package>` is not Kotlin); fixed.
