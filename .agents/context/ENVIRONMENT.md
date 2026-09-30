# Environment

STATUS: ready
UPDATED_AT: 2026-09-28

## Required runtimes

- Node.js >= 20 (verified on v22.23.2)
- npm >= 10 (verified on 10.9.8)

## Commands

| Action | Command |
|---|---|
| install | `npm install` |
| run | `npm run dev` |
| test | `npm test` |
| lint | `npm run lint` |
| typecheck | `npm run typecheck` |
| build | `npm run build` |

## Services and infrastructure

- Modern client-side web application (Vite + React 19 + TypeScript).
- No external backend required for core gameplay; state stored in `localStorage`.

## Portability constraints

- Standard Web APIs (Canvas 2D / DOM, Web Audio API, Web Storage).
- Fully responsive across desktop, tablet, and mobile screens.

Machine-specific paths belong in ignored `AGENT.local.md`, not here.
