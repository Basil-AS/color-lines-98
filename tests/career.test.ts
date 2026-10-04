import { describe, expect, it } from 'vitest';
import { addGame, ledgerFromHistory, mergeLedgers, mergeLedgersWithHistory, sanitizeLedger } from '../src/ledger';
import type { Ledger } from '../src/ledger';
import { heatmap, ladder, milestones, monthly, records, season, seasonBaseline, streaks, tierOf, totals, yearly, memories } from '../src/career';
import type { GameRecord } from '../src/stats';
import { dayKey } from '../src/progress';

const at = (y: number, m: number, d: number, h = 12) => new Date(y, m - 1, d, h).getTime();
const game = (over: Partial<GameRecord> = {}): GameRecord => ({
  score: 100, endedAt: at(2026, 9, 1), moves: 30, lines: 3, balls: 15, completed: true, maxLine: 5, durationMs: 60_000, mode: 'classic', ...over,
});

describe('ledger', () => {
  it('adds games to their day and keeps totals', () => {
    let l: Ledger = {};
    l = addGame(l, game({ score: 100 }));
    l = addGame(l, game({ score: 300, completed: false }));
    expect(l['2026-09-01']).toEqual({ games: 2, completed: 1, score: 400, best: 300, moves: 60, lines: 6, playMs: 120_000 });
    expect(Object.keys(ledgerFromHistory([game(), game({ endedAt: at(2026, 9, 2) })]))).toEqual(['2026-09-01', '2026-09-02']);
  });

  it('merges by keeping the fuller day and sanitizes hostile input', () => {
    const a = ledgerFromHistory([game()]);
    const b = ledgerFromHistory([game(), game()]);
    expect(mergeLedgers(a, b)['2026-09-01'].games).toBe(2);
    expect(mergeLedgers(b, a)['2026-09-01'].games).toBe(2);
    const dirty = sanitizeLedger({ '2026-01-01': { games: 2, completed: 9, score: -5, best: 'x' }, 'nope': { games: 1 }, '2026-02-02': { games: 0 } });
    expect(dirty).toEqual({ '2026-01-01': { games: 2, completed: 2, score: 0, best: 0, moves: 0, lines: 0, playMs: 0 } });
    expect(sanitizeLedger(null)).toEqual({});
    expect(sanitizeLedger([1, 2])).toEqual({});
  });
});

describe('totals, streaks and months', () => {
  const l = ledgerFromHistory([
    game({ endedAt: at(2026, 8, 30), score: 50 }),
    game({ endedAt: at(2026, 8, 31), score: 70 }),
    game({ endedAt: at(2026, 9, 1), score: 200 }),
    game({ endedAt: at(2026, 9, 5), score: 400 }),
    game({ endedAt: at(2026, 9, 5), score: 100 }),
  ]);

  it('sums everything', () => {
    expect(totals(l)).toMatchObject({ games: 5, score: 820, activeDays: 4, lines: 15 });
  });

  it('counts streaks across a month border and lets today be empty', () => {
    expect(streaks(l, '2026-09-01')).toEqual({ current: 3, longest: 3 });
    expect(streaks(l, '2026-09-02')).toEqual({ current: 3, longest: 3 });
    expect(streaks(l, '2026-09-04')).toEqual({ current: 0, longest: 3 });
    expect(streaks(l, '2026-09-05').current).toBe(1);
    expect(streaks({}, '2026-09-05')).toEqual({ current: 0, longest: 0 });
  });

  it('rolls up months', () => {
    const m = monthly(l);
    expect(m.map((x) => x.month)).toEqual(['2026-08', '2026-09']);
    expect(m[1]).toMatchObject({ games: 3, score: 700, best: 400, average: 233, activeDays: 2 });
  });
});

describe('seasons', () => {
  it('judges a season against your typical month', () => {
    expect(seasonBaseline([])).toBe(1500);
    const months = [1000, 2000, 3000].map((score, i) => ({ month: `2026-0${i + 1}`, games: 1, score, best: score, average: score, activeDays: 1, playMs: 0 }));
    expect(seasonBaseline(months)).toBe(2000);
    expect(tierOf(0, 2000)).toBe('bronze');
    expect(tierOf(999, 2000)).toBe('bronze');
    expect(tierOf(1000, 2000)).toBe('silver');
    expect(tierOf(2000, 2000)).toBe('gold');
    expect(tierOf(3000, 2000)).toBe('platinum');
    expect(tierOf(5000, 2000)).toBe('legend');
  });

  it('reports progress, days left and the past seasons', () => {
    const l: Ledger = {
      '2026-07-10': { games: 4, completed: 4, score: 2000, best: 700, moves: 1, lines: 1, playMs: 1 },
      '2026-08-10': { games: 4, completed: 4, score: 1000, best: 400, moves: 1, lines: 1, playMs: 1 },
      '2026-09-10': { games: 2, completed: 2, score: 1200, best: 800, moves: 1, lines: 1, playMs: 1 },
    };
    const s = season(l, at(2026, 9, 20));
    expect(s.month).toBe('2026-09');
    expect(s.baseline).toBe(1500);
    expect(s.tier).toBe('silver');
    expect(s.nextTier).toBe('gold');
    expect(s.toNext).toBe(300);
    expect(s.daysLeft).toBe(10);
    expect(s.past.map((p) => p.month)).toEqual(['2026-08', '2026-07']);
    expect(s.bestMonth?.month).toBe('2026-07');
  });

  it('starts empty and has no next tier at the top', () => {
    const s = season({}, at(2026, 1, 15));
    expect(s).toMatchObject({ points: 0, tier: 'bronze', past: [], bestMonth: null });
    const top = season({ '2026-01-02': { games: 1, completed: 1, score: 99999, best: 99999, moves: 1, lines: 1, playMs: 1 } }, at(2026, 1, 15));
    expect(top.tier).toBe('legend');
    expect(top.toNext).toBeNull();
  });
});

