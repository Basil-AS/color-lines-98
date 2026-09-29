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

describe('game statistics counters', () => {
  it('counts moves and cleared lines/balls', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(11));
    engine.board.clear();
    for (let x = 0; x <= 3; x++) engine.board.set(x, 0, 'red');
    engine.board.set(5, 5, 'red');
    expect(engine.moves).toBe(0);

    engine.moveBall({ x: 5, y: 5 }, { x: 4, y: 0 });
    expect(engine.moves).toBe(1);
    expect(engine.linesCleared).toBe(1);
    expect(engine.ballsCleared).toBe(5);
  });

  it('restores counters on undo', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(12));
    engine.board.clear();
    engine.board.set(0, 0, 'red');
    engine.moveBall({ x: 0, y: 0 }, { x: 1, y: 0 });
    expect(engine.moves).toBe(1);
    engine.undo();
    expect(engine.moves).toBe(0);
    expect(engine.linesCleared).toBe(0);
  });

  it('resets counters on a new game', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(13));
    engine.board.clear();
    engine.board.set(0, 0, 'red');
    engine.moveBall({ x: 0, y: 0 }, { x: 1, y: 0 });
    engine.startNewGame();
    expect([engine.moves, engine.linesCleared, engine.ballsCleared]).toEqual([0, 0, 0]);
  });

  it('round-trips counters and accepts legacy saves without them', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(14));
    engine.board.clear();
    engine.board.set(0, 0, 'red');
    engine.moveBall({ x: 0, y: 0 }, { x: 1, y: 0 });
    const state = engine.getState();
    expect(state.moves).toBe(1);
    expect(GameEngine.fromState(state)!.moves).toBe(1);

    const legacy: Record<string, unknown> = { ...state };
    delete legacy.moves;
    delete legacy.linesCleared;
    delete legacy.ballsCleared;
    const restored = GameEngine.fromState(legacy);
    expect(restored).not.toBeNull();
    expect(restored!.moves).toBe(0);
  });

  it.each([-1, 1.5, 'x'])('rejects invalid counter %j', (bad) => {
    expect(GameEngine.fromState({ ...validState(), moves: bad })).toBeNull();
  });
});

describe('engine invariants over random games', () => {
  function movableBalls(engine: GameEngine) {
    const list: { from: { x: number; y: number }; targets: string[] }[] = [];
    for (let y = 0; y < 9; y++) {
      for (let x = 0; x < 9; x++) {
        if (!engine.board.get(x, y)) continue;
        engine.select({ x, y });
        const targets = [...engine.getReachableCells()];
        if (targets.length > 0) list.push({ from: { x, y }, targets });
      }
    }
    engine.unselect();
    return list;
  }

  function playRandomGame(seed: number, maxMoves: number) {
    const rng = seededRng(seed);
    const engine = new GameEngine(9, 3, 5, 'gamos', rng);
    let moves = 0;
    while (!engine.isGameOver && moves < maxMoves) {
      const movable = movableBalls(engine);
      if (movable.length === 0) break; // every ball is walled in
      const pick = movable[Math.floor(rng() * movable.length)];
      const [tx, ty] = pick.targets[Math.floor(rng() * pick.targets.length)].split(',').map(Number);
      const ballsBefore = 81 - engine.board.getEmptyCells().length;
      const scoreBefore = engine.score;
      const movesBefore = engine.moves;

      const res = engine.moveBall(pick.from, { x: tx, y: ty });
      expect(res.success).toBe(true);
      moves++;

      const ballsAfter = 81 - engine.board.getEmptyCells().length;
      expect(ballsAfter).toBe(ballsBefore + res.spawnedBalls.length - res.clearedPoints.length);
      expect(engine.score - scoreBefore).toBe(res.pointsEarned);
      expect(engine.moves).toBe(movesBefore + 1);
      // Nothing that could still be cleared may remain on the board.
      expect(LineDetector.findLines(engine.board, 5).matchedPoints).toHaveLength(0);
      expect(engine.isGameOver).toBe(ballsAfter === 81);
      expect(engine.nextColors).toHaveLength(3);
    }
    return { engine, moves };
  }

  it.each([1, 2, 3, 4, 5, 6, 7, 8])('keeps all invariants for seed %i', (seed) => {
    const { engine, moves } = playRandomGame(seed, 400);
    expect(moves).toBeGreaterThan(5);
    expect(engine.score).toBeGreaterThanOrEqual(0);
  });

  it('undo restores the exact previous state after every move', () => {
    const rng = seededRng(99);
    const engine = new GameEngine(9, 3, 5, 'gamos', rng);
    for (let i = 0; i < 60 && !engine.isGameOver; i++) {
      let moved = false;
      for (let y = 0; y < 9 && !moved; y++) {
        for (let x = 0; x < 9 && !moved; x++) {
          if (!engine.board.get(x, y)) continue;
          engine.select({ x, y });
          const t = [...engine.getReachableCells()][0];
          if (!t) continue;
          const [tx, ty] = t.split(',').map(Number);
          const before = JSON.stringify(engine.getState());
          expect(engine.moveBall({ x, y }, { x: tx, y: ty }).success).toBe(true);
          if (!engine.isGameOver) {
            expect(engine.undo()).toBe(true);
            expect(JSON.stringify(engine.getState())).toBe(before);
            engine.moveBall({ x, y }, { x: tx, y: ty });
          }
          moved = true;
        }
      }
      if (!moved) break;
    }
  });

  it('ends the game when the board fills up', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(5));
    engine.board.clear();
    // Neighbouring cells along every axis always differ, so no line can form.
    const palette = ['red', 'green', 'blue', 'cyan', 'magenta', 'yellow', 'brown'] as const;
    for (let y = 0; y < 9; y++) {
      for (let x = 0; x < 9; x++) {
        engine.board.set(x, y, palette[(x + 2 * y) % 7]);
      }
    }
    expect(LineDetector.findLines(engine.board, 5).matchedPoints).toHaveLength(0);
    // Exactly three empty cells: the move keeps three empty, the spawn fills them.
    engine.board.set(0, 0, null);
    engine.board.set(1, 0, null);
    engine.board.set(8, 8, null);

    const res = engine.moveBall({ x: 2, y: 0 }, { x: 1, y: 0 });
    expect(res.success).toBe(true);
    expect(res.spawnedBalls).toHaveLength(3);
    expect(res.isGameOver).toBe(true);
    expect(engine.isGameOver).toBe(true);
    expect(engine.canUndo).toBe(false);
    expect(engine.moveBall({ x: 3, y: 0 }, { x: 2, y: 0 }).success).toBe(false);
  });
});

