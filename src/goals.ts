import { hashString } from './engine/rng';
import type { GameRecord } from './stats';

/**
 * Daily goals built from the player's own results, so they stay a fair stretch whatever the skill level:
 * a weak player is asked for a bit more than his average, a strong one for a lot more.
 */
export type GoalType = 'score' | 'line' | 'moves' | 'lines' | 'efficiency' | 'beatYesterday';

export interface Goal {
  id: string;
  type: GoalType;
  target: number;
}

export interface GoalProgress {
  goal: Goal;
  value: number;
  done: boolean;
}

export const GOAL_BONUS_XP = 25;
const POOL: readonly GoalType[] = ['line', 'moves', 'lines', 'efficiency', 'beatYesterday'];
const MIN_EFFICIENCY_MOVES = 15;

/** Easy mode has fewer colours, so its results would distort the targets. */
const counts = (g: GameRecord) => g.mode !== 'easy';

function dayStart(dayKey: string): number {
  const [y, m, d] = dayKey.split('-').map(Number);
  return new Date(y, m - 1, d).getTime();
}

const mean = (values: readonly number[]) => (values.length === 0 ? 0 : values.reduce((a, b) => a + b, 0) / values.length);

export function dailyGoals(newestFirst: readonly GameRecord[], dayKey: string): Goal[] {
  const start = dayStart(dayKey);
  const past = newestFirst.filter((g) => counts(g) && g.endedAt < start).slice(0, 20);
  const beginner = past.length < 3;

  const scores = past.map((g) => g.score);
  const avg = mean(scores);
  const best = Math.max(0, ...scores);
  const scoreTarget = beginner ? 60 : Math.ceil(Math.max(avg + 0.5 * (best - avg), avg + 10) / 10) * 10;

  const lineTarget = Math.min(9, Math.max(5, Math.round(mean(past.map((g) => g.maxLine))) + 1));
  const movesTarget = beginner ? 30 : Math.max(20, Math.round(mean(past.map((g) => g.moves)) * 1.2));
  const linesTarget = beginner ? 3 : Math.max(2, Math.round(mean(past.map((g) => g.lines)) * 1.25));
  const efficiencies = past.filter((g) => g.moves > 0).map((g) => g.score / g.moves);
  const efficiencyTarget = beginner || efficiencies.length === 0 ? 1.5 : Math.max(1, Math.round(mean(efficiencies) * 1.15 * 10) / 10);

  let yesterdayBest = 50;
  if (past.length > 0) {
    const lastDay = new Date(past[0].endedAt);
    const from = new Date(lastDay.getFullYear(), lastDay.getMonth(), lastDay.getDate()).getTime();
    yesterdayBest = Math.max(...past.filter((g) => g.endedAt >= from).map((g) => g.score)) + 1;
  }

  const target: Record<GoalType, number> = {
    score: scoreTarget,
    line: lineTarget,
    moves: movesTarget,
    lines: linesTarget,
    efficiency: efficiencyTarget,
    beatYesterday: yesterdayBest,
  };

  const h = hashString(dayKey);
  const a = h % POOL.length;
  const b = (a + 1 + ((h >>> 8) % (POOL.length - 1))) % POOL.length;
  return (['score', POOL[a], POOL[b]] as GoalType[]).map((type) => ({ id: `${dayKey}:${type}`, type, target: target[type] }));
}

function valueOf(type: GoalType, games: readonly GameRecord[]): number {
  const list = games.filter(counts);
  switch (type) {
    case 'score':
    case 'beatYesterday':
      return Math.max(0, ...list.map((g) => g.score));
    case 'line':
      return Math.max(0, ...list.map((g) => g.maxLine));
    case 'moves':
      return Math.max(0, ...list.map((g) => g.moves));
    case 'lines':
      return Math.max(0, ...list.map((g) => g.lines));
    case 'efficiency':
      return Math.max(0, ...list.filter((g) => g.moves >= MIN_EFFICIENCY_MOVES).map((g) => g.score / g.moves));
  }
}

/** The best value reached today for each goal. */
export function evaluateGoals(goals: readonly Goal[], todaysGames: readonly GameRecord[]): GoalProgress[] {
  return goals.map((goal) => {
    const value = valueOf(goal.type, todaysGames);
    return { goal, value, done: value >= goal.target };
  });
}

export interface GoalBonus {
  newlyDone: string[];
  xp: number;
  /** All goals of the day are done, and this game finished the last one. */
  allDone: boolean;
}

export function goalBonus(alreadyDone: readonly string[], nowDone: readonly string[], total: number): GoalBonus {
  const newlyDone = nowDone.filter((id) => !alreadyDone.includes(id));
  return { newlyDone, xp: newlyDone.length * GOAL_BONUS_XP, allDone: nowDone.length >= total && newlyDone.length > 0 };
}
