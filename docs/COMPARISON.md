# Twelve implementations of Color Lines, compared

[Русская версия](COMPARISON.ru.md)

This review compares twelve implementations of the game: mechanics, sounds, look, features and how each one is built.
Every cell says where it comes from. **n/s** means *not stated* on the source I could read; I did not guess.

## How it was checked

| Kind of evidence | Used for |
|---|---|
| Web pages read on 2026-09-30 | items 3–8 (rules as the page states them) |
| The APK files kept locally, opened with `aapt2` and `unzip` | items 8–11 (package, version, engine, ad code, permissions, sound files) |
| Wikipedia and the Internet Archive | item 1–2 (history, rules of the original) |
| This repository | item 12, and the decoded `lines.lib` of the 1992 game (`docs/ORIGINALS.md`) |

Not done: the apps were **not run** (no device), so anything that only shows at play time (feel of the animation,
exact sound design, difficulty) is not judged here. Store pages that refused the request (HTTP 403) are listed as such.

## The twelve

| # | Name | Kind and platform | Built with | Licence or model | Evidence |
|---|---|---|---|---|---|
| 1 | **Color Lines** (Gamos, 1992) | MS-DOS game | n/s | commercial, Gamos Ltd; authors Oleg Demin, Gennady Denisov, Igor Ivkin | [Wikipedia](https://en.wikipedia.org/wiki/Color_Lines); decoded `lines.lib` |
| 2 | **Color Lines for Windows 3.1 / 95** (1995) and **Lines 98 v5.0** (Sorcerersoft) | Windows | n/s | commercial / shareware (n/s) | Wikipedia (1995 ports); v5.0 screenshot as visual reference only |
| 3 | **lines-98.ru** | web game (Russian) | n/s | free to play; score saving after registration | <https://lines-98.ru/> |
| 4 | **min2win.ru** "Lines 98" | web game (Russian) | n/s | free to play | <https://www.min2win.ru/game/lines-98.html> |
| 5 | **bubbleshooter.net** "Lines 98" | web game (English) | n/s | free to play | <https://www.bubbleshooter.net/game/lines-98/> |
| 6 | **KLines** (KDE, "Kolor Lines") | desktop app: Linux, macOS, Windows | Qt/KDE | GPL-2.0 | <https://apps.kde.org/klines/>, man page |
| 7 | **Five or More** (GNOME) | desktop app: Linux (GNOME Games) | n/s | GPL-2.0-or-later | [GitLab](https://gitlab.gnome.org/GNOME/five-or-more); Wikipedia lists it as a clone |
| 8 | **ColorLinesUnity** / "Цветные линии" (`com.newbilius.lines` 1.3) | Windows, Android, HTML5 | Unity, C# (IL2CPP in the APK) | Apache-2.0 | [GitHub](https://github.com/Newbilius/ColorLinesUnity); APK |
| 9 | **Lines 98 Classic** (`com.colorball.line` 4.5.1) | Android | Unity; AdMob classes | free with ads (ad code present) | APK |
| 10 | **Line 98 – Color lines 98** (`com.senspark.lines98` 3.2.6) | Android | Unity; AdMob classes | free with ads (ad code present) | APK |
| 11 | **Line 98** (`org.game.line98basic` 1.1) | Android | Axmol (a cocos2d-x fork); AdMob classes | free with ads (ad code present) | APK |
| 12 | **Color Lines** (this project, 1.5) | web PWA + Android | React/TypeScript; Kotlin/Compose | MIT | this repository |

## Mechanics

| # | Board and colours | Start | Balls per move | Clearing | After a clearing move | Next balls shown | Scoring | Game ends |
|---|---|---|---|---|---|---|---|---|
| 1 | 9×9, 7 | 5 balls | 3 | lines of 5+, horizontal, vertical, diagonal | free turn, no new balls | panel with 3 colours (`F3` hides it); **no marks on the board** | longer lines give "significantly more" (formula not stated); our decode and the table below agree with item 3 | board full |
| 2 | 9×9, 7 | n/s | 3 | as 1 | as 1 (per Wikipedia) | v5.0 screenshot: small balls standing **on the board** | n/s | board full |
| 3 | 9×9, 7 | n/s | 3 | lines of 5+ | "the appearance of the new three is postponed for one turn" | colours shown above the field | **10 / 12 / 18 / 28 / 42** for 5 to 9 | n/s |
| 4 | "grid" (size n/s) | n/s | n/s | chains of 5+ | n/s | n/s | n/s | n/s |
| 5 | 9×9 | n/s | "up to three" | 5+ | n/s | small balls on the target cells | n/s | "board resets when no moves remain" |
| 6 | 9×9 | n/s | 3 | 5+ | n/s | "Show Next"; **score is reduced when it is on** | "depends solely on the amount of erased balls" | "can not be won, played against the highscore" |
| 7 | n/s | n/s | n/s | "forming lines" | n/s | n/s | n/s | n/s |
| 8 | as the DOS original | n/s | n/s | 5+ | n/s | n/s | "score tracking with high scores" | n/s |
| 9–11 | not inspected (not run) | | | | | | | |
| 12 | 9×9, 7 (Easy: 5) | 5 balls, never a finished line | 3, at the **announced cells** | 5+ on four axes, every line scored once | free turn, colours stay | colours in a panel **and** small balls on the board (off in the 1992 look) | `2L² − 20L + 60` = 10 / 12 / 18 / 28 / 42 | board full (Blitz: time up) |

Path rule (items 1, 3, 5, 12): a ball moves only through free cells in the four orthogonal directions; diagonal steps
are not allowed. Item 6's man page says marbles move "from cell to cell", n/s on diagonals.

**What this shows.** Where sources state the rules they agree with each other and with item 12: 9×9, 7 colours,
3 balls per move, lines of 5 or more, a free turn after a clearing move, and the 10 / 12 / 18 / 28 / 42 table (the
formula `2L² − 20L + 60` reproduces it exactly and is tested in `tests/rules.test.ts` and the Kotlin
`RulesConformanceTest`). The real disagreements are in presentation, not rules: marks on the board (Windows and
bubbleshooter: yes; DOS 1992: no) and KLines' choice to cost score for showing the next balls.

## Features

| # | Undo | Hint | Modes | Stats / records | Themes or looks | Sounds | Languages | Offline | Ads, permissions |
|---|---|---|---|---|---|---|---|---|---|
| 1 | no | no | one | Top Ten, king to beat | one | PC speaker | English version on the Internet Archive (`lines_eng`) | yes (DOS) | none |
| 2 | n/s | n/s | n/s | n/s | n/s | n/s | n/s | yes (installed) | n/s |
| 3 | not listed | n/s | one | score saving after registration | n/s | toggle | Russian | no | n/s |
| 4 | n/s | n/s | one | n/s | n/s | n/s | Russian | no | n/s |
| 5 | **yes** (lower left) | n/s | one (a mode indicator is shown) | ball count, score | retro blue digital text | n/s | English | no | n/s |
| 6 | n/s | n/s | one | highscores | n/s | n/s | n/s | yes | n/s |
| 7 | n/s | n/s | n/s | n/s | n/s | n/s | n/s | yes | n/s |
| 8 | n/s | n/s | one | high scores | sprites from the DOS game | DOS sounds recorded through DosBox | Russian title (`Цветные линии`) | yes | no ad code in the file list |
| 9 | n/s | n/s | n/s | n/s | 315 images | no sound files in the package (may be inside Unity data) | n/s | n/s | AdMob code; `READ_PHONE_STATE`, storage permissions |
| 10 | n/s | n/s | n/s | n/s | 349 images | as 9 | n/s | n/s | AdMob code; `READ_PHONE_STATE`, storage permissions |
| 11 | n/s | n/s | n/s | n/s | 92 images, 3 fonts | **20 sound files**: click, win, lose, screen appear, bomb, break, `eat`, `eatScore_1…5` … | English and German splits | n/s | AdMob code; `READ_PHONE_STATE`, storage permissions |
| 12 | yes (20 steps) | **yes**, 3 per game | **4**: Classic, Easy, Blitz, Daily (same on web and Android) | ledger for every day, months, seasons, milestones, records, heatmap, 18 achievements, goals from your own history; Top Ten | **12 looks** | sampled (Windows look), PC-speaker synthesis (DOS), 10 synthesised voices, one per look | English, Russian, switchable in the game | yes (PWA, APK) | no ads; the APK asks only for `VIBRATE` (haptic feedback) |

The three Android clones (9–11) ask for the phone-state permission and ship ad code; the game does not need either. This
project asks for neither (only `VIBRATE`) and sends nothing anywhere: results live on the device and move only through a backup file
you save yourself.

## Sounds and look

| # | Sound | Look |
|---|---|---|
| 1 | PC-speaker beeps (the melodies were not extracted; this project approximates them) | EGA 16 colours, bevelled grey board, two LCD displays, red king and magenta pretender |
| 2 | n/s | grey window, menu bar, LED panel (v5.0 screenshot: cyan digits: best, next balls, score) |
| 8 | original sounds recorded from DOSBox | sprites cut from DOSBox screenshots |
| 11 | 20 mp3 files (the Windows look of this project uses samples with the same names) | 92 images, 3 fonts |
| 12 | every event has a sound in every look (a test checks it) | twelve looks, from the DOS screen to Game Boy and an amber terminal |

## What was adopted, changed or left out

* **Kept from the originals:** every rule in the mechanics table; the king and pretender with a Top Ten; `F1`–`F4`;
  the LED panel of the Windows look; marks on the board where the next balls will land (default on, off in DOS).
* **Added by this project:** hints, four modes with the same daily game on both platforms, permanent statistics and a
  backup file, twelve looks with a voice each, reflowed layouts so the board is as big as the screen allows.
* **Seen elsewhere, not copied:** KLines' score penalty for showing the next balls (would be a natural "hard" mode);
  lines-98.ru's switch that shows the path of a moving ball (here the reachable cells are highlighted instead); the
  registration and social sharing of the web clones (this game has no accounts and no server).
* **Not verifiable here:** how each original scores two lines cleared by one move (this project scores each line
  separately); the exact original sound designs; everything in items 4, 7, 9–11 that needs the app to be run.

## A correction

An earlier note in this repository (`docs/RESEARCH_REPORT.md`, now removed) listed 24, 42, 64 and 90 points for lines of
6 to 9 balls. That was a mis-applied formula. The verified table is **10 / 12 / 18 / 28 / 42**, given by
<https://lines-98.ru/> and reproduced by `2L² − 20L + 60`.

## Sources

[Wikipedia: Color Lines](https://en.wikipedia.org/wiki/Color_Lines) ·
[lines-98.ru](https://lines-98.ru/) ·
[min2win.ru](https://www.min2win.ru/game/lines-98.html) ·
[bubbleshooter.net](https://www.bubbleshooter.net/game/lines-98/) ·
[KLines](https://apps.kde.org/klines/) ·
[Five or More](https://gitlab.gnome.org/GNOME/five-or-more) ·
[ColorLinesUnity](https://github.com/Newbilius/ColorLinesUnity) ·
[Internet Archive: Color Lines](https://archive.org/details/msdos_Color_Lines_1992)
