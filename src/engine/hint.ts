import { Board } from './board';
import { LineDetector } from './linedetector';
import type { Point } from './models';
import { PathFinder } from './pathfinder';

export interface Hint {
  from: Point;
  to: Point;
  /** True when the move completes a line (and, by the rules, earns a free turn). */
  clears: boolean;
  /** Balls removed by the move, or the longest run it builds when nothing is cleared. */
  value: number;
}

const AXES: Point[] = [
  { x: 1, y: 0 },
  { x: 0, y: 1 },
  { x: 1, y: 1 },
  { x: 1, y: -1 }
];

/** Longest run of the ball's colour through (x, y) on any axis. */
function longestRun(board: Board, x: number, y: number): number {
  const color = board.get(x, y);
  if (!color) return 0;
  let best = 1;
  for (const a of AXES) {
    let n = 1;
    for (const s of [1, -1]) {
      let cx = x + a.x * s;
      let cy = y + a.y * s;
      while (board.get(cx, cy) === color) {
        n++;
        cx += a.x * s;
        cy += a.y * s;
      }
    }
    best = Math.max(best, n);
  }
  return best;
}

/**
 * Suggests a move: the one that clears the most balls, or failing that the one
 * that builds the longest run. Null when no ball can move at all.
 */
export function findHint(board: Board, minLength = LineDetector.DEFAULT_MIN_LENGTH): Hint | null {
  let best: Hint | null = null;
  let bestKey = -1;
  for (let y = 0; y < board.size; y++) {
    for (let x = 0; x < board.size; x++) {
      const color = board.get(x, y);
      if (!color) continue;
      const from = { x, y };
      for (const key of PathFinder.getReachableCells(from, board)) {
        const [tx, ty] = key.split(',').map(Number);
        const trial = board.copy();
        trial.set(x, y, null);
        trial.set(tx, ty, color);
        const cleared = LineDetector.findLines(trial, minLength).matchedPoints.length;
        const run = cleared > 0 ? cleared : longestRun(trial, tx, ty);
        // A clear always beats a build; leaving a ball where it was already in a run is no progress.
        const gain = cleared > 0 ? 100 + cleared : run - longestRun(board, x, y);
        if (gain <= 0 && best) continue;
        if (gain > bestKey) {
          bestKey = gain;
          best = { from, to: { x: tx, y: ty }, clears: cleared > 0, value: run };
        }
      }
    }
  }
  return best;
}
