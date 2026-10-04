import { describe, expect, it } from 'vitest';
import { chronotypeOf, findings, mindReport } from '../src/cognition';
import { emptyCognition } from '../src/engine/telemetry';
import type { GameRecord } from '../src/stats';

const TZ = 100; // UTC
const MIN = 60_000;

/** A game that started at the given UTC hour of 2026-03-<day>, with the given score and average decision time. */
function game(day: number, hour: number, score: number, decisionMs = 3000, over: Partial<GameRecord> = {}, cog: Partial<ReturnType<typeof emptyCognition>> = {}): GameRecord {
  const moves = 40;
  const durationMs = 10 * MIN;
  const start = Date.UTC(2026, 2, day, hour, 0);
  const tm = moves;
  return {
    score, endedAt: start + durationMs, moves, lines: 3, balls: 15, completed: true, maxLine: 5, durationMs, mode: 'classic',
    cog: { ...emptyCognition(), tz: TZ, tm, think: tm * decisionMs, thinkSq: Math.round(tm * (decisionMs / 100) ** 2), minEmpty: 30, p1: 20 * decisionMs, n1: 20, p2: 20 * decisionMs, n2: 20, ...cog },
    ...over,
  };
}

describe('mindReport', () => {
  it('has nothing to say without play data', () => {
    const r = mindReport([{ ...game(1, 9, 100), cog: undefined }]);
    expect(r.games).toBe(0);
    expect(findings(r)).toEqual([]);
  });

  it('finds the best time of day and the chronotype', () => {
    const history: GameRecord[] = [];
    for (let d = 1; d <= 10; d++) history.push(game(d, 21, 400), game(d, 8, 200), game(d, 14, 250));
    const r = mindReport(history);
    expect(r.games).toBe(30);
    expect(r.byHour[21].games).toBe(10);
    expect(r.byHour[21].index).toBeGreaterThan(r.byHour[8].index);
    expect(r.bestWindow).not.toBeNull();
    expect(r.bestWindow!.from).toBeGreaterThanOrEqual(19);
    expect(r.bestWindow!.from).toBeLessThanOrEqual(21);
    expect(r.chronotype).toBe('evening');
    expect(findings(r).map((f) => f.id)).toContain('window');
  });

  it('indexes every game against its own mode', () => {
    const history = [
      ...Array.from({ length: 6 }, (_, i) => game(i + 1, 10, 1000, 3000, { mode: 'blitz' })),
      ...Array.from({ length: 6 }, (_, i) => game(i + 1, 10, 100, 3000, { mode: 'classic' })),
    ];
    const r = mindReport(history);
    expect(r.byHour[10].index).toBe(100);
  });

  it('measures the decision time, its spread and the tempo groups', () => {
    const history = Array.from({ length: 12 }, (_, i) => game(i + 1, 10, 100 + i * 20, 1000 + i * 500));
    const r = mindReport(history);
    expect(r.avgDecisionMs).toBeCloseTo(3750, -2);
    expect(r.tempo).not.toBeNull();
    expect(r.tempo!.slow.index).toBeGreaterThan(r.tempo!.fast.index);
    expect(findings(r).some((f) => f.id === 'tempoSlow')).toBe(true);
  });

  it('notices tiredness over a sitting', () => {
    const history: GameRecord[] = [];
    // Ten sittings of three games an hour... close together: scores fall from the first to the third game.
    for (let d = 1; d <= 10; d++) history.push(game(d, 10, 500), game(d, 10, 300, 3000, { endedAt: Date.UTC(2026, 2, d, 10, 25) }), game(d, 10, 200, 3000, { endedAt: Date.UTC(2026, 2, d, 10, 50) }));
    // Three games per day starting at 10:00, 10:15 and 10:40 (durations are 10 minutes).
    const spaced = history.map((g, i) => ({ ...g, endedAt: Date.UTC(2026, 2, Math.floor(i / 3) + 1, 10, 10 + (i % 3) * 15) }));
    const r = mindReport(spaced);
    expect(r.sittings[0].games).toBe(10);
    expect(r.sittings[0].index).toBeGreaterThan(r.sittings[2].index);
    expect(findings(r).some((f) => f.id === 'tired')).toBe(true);
  });

  it('counts danger, help and misses per 100 moves', () => {
    const history = Array.from({ length: 10 }, (_, i) => game(i + 1, 10, 200, 3000, {}, { danger: 20, undo: 2, hint: 1, miss: 4, clears: 10, minEmpty: 5 }));
    const r = mindReport(history);
    expect(r.dangerPer100).toBe(50);
    expect(r.undosPer100).toBe(5);
    expect(r.hintsPer100).toBe(2.5);
    expect(r.missesPer100).toBe(10);
    expect(r.clearingShare).toBe(0.25);
    expect(r.tightest).toBe(5);
    expect(findings(r).some((f) => f.id === 'tight')).toBe(true);
  });

  it('compares the first and the latest games once there are enough', () => {
    const history = Array.from({ length: 120 }, (_, i) => game((i % 28) + 1, 10, 100 + i, 3000, { endedAt: Date.UTC(2026, 0, 1) + i * 86_400_000 }));
    const r = mindReport(history);
    expect(r.growth).not.toBeNull();
    expect(r.growth!.latest).toBeGreaterThan(r.growth!.first);
  });

  it('names the part of the day', () => {
    expect([3, 8, 13, 19, 23].map(chronotypeOf)).toEqual(['owl', 'lark', 'day', 'evening', 'owl']);
  });
});
