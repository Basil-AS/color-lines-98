import { dayKey } from './progress';
import type { Ledger } from './ledger';
import type { GameRecord } from './stats';

/** Long-term analysis built on the permanent ledger: careers, streaks, seasons, endless milestones, records. */

export interface Totals {
  games: number;
  completed: number;
  score: number;
  lines: number;
  moves: number;
  playMs: number;
  activeDays: number;
}

export function totals(ledger: Ledger): Totals {
  const t: Totals = { games: 0, completed: 0, score: 0, lines: 0, moves: 0, playMs: 0, activeDays: 0 };
  for (const d of Object.values(ledger)) {
    t.games += d.games;
    t.completed += d.completed;
    t.score += d.score;
    t.lines += d.lines;
    t.moves += d.moves;
    t.playMs += d.playMs;
    t.activeDays++;
  }
  return t;
}

const parseDay = (key: string): number => {
  const [y, m, d] = key.split('-').map(Number);
  return Date.UTC(y, m - 1, d) / 86_400_000;
};

/** Consecutive days with at least one game; the current streak survives until the end of today. */
export function streaks(ledger: Ledger, today: string): { current: number; longest: number } {
  const days = Object.keys(ledger).map(parseDay).sort((a, b) => a - b);
  let longest = 0;
  let run = 0;
  for (let i = 0; i < days.length; i++) {
    run = i > 0 && days[i] - days[i - 1] === 1 ? run + 1 : 1;
    longest = Math.max(longest, run);
  }
  const set = new Set(days);
  let cursor = parseDay(today);
  if (!set.has(cursor)) cursor -= 1;
  let current = 0;
  while (set.has(cursor)) {
    current++;
    cursor -= 1;
  }
  return { current, longest };
}

// ---- months and seasons ------------------------------------------------------------------

export interface MonthStat {
  /** "YYYY-MM" */
  month: string;
  games: number;
  score: number;
  best: number;
  average: number;
  activeDays: number;
  playMs: number;
}

/** Every month with games, oldest first. */
export function monthly(ledger: Ledger): MonthStat[] {
  const months = new Map<string, MonthStat>();
  for (const [day, d] of Object.entries(ledger)) {
    const key = day.slice(0, 7);
    const m = months.get(key) ?? { month: key, games: 0, score: 0, best: 0, average: 0, activeDays: 0, playMs: 0 };
    m.games += d.games;
    m.score += d.score;
    m.best = Math.max(m.best, d.best);
    m.activeDays++;
    m.playMs += d.playMs;
    months.set(key, m);
  }
  return [...months.values()]
    .map((m) => ({ ...m, average: Math.round(m.score / m.games) }))
    .sort((a, b) => a.month.localeCompare(b.month));
}

export const SEASON_TIERS = ['bronze', 'silver', 'gold', 'platinum', 'legend'] as const;
export type SeasonTier = (typeof SEASON_TIERS)[number];

/** A season is a calendar month. Tiers are measured against your own typical month, so they stay a fair chase. */
const TIER_FROM = [0, 0.5, 1, 1.5, 2.5];
const DEFAULT_BASELINE = 1500;

/** Typical month: the median points of your latest six months before this one. */
export function seasonBaseline(pastMonths: readonly MonthStat[]): number {
  const recent = pastMonths
    .slice(-6)
    .map((m) => m.score)
    .filter((s) => s > 0)
    .sort((a, b) => a - b);
  if (recent.length === 0) return DEFAULT_BASELINE;
  const mid = Math.floor(recent.length / 2);
  return Math.max(200, recent.length % 2 ? recent[mid] : Math.round((recent[mid - 1] + recent[mid]) / 2));
}

export function tierOf(points: number, baseline: number): SeasonTier {
  let tier = 0;
  TIER_FROM.forEach((from, i) => {
    if (points >= from * baseline) tier = i;
  });
  return SEASON_TIERS[tier];
}

export interface SeasonSummary {
  month: string;
  points: number;
  games: number;
  baseline: number;
  tier: SeasonTier;
  /** Points still needed for the next tier, or null at the top. */
  toNext: number | null;
  nextTier: SeasonTier | null;
  daysLeft: number;
  /** The finished seasons, newest first (up to 12). */
  past: { month: string; points: number; tier: SeasonTier }[];
  bestMonth: MonthStat | null;
}

