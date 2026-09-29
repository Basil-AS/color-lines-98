import { describe, expect, it } from 'vitest';
import {
  ACHIEVEMENTS,
  LEVEL_TITLES,
  applyGame,
  bestStreak,
  currentStreak,
  dayKey,
  emptyProgress,
  levelInfo,
  levelThreshold,
  rebuildProgress,
  sanitizeProgress,
  scoreTrend,
  titleIndex,
  xpOf,
} from '../src/progress';
import type { GameRecord } from '../src/stats';

const at = (y: number, m: number, d: number, h = 12) => new Date(y, m - 1, d, h).getTime();

const game = (over: Partial<GameRecord> = {}): GameRecord => ({
  score: 40,
  endedAt: at(2026, 9, 29),
  moves: 30,
  lines: 3,
  balls: 15,
  completed: true,
  maxLine: 5,
  durationMs: 120_000,
  ...over,
});

describe('levels', () => {
  it('needs 50*n*(n-1) experience for level n', () => {
    expect([1, 2, 3, 4, 5].map(levelThreshold)).toEqual([0, 100, 300, 600, 1000]);
  });

  it('finds the level and progress towards the next one', () => {
    expect(levelInfo(0)).toMatchObject({ level: 1, into: 0, needed: 100, fraction: 0 });
    expect(levelInfo(99).level).toBe(1);
    expect(levelInfo(100).level).toBe(2);
    expect(levelInfo(299).level).toBe(2);
    expect(levelInfo(300)).toMatchObject({ level: 3, into: 0, needed: 300 });
    expect(levelInfo(450).fraction).toBeCloseTo(0.5);
  });

  it('maps levels to titles and never runs out of titles', () => {
    expect(titleIndex(1)).toBe(0);
    expect(titleIndex(1000)).toBe(LEVEL_TITLES.length - 1);
    for (let l = 1; l < 60; l++) expect(titleIndex(l)).toBeGreaterThanOrEqual(titleIndex(Math.max(1, l - 1)));
  });

  it('computes experience from score, lines and games', () => {
    expect(xpOf({ ...emptyProgress(), totalScore: 100, totalLines: 4, totalGames: 3 })).toBe(100 + 20 + 30);
  });
});

describe('applyGame', () => {
  it('accumulates totals and personal bests', () => {
    let p = emptyProgress();
    p = applyGame(p, game({ score: 40, lines: 3, moves: 30, maxLine: 5 })).progress;
    p = applyGame(p, game({ score: 90, lines: 2, moves: 80, maxLine: 7, completed: false })).progress;
    expect(p).toMatchObject({
      totalGames: 2,
      completedGames: 1,
      totalScore: 130,
      totalLines: 5,
      totalMoves: 110,
      totalBalls: 30,
      totalPlayMs: 240_000,
      bestScore: 90,
      bestLine: 7,
      mostLinesInGame: 3,
      longestGameMoves: 80,
    });
  });

  it('does not mutate its input', () => {
    const before = emptyProgress();
    applyGame(before, game());
    expect(before).toEqual(emptyProgress());
  });

  it('unlocks achievements once and reports only the new ones', () => {
    const first = applyGame(emptyProgress(), game({ lines: 1, score: 10 }));
    expect(first.unlocked).toEqual(expect.arrayContaining(['first_game', 'first_line']));
    expect(first.progress.achievements.first_game).toBe(game().endedAt);
    const second = applyGame(first.progress, game({ lines: 1, score: 10, endedAt: at(2026, 9, 30) }));
    expect(second.unlocked).not.toContain('first_game');
    expect(second.progress.achievements.first_game).toBe(game().endedAt);
  });

  it('unlocks score, line-length and marathon achievements', () => {
    const r = applyGame(emptyProgress(), game({ score: 260, maxLine: 9, moves: 200 }));
    expect(r.unlocked).toEqual(
      expect.arrayContaining(['score_100', 'score_250', 'long_line_7', 'long_line_9', 'marathon'])
    );
    expect(r.unlocked).not.toContain('score_500');
  });

  it('records each play day once', () => {
    let p = emptyProgress();
    p = applyGame(p, game()).progress;
    p = applyGame(p, game({ endedAt: at(2026, 9, 29, 20) })).progress;
    p = applyGame(p, game({ endedAt: at(2026, 9, 30) })).progress;
    expect(p.days).toEqual(['2026-09-29', '2026-09-30']);
  });

  it('unlocks a level achievement when experience crosses the threshold', () => {
    const p = { ...emptyProgress(), totalScore: 950 };
    const r = applyGame(p, game({ score: 60, lines: 0 }));
    expect(xpOf(r.progress)).toBeGreaterThanOrEqual(levelThreshold(5));
    expect(r.unlocked).toContain('level_5');
  });

  it('has unique achievement ids', () => {
    const ids = ACHIEVEMENTS.map((a) => a.id);
    expect(new Set(ids).size).toBe(ids.length);
  });
});

