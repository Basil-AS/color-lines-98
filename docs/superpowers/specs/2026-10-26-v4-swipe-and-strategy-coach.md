# Swipe tabs and strategy coach

User requests: swipe between Settings/Statistics tabs, full convenient UX, and a strategy tutor beyond basic rules. Existing design4.0 remains approved; this is explicit additional scope.

## Interaction
Android uses native horizontal paging and independently scrolling page bodies; tab buttons stay available. Web preserves existing five Settings and six Statistics tabs. Horizontal intent must not steal vertical scroll, form/slider gestures or cancelled/multitouch sequences. First/last tabs clamp. Selected tabs remain visible.

## Training
Separate Help and Settings entry alongside the existing basic tutorial. Six deterministic, real-engine puzzles: open-ended threes, fill gaps, diagonals, intersections, open passages and plan from spawn preview. Bilingual explanation, optional coordinate/board hint, retry feedback, success explanation and explicit Continue. Equivalent solutions are accepted where the taught principle permits them. These are curated strategy exercises, not a general optimal-move evaluator.

Training uses a temporary board; leaving or finishing restores the original party. No history, progress or save writes from exercises; reset transient timing/combo state. Theme and full constructor remain available. Classic DOS/98 identities preserved.

## Verification
Core tests solve all lessons using the actual engines. UI tests cover wrong move, hint, explicit Continue, all6 completion and exact saved-game/history restoration. Gesture tests cover direction, boundaries and scrolling. Run full web/Android checks and actual Artemis touch/rotation/font tests on signed Huawei candidate before PR73/release.

## User correction: preserve the board (2026-10-26)
The user rejects the training layout consuming40%ofscreen and a visually incoherent control deck. This supersedes the earlier separatebannerrow solution: training now replaces the ordinary action dock or landscape rail, while the game board retains its exact bounds. Compact native instructions provide a separate Details dialog, with hint/continue/leave controls visible at everyday130%font. Web uses bounded instruction copy and touch-sized actions in the same reserved controls area. Modern chrome uses a single rounded control language: primaryNewGame, outlined Undo/Hint, bordered utilitybuttons, aligned Score/Best/Next labels and restrained spacing. All original themes/constructor and DOS/98identities remain.

Regression evidence: Android geometry RED403->267dp; web landscapeRED380->238.8/217.2px. New regressions require exactbefore/afterwidth/height/position and unobscured81cells, plus native130%font actions. IndependentreviewcaughtWindows98missingpracticebanner; restorelegacypracticecontrols and coverbothtrainingflows.
