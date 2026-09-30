import { describe, expect, it } from 'vitest';
import { GOAL_BONUS_XP, dailyGoals, evaluateGoals, goalBonus } from '../src/goals';
import type { Goal } from '../src/goals';
import type { GameRecord } from '../src/stats';

const at = (y: number, m: number, d: number, h = 12) => new Date(y, m - 1, d, h).getTime();
const g = (score: number, over: Partial<GameRecord> = {}): GameRecord => ({
  score,
  endedAt: at(2026, 9, 20),
  moves: 40,
  lines: 4,
  balls: 20,
  completed: true,
  maxLine: 5,
  durationMs: 120_000,
  mode: 'classic',
  ...over,
});
const history = (scores: number[], over: Partial<GameRecord> = {}) => scores.map((s, i) => g(s, { endedAt: at(2026, 9, 20) - i * 3_600_000, ...over }));
const byType = (goals: Goal[], type: string) => goals.find((x) => x.type === type);

describe('dailyGoals', () => {
  it('always gives three different goals, including a score goal', () => {
    for (let day = 1; day <= 28; day++) {
      const goals = dailyGoals(history([100, 120, 90, 110, 130]), `2026-09-${String(day).padStart(2, '0')}`);
      expect(goals).toHaveLength(3);
      expect(new Set(goals.map((x) => x.type)).size).toBe(3);
      expect(goals.some((x) => x.type === 'score')).toBe(true);
      expect(new Set(goals.map((x) => x.id)).size).toBe(3);
    }
  });

  it('is the same all day and changes from day to day', () => {
    const h = history([100, 120, 90, 110, 130]);
    expect(dailyGoals(h, '2026-09-30')).toEqual(dailyGoals(h, '2026-09-30'));
    const types = (day: string) => dailyGoals(h, day).map((x) => x.type).join();
    expect(new Set([1, 2, 3, 4, 5, 6, 7, 8].map((d) => types(`2026-10-0${d}`))).size).toBeGreaterThan(2);
  });

  it('starts gently for a new player', () => {
    const goals = dailyGoals([], '2026-09-30');
    expect(byType(goals, 'score')!.target).toBeGreaterThanOrEqual(50);
    expect(byType(goals, 'score')!.target).toBeLessThanOrEqual(100);
  });

  it('asks for a stretch above the player\'s average but not above the best', () => {
    const h = history([200, 100, 100, 100, 100, 100, 100, 100, 100, 100]); // average 110, best 200
    const target = byType(dailyGoals(h, '2026-09-30'), 'score')!.target;
    expect(target).toBeGreaterThan(110);
    expect(target).toBeLessThanOrEqual(200);
  });

  it('grows with the player: a stronger history asks for more', () => {
    const weak = byType(dailyGoals(history(Array(10).fill(60)), '2026-09-30'), 'score')!.target;
    const strong = byType(dailyGoals(history(Array(10).fill(300)), '2026-09-30'), 'score')!.target;
    expect(strong).toBeGreaterThan(weak);
  });

  it('ignores easy-mode games and games played on the day itself', () => {
    const base = history(Array(10).fill(100));
    const withEasy = [...base, ...history(Array(10).fill(1000), { mode: 'easy' })];
    const withToday = [...base, g(900, { endedAt: at(2026, 9, 30, 8) })];
    const target = (h: GameRecord[]) => byType(dailyGoals(h, '2026-09-30'), 'score')!.target;
    expect(target(withEasy)).toBe(target(base));
    expect(target(withToday)).toBe(target(base));
  });

  it('keeps the line goal between 5 and 9', () => {
    for (const maxLine of [0, 5, 8, 9]) {
      const line = byType(dailyGoals(history(Array(10).fill(80), { maxLine }), '2026-09-28'), 'line');
      if (line) {
        expect(line.target).toBeGreaterThanOrEqual(5);
        expect(line.target).toBeLessThanOrEqual(9);
      }
    }
  });
});

describe('evaluateGoals', () => {
  const goals: Goal[] = [
    { id: 'a', type: 'score', target: 100 },
    { id: 'b', type: 'line', target: 6 },
    { id: 'c', type: 'moves', target: 50 },
  ];

  it('takes the best value over the games of the day', () => {
    const today = [g(80, { maxLine: 5, moves: 30 }), g(130, { maxLine: 7, moves: 60 })];
    const result = evaluateGoals(goals, today);
    expect(result.map((r) => r.value)).toEqual([130, 7, 60]);
    expect(result.every((r) => r.done)).toBe(true);
  });

  it('shows progress when a goal is not reached and ignores easy mode', () => {
    const result = evaluateGoals(goals, [g(60, { maxLine: 5, moves: 20 }), g(999, { mode: 'easy', maxLine: 9, moves: 99 })]);
    expect(result.map((r) => r.done)).toEqual([false, false, false]);
    expect(result[0].value).toBe(60);
  });

  it('handles every goal type', () => {
    const all: Goal[] = [
      { id: '1', type: 'lines', target: 3 },
      { id: '2', type: 'efficiency', target: 2 },
      { id: '3', type: 'beatYesterday', target: 100 },
    ];
    const result = evaluateGoals(all, [g(120, { lines: 5, moves: 40 })]);
    expect(result.map((r) => r.done)).toEqual([true, true, true]);
    expect(evaluateGoals(all, [])).toHaveLength(3);
  });

  it('does not count a very short game for efficiency', () => {
    const r = evaluateGoals([{ id: 'e', type: 'efficiency', target: 2 }], [g(50, { moves: 5 })]);
    expect(r[0].done).toBe(false);
  });
});

describe('goalBonus', () => {
  it('rewards each newly completed goal once and marks a full day', () => {
    expect(GOAL_BONUS_XP).toBe(25);
    expect(goalBonus(['a'], ['a', 'b'], 3)).toEqual({ newlyDone: ['b'], xp: 25, allDone: false });
    expect(goalBonus([], ['a', 'b', 'c'], 3)).toEqual({ newlyDone: ['a', 'b', 'c'], xp: 75, allDone: true });
    expect(goalBonus(['a'], ['a'], 3)).toEqual({ newlyDone: [], xp: 0, allDone: false });
  });
});
