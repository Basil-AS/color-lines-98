[Русская версия](REQUIREMENTS.ru.md)

# Requirements and how they are met

Every wish from the work on this project. "Verified" means an automated test, a browser screenshot or a CI run exists. What could not be checked is marked.

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Study the project and bring it to a finished state | done | branches, PRs, releases v1.0.0 to v1.5.1 |
| 2 | Workstation rules for every Claude launch | done | hook `~/.claude/hooks/workstation-guard.sh` (logged in `~/System/CHANGELOG.md`) |
| 3 | Releases, GitHub Pages, CI/CD, PRs, branches, commits | done | pipelines `ci`, `web-ci` (+ e2e), `android-ci`, `release` |
| 4 | Serious tests, all errors fixed | verified | hundreds of Vitest tests, Playwright in a real browser, Kotlin engine tests, Android UI tests |
| 5 | Window centred, no horizontal scroll, on desktop, tablet and phone | verified | `e2e/layout.spec.ts`: every look at 5 screen sizes |
| 6 | A proper name for the game and the release file | done | "Color Lines" in every language; `ColorLines-<version>.apk`, `...-web.zip` |
| 7 | Android package with my name and the `io` prefix | done | `io.github.basil_as.basillines` (checked in the built APK) |
| 8 | History, records, progress, statistics with analysis | verified | levels, achievements, streaks, trend, charts; tests on both platforms |
| 9 | Record every result | done | up to 1000 games plus the permanent day ledger, Top Ten |
| 10 | Russian, language follows the system | verified | auto-detection; `e2e/game.spec.ts`, dictionary tests |
| 11 | App icon, favicon | done | adaptive Android icon, PNG/SVG for the site and PWA |
| 12 | Android: insets under system bars | verified by tests | `safeDrawing`, edge-to-edge; **not run on a device** |
| 13 | The New button on Android must not keep the old board | verified | test `aMoveSpawnsBallsAndNewGameReplacesTheWholeBoard` |
| 14 | A light modern theme | done | "Modern light" on web and Android |
| 15 | Settings are remembered (web and app) | verified | theme, language, sound, preview marks, name, NEXT |
| 16 | All themes on the phone | done | 12 looks on Android |
| 17 | Looks that repeat the originals Color Lines (1992) and Lines 98 | done | the 1992 screen from `lines.lib`; Lines 98: LED panel, board, menu |
| 18 | The knight and the king, the look of the score display | done | original sprites and LCD |
| 19 | Sounds of the old versions, new ones for the new | done | Lines 98 samples; PC-speaker beeps (DOS); new soft tones |
| 20 | Check the sounds | verified | test: every event has a sound in every look, every sample exists |
| 21 | Marks where balls will appear: "as in the original?" | clarified | not in DOS 1992, yes in Windows Lines 98; on by default except DOS |
| 22 | Check the mechanics and algorithms | verified | `tests/rules.test.ts`, `RulesConformanceTest.kt`, `docs/ORIGINALS.md` |
| 23 | PWA for iOS, Android, Windows, macOS | verified | manifest, offline (e2e), Install button, Safari instructions |
| 24 | One style everywhere | done | same dialogs and elements across looks |
| 25 | The game must stay interesting, not only scripted tasks | done | modes Classic / Easy / Blitz / Daily (same on web and Android), adaptive goals from your own history, hints |
| 26 | Reset asks for confirmation reliably | verified | web: warning plus checkbox; Android: a separate confirmation step |
| 27 | Should balls spawn after a cleared line? | verified | no, the turn is free (as in the original); `tests/rules.test.ts`, `RulesConformanceTest.kt` |
| 28 | Game name in English, package with basil-as | done | "Color Lines" in any language; package `io.github.basil_as.basillines` |
| 29 | Web and Android in step | verified | strings generated from one dictionary; same modes, hint, themes, goals, daily game (value-for-value test) |
| 30 | Change the language inside the game on Android | verified | Auto / English / Русский in Settings; Robolectric test |
| 31 | Android portrait: the widest possible board | done | full-width board, compact HUD; also for the DOS look |
| 32 | More looks, Material | done | Material, Neon, Synthwave, Ocean, Paper, Game Boy, Amber terminal, High contrast |
| 33 | Localisation of the DOS look and old interface | verified | Russian labels, help, Top Ten over the original sprites; captions stay English |
| 34 | A living DOS background: animation on both sides | fixed | the pretender lifts his sword (more often near the record); web and Android |
| 35 | Functions consistent across looks | verified | test `consistency between themes`: every action in each look |
| 36 | "Rating" feature | none | there is no online rating and no server; the local Top Ten and history work |
| 37 | Tab title in English everywhere | fixed | `Color Lines — the classic ball puzzle` in any language |
| 38 | At least 6 different looks, each with its own sound | done | 12 looks; 10 synthesised voices, Lines 98 samples, PC speaker; uniqueness test |
| 39 | Contrast of the new looks | verified | WCAG ratios by pair; axe audit of all looks and dialogs |
| 40 | Everything that loses data asks explicitly | verified | new game (warning, safe default), history reset (checkbox), backup replace (checkbox), DOS F4 and the Windows menu use the same dialog, `?new=1` shortcut opens the dialog |
| 41 | PWA installation must work | fixed | the install event is kept from page load; a menu hint when the browser offers no prompt; `tests/earlyinstall.test.ts`; **not tried on a real phone** |
| 42 | The download button gives the newest APK directly | done | `releases/latest/download/ColorLines.apk`: every release carries a fixed-name file |
| 43 | Check and resolve all PRs on GitHub | done | merged #8, #10, #15 to #17; #9 and #11 to #14 closed with a reason (need AGP 9 / Gradle 9 / TypeScript 7 migration) |
| 44 | Use GitHub's mechanisms to the maximum | done | signed release on merge, CodeQL, Dependabot with auto-merge, labels, PR title check, Lighthouse, protected `main`, templates |
| 45 | Permanent statistics, results moved by file | done | day ledger, seasons, milestones, heatmap, records; JSON/CSV, import with confirmation; files readable by both platforms |
| 46 | Code and quality reviewed with the workstation skills | done | `docs/SECURITY_REVIEW.md`; axe over 12 looks and dialogs (`e2e/quality.spec.ts`); independent diff review |

## Not verified or approximated

* **The apps on a device or emulator** were never run (the emulator was ruled out): JVM tests (Robolectric), browser tests and builds only. Looks, insets and the PWA install flow need a real phone.
* **DOS 1992 sounds** are synthesised (the original melodies were not extracted).
* **The original Windows Lines 98** ships in a closed installer; its look was taken from a screenshot, no assets.
* **APK signing:** releases from 1.4.0 are signed with the project key (in repository secrets, a copy with the owner); 1.3 and older used the debug key and cannot be updated in place.
* **DOS screen captions** stay English on purpose, like the original.
