# Contributing

- Work in a branch named `agent/<type>-<slug>` or `<type>/<slug>` and open a pull request to `main`; titles look like
  `feat(scope): summary`.
- Web: `npm ci`, then `npm run lint && npm run typecheck && npm test && npm run e2e`.
- Android: `cd android && ./gradlew testDebugUnitTest assembleDebug`.
- Texts live in `src/i18n.ts` (English and Russian); run `node --experimental-strip-types scripts/gen-android-strings.mjs`
  to update the Android strings.
- Merging to `main` deploys the site to GitHub Pages and, when `package.json` and the Android `versionName` hold a new
  version, publishes a release with the signed APK, the web zip and checksums.
