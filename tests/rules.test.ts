import { describe, expect, it } from 'vitest';
import { Board } from '../src/engine/board';
import { GameEngine } from '../src/engine/gameengine';
import { LineDetector } from '../src/engine/linedetector';
import { PathFinder } from '../src/engine/pathfinder';
import { ALL_COLORS } from '../src/engine/models';

/**
 * The rules of Color Lines / Lines 98 as documented by the originals (see docs/ORIGINALS.md),
 * one test per rule so a regression names the rule that broke.
 */
function seeded(seed: number): () => number {
  let s = seed >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const fresh = (seed = 1) => new GameEngine(9, 3, 5, 'gamos', seeded(seed));

describe('rules of the original game', () => {
  it('is played on a 9x9 board with seven colours', () => {
    const e = fresh();
    expect(e.size).toBe(9);
    expect(ALL_COLORS).toHaveLength(7);
  });

  it('starts with five balls and shows three next colours', () => {
    const e = fresh();
    expect(81 - e.board.getEmptyCells().length).toBe(5);
    expect(e.nextColors).toHaveLength(3);
    expect(e.score).toBe(0);
  });

  it('moves a ball only through free cells, in four directions (no diagonal steps)', () => {
    const board = new Board(9);
    // Walls on the orthogonal neighbours of (0,0)'s diagonal target: (1,1) is reachable only around them.
    board.set(1, 0, 'red');
    board.set(0, 1, 'red');
    expect(PathFinder.findPath({ x: 0, y: 0 }, { x: 1, y: 1 }, board)).toBeNull();
    // Every step of a found path changes exactly one coordinate.
    const path = PathFinder.findPath({ x: 0, y: 8 }, { x: 8, y: 0 }, new Board(9))!;
    for (let i = 1; i < path.length; i++) {
      expect(Math.abs(path[i].x - path[i - 1].x) + Math.abs(path[i].y - path[i - 1].y)).toBe(1);
    }
  });

  it('refuses a move when the path is blocked', () => {
    const e = fresh();
    e.board.clear();
    e.board.set(0, 0, 'red');
    e.board.set(1, 0, 'green');
    e.board.set(0, 1, 'green');
    const res = e.moveBall({ x: 0, y: 0 }, { x: 5, y: 5 });
    expect(res.success).toBe(false);
    expect(e.moves).toBe(0);
  });

  it('clears lines of five or more in a row, a column and both diagonals', () => {
    const shapes: Array<(i: number) => [number, number]> = [(i) => [i, 2], (i) => [2, i], (i) => [i, i], (i) => [i, 6 - i]];
    for (const at of shapes) {
      const b = new Board(9);
      for (let i = 0; i < 5; i++) b.set(...at(i), 'blue');
      expect(LineDetector.findLines(b, 5).matchedPoints).toHaveLength(5);
    }
    const four = new Board(9);
    for (let i = 0; i < 4; i++) four.set(i, 0, 'blue');
    expect(LineDetector.findLines(four, 5).matchedPoints).toHaveLength(0);
  });

  it('scores 10, 12, 18, 28 and 42 for lines of 5 to 9 balls', () => {
    expect([5, 6, 7, 8, 9].map((n) => LineDetector.calculateScore(n, 'gamos'))).toEqual([10, 12, 18, 28, 42]);
    expect([5, 6, 7, 8, 9].map((n) => LineDetector.calculateScore(n, 'lines98'))).toEqual([10, 12, 18, 28, 42]);
  });

  it('gives a free turn after clearing a line: no new balls, the announced colours stay', () => {
    const e = fresh(3);
    e.board.clear();
    for (let x = 0; x < 4; x++) e.board.set(x, 0, 'red');
    e.board.set(5, 5, 'red');
    const next = [...e.nextColors];
    const res = e.moveBall({ x: 5, y: 5 }, { x: 4, y: 0 });
    expect(res.clearedPoints).toHaveLength(5);
    expect(res.spawnedBalls).toHaveLength(0);
    expect(e.nextColors).toEqual(next);
    expect(e.board.getEmptyCells()).toHaveLength(81);
  });

  it('otherwise adds three new balls of the announced colours and announces three new ones', () => {
    const e = fresh(4);
    e.board.clear();
    e.board.set(0, 0, 'red');
    e.board.set(1, 1, 'green');
    e.board.set(2, 2, 'blue');
    const announced = [...e.nextColors];
    // A repair pass keeps the plan valid after clear(): make one harmless move first.
    const res = e.moveBall({ x: 0, y: 0 }, { x: 0, y: 5 });
    expect(res.success).toBe(true);
    if (res.clearedPoints.length === 0) {
      expect(res.spawnedBalls).toHaveLength(3);
      expect(res.spawnedBalls.map((s) => s.color)).toEqual(announced);
    }
    expect(e.nextColors).toHaveLength(3);
  });

  it('clears and scores lines made by the new balls themselves', () => {
    const e = fresh(5);
    e.board.clear();
    // Four reds in a row and a fifth cell that the coming red will fill.
    for (let x = 0; x < 4; x++) e.board.set(x, 8, 'red');
    e.board.set(0, 0, 'blue');
    e.nextColors = ['red', 'green', 'green'];
    e.nextSpawnPoints = [{ x: 4, y: 8 }, { x: 7, y: 3 }, { x: 8, y: 3 }];
    const res = e.moveBall({ x: 0, y: 0 }, { x: 0, y: 1 });
    expect(res.spawnedBalls).toHaveLength(3);
    expect(res.clearedPoints).toHaveLength(5);
    expect(e.score).toBe(10);
  });

  it('ends the game when the board is full', () => {
    const e = fresh(6);
    e.board.clear();
    const palette = ALL_COLORS;
    for (let y = 0; y < 9; y++) for (let x = 0; x < 9; x++) e.board.set(x, y, palette[(x + 2 * y) % 7]);
    e.board.set(0, 0, null);
    e.board.set(1, 0, null);
    e.board.set(8, 8, null);
    const res = e.moveBall({ x: 2, y: 0 }, { x: 1, y: 0 });
    expect(res.isGameOver).toBe(true);
    expect(e.moveBall({ x: 3, y: 0 }, { x: 2, y: 0 }).success).toBe(false);
  });

  it('never starts a game with a finished line', () => {
    for (let seed = 1; seed <= 100; seed++) {
      expect(LineDetector.findLines(fresh(seed).board, 5).matchedPoints).toHaveLength(0);
    }
  });
});
