# Android toolchain

The 4.0 migration keeps `minSdk = 26`, the JVM engine module, Java/JVM target 21,
saved-data formats and all theme implementations. Android 10 remains supported.

## Pinned versions

| Component | Version |
| --- | --- |
| Android Gradle plugin | 9.4.1 |
| Kotlin JVM and Compose compiler plugins | 2.4.21 |
| Gradle wrapper | 9.8.2 |
| JDK and JVM bytecode target | 21 |
| Compile and target SDK | 37 |
| SDK platform package | platforms;android-37.0 |
| AGP default Build Tools | 36.0.0 |
| Compose BOM | 2026.09.00 |
| Compose UI, Foundation and Runtime (BOM) | 1.12.1 |
| Material 3 / Material 3 Adaptive (BOM) | 1.4.0 / 1.3.0 |
| AndroidX Core | 1.19.1 |
| Lifecycle | 2.11.0 |
| Activity Compose | 1.13.0 |
| AndroidX Test Core / JUnit extension / Espresso | 1.7.0 / 1.3.0 / 3.7.0 |
| Robolectric | 4.17 |
| Coroutines / DataStore | 1.11.0 / 1.3.0 |

Google Maven metadata and the BOM POM were checked on 2026-10-25. There is no
stable October BOM in that metadata; 2026.09.00 already maps to the versions above.
Core's Kotlin extensions are now included in `core`; the `core-ktx` compatibility
artifact is empty.

## Compatibility and migration

AGP 9.4 supports API 37 and requires Gradle 9.6.0 or higher and JDK 17 or higher.
The 9.4.1 patch is listed in the API reference and exists in Google Maven, although
the release-note example still says 9.4.0. Gradle 9.8.2 includes the 9.8.1 security
fixes and a dependency-alignment fix.

Kotlin 2.4.21's published fully supported matrix ends at Gradle 9.7.0 and AGP
9.3.1. Kotlin documents that newer versions can be used with possible warnings or
feature limitations. This newest stable combination therefore requires project
build/test evidence; it must not be described as fully covered by that vendor
matrix. CI is the ongoing compatibility gate.

AGP 9 enables built-in Kotlin. The Android module no longer applies
`org.jetbrains.kotlin.android`; the pure JVM engine keeps
`org.jetbrains.kotlin.jvm`. Both JVM and Compose compiler plugin declarations
share Kotlin 2.4.21 at the root, aligning the Kotlin Gradle plugin classpath above
AGP's bundled 2.2.10. Verify the resolved compiler when changing these pins.
The existing `kotlin.compilerOptions` DSL is retained, and the engine explicitly
selects a Java 21 toolchain.

The base API 37 SDK is used intentionally: SDK repository metadata also exposes
37.1/37.2 packages, while the official QPR2 documentation still calls the minor
release beta and AGP 9.4 documents maximum API 37. Do not infer a verified minor
SDK migration from package availability alone. AGP's default Build Tools 36.0.0
is installed explicitly in every Android CI job; no experimental SDK is required.

## Verification requirements

On 2026-10-25, the checked Gradle distribution generated the wrapper twice, the
local SDK installation completed, and `help buildEnvironment :app:buildEnvironment`
passed on JDK 21. The root classpath report confirms AGP's Kotlin Gradle plugin
2.2.10 resolves to 2.4.21. A subsequent run with `--warning-mode all` identifies
deprecated `Configuration.setVisible(boolean)`, scheduled for removal in Gradle
11. Inspection of the cached AGP 9.4.1 bytecode finds unconditional calls in
`SourceSetManager` and the Android JDK-image configuration setup. KGP 2.4.21
guards its corresponding call on Gradle versions below 9. The exact live caller
can be captured with `-Dorg.gradle.deprecation.trace=true --warning-mode all`.
No production workaround or warning suppression is applied. This configuration
check does not prove compilation, unit/UI tests, release signing or device behavior.

Run `:core-engine:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
and the release APK build. Inspect dependency resolution for Kotlin 2.4.21,
Compose UI 1.12.1 and the BOM versions, and record any new deprecations.
Robolectric 4.17 supports API 37; retain older-API coverage as well as adding
current-platform coverage. A local build alone does not verify adaptive visuals.

Validate the signed APK on the existing Android 10 phone without uninstalling
the installed game or losing its data. Also cover API 26, 35, 36 and 37; gesture
and three-button navigation; portrait and landscape; IME; cutouts; resizable
windows and tablets; large fonts; activity/process recreation; all classic
themes and the complete appearance constructor.

Target 36+ enforces edge-to-edge and changes system back dispatch. Target 37
removes the large-screen resizability opt-out. Use supported AndroidX/Compose
back handlers and consume drawing/IME insets at layout boundaries. Size decisions
must follow the current available window, including compact height in landscape.

## Official sources

- [AGP API release reference](https://developer.android.com/reference/tools/gradle-api)
- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [Built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Kotlin release history](https://kotlinlang.org/docs/releases.html)
- [Gradle 9.8.2 release notes](https://docs.gradle.org/9.8.2/release-notes.html)
- [Compose BOM and compiler alignment](https://developer.android.com/develop/ui/compose/bom)
- [Core release notes](https://developer.android.com/jetpack/androidx/releases/core)
- [Lifecycle release notes](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [Activity release notes](https://developer.android.com/jetpack/androidx/releases/activity)
- [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test)
- [Robolectric 4.17](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.17)
- [Android 16 target behavior](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android 17 target behavior](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)
- [Google Maven BOM metadata](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml)
- [Android SDK repository metadata](https://dl.google.com/android/repository/repository2-3.xml)
