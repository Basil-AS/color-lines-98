# Visible controls and maximum game area

The user's corrective direction is explicit: basic actions must be visible, controls must look like rounded buttons, all functions must remain available, landscape settings must not reveal the notification bar or move the board, and unnecessary spacing must yield to a larger board. Continue the approved 4.0 design without another routine approval gate.

Modern Android exposes New game, Undo, Hint (Result after game over), Settings, Statistics, Sound and Help directly. Buttons use the selected palette, filled surfaces, clear borders, icons and translated labels with at least 48dp targets. Extremely short or enlarged interfaces scroll their supporting rail, while the board remains fixed. The web exposes the same named actions in compact rows. Original DOS/Windows chrome and every constructor option remain available; explicit board-cell shapes are preserved.

The safe outer inset is 4dp on Android and 4px plus required safe-area insets on web. Android panel gaps are 6dp. The board fills the limiting safe dimension; the landscape panel uses the remaining width instead of centering a narrower group within unused margins.

Each Compose dialog owns a separate window. Supporting dialogs and the appearance studio must request landscape immersive bars before drawing and when regaining focus, using the same window policy as the activity. Portrait retains system bars, and edge swipes can reveal transient bars. No global phone settings change.

Verification combines actual touch regressions, all-action visibility and game-over callbacks, size/font sweeps, API29 dialog-window flags, complete Android and browser suites, independent review, and pinned Artemis screenshots/actions on Huawei Android10 and the API37 emulator. Physical before/after Settings captures must establish stable board placement; flag assertions alone do not prove that.
