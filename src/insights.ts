import { dayKey } from './progress';
import type { GameRecord } from './stats';

/** Analysis of the recorded games. All inputs are newest first, as kept in the history. */

const average = (values: readonly number[]) => (values.length === 0 ? 0 : values.reduce((a, b) => a + b, 0) / values.length);

export interface Trend {
  recentAverage: number;
  previousAverage: number;
  changePercent: number;
  direction: 'up' | 'down' | 'flat';
}

/** Average of the latest `window` games against the `window` before them; null until both exist. */
export function trendOf(newestFirst: readonly GameRecord[], window = 10): Trend | null {
  if (newestFirst.length < window * 2) return null;
  const recent = average(newestFirst.slice(0, window).map((g) => g.score));
  const previous = average(newestFirst.slice(window, window * 2).map((g) => g.score));
  const changePercent = previous === 0 ? (recent === 0 ? 0 : 100) : Math.round(((recent - previous) / previous) * 100);
  const direction = Math.abs(changePercent) < 3 ? 'flat' : changePercent > 0 ? 'up' : 'down';
  return { recentAverage: Math.round(recent), previousAverage: Math.round(previous), changePercent, direction };
}

export interface DayActivity {
  day: string;
  games: number;
  best: number;
}

/** The last `days` calendar days ending at `now`, oldest first. */
export function dailyActivity(newestFirst: readonly GameRecord[], days: number, now: number): DayActivity[] {
  const byDay = new Map<string, DayActivity>();
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const key = dayKey(d.getTime());
    byDay.set(key, { day: key, games: 0, best: 0 });
  }
  for (const game of newestFirst) {
    const entry = byDay.get(dayKey(game.endedAt));
    if (!entry) continue;
    entry.games++;
    entry.best = Math.max(entry.best, game.score);
  }
  return [...byDay.values()];
}

export interface HistogramBar {
  from: number;
  count: number;
}

/** Scores in buckets of `size`, including empty buckets between the lowest and highest. */
export function scoreHistogram(games: readonly GameRecord[], size: number): HistogramBar[] {
  if (games.length === 0) return [];
  const last = Math.floor(Math.max(...games.map((g) => g.score)) / size);
  const bars = Array.from({ length: last + 1 }, (_, i) => ({ from: i * size, count: 0 }));
  for (const g of games) bars[Math.floor(g.score / size)].count++;
  return bars;
}

export interface WeekdayStat {
  /** 0 = Monday ... 6 = Sunday. */
  weekday: number;
  games: number;
  average: number;
}

export function weekdayActivity(games: readonly GameRecord[]): WeekdayStat[] {
  const sums = Array.from({ length: 7 }, () => ({ games: 0, total: 0 }));
  for (const g of games) {
    const weekday = (new Date(g.endedAt).getDay() + 6) % 7;
    sums[weekday].games++;
    sums[weekday].total += g.score;
  }
  return sums.map((s, weekday) => ({ weekday, games: s.games, average: s.games === 0 ? 0 : Math.round(s.total / s.games) }));
}

/** Points per move: the overall rate and the best single game. */
export function efficiency(games: readonly GameRecord[]): { average: number; best: number } {
  let score = 0;
  let moves = 0;
  let best = 0;
  for (const g of games) {
    score += g.score;
    moves += g.moves;
    if (g.moves > 0) best = Math.max(best, g.score / g.moves);
  }
  return { average: moves === 0 ? 0 : score / moves, best };
}

export interface RecordStep {
  score: number;
  at: number;
}

/** Every time the personal best improved, oldest first. */
export function recordProgression(newestFirst: readonly GameRecord[]): RecordStep[] {
  const steps: RecordStep[] = [];
  let best = 0;
  for (const g of [...newestFirst].reverse()) {
    if (g.score > best) {
      best = g.score;
      steps.push({ score: g.score, at: g.endedAt });
    }
  }
  return steps;
}

/** Games needed to earn `remainingXp` at the pace of the latest games; null without any games. */
export function levelEta(newestFirst: readonly GameRecord[], remainingXp: number, window = 10): number | null {
  const recent = newestFirst.slice(0, window);
  if (recent.length === 0) return null;
  const perGame = average(recent.map((g) => g.score + g.lines * 5 + 10));
  return remainingXp <= 0 ? 0 : Math.ceil(remainingXp / perGame);
}