export function season(ledger: Ledger, now: number): SeasonSummary {
  const current = dayKey(now).slice(0, 7);
  const all = monthly(ledger);
  const before = all.filter((m) => m.month < current);
  const mine = all.find((m) => m.month === current);
  const baseline = seasonBaseline(before);
  const points = mine?.score ?? 0;
  const tier = tierOf(points, baseline);
  const index = SEASON_TIERS.indexOf(tier);
  const nextTier = index + 1 < SEASON_TIERS.length ? SEASON_TIERS[index + 1] : null;
  const d = new Date(now);
  const daysLeft = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate() - d.getDate();
  return {
    month: current,
    points,
    games: mine?.games ?? 0,
    baseline,
    tier,
    nextTier,
    toNext: nextTier ? Math.max(0, Math.ceil(TIER_FROM[index + 1] * baseline) - points) : null,
    daysLeft,
    // Each finished season is judged against the typical month that came before it.
    past: before
      .map((m, i) => ({ month: m.month, points: m.score, tier: tierOf(m.score, seasonBaseline(before.slice(0, i))) }))
      .slice(-12)
      .reverse(),
    bestMonth: all.reduce<MonthStat | null>((best, m) => (best === null || m.score > best.score ? m : best), null),
  };
}

// ---- endless milestones ------------------------------------------------------------------

export interface Ladder {
  /** How many steps of the ladder are behind you. */
  reached: number;
  previous: number;
  next: number;
  fraction: number;
}

/** 1, 2.5, 5, 10, 25, 50, 100 ... scaled by `unit`: there is always a next step, however long you play. */
export function ladder(value: number, unit: number): Ladder {
  const steps = [1, 2.5, 5];
  let reached = 0;
  let previous = 0;
  for (let scale = 1; ; scale *= 10) {
    for (const s of steps) {
      const target = Math.round(s * scale * unit * 1000) / 1000;
      if (value < target) return { reached, previous, next: target, fraction: (value - previous) / (target - previous) };
      previous = target;
      reached++;
    }
  }
}

export type MilestoneId = 'games' | 'score' | 'lines' | 'hours' | 'days';

export function milestones(t: Totals): { id: MilestoneId; value: number; ladder: Ladder }[] {
  const hours = Math.floor(t.playMs / 360_000) / 10;
  return [
    { id: 'games', value: t.games, ladder: ladder(t.games, 10) },
    { id: 'score', value: t.score, ladder: ladder(t.score, 1000) },
    { id: 'lines', value: t.lines, ladder: ladder(t.lines, 10) },
    { id: 'hours', value: hours, ladder: ladder(hours, 1) },
    { id: 'days', value: t.activeDays, ladder: ladder(t.activeDays, 1) },
  ];
}

// ---- heatmap and records -----------------------------------------------------------------

export interface HeatCell {
  day: string;
  games: number;
  /** 0 (none) to 4 (a lot), relative to your busiest days. */
  level: 0 | 1 | 2 | 3 | 4;
}

/** Weeks as columns, Monday first, ending with the week of `now`; days after today are null. */
export function heatmap(ledger: Ledger, now: number, weeks: number): (HeatCell | null)[][] {
  const max = Math.max(1, ...Object.values(ledger).map((d) => d.games));
  const today = new Date(now);
  today.setHours(12, 0, 0, 0);
  const mondayOffset = (today.getDay() + 6) % 7;
  const start = new Date(today);
  start.setDate(start.getDate() - mondayOffset - (weeks - 1) * 7);
  const columns: (HeatCell | null)[][] = [];
  for (let w = 0; w < weeks; w++) {
    const col: (HeatCell | null)[] = [];
    for (let r = 0; r < 7; r++) {
      const d = new Date(start);
      d.setDate(start.getDate() + w * 7 + r);
      if (d.getTime() > today.getTime()) {
        col.push(null);
        continue;
      }
      const key = dayKey(d.getTime());
      const games = ledger[key]?.games ?? 0;
      const level = games === 0 ? 0 : (Math.min(4, Math.max(1, Math.ceil((games / max) * 4))) as 1 | 2 | 3 | 4);
      col.push({ day: key, games, level });
    }
    columns.push(col);
  }
  return columns;
}

