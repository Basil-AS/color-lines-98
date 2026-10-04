import { localTime } from './engine/telemetry';
import type { ModeId } from './engine/modes';
import { MODE_IDS } from './engine/modes';
import type { GameRecord } from './stats';

/**
 * A look at how the player thinks and when they play well, built from the play data of every game (see telemetry.ts).
 * Scores of different modes are not comparable, so every game is also given an index against the average of its own mode
 * (100 = a typical game of that mode); the time-of-day, fatigue and tempo findings use that index.
 * The same rules are in Cognition.kt: the two apps must show the same findings for the same games.
 */
export const MIN_MOVES = 10;
const SESSION_GAP_MS = 45 * 60_000;
const MIN_GROUP = 3;

export interface Bucket {
  games: number;
  /** Average index (100 = typical). */
  index: number;
}

export interface HourBucket extends Bucket {
  hour: number;
  /** Average decision time, ms. */
  decisionMs: number;
}

export type Chronotype = 'lark' | 'day' | 'evening' | 'owl';

export interface MindReport {
  /** Games with play data, and timed decisions in them. */
  games: number;
  decisions: number;
  avgDecisionMs: number;
  /** Typical spread of decision times relative to their mean (coefficient of variation, 0..). */
  variation: number;
  fastShare: number;
  slowShare: number;
  /** Share of the thinking time that happens before the first touch (planning rather than fiddling). */
  planning: number;
  byHour: HourBucket[];
  /** The best three consecutive hours (wrapping past midnight) and their index, when there is enough data. */
  bestWindow: { from: number; index: number; games: number } | null;
  chronotype: Chronotype | null;
  /** Monday first. */
  byWeekday: Bucket[];
  /** Average decision time in moves 1-20, 21-60 and 61+. */
  phases: { early: number; mid: number; late: number };
  /** Index of the 1st, 2nd, 3rd and later games of a sitting. */
  sittings: Bucket[];
  /** Index of the games with the quickest, middle and slowest third of decision times. */
  tempo: { fast: Bucket; mid: Bucket; slow: Bucket } | null;
  /** Free cells the board was down to, on average at its tightest; and tight moves per 100. */
  tightest: number;
  dangerPer100: number;
  undosPer100: number;
  hintsPer100: number;
  missesPer100: number;
  clearingShare: number;
  /** Average score of the first and the latest 50 games with play data, and the change in percent. */
  growth: { first: number; latest: number; percent: number } | null;
}

interface Played {
  r: GameRecord;
  rel: number;
  start: number;
  decisionMs: number;
}

const avg = (xs: readonly number[]): number => (xs.length === 0 ? 0 : xs.reduce((a, b) => a + b, 0) / xs.length);
const bucket = (xs: readonly number[]): Bucket => ({ games: xs.length, index: Math.round(avg(xs)) });

/** Mean score of each mode over the games worth judging. */
function modeMeans(games: readonly GameRecord[]): Record<ModeId, number> {
  const out = {} as Record<ModeId, number>;
  for (const m of MODE_IDS) out[m] = avg(games.filter((g) => g.mode === m && g.moves >= MIN_MOVES).map((g) => g.score));
  return out;
}

export function chronotypeOf(hour: number): Chronotype {
  if (hour >= 5 && hour < 11) return 'lark';
  if (hour >= 11 && hour < 17) return 'day';
  if (hour >= 17 && hour < 23) return 'evening';
  return 'owl';
}