describe('streaks', () => {
  const days = ['2026-09-20', '2026-09-27', '2026-09-28', '2026-09-29'];

  it('counts consecutive days ending today or yesterday', () => {
    expect(currentStreak(days, '2026-09-29')).toBe(3);
    expect(currentStreak(days, '2026-09-30')).toBe(3);
    expect(currentStreak(days, '2026-10-02')).toBe(0);
    expect(currentStreak([], '2026-09-29')).toBe(0);
  });

  it('finds the best streak ever', () => {
    expect(bestStreak(days)).toBe(3);
    expect(bestStreak(['2026-01-01', '2026-01-02', '2026-01-03', '2026-01-04', '2026-02-01'])).toBe(4);
    expect(bestStreak([])).toBe(0);
  });

  it('works across month and year boundaries', () => {
    expect(bestStreak(['2025-12-31', '2026-01-01'])).toBe(2);
    expect(currentStreak(['2026-02-28', '2026-03-01'], '2026-03-01')).toBe(2);
  });

  it('unlocks the streak achievement', () => {
    let p = emptyProgress();
    let unlocked: string[] = [];
    for (const d of [27, 28, 29]) {
      const r = applyGame(p, game({ endedAt: at(2026, 9, d) }));
      p = r.progress;
      unlocked = unlocked.concat(r.unlocked);
    }
    expect(unlocked).toContain('streak_3');
  });
});

describe('dayKey', () => {
  it('uses the local calendar date', () => {
    expect(dayKey(at(2026, 1, 5))).toBe('2026-01-05');
    expect(dayKey(at(2026, 12, 31, 23))).toBe('2026-12-31');
  });
});

describe('rebuildProgress', () => {
  it('equals applying the games oldest first', () => {
    const newestFirst = [
      game({ score: 120, endedAt: at(2026, 9, 29), maxLine: 6 }),
      game({ score: 30, endedAt: at(2026, 9, 28), lines: 1 }),
    ];
    let expected = emptyProgress();
    for (const g of [...newestFirst].reverse()) expected = applyGame(expected, g).progress;
    expect(rebuildProgress(newestFirst)).toEqual(expected);
    expect(rebuildProgress([])).toEqual(emptyProgress());
  });
});

describe('sanitizeProgress', () => {
  it('returns an empty profile for junk', () => {
    for (const bad of [null, undefined, 3, 'x', [], { totalGames: 'many' }]) {
      expect(sanitizeProgress(bad)).toEqual(emptyProgress());
    }
  });

  it('keeps valid values and drops invalid parts', () => {
    const valid = applyGame(emptyProgress(), game()).progress;
    expect(sanitizeProgress(JSON.parse(JSON.stringify(valid)))).toEqual(valid);
    const dirty = {
      ...valid,
      totalScore: -5,
      days: ['2026-09-29', 'yesterday', 7],
      achievements: { first_game: 5, nonsense: 1, first_line: 'x' },
    };
    const clean = sanitizeProgress(dirty);
    expect(clean.totalScore).toBe(0);
    expect(clean.days).toEqual(['2026-09-29']);
    expect(clean.achievements).toEqual({ first_game: 5 });
  });
});

describe('scoreTrend', () => {
  it('returns the latest scores oldest first', () => {
    const history = [5, 4, 3, 2, 1].map((s) => game({ score: s }));
    expect(scoreTrend(history, 3)).toEqual([3, 4, 5]);
    expect(scoreTrend(history, 10)).toEqual([1, 2, 3, 4, 5]);
    expect(scoreTrend([], 5)).toEqual([]);
  });
});
