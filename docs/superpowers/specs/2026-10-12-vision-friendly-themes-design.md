# Vision-Friendly Themes Design

## Goal

Add two calm, high-visibility themes that make the board grid and all seven ball
colours easy to distinguish at a glance for players with reduced visual acuity.
The themes must have the same names and semantics in web and Android.

## Design

`clear` uses a warm off-white surface, a charcoal board frame and visible grid
cells. `clearNight` uses a deep blue-charcoal surface, a pale grid and the same
high-chroma ball palette. Both themes use shape markers in each ball as a
redundant cue, preserve keyboard/focus visibility, and avoid animated glare.

The existing `contrast` theme remains available for users who prefer a strict
black/yellow presentation. The new themes are additive and do not change save
compatibility: old theme identifiers continue to parse unchanged.

## Platform contract

- Web and Android expose `clear` and `clearNight` as selectable themes.
- Board cells use a visible fixed grid, not only gaps or shadows.
- Ball meaning is carried by both saturated colour and a stable shape marker.
- Text and controls use semantic tokens with measured contrast against their
  actual surfaces.
- Theme persistence and backup round-tripping continue to work.

## Delivery

GitHub Actions will run web tests/build and Android tests/build on pull requests.
A tag-triggered release workflow will publish the web artifact and Android APK
artifact, with checksums, to a GitHub Release. No signing secret is added to the
repository; unsigned debug/release artifacts remain explicit in the workflow.
