# Retro 98 Theme Design

## Goal

Add a separate `retro98` theme inspired by the requested Yandex Lines 98 visual language: a muted purple chrome, warm beige board, visible grid, and saturated balls.

## Design

- Web and Android use the same theme identifier: `retro98`.
- Page and controls use deep plum/purple surfaces with cream text.
- The board uses a warm beige background and a medium brown grid; cells remain distinct without becoming harsh black lines.
- Balls reuse the existing high-distinction palette and contrast symbols so color is never the only cue.
- Existing themes remain unchanged.
- Theme labels are localized in English and Russian.

## Accessibility and verification

Measure text, grid, cell, and accent pairs against WCAG AA targets where applicable. Add web theme rendering tests and Android palette/storage tests. Run full web checks, Android unit tests, release assembly, and CI/release workflows.

