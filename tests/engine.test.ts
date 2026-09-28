import { describe, it, expect } from 'vitest';
import { Board } from '../src/engine/board';
import { PathFinder } from '../src/engine/pathfinder';
import { LineDetector } from '../src/engine/linedetector';
import { GameEngine } from '../src/engine/gameengine';

describe('TypeScript Game Engine', () => {
  it('initializes board and handles basic get/set', () => {
    const board = new Board(9);
    expect(board.getEmptyCells().length).toBe(81);

    board.set(0, 0, 'red');
    board.set(8, 8, 'blue');
    expect(board.get(0, 0)).toBe('red');
    expect(board.get(8, 8)).toBe('blue');
    expect(board.isEmpty(1, 1)).toBe(true);

    const copy = board.copy();
    copy.set(0, 0, 'green');
    expect(board.get(0, 0)).toBe('red');
    expect(copy.get(0, 0)).toBe('green');
  });

  it('finds path around obstacles with BFS', () => {
    const board = new Board(9);
    const start = { x: 0, y: 0 };
    const target = { x: 2, y: 0 };

    board.set(1, 0, 'brown'); // Wall

    const path = PathFinder.findPath(start, target, board);
    expect(path).not.toBeNull();
    expect(path!.length).toBeGreaterThanOrEqual(5);
    expect(path![0]).toEqual(start);
    expect(path![path!.length - 1]).toEqual(target);
    expect(path!.some((p) => p.x === 1 && p.y === 0)).toBe(false);
  });

  it('detects horizontal line of 5 balls and computes score', () => {
    const board = new Board(9);
    for (let x = 2; x <= 6; x++) {
      board.set(x, 3, 'red');
    }

    const match = LineDetector.findLines(board, 5, 'gamos');
    expect(match.matchedPoints.length).toBe(5);
    expect(match.score).toBe(10);
  });

  it('detects vertical line of 6 balls and computes score 12', () => {
    const board = new Board(9);
    for (let y = 1; y <= 6; y++) {
      board.set(4, y, 'blue');
    }

    const match = LineDetector.findLines(board, 5, 'gamos');
    expect(match.matchedPoints.length).toBe(6);
    expect(match.score).toBe(12); // 2*(36) - 120 + 60 = 12
  });

  it('detects diagonal lines', () => {
    const board = new Board(9);
    for (let i = 0; i < 5; i++) {
      board.set(i, i, 'yellow');
    }

    const match = LineDetector.findLines(board, 5);
    expect(match.matchedPoints.length).toBe(5);
    expect(match.score).toBe(10);
  });

  it('detects intersecting cross lines with 9 unique balls', () => {
    const board = new Board(9);
    for (let x = 2; x <= 6; x++) {
      board.set(x, 4, 'green');
    }
    for (let y = 2; y <= 6; y++) {
      board.set(4, y, 'green');
    }

    const match = LineDetector.findLines(board, 5);
    expect(match.lines.length).toBe(2);
    expect(match.matchedPoints.length).toBe(9);
    expect(match.score).toBe(20);
  });

  it('clears line on move without spawning balls', () => {
    const engine = new GameEngine(9);
    engine.board.clear();

    for (let x = 0; x <= 3; x++) {
      engine.board.set(x, 0, 'red');
    }
    engine.board.set(5, 5, 'red');

    const emptyBefore = engine.board.getEmptyCells().length;

    const res = engine.moveBall({ x: 5, y: 5 }, { x: 4, y: 0 });
    expect(res.success).toBe(true);
    expect(res.clearedPoints.length).toBe(5);
    expect(res.pointsEarned).toBe(10);
    expect(res.spawnedBalls.length).toBe(0);
    expect(engine.score).toBe(10);

    for (let x = 0; x <= 4; x++) {
      expect(engine.board.get(x, 0)).toBeNull();
    }
    expect(engine.board.getEmptyCells().length).toBe(emptyBefore + 5);
  });

  it('supports undo', () => {
    const engine = new GameEngine(9);
    engine.board.clear();

    engine.board.set(0, 0, 'red');
    const initialEmpty = engine.board.getEmptyCells().length;

    const res = engine.moveBall({ x: 0, y: 0 }, { x: 1, y: 0 });
    expect(res.success).toBe(true);
    expect(res.spawnedBalls.length).toBe(3);

    expect(engine.canUndo).toBe(true);
    const undone = engine.undo();
    expect(undone).toBe(true);

    expect(engine.board.get(0, 0)).toBe('red');
    expect(engine.board.get(1, 0)).toBeNull();
    expect(engine.board.getEmptyCells().length).toBe(initialEmpty);
  });
});
