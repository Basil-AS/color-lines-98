import { GameEngine } from './engine/gameengine';
import type { GameState } from './engine/gameengine';
import type { BallColor, Point } from './engine/models';

/**
 * The tutorial: a short scripted game on the real board and the real rules. It is a model only (steps, boards and
 * what counts as done); the screens decide how to show it. It never touches saved games, records or statistics.
 */
export type StepId = 'select' | 'move' | 'next' | 'line' | 'free' | 'blocked' | 'end';

export interface TutorialStep {
  id: StepId;
  /** Read-only step: a text and a "Continue" button, nothing to do on the board. */
  info: boolean;
  /** The board this step starts on; when absent the board of the previous step simply goes on. */
  setup?: { balls: readonly (readonly [number, number, BallColor])[]; next: readonly BallColor[]; nextPoints: readonly Point[] };
  /** The ball to pick up and the cell to drop it on, shown as a hint. */
  from?: Point;
  to?: Point;
}

const p = (x: number, y: number): Point => ({ x, y });

export const STEPS: readonly TutorialStep[] = [
  {
    id: 'select',
    info: false,
    setup: {
      balls: [
        [2, 4, 'red'],
        [6, 2, 'blue'],
        [5, 6, 'green'],
        [1, 1, 'yellow'],
      ],
      next: ['blue', 'green', 'yellow'],
      nextPoints: [p(7, 7), p(0, 8), p(8, 0)],
    },
    from: p(2, 4),
  },
  { id: 'move', info: false, from: p(2, 4), to: p(2, 2) },
  { id: 'next', info: true },
  {
    id: 'line',
    info: false,
    setup: {
      balls: [
        [1, 5, 'red'],
        [2, 5, 'red'],
        [3, 5, 'red'],
        [4, 5, 'red'],
        [7, 2, 'red'],
        [6, 6, 'blue'],
        [0, 0, 'green'],
      ],
      next: ['blue', 'green', 'yellow'],
      nextPoints: [p(8, 8), p(0, 8), p(8, 0)],
    },
    from: p(7, 2),
    to: p(5, 5),
  },
  { id: 'free', info: false },
  {
    id: 'blocked',
    info: false,
    setup: {
      balls: [
        [4, 4, 'red'],
        [3, 4, 'blue'],
        [5, 4, 'green'],
        [4, 3, 'yellow'],
        [4, 5, 'magenta'],
      ],
      next: ['cyan', 'blue', 'green'],
      nextPoints: [p(8, 8), p(0, 8), p(8, 0)],
    },
    from: p(4, 4),
    to: p(0, 0),
  },
  { id: 'end', info: true },
];

/** What the player did on the board. */
export type TutorialEvent =
  | { kind: 'select'; point: Point }
  | { kind: 'move'; success: boolean; cleared: number };

/** `done`: go to the next step. `retry`: the step starts again with a hint at what was missed. `ignore`: nothing happens. */
export type Verdict = 'done' | 'retry' | 'ignore';

export function judge(step: TutorialStep, e: TutorialEvent): Verdict {
  if (step.info) return 'ignore';
  switch (step.id) {
    case 'select':
      return e.kind === 'select' ? 'done' : 'ignore';
    case 'move':
    case 'free':
      return e.kind === 'move' && e.success ? 'done' : 'ignore';
    case 'line':
      if (e.kind !== 'move' || !e.success) return 'ignore';
      return e.cleared > 0 ? 'done' : 'retry';
    case 'blocked':
      if (e.kind !== 'move') return 'ignore';
      return e.success ? 'retry' : 'done';
    default:
      return 'ignore';
  }
}

/** The full game state of a step's own board. */
export function stepState(step: TutorialStep): GameState | null {
  if (!step.setup) return null;
  const board: (BallColor | null)[][] = Array.from({ length: 9 }, () => Array<BallColor | null>(9).fill(null));
  for (const [x, y, color] of step.setup.balls) board[y][x] = color;
  return {
    size: 9,
    board,
    score: 0,
    nextColors: [...step.setup.next],
    nextPoints: step.setup.nextPoints.map((q) => ({ ...q })),
    isGameOver: false,
    mode: 'classic',
  };
}

/** A fresh engine on the board of a step, with a tame random source so the balls that follow are not a surprise. */
export function stepEngine(step: TutorialStep, rng: () => number = Math.random): GameEngine | null {
  const state = stepState(step);
  return state ? GameEngine.fromState(state, rng) : null;
}