export function mindReport(history: readonly GameRecord[]): MindReport {
  const means = modeMeans(history);
  const played: Played[] = [];
  for (const r of history) {
    if (!r.cog || r.cog.tm === 0 || r.moves < MIN_MOVES) continue;
    const mean = means[r.mode];
    played.push({ r, rel: mean > 0 ? (100 * r.score) / mean : 100, start: r.endedAt - r.durationMs, decisionMs: r.cog.think / r.cog.tm });
  }

  const sum = (f: (c: NonNullable<GameRecord['cog']>) => number) => played.reduce((a, p) => a + f(p.r.cog!), 0);
  const decisions = sum((c) => c.tm);
  const think = sum((c) => c.think);
  const moves = played.reduce((a, p) => a + p.r.moves, 0);
  const per100 = (n: number) => (moves === 0 ? 0 : (100 * n) / moves);

  // Variation: spread over mean for each game with enough decisions, averaged.
  const variations: number[] = [];
  for (const p of played) {
    const c = p.r.cog!;
    if (c.tm < 5) continue;
    const mean = c.think / c.tm;
    const variance = Math.max(0, (c.thinkSq * 10_000) / c.tm - mean * mean);
    variations.push(mean > 0 ? Math.sqrt(variance) / mean : 0);
  }

  const hours: HourBucket[] = Array.from({ length: 24 }, (_, hour) => {
    const inHour = played.filter((p) => localTime(p.start, p.r.cog!.tz).hour === hour);
    return { hour, games: inHour.length, index: Math.round(avg(inHour.map((p) => p.rel))), decisionMs: Math.round(avg(inHour.map((p) => p.decisionMs))) };
  });

  // The best three hours in a row: weigh each hour by its games, need a few games in the window.
  let bestWindow: MindReport['bestWindow'] = null;
  for (let from = 0; from < 24; from++) {
    const parts = [0, 1, 2].map((i) => hours[(from + i) % 24]);
    const games = parts.reduce((a, h) => a + h.games, 0);
    if (games < MIN_GROUP * 2) continue;
    const index = parts.reduce((a, h) => a + h.index * h.games, 0) / games;
    if (bestWindow === null || index > bestWindow.index) bestWindow = { from, index: Math.round(index), games };
  }
  const overall = avg(played.map((p) => p.rel));
  // A window is only "best" if it clearly beats the average of everything.
  if (bestWindow && (played.length < 12 || bestWindow.index < overall + 3)) bestWindow = null;

  const byWeekday: Bucket[] = Array.from({ length: 7 }, (_, d) => bucket(played.filter((p) => localTime(p.start, p.r.cog!.tz).weekday === d).map((p) => p.rel)));

  const phase = (p: 'p1' | 'p2' | 'p3', n: 'n1' | 'n2' | 'n3') => {
    const count = sum((c) => c[n]);
    return count === 0 ? 0 : Math.round(sum((c) => c[p]) / count);
  };

  // Sittings: games closer than SESSION_GAP_MS belong together; look at every game that has play data.
  const chrono = [...history].sort((a, b) => a.endedAt - b.endedAt);
  const relOf = new Map<GameRecord, number>(played.map((p) => [p.r, p.rel]));
  const groups: number[][] = [[], [], [], []];
  let position = 0;
  let lastEnd = -Infinity;
  for (const g of chrono) {
    const start = g.endedAt - g.durationMs;
    position = start - lastEnd > SESSION_GAP_MS ? 1 : position + 1;
    lastEnd = g.endedAt;
    const rel = relOf.get(g);
    if (rel !== undefined) groups[Math.min(position, 4) - 1].push(rel);
  }

  let tempo: MindReport['tempo'] = null;
  if (played.length >= 9) {
    const sorted = [...played].sort((a, b) => a.decisionMs - b.decisionMs);
    const third = Math.floor(sorted.length / 3);
    tempo = {
      fast: bucket(sorted.slice(0, third).map((p) => p.rel)),
      mid: bucket(sorted.slice(third, sorted.length - third).map((p) => p.rel)),
      slow: bucket(sorted.slice(sorted.length - third).map((p) => p.rel)),
    };
  }

  let growth: MindReport['growth'] = null;
  const byTime = [...played].sort((a, b) => a.r.endedAt - b.r.endedAt);
  if (byTime.length >= 100) {
    const first = Math.round(avg(byTime.slice(0, 50).map((p) => p.rel)));
    const latest = Math.round(avg(byTime.slice(-50).map((p) => p.rel)));
    growth = { first, latest, percent: first > 0 ? Math.round((100 * (latest - first)) / first) : 0 };
  }

  const owlHours = bestWindow ? (bestWindow.from + 1) % 24 : null;

  return {
    games: played.length,
    decisions,
    avgDecisionMs: decisions === 0 ? 0 : Math.round(think / decisions),
    variation: Math.round(avg(variations) * 100) / 100,
    fastShare: decisions === 0 ? 0 : sum((c) => c.fast) / decisions,
    slowShare: decisions === 0 ? 0 : sum((c) => c.slow) / decisions,
    planning: think === 0 ? 0 : sum((c) => c.lat) / think,
    byHour: hours,
    bestWindow,
    chronotype: owlHours === null ? null : chronotypeOf(owlHours),
    byWeekday,
    phases: { early: phase('p1', 'n1'), mid: phase('p2', 'n2'), late: phase('p3', 'n3') },
    sittings: groups.map(bucket),
    tempo,
    tightest: Math.round(avg(played.map((p) => p.r.cog!.minEmpty)) * 10) / 10,
    dangerPer100: Math.round(per100(sum((c) => c.danger)) * 10) / 10,
    undosPer100: Math.round(per100(sum((c) => c.undo)) * 10) / 10,
    hintsPer100: Math.round(per100(sum((c) => c.hint)) * 10) / 10,
    missesPer100: Math.round(per100(sum((c) => c.miss)) * 10) / 10,
    clearingShare: moves === 0 ? 0 : sum((c) => c.clears) / moves,
    growth,
  };
}

