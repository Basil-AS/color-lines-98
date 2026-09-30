# Участие в разработке

[English](CONTRIBUTING.md)

- Работайте в ветке `agent/<тип>-<кратко>` или `<тип>/<кратко>` и открывайте PR в `main`; заголовки вида
  `feat(область): суть`.
- Веб: `npm ci`, затем `npm run lint && npm run typecheck && npm test && npm run e2e`.
- Android: `cd android && ./gradlew testDebugUnitTest assembleDebug`.
- Тексты лежат в `src/i18n.ts` (английский и русский); `node --experimental-strip-types scripts/gen-android-strings.mjs`
  обновляет строки Android.
- Слияние в `main` публикует сайт на GitHub Pages и, если версия в `package.json` и в `versionName` Android новая,
  выпускает релиз с подписанным APK, архивом веб-версии и контрольными суммами.
