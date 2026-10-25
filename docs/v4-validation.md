# Color Lines4.0 validation

Status: PR67 merged as15e2bb05. A physical tutorial touch audit found a covered required cell; release40225026151 was cancelled before publication. A reviewed training-only layout correction is being validated before release resumes.

## Automated evidence

- Engine:197 pure Kotlin tests. Required in regular CI and release workflows.
- Android: API26/29/35/36/37 adaptive Compose cases, all-theme play/recreation, builder/settings/stats/tutorial/import regressions and terminal audio lifecycle. Full build/tests/lint results in project-local `v4-android-final.log`; 143 tests pass after DOS reachability and native Back regressions; latest full run: `.agent-cache/v4-dos-full-green.log`. Whole-branch independent review found no actionable application defects.
- Web:1026 Vitest tests,180 production Chromium Playwright tests, typecheck/build pass. Lint:two pre-existing FastRefresh warnings. Includes18 themes, DOS4:3 artwork, Windows sprites, builder, backups, offline/CSP, axe,320px phone through1440px desktop,280px landscape, Russian24/32px text with scrolling control reachability.
- Both Android APK variants built locally. These carry local debug signing and must never replace the signed phone installation.

## Artemis

Pinned Google Artemis351ca8422f7b5b54e80a9c1ce03a222e02415b6b. Use project-local venv; no global MCP install or model API credentials. `scripts/audit_android_device.py` calls the actual AndroidAdbDriver, validates decoded screenshots, uses scoped ADB UI hierarchy, rejects system-obscured windows, and restores rotation/font settings in finally. No install, uninstall or data clearing in the runner.

API37 ARM64 Pixel6 emulator:1080×2400 portrait and2400×1080 landscape at font1/2, all four captures contain81 labelled cells. Captures in ignored `.agent-cache/v4-artemis-emulator37/`. Android immersive-mode first-use tip required dismissal before valid landscape measurements; obscured captures were rejected and replaced.

Tablet emulation at160dpi:840×1280 and1280×840, font1/2, all four Artemis captures contain81cells; resolution and density restored afterward.

Huawei CDY-NX9A Android10 (USB X6H4C20B16000698): signed candidate142dc8e updated3.2.2 to4.0.0 with `adb install -r`, matching the project certificate and preserving data. Baseline and candidate board fingerprints match. Modern and original Windows98 matrices (portrait/landscape × font1/2) each contain81 cells and an unchanged board. Studio and builder were opened on the phone. Exact original rotation/font settings restored; original Modern theme restored. Evidence: `.agent-cache/v4-artemis-huawei-{142,win98,studio}/`. Final candidate33cebf5 also passed the modern, DOS and Windows98 matrices on this phone. All12 captures contain81cells and match the original3.2.2board fingerprint. Native Back closes both DOS Help andTopTen without finishing the game.

## Limits and remaining gates

No claim of universal device perfection: automated API/viewport coverage and actual runtime evidence are complementary. Signed release assets/Pages deployment verification remains in progress; required PR checks and independent reviews are green. Browser coverage currently Chromium; WebKit/Firefox are not included.

Artemis configuration audit also found that a brand-new board was not saved until the first action. A regression comparing all81cells and next preview failed before fixing initial persistence through the existing codec. After the fix, four phone-shaped API37 captures have one identical board fingerprint;133Android tests and all builds/lint pass. Native Linux browser CI exposed fractional sticky rounding and two tighter font layouts; scoped CSS containment/compact98Modern HUD fixes pass the40affected local browser cases, all required Linux checks at142dc8e are green; signed candidate run40197484406 succeeded.

DOS follow-up: large-text landscape side rails reserve measured captions, mode and keys, shrinking both original heroes equally and scrolling only when necessary. Classic mode/goals/clock wrap without losing goals to zero width. Android Back closes native DOS Help/TopTen overlays. Corrected regression suite fails3/4 on142dc8e and passes4/4 after the fix; all143 Android tests pass. API37 Artemis four-pose DOS matrix passes with stable81 cells; font2 screenshot confirms full F1–F4 and goals. The same corrected matrix passes on the physical Huawei, recorded in `.agent-cache/v4-artemis-huawei-dos33/`. No DOS art, audio or game rules changed.

Additional physical checks: goals, statistics tabs, backup system-save picker, Studio and custom builder; copied look code applied successfully. Existing Modern theme restored after classic checks; runner restored exact original font/rotation settings. Backup picker was cancelled rather than overwriting a user document. Full backup/restore and gameplay regressions remain automated coverage. Scoped current-process logcat contained no FATAL/ANR markers; Android10 lacks the newer exit-info command.

Coverage limits: no autonomous Artemis model was used; this is the actual Artemis ADB driver with observed UI actions. No in-app editable text/IME path exists. Gesture Back was checked on API37; physical DOS native Back checked on Android10. Original audio assets are unchanged and lifecycle tests pass; human listening and full TalkBack navigation are not claimed. The Huawei UI Automator dump reports zero bounds for DOS tool icons although screenshot-derived taps work; native Compose tests and screenshots cover placement, but that bridge limitation requires separate accessibility investigation.

Tutorial follow-up: semantic-click tests missed an instruction banner covering the required line destination. Real Huawei touches reproduced it; new gesture tests fail at step5 on APIs29/37 before the fix. The banner now occupies a separate bounded scroll region; training alone uses adaptive game chrome with the selected palette/sprites, restoring authentic classic chrome on exit. Tests cover Russian914×411/font1.3 and740×280/411×891 at font2; both enlarged cases exercise Modern,DOS,Windows98,98Modern, assert all81cell bounds above instructions, complete the line by touch, and retain exact persisted game/theme after Skip. Independent review approved.
