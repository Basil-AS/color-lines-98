import { describe, expect, it } from 'vitest';
import {
  dailyActivity,
  efficiency,
  levelEta,
  recordProgression,
  scoreHistogram,
  trendOf,
  weekdayActivity,
} from '../src/insights';
import type { GameRecord } from '../src/stats';

const at = (y: number, m: number, d: number, h = 12) => new Date(y, m - 1, d, h).getTime();
const g = (score: number, over: Partial<GameRecord> = {}): GameRecord => ({
  score,
  endedAt: at(2026, 9, 29),
  moves: 20,
  lines: 2,
  balls: 10,
  completed: true,
  maxLine: 5,
  durationMs: 60_000,
  ...over,
});

describe('trendOf', () => {
  it('needs enough games to say anything', () => {
    expect(trendOf([g(10), g(20)], 5)).toBeNull();
  });

  it('compares the latest games with the ones before', () => {
    const newestFirst = [...Array(5).fill(120), ...Array(5).fill(100)].map((s) => g(s));
    const t = trendOf(newestFirst, 5)!;
    expect(t.recentAverage).toBe(120);
    expect(t.previousAverage).toBe(100);
    expect(t.changePercent).toBe(20);
    expect(t.direction).toBe('up');
  });

  it('reports down and flat trends', () => {
    expect(trendOf([...Array(5).fill(80), ...Array(5).fill(100)].map((s) => g(s)), 5)!.direction).toBe('down');
    expect(trendOf(Array.from({ length: 10 }, () => g(100)), 5)!.direction).toBe('flat');
    // A change below 3 % is noise.
    expect(trendOf([...Array(5).fill(101), ...Array(5).fill(100)].map((s) => g(s)), 5)!.direction).toBe('flat');
  });

  it('avoids dividing by zero', () => {
    const t = trendOf([...Array(5).fill(50), ...Array(5).fill(0)].map((s) => g(s)), 5)!;
    expect(Number.isFinite(t.changePercent)).toBe(true);
  });
});

describe('dailyActivity', () => {
  it('returns one entry per day, oldest first, with games and the best score', () => {
    const history = [
      g(50, { endedAt: at(2026, 9, 29, 20) }),
      g(80, { endedAt: at(2026, 9, 29, 9) }),
      g(30, { endedAt: at(2026, 9, 27) }),
    ];
    const days = dailyActivity(history, 5, at(2026, 9, 29));
    expect(days.map((d) => d.day)).toEqual(['2026-09-25', '2026-09-26', '2026-09-27', '2026-09-28', '2026-09-29']);
    expect(days.map((d) => d.games)).toEqual([0, 0, 1, 0, 2]);
    expect(days[4].best).toBe(80);
    expect(days[0].best).toBe(0);
  });

  it('handles an empty history', () => {
    expect(dailyActivity([], 3, at(2026, 9, 29)).every((d) => d.games === 0)).toBe(true);
  });
});

describe('scoreHistogram', () => {
  it('groups scores into equal buckets and keeps empty buckets in between', () => {
    const bars = scoreHistogram([g(5), g(15), g(16), g(95)].map((x) => x), 50);
    expect(bars.map((b) => b.from)).toEqual([0, 50]);
    expect(bars.map((b) => b.count)).toEqual([3, 1]);
    expect(scoreHistogram([g(10), g(160)], 50).map((b) => b.count)).toEqual([1, 0, 0, 1]);
    expect(scoreHistogram([], 50)).toEqual([]);
  });
});

describe('weekdayActivity', () => {
  it('counts games and averages the score per weekday (Monday first)', () => {
    // 2026-09-28 is a Monday, 2026-09-29 a Tuesday.
    const w = weekdayActivity([g(10, { endedAt: at(2026, 9, 28) }), g(30, { endedAt: at(2026, 9, 28) }), g(50, { endedAt: at(2026, 9, 29) })]);
    expect(w).toHaveLength(7);
    expect(w[0]).toEqual({ weekday: 0, games: 2, average: 20 });
    expect(w[1]).toEqual({ weekday: 1, games: 1, average: 50 });
    expect(w[6]).toEqual({ weekday: 6, games: 0, average: 0 });
  });
});

describe('efficiency', () => {
  it('measures points per move over finished and unfinished games', () => {
    const e = efficiency([g(100, { moves: 50 }), g(30, { moves: 10 }), g(0, { moves: 0 })]);
    expect(e.average).toBeCloseTo(130 / 60, 5);
    expect(e.best).toBe(3);
    expect(efficiency([])).toEqual({ average: 0, best: 0 });
  });
});

describe('recordProgression', () => {
  it('lists when the personal best was improved, oldest first', () => {
    const newestFirst = [g(90, { endedAt: 4 }), g(40, { endedAt: 3 }), g(60, { endedAt: 2 }), g(30, { endedAt: 1 })];
    expect(recordProgression(newestFirst).map((r) => [r.score, r.at])).toEqual([
      [30, 1],
      [60, 2],
      [90, 4],
    ]);
    expect(recordProgression([])).toEqual([]);
  });
});

describe('levelEta', () => {
  it('estimates the games needed to reach the next level from recent experience', () => {
    const newestFirst = Array.from({ length: 5 }, () => g(40, { lines: 2 })); // 40 + 10 + 10 = 60 xp per game
    expect(levelEta(newestFirst, 120)).toBe(2);
    expect(levelEta(newestFirst, 0)).toBe(0);
  });

  it('returns null without games to base it on and counts the 10 xp every game gives', () => {
    expect(levelEta([], 100)).toBeNull();
    expect(levelEta([g(0, { lines: 0 })], 100)).toBe(10);
  });
});
