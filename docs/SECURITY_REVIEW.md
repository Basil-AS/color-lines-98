# Security and quality review

Reviewed with the workstation's `security-review`, `seo`, `better-accessibility`, `better-layout`, `better-typography`,
`better-colors`, `better-ui` and `better-writing` checklists. Each row says what was checked and how.

| Area | Finding | Status |
|---|---|---|
| Web content security policy | none | added to the built page (`default-src 'self'`, no inline scripts, no outside hosts, forms disabled); `e2e/quality.spec.ts` fails on any violation while playing |
| Data the game stores | only on the device; nothing is sent anywhere | checked: no network calls beyond the game's own files |
| Files the player loads (backup import) | hostile content | strict parser, size limit, every part validated as stored data; spreadsheet export defuses formula cells; tests on both platforms |
| Android permissions | only `VIBRATE` | checked with `aapt2` on the release APK; no internet, no storage, no phone state |
| Android backup | `allowBackup` is on | accepted: it restores the game after a reinstall and holds no secrets |
| GitHub Actions | secrets and untrusted input | secrets only in jobs that run on `main` or tags; the PR-title check reads the title through an environment variable; the labeler runs without checking out pull-request code |
| Release signing | debug key used before 1.4.0 | project key in repository secrets; the key file is kept only outside the repository |
| Dependencies | `npm audit` | clean at high level and above (runs in CI); dependency review on every PR |
| SEO | no canonical link, sharing tags, structured data, robots or sitemap; empty page for crawlers | added; the page has a real heading and text before the app starts |
| Accessibility | automated audit | axe (WCAG 2 A/AA) over all 12 looks and the statistics, new-game, settings dialogs: no serious or critical violations; one contrast failure found and fixed |
| Layout | physical margins, colour-only warning | logical properties; the last seconds of Blitz carry a symbol too |
| Not verified | the apps were not run on a device; screen readers were not used by hand | |