export interface Records {
  bestScore: GameRecord | null;
  longestGame: GameRecord | null;
  mostLines: GameRecord | null;
  bestLine: number;
  bestEfficiency: { value: number; game: GameRecord } | null;
  bestDay: { day: string; score: number } | null;
  bestDayGames: { day: string; games: number } | null;
}

export function records(history: readonly GameRecord[], ledger: Ledger): Records {
  const top = (pick: (g: GameRecord) => number): GameRecord | null =>
    history.reduce<GameRecord | null>((best, g) => (pick(g) > 0 && (best === null || pick(g) > pick(best)) ? g : best), null);
  let bestEfficiency: Records['bestEfficiency'] = null;
  for (const g of history) {
    // Short games would make the ratio meaningless.
    if (g.moves < 15) continue;
    const value = g.score / g.moves;
    if (bestEfficiency === null || value > bestEfficiency.value) bestEfficiency = { value, game: g };
  }
  let bestDay: Records['bestDay'] = null;
  let bestDayGames: Records['bestDayGames'] = null;
  for (const [day, d] of Object.entries(ledger)) {
    if (bestDay === null || d.score > bestDay.score) bestDay = { day, score: d.score };
    if (bestDayGames === null || d.games > bestDayGames.games) bestDayGames = { day, games: d.games };
  }
  return {
    bestScore: top((g) => g.score),
    longestGame: top((g) => g.durationMs),
    mostLines: top((g) => g.lines),
    bestLine: history.reduce((m, g) => Math.max(m, g.maxLine), 0),
    bestEfficiency,
    bestDay,
    bestDayGames,
  };
}

// ---- years and memories ------------------------------------------------------------------

export interface YearStat {
  year: number;
  games: number;
  score: number;
  best: number;
  activeDays: number;
  playMs: number;
  /** The month with the most points. */
  bestMonth: string | null;
}

/** Every year with games, oldest first. */
export function yearly(ledger: Ledger): YearStat[] {
  const years = new Map<number, YearStat>();
  for (const [day, d] of Object.entries(ledger)) {
    const year = Number(day.slice(0, 4));
    const y = years.get(year) ?? { year, games: 0, score: 0, best: 0, activeDays: 0, playMs: 0, bestMonth: null };
    y.games += d.games;
    y.score += d.score;
    y.best = Math.max(y.best, d.best);
    y.activeDays++;
    y.playMs += d.playMs;
    years.set(year, y);
  }
  const topScore = new Map<number, number>();
  for (const m of monthly(ledger)) {
    const year = Number(m.month.slice(0, 4));
    if (m.score > (topScore.get(year) ?? -1)) {
      topScore.set(year, m.score);
      years.get(year)!.bestMonth = m.month;
    }
  }
  return [...years.values()].sort((a, b) => a.year - b.year);
}

export interface Memory {
  /** "YYYY-MM-DD" of the remembered day. */
  day: string;
  /** Whole years back, or 0 for "a month ago". */
  yearsAgo: number;
  games: number;
  best: number;
}

/** The same date in earlier years, and the same date one month ago: what you were playing then. */
export function memories(ledger: Ledger, today: string): Memory[] {
  const [y, m, d] = today.split('-').map(Number);
  const out: Memory[] = [];
  const pad = (n: number) => String(n).padStart(2, '0');
  const look = (year: number, month: number, day: number, yearsAgo: number) => {
    const key = `${year}-${pad(month)}-${pad(day)}`;
    const e = ledger[key];
    if (e && e.games > 0) out.push({ day: key, yearsAgo, games: e.games, best: e.best });
  };
  const lastMonth = m === 1 ? 12 : m - 1;
  look(m === 1 ? y - 1 : y, lastMonth, d, 0);
  for (let back = 1; back <= 30; back++) look(y - back, m, d, back);
  return out;
}
