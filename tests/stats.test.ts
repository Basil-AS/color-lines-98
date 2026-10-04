import { emptyCognition } from '../src/engine/telemetry';
import { describe, expect, it } from 'vitest';
import {
  addRecord,
  compactHistory,
  expandHistory,
  isNewRecord,
  sanitizeHistory,
  summarize,
} from '../src/stats';
import type { GameRecord } from '../src/stats';

const rec = (score: number, over: Partial<GameRecord> = {}): GameRecord => ({
  score,
  endedAt: 1_700_000_000_000 + score,
  moves: score,
  lines: 1,
  balls: 5,
  completed: true,
  maxLine: 5,
  durationMs: 60_000,
  mode: 'classic',
  ...over,
});

describe('addRecord', () => {
  it('puts the newest game first and does not mutate the input', () => {
    const before = [rec(10)];
    const after = addRecord(before, rec(20));
    expect(after.map((r) => r.score)).toEqual([20, 10]);
    expect(before).toHaveLength(1);
  });

  it('keeps every game, however many (no limit)', () => {
    let history: GameRecord[] = [];
    for (let i = 1; i <= 2500; i++) history = addRecord(history, rec(i));
    expect(history).toHaveLength(2500);
    expect(history[0].score).toBe(2500);
    expect(history[2499].score).toBe(1);
  });
});

describe('summarize', () => {
  it('handles an empty history', () => {
    expect(summarize([])).toEqual({
      gamesPlayed: 0,
      completedGames: 0,
      bestScore: 0,
      averageScore: 0,
      totalScore: 0,
      totalMoves: 0,
      totalLines: 0,
      lastScore: null,
      bestRecord: null,
    });
  });

  it('aggregates scores, moves and lines', () => {
    const s = summarize([rec(30), rec(10, { completed: false }), rec(20)]);
    expect(s.gamesPlayed).toBe(3);
    expect(s.completedGames).toBe(2);
    expect(s.bestScore).toBe(30);
    expect(s.bestRecord!.score).toBe(30);
    expect(s.totalScore).toBe(60);
    expect(s.averageScore).toBe(20);
    expect(s.totalMoves).toBe(60);
    expect(s.totalLines).toBe(3);
    expect(s.lastScore).toBe(30);
  });

  it('rounds the average to a whole number', () => {
    expect(summarize([rec(10), rec(11)]).averageScore).toBe(11);
  });
});

describe('isNewRecord', () => {
  it('needs a positive score that beats the previous best', () => {
    expect(isNewRecord(100, 50)).toBe(true);
    expect(isNewRecord(50, 50)).toBe(false);
    expect(isNewRecord(0, 0)).toBe(false);
    expect(isNewRecord(10, 0)).toBe(true);
  });
});

describe('sanitizeHistory', () => {
  it('returns [] for anything that is not an array', () => {
    for (const bad of [null, undefined, 'x', 3, {}]) expect(sanitizeHistory(bad)).toEqual([]);
  });

  it('drops malformed entries and keeps valid ones', () => {
    const good = rec(40);
    const out = sanitizeHistory([
      good,
      null,
      { ...good, score: -1 },
      { ...good, score: 1.5 },
      { ...good, endedAt: 'yesterday' },
      { ...good, completed: 'yes' },
      { score: 5 },
      { ...good, maxLine: -2 },
    ]);
    expect(out).toEqual([good]);
  });

  it('accepts older records without maxLine and durationMs', () => {
    const { maxLine: _m, durationMs: _d, mode: _mode, ...old } = rec(7);
    expect(sanitizeHistory([old])).toEqual([{ ...old, maxLine: 0, durationMs: 0, mode: 'classic' }]);
    expect(sanitizeHistory([{ ...rec(7), mode: 'chaos' }])).toEqual([]);
  });

  it('does not cap the length', () => {
    const many = Array.from({ length: 1500 }, (_, i) => rec(i));
    expect(sanitizeHistory(many)).toHaveLength(1500);
  });

  it('round-trips through the compact device format, with and without play data', () => {
    const withPlay = { ...rec(5), cog: { ...emptyCognition(), tz: 112, tm: 10, think: 40000, minEmpty: 20 } };
    const history = [withPlay, rec(4)];
    expect(expandHistory(JSON.parse(JSON.stringify(compactHistory(history))))).toEqual(history);
    expect(expandHistory([[1, 2, 3]])).toEqual([]);
  });
});
