# The original games and what this port keeps

## Sources

| What | Where |
|---|---|
| Color Lines (Gamos, 1992, MS-DOS) | Internet Archive: [`lines_eng`](https://archive.org/details/lines_eng), [`msdos_Color_Lines_1992`](https://archive.org/details/msdos_Color_Lines_1992) |
| Rules and scoring of Lines 98 | <https://lines-98.ru/> (10 / 12 / 18 / 28 / 42 points for 5 to 9 balls, 3 new balls after every move, delayed when a line is cleared) |
| Description of the game | <https://www.min2win.ru/game/lines-98.html>, [Wikipedia: Color Lines](https://en.wikipedia.org/wiki/Color_Lines) |
| Lines 98 look and sounds | the reference APKs kept locally (`org.game.line98basic`, Win98 GUI assets, ball atlas, sounds) |

## Color Lines 1992 (DOS)

`lines.lib` is a *Genus pcxLib* archive of five 640x350 EGA (16-colour, 4-plane) PCX images. Decoded:

* the empty **game screen** (`layout`): grey bevelled frame, two LCD displays, the `Next` panel, the 9x9 board of
  34x24 cells, buttons `F1 HELP`, `F2 SOUND`, `F3 NEXT`, `F4 RESTART` and the Gamos logo;
* the **sprite sheet**: every ball in 7 colours and 6 frames (on its cell, small, medium, squashed, two burst frames),
  the red **king** on his pillar (crowned, crown fading, crownless), the magenta **pretender** with a sword (crown
  appearing, big crown), the `Top Ten` and `Help` windows, the 8x8 green bitmap font, the button labels;
* the **title screen** and the trumpeting characters.

The web (`src/dos/`) and Android (`engine/Dos*.kt`) versions draw exactly these images at the original coordinates,
stretched 4:3 (DOS pixels are 1.37 times taller than wide), with the same animations (balls grow, the selected ball
bounces, cleared balls burst, the pretender takes the crown when the king's score is beaten).

Rules stated by the original's help window: *"The purpose of the game is to score points more than the king has."*
The default king is **Handicap with 100 points**; the best entry of the Top Ten is the reigning king.

## Mechanics: checked against the sources

| Rule | Original | Here |
|---|---|---|
| Board 9x9, 7 colours | yes | yes |
| Start with 5 balls, never a finished line | yes | yes (re-dealt if a line appears) |
| Move any ball through free cells, 4 directions | yes | BFS, 4 neighbours |
| Line of 5+ in a row, column or diagonal clears | yes | yes |
| A clearing move gives a free turn (no new balls) | yes | yes; the announced colours stay |
| Otherwise 3 new balls of random colours in random cells | yes | yes |
| Lines made by the new balls also clear and score | yes | yes |
| Next 3 colours shown in a panel | yes (`F3` hides them) | yes |
| Score 10 / 12 / 18 / 28 / 42 for 5 to 9 | yes | `2L^2 - 20L + 60` gives exactly these |
| Game over when the board is full | yes | yes |
| Marking *where* the new balls will appear | **no** | optional, off in the original looks |
| Undo | not in the originals | extra, optional |

Not verifiable offline: how the original scores two lines cleared by one move (this port scores each line
separately) and the exact PC-speaker sounds (approximated, see below).

## Sounds

* **Lines 98 (Windows)**: the recorded samples from the reference app.
* **Color Lines 1992 (DOS)**: square-wave PC-speaker beeps synthesised in code (the original melodies were not
  extracted).
* **Modern looks**: new soft synthesised sounds (sine/triangle notes with a smooth decay).

## Licensing note

The 1992 artwork belongs to its authors (Oleg Demin, Gennady Denisov, Igor Ivkin, Gamos Ltd.) and the Lines 98 sprites
and sounds come from third-party apps. They are included here as a tribute and for preservation, are kept in separate
folders (`public/originals/`, `assets/`, `android/app/src/main/res/drawable-nodpi/cl92_*`) and can be removed without
touching the code: the themes then fall back to the modern look.
