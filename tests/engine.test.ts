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

describe('LineDetector regressions', () => {
  it('scores an anti-diagonal (/) line of 7 as a single line', () => {
    const board = new Board(9);
    for (let i = 0; i < 7; i++) {
      board.set(i, 8 - i, 'magenta');
    }

    const match = LineDetector.findLines(board, 5, 'gamos');
    expect(match.lines.length).toBe(1);
    expect(match.lines[0].length).toBe(7);
    expect(match.matchedPoints.length).toBe(7);
    expect(match.score).toBe(18);
  });

  it('scores every axis of a long line identically', () => {
    const shapes: Array<(i: number) => [number, number]> = [
      (i) => [i, 0],
      (i) => [0, i],
      (i) => [i, i],
      (i) => [i, 8 - i],
    ];
    for (const at of shapes) {
      const board = new Board(9);
      for (let i = 0; i < 9; i++) {
        const [x, y] = at(i);
        board.set(x, y, 'cyan');
      }
      const match = LineDetector.findLines(board, 5, 'gamos');
      expect(match.lines.length).toBe(1);
      expect(match.score).toBe(42);
    }
  });
});

function seededRng(seed: number): () => number {
  let s = seed >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

describe('Deterministic play and persistence', () => {
  it('produces identical games from identical RNG seeds', () => {
    const a = new GameEngine(9, 3, 5, 'gamos', seededRng(42));
    const b = new GameEngine(9, 3, 5, 'gamos', seededRng(42));
    expect(a.getState()).toEqual(b.getState());
    expect(a.nextColors).toHaveLength(3);
  });

  it('starts a new game with exactly 5 balls on the board', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(7));
    expect(81 - engine.board.getEmptyCells().length).toBe(5);
    expect(engine.score).toBe(0);
    expect(engine.isGameOver).toBe(false);
  });

  it('round-trips state through JSON', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(1));
    const ball = findBall(engine);
    const target = engine.board.getEmptyCells()[0];
    engine.moveBall(ball, target);

    const restored = GameEngine.fromState(JSON.parse(JSON.stringify(engine.getState())));
    expect(restored).not.toBeNull();
    expect(restored!.getState()).toEqual(engine.getState());
  });

  it.each([
    ['null', null],
    ['a string', 'nope'],
    ['wrong board size', { ...validState(), board: [[null]] }],
    ['unknown color', mutate((s) => { s.board[0][0] = 'purple'; })],
    ['negative score', { ...validState(), score: -5 }],
    ['fractional score', { ...validState(), score: 1.5 }],
    ['bad nextColors', { ...validState(), nextColors: ['red'] }],
    ['non-boolean gameOver', { ...validState(), isGameOver: 'yes' }],
  ])('rejects malformed state: %s', (_name, value) => {
    expect(GameEngine.fromState(value)).toBeNull();
  });
});

function findBall(engine: GameEngine) {
  for (let y = 0; y < engine.size; y++) {
    for (let x = 0; x < engine.size; x++) {
      if (engine.board.get(x, y)) return { x, y };
    }
  }
  throw new Error('no ball on board');
}

function validState() {
  return new GameEngine(9, 3, 5, 'gamos', seededRng(3)).getState();
}

function mutate(fn: (s: ReturnType<typeof validState>) => void) {
  const s = validState();
  fn(s as never);
  return s;
}

describe('selection', () => {
  it('selects only cells that hold a ball', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(5));
    engine.board.clear();
    engine.board.set(2, 2, 'red');

    engine.select({ x: 4, y: 4 });
    expect(engine.selectedPoint).toBeNull();

    engine.select({ x: 2, y: 2 });
    expect(engine.selectedPoint).toEqual({ x: 2, y: 2 });

    engine.unselect();
    expect(engine.selectedPoint).toBeNull();
  });
});
