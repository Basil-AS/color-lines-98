# Project context

STATUS: initialized
UPDATED_AT: 2026-09-28

## Purpose

Modern, responsive, highly polished recreation of the classic 1998 puzzle game "Color Lines" (Линии 98). Features authentic game rules, fluid animations, crisp modern graphics, sound synthesis/effects, undo capabilities, session persistence, and responsive controls for both desktop and mobile/touch devices.

## Architecture

- **Engine / Core (`src/engine/`)**: Pure TypeScript game logic (9x9 grid representation, BFS pathfinding, line matching algorithm for 5+ balls, RNG spawn logic, scoring system, undo history stack). Zero UI dependencies for 100% testability.
- **Renderer / Presentation (`src/components/`, `src/render/`)**: React UI components for HUD, menus, game settings, leaderboards + Canvas/CSS rendered game board with smooth animations, ball movements, bouncing effects, particle celebrations.
- **Audio (`src/audio/`)**: Web Audio API synthesizer / sound effects for ball moves, invalid paths, line clears, game over.
- **Persistence (`src/storage/`)**: LocalStorage persistence for high scores, game statistics, and active game session state.

## Structure

| Path | Purpose |
|---|---|
| `src/` | Main application source code (components, engine, audio, storage) |
| `src/engine/` | Pure game engine, board representation, pathfinding, rule validator |
| `tests/` | Unit and integration tests (Vitest) |
| `.agents/` | Canonical agent context, skills, and task state |
| `scripts/` | Agent tooling and validation utilities |

## Public contracts

- Game state format serializable to JSON for LocalStorage saves and undo stacks.
- Classic Color Lines rule compatibility: 9x9 grid, 7 colors, 3 balls spawn per turn, lines of >= 5 matching balls clear and grant a free turn without ball spawn.

## Important invariants

- Core engine logic must remain decoupled from rendering and browser DOM to permit rigorous unit testing.
- Pathfinding must verify an unblocked orthogonal path between start and destination coordinates.
- Spawn logic must never overwrite existing occupied cells.

## Known risks and traps

- Pathfinding blocking: players can trap areas; BFS must gracefully return no-path without freezing UI.
- Diagonal line detection edge cases: off-by-one bounds checks on 9x9 matrix diagonals.

Update this file only when stable project facts change.