describe('endless milestones', () => {
  it('has a next step however far you are', () => {
    expect(ladder(0, 10)).toMatchObject({ reached: 0, previous: 0, next: 10 });
    expect(ladder(10, 10)).toMatchObject({ reached: 1, previous: 10, next: 25 });
    expect(ladder(99, 10)).toMatchObject({ next: 100 });
    expect(ladder(100, 10)).toMatchObject({ next: 250 });
    const far = ladder(3_000_000, 1000);
    expect(far.next).toBeGreaterThan(3_000_000);
    expect(far.fraction).toBeGreaterThanOrEqual(0);
    expect(far.fraction).toBeLessThan(1);
    expect(ladder(2.4, 1)).toMatchObject({ previous: 1, next: 2.5 });
  });

  it('builds all tracks from the totals', () => {
    const tracks = milestones({ games: 120, completed: 100, score: 40_000, lines: 300, moves: 1, playMs: 7_200_000, activeDays: 12 });
    expect(tracks.map((t) => t.id)).toEqual(['games', 'score', 'lines', 'hours', 'days']);
    expect(tracks.find((t) => t.id === 'hours')?.value).toBe(2);
    expect(tracks.find((t) => t.id === 'games')?.ladder.next).toBe(250);
  });
});

describe('heatmap and records', () => {
  it('draws whole weeks, Monday first, without the future', () => {
    const now = at(2026, 9, 30); // a Wednesday
    const l = ledgerFromHistory([game({ endedAt: now }), game({ endedAt: now }), game({ endedAt: at(2026, 9, 28) })]);
    const grid = heatmap(l, now, 4);
    expect(grid).toHaveLength(4);
    expect(grid.every((c) => c.length === 7)).toBe(true);
    const last = grid[3];
    expect(last[0]?.day).toBe('2026-09-28');
    expect(last[2]?.day).toBe('2026-09-30');
    expect(last[2]?.level).toBe(4);
    expect(last[0]?.level).toBe(2);
    expect(last[3]).toBeNull();
    expect(dayKey(at(2026, 9, 30))).toBe('2026-09-30');
  });

  it('finds the personal records', () => {
    const history = [
      game({ score: 500, moves: 20, lines: 4, maxLine: 6, durationMs: 100_000 }),
      game({ score: 900, moves: 60, lines: 9, maxLine: 8, durationMs: 400_000 }),
      game({ score: 300, moves: 5, lines: 1, maxLine: 5, durationMs: 10_000 }),
    ];
    const r = records(history, ledgerFromHistory(history));
    expect(r.bestScore?.score).toBe(900);
    expect(r.longestGame?.durationMs).toBe(400_000);
    expect(r.mostLines?.lines).toBe(9);
    expect(r.bestLine).toBe(8);
    expect(r.bestEfficiency?.value).toBe(25);
    expect(r.bestDay).toEqual({ day: '2026-09-01', score: 1700 });
    expect(records([], {})).toMatchObject({ bestScore: null, bestEfficiency: null, bestDay: null, bestLine: 0 });
  });
});

describe('combining two devices', () => {
  it('keeps the games both devices played on the same day', () => {
    const mine = [game({ endedAt: at(2026, 9, 1, 10), score: 100, moves: 11 }), game({ endedAt: at(2026, 9, 1, 11), score: 200, moves: 12 })];
    const theirs = [game({ endedAt: at(2026, 9, 1, 15), score: 300, moves: 13 })];
    const merged = [...theirs, ...mine];
    const ledger = mergeLedgersWithHistory(ledgerFromHistory(mine), ledgerFromHistory(theirs), merged);
    expect(ledger['2026-09-01'].games).toBe(3);
    expect(ledger['2026-09-01'].score).toBe(600);
  });
});

describe('years and memories', () => {
  const ledger = ledgerFromHistory([
    game({ score: 300, endedAt: at(2024, 3, 5) }),
    game({ score: 500, endedAt: at(2024, 3, 6) }),
    game({ score: 100, endedAt: at(2024, 7, 1) }),
    game({ score: 900, endedAt: at(2025, 3, 5) }),
  ]);

  it('sums every year and names its best month', () => {
    const years = yearly(ledger);
    expect(years.map((y) => y.year)).toEqual([2024, 2025]);
    expect(years[0]).toMatchObject({ games: 3, score: 900, best: 500, activeDays: 3, bestMonth: '2024-03' });
    expect(years[1]).toMatchObject({ games: 1, bestMonth: '2025-03' });
    expect(yearly({})).toEqual([]);
  });

  it('remembers the same date in earlier years and a month ago', () => {
    expect(memories(ledger, '2026-03-05')).toEqual([
      { day: '2025-03-05', yearsAgo: 1, games: 1, best: 900 },
      { day: '2024-03-05', yearsAgo: 2, games: 1, best: 300 },
    ]);
    expect(memories(ledger, '2024-04-05')).toEqual([{ day: '2024-03-05', yearsAgo: 0, games: 1, best: 300 }]);
    expect(memories(ledger, '2026-01-01')).toEqual([]);
  });
});