export type FindingId =
  | 'window'
  | 'chronotype'
  | 'tired'
  | 'fresh'
  | 'tempoFast'
  | 'tempoSlow'
  | 'tempoMid'
  | 'slowdown'
  | 'steady'
  | 'erratic'
  | 'impulsive'
  | 'planner'
  | 'tight'
  | 'calm'
  | 'growth'
  | 'weekday';

export interface Finding {
  id: FindingId;
  params: Record<string, string | number>;
}

/** Plain-language findings (each only when the data supports it), ready for translation. */
export function findings(r: MindReport): Finding[] {
  const out: Finding[] = [];
  if (r.games < 8) return out;
  if (r.bestWindow) {
    out.push({ id: 'window', params: { from: r.bestWindow.from, to: (r.bestWindow.from + 3) % 24, index: r.bestWindow.index - 100 } });
    if (r.chronotype) out.push({ id: 'chronotype', params: { type: r.chronotype } });
  }
  const [s1, , s3, s4] = r.sittings;
  const later = s3.games + s4.games >= MIN_GROUP * 2 ? (s3.index * s3.games + s4.index * s4.games) / (s3.games + s4.games) : null;
  if (later !== null && s1.games >= MIN_GROUP) {
    const drop = Math.round(s1.index - later);
    if (drop >= 6) out.push({ id: 'tired', params: { percent: drop } });
    else if (drop <= -6) out.push({ id: 'fresh', params: { percent: -drop } });
  }
  if (r.tempo) {
    const { fast, mid, slow } = r.tempo;
    const best = Math.max(fast.index, mid.index, slow.index);
    if (best - Math.min(fast.index, mid.index, slow.index) >= 6) {
      out.push({ id: best === fast.index ? 'tempoFast' : best === slow.index ? 'tempoSlow' : 'tempoMid', params: { index: best - 100 } });
    }
  }
  if (r.phases.early > 0 && r.phases.late > 0 && r.phases.late / r.phases.early >= 1.3) {
    out.push({ id: 'slowdown', params: { percent: Math.round((100 * (r.phases.late - r.phases.early)) / r.phases.early) } });
  }
  if (r.variation > 0) out.push({ id: r.variation <= 0.8 ? 'steady' : 'erratic', params: { value: r.variation } });
  if (r.fastShare >= 0.35) out.push({ id: 'impulsive', params: { percent: Math.round(r.fastShare * 100) } });
  else if (r.planning >= 0.6) out.push({ id: 'planner', params: { percent: Math.round(r.planning * 100) } });
  if (r.dangerPer100 >= 25) out.push({ id: 'tight', params: { percent: Math.round(r.dangerPer100) } });
  else if (r.tightest >= 20) out.push({ id: 'calm', params: { cells: r.tightest } });
  if (r.growth && Math.abs(r.growth.percent) >= 5) out.push({ id: 'growth', params: { percent: r.growth.percent, first: r.growth.first, latest: r.growth.latest } });
  const days = r.byWeekday.filter((d) => d.games >= MIN_GROUP);
  if (days.length >= 4) {
    const top = days.reduce((a, b) => (b.index > a.index ? b : a));
    const day = r.byWeekday.indexOf(top);
    if (top.index >= 108) out.push({ id: 'weekday', params: { day, index: top.index - 100 } });
  }
  return out;
}
