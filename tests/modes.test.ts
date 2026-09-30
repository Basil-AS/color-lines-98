import { describe, expect, it } from 'vitest';
import { GameEngine } from '../src/engine/gameengine';
import { MODES, MODE_IDS } from '../src/engine/modes';
import { hashString, mixSeed, mulberry32 } from '../src/engine/rng';
import { createEngine, dailySeed, remainingMs } from '../src/modes';

function boardKey(e: GameEngine): string {
  return JSON.stringify([e.getState().board, e.nextColors, e.nextSpawnPoints, e.score]);
}

/** A deterministic "player": always moves the first movable ball to its first reachable cell. */
function playFirstMoves(e: GameEngine, count: number) {
  for (let i = 0; i < count && !e.isGameOver; i++) {
    let moved = false;
    for (let y = 0; y < 9 && !moved; y++) {
      for (let x = 0; x < 9 && !moved; x++) {
        if (!e.board.get(x, y)) continue;
        e.select({ x, y });
        const target = [...e.getReachableCells()].sort()[0];
        e.unselect();
        if (!target) continue;
        const [tx, ty] = target.split(',').map(Number);
        moved = e.moveBall({ x, y }, { x: tx, y: ty }).success;
      }
    }
    if (!moved) break;
  }
}

describe('rng helpers', () => {
  it('mulberry32 is deterministic, in [0,1) and differs per seed', () => {
    const a = mulberry32(1);
    const b = mulberry32(1);
    const seq = Array.from({ length: 50 }, () => a());
    expect(seq).toEqual(Array.from({ length: 50 }, () => b()));
    expect(seq.every((v) => v >= 0 && v < 1)).toBe(true);
    expect(seq).not.toEqual(Array.from({ length: 50 }, ((r) => () => r())(mulberry32(2))));
  });

  it('hashes strings and mixes seeds stably', () => {
    expect(hashString('2026-09-30')).toBe(hashString('2026-09-30'));
    expect(hashString('2026-09-30')).not.toBe(hashString('2026-10-01'));
    expect(mixSeed(5, 1)).not.toBe(mixSeed(5, 2));
    expect(mixSeed(5, 1)).toBe(mixSeed(5, 1));
  });
});

describe('mode definitions', () => {
  it('has the four modes with their rules', () => {
    expect(MODE_IDS).toEqual(['classic', 'easy', 'blitz', 'daily']);
    expect(MODES.classic).toMatchObject({ colors: 7, timeLimitMs: null, seeded: false });
    expect(MODES.easy.colors).toBe(5);
    expect(MODES.blitz.timeLimitMs).toBe(180_000);
    expect(MODES.daily.seeded).toBe(true);
  });
});

describe('colours of a mode', () => {
  it('easy mode only ever uses five colours', () => {
    const e = createEngine('easy');
    const seen = new Set<string>();
    for (let i = 0; i < 60 && !e.isGameOver; i++) {
      playFirstMoves(e, 1);
      e.getState().board.flat().forEach((c) => c && seen.add(c));
      e.nextColors.forEach((c) => seen.add(c));
    }
    expect(e.colors).toHaveLength(5);
    expect(seen.size).toBeLessThanOrEqual(5);
    expect(e.mode).toBe('easy');
  });
});

describe('seeded (daily) games', () => {
  it('start identically for the same day and differently for another', () => {
    const a = createEngine('daily', { dayKey: '2026-09-30' });
    const b = createEngine('daily', { dayKey: '2026-09-30' });
    const c = createEngine('daily', { dayKey: '2026-10-01' });
    expect(boardKey(a)).toBe(boardKey(b));
    expect(boardKey(a)).not.toBe(boardKey(c));
    expect(dailySeed('2026-09-30')).toBe(dailySeed('2026-09-30'));
  });

  it('play out the same way when the moves are the same', () => {
    const a = createEngine('daily', { dayKey: '2026-09-30' });
    const b = createEngine('daily', { dayKey: '2026-09-30' });
    playFirstMoves(a, 40);
    playFirstMoves(b, 40);
    expect(boardKey(a)).toBe(boardKey(b));
    expect(a.moves).toBeGreaterThan(10);
  });

  it('are not disturbed by an undo and a repeated move', () => {
    const a = createEngine('daily', { dayKey: '2026-09-30' });
    const b = createEngine('daily', { dayKey: '2026-09-30' });
    playFirstMoves(a, 8);
    playFirstMoves(b, 8);
    a.undo();
    playFirstMoves(a, 1);
    expect(boardKey(a)).toBe(boardKey(b));
  });

  it('continue identically after being saved and restored', () => {
    const a = createEngine('daily', { dayKey: '2026-09-30' });
    playFirstMoves(a, 12);
    const restored = GameEngine.fromState(JSON.parse(JSON.stringify(a.getState())))!;
    expect(restored.mode).toBe('daily');
    playFirstMoves(a, 20);
    playFirstMoves(restored, 20);
    expect(boardKey(restored)).toBe(boardKey(a));
  });
});

describe('blitz', () => {
  it('counts down the active play time and can be ended', () => {
    const e = createEngine('blitz');
    expect(remainingMs('blitz', e)).toBe(180_000);
    e.addPlayTime(60_000);
    expect(remainingMs('blitz', e)).toBe(120_000);
    e.addPlayTime(500_000);
    expect(remainingMs('blitz', e)).toBe(0);
    e.endGame();
    expect(e.isGameOver).toBe(true);
    expect(e.canUndo).toBe(false);
    expect(remainingMs('classic', createEngine('classic'))).toBeNull();
  });
});

describe('saved state of a mode', () => {
  it('rejects unknown modes, colours and seeds', () => {
    const base = createEngine('easy').getState();
    expect(GameEngine.fromState({ ...base, mode: 'chaos' })).toBeNull();
    expect(GameEngine.fromState({ ...base, colors: ['red', 'purple'] })).toBeNull();
    expect(GameEngine.fromState({ ...base, colors: ['red', 'green'] })).toBeNull();
    expect(GameEngine.fromState({ ...base, seed: 'x' })).toBeNull();
    expect(GameEngine.fromState(base)!.colors).toHaveLength(5);
  });

  it('loads older saves as a classic game', () => {
    const legacy: Record<string, unknown> = { ...new GameEngine().getState() };
    delete legacy.mode;
    delete legacy.colors;
    delete legacy.seed;
    const e = GameEngine.fromState(legacy)!;
    expect(e.mode).toBe('classic');
    expect(e.colors).toHaveLength(7);
  });
});