describe('spawn preview (cells where the next balls will appear)', () => {
  const key = (p: { x: number; y: number }) => `${p.x},${p.y}`;

  it('plans three distinct empty cells for a new game', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(21));
    expect(engine.nextSpawnPoints).toHaveLength(3);
    expect(new Set(engine.nextSpawnPoints.map(key)).size).toBe(3);
    for (const p of engine.nextSpawnPoints) expect(engine.board.isEmpty(p.x, p.y)).toBe(true);
  });

  it('spawns exactly where it announced', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(22));
    engine.board.clear();
    engine.board.set(0, 0, 'red');
    // A full-board plan is needed after clear(): re-plan through a harmless move first.
    engine.moveBall({ x: 0, y: 0 }, { x: 0, y: 1 });
    const planned = engine.nextSpawnPoints.map((p) => ({ ...p }));
    const colors = [...engine.nextColors];
    const target = engine.board.getEmptyCells().find((c) => !planned.some((p) => key(p) === key(c)))!;
    const from = findBall(engine);

    const res = engine.moveBall(from, target);
    expect(res.success).toBe(true);
    if (res.clearedPoints.length === 0) {
      expect(res.spawnedBalls.map((s) => key(s.point)).sort()).toEqual(planned.map(key).sort());
      expect(res.spawnedBalls.map((s) => s.color).sort()).toEqual([...colors].sort());
    }
  });

  it('relocates a planned cell that the player moves onto', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(23));
    const from = findBall(engine);
    const target = engine.nextSpawnPoints[0];
    const res = engine.moveBall(from, target);
    expect(res.success).toBe(true);
    expect(res.spawnedBalls).toHaveLength(3);
    expect(new Set(res.spawnedBalls.map((s) => key(s.point))).size).toBe(3);
    expect(engine.board.get(target.x, target.y)).not.toBeNull();
  });

  it('keeps the preview valid after every move of random games', () => {
    const rng = seededRng(24);
    const engine = new GameEngine(9, 3, 5, 'gamos', rng);
    for (let i = 0; i < 120 && !engine.isGameOver; i++) {
      const balls: { x: number; y: number }[] = [];
      for (let y = 0; y < 9; y++) for (let x = 0; x < 9; x++) if (engine.board.get(x, y)) balls.push({ x, y });
      let moved = false;
      for (const b of balls) {
        engine.select(b);
        const t = [...engine.getReachableCells()][0];
        if (!t) continue;
        const [tx, ty] = t.split(',').map(Number);
        engine.moveBall(b, { x: tx, y: ty });
        moved = true;
        break;
      }
      if (!moved) break;
      const empty = engine.board.getEmptyCells().length;
      expect(engine.nextSpawnPoints).toHaveLength(Math.min(3, empty));
      expect(new Set(engine.nextSpawnPoints.map(key)).size).toBe(engine.nextSpawnPoints.length);
      for (const p of engine.nextSpawnPoints) expect(engine.board.isEmpty(p.x, p.y)).toBe(true);
    }
  });

  it('restores the preview on undo and through saved state', () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', seededRng(25));
    const before = JSON.stringify(engine.nextSpawnPoints);
    const from = findBall(engine);
    const target = engine.board.getEmptyCells().find(
      (c) => !engine.nextSpawnPoints.some((p) => key(p) === key(c))
    )!;
    engine.moveBall(from, target);
    engine.undo();
    expect(JSON.stringify(engine.nextSpawnPoints)).toBe(before);

    const restored = GameEngine.fromState(JSON.parse(JSON.stringify(engine.getState())));
    expect(restored!.nextSpawnPoints).toEqual(engine.nextSpawnPoints);
  });

  it('re-plans when loading a legacy save without preview cells and rejects bad ones', () => {
    const state = new GameEngine(9, 3, 5, 'gamos', seededRng(26)).getState();
    const legacy: Record<string, unknown> = { ...state };
    delete legacy.nextPoints;
    const restored = GameEngine.fromState(legacy);
    expect(restored!.nextSpawnPoints).toHaveLength(3);

    const occupied = findBall(new GameEngine(9, 3, 5, 'gamos', seededRng(26)));
    for (const bad of [
      [{ x: 99, y: 0 }, { x: 1, y: 1 }, { x: 2, y: 2 }],
      [{ x: 1, y: 1 }, { x: 1, y: 1 }, { x: 2, y: 2 }],
      [occupied, { x: 1, y: 1 }, { x: 2, y: 2 }],
      'nope',
    ]) {
      expect(GameEngine.fromState({ ...state, nextPoints: bad })).toBeNull();
    }
  });
});
