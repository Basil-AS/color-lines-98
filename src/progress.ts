import { MODE_IDS } from './engine/modes';
import type { ModeId } from './engine/modes';
import type { GoalBonus } from './goals';
import type { GameRecord } from './stats';

const zeroByMode = (): Record<ModeId, number> => ({ classic: 0, easy: 0, blitz: 0, daily: 0 });

/** Everything the player has achieved over all games, independent of the capped history list. */
export interface Progress {
  totalGames: number;
  completedGames: number;
  totalScore: number;
  totalLines: number;
  totalBalls: number;
  totalMoves: number;
  totalPlayMs: number;
  bestScore: number;
  bestLine: number;
  mostLinesInGame: number;
  longestGameMoves: number;
  /** Local calendar days (YYYY-MM-DD) with at least one finished game, ascending. */
  days: string[];
  /** Achievement id -> when it was unlocked (epoch ms). */
  achievements: Record<string, number>;
  /** Experience from completed daily goals. */
  bonusXp: number;
  /** Days on which every daily goal was completed. */
  goalDays: string[];
  gamesByMode: Record<ModeId, number>;
  bestByMode: Record<ModeId, number>;
}

export const DAYS_LIMIT = 400;

export function emptyProgress(): Progress {
  return {
    totalGames: 0,
    completedGames: 0,
    totalScore: 0,
    totalLines: 0,
    totalBalls: 0,
    totalMoves: 0,
    totalPlayMs: 0,
    bestScore: 0,
    bestLine: 0,
    mostLinesInGame: 0,
    longestGameMoves: 0,
    days: [],
    achievements: {},
    bonusXp: 0,
    goalDays: [],
    gamesByMode: zeroByMode(),
    bestByMode: zeroByMode(),
  };
}

// ---- levels ------------------------------------------------------------------------------

export const LEVEL_TITLES = [
  'novice',
  'apprentice',
  'skilled',
  'expert',
  'master',
  'grandmaster',
  'legend',
] as const;
export type LevelTitle = (typeof LEVEL_TITLES)[number];

export function xpOf(p: Progress): number {
  return p.totalScore + p.totalLines * 5 + p.totalGames * 10 + p.bonusXp;
}

/** Experience needed to reach level n (level 1 needs none). */
export function levelThreshold(n: number): number {
  return 50 * n * (n - 1);
}

export interface LevelInfo {
  level: number;
  xp: number;
  /** Experience gathered since the start of this level. */
  into: number;
  /** Experience this level spans. */
  needed: number;
  fraction: number;
}

export function levelInfo(xp: number): LevelInfo {
  let level = 1;
  while (levelThreshold(level + 1) <= xp) level++;
  const into = xp - levelThreshold(level);
  const needed = levelThreshold(level + 1) - levelThreshold(level);
  return { level, xp, into, needed, fraction: into / needed };
}

const TITLE_FROM_LEVEL = [1, 3, 5, 7, 10, 15, 20];

export function titleIndex(level: number): number {
  let index = 0;
  TITLE_FROM_LEVEL.forEach((from, i) => {
    if (level >= from) index = i;
  });
  return index;
}

// ---- days and streaks --------------------------------------------------------------------

export function dayKey(epochMs: number): string {
  const d = new Date(epochMs);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

const DAY_RE = /^\d{4}-\d{2}-\d{2}$/;

function dayNumber(key: string): number {
  const [y, m, d] = key.split('-').map(Number);
  return Math.round(Date.UTC(y, m - 1, d) / 86_400_000);
}

/** Consecutive days ending today (or yesterday, so the streak survives until tonight). */
export function currentStreak(days: readonly string[], today: string): number {
  if (days.length === 0) return 0;
  const set = new Set(days.map(dayNumber));
  const todayN = dayNumber(today);
  let cursor = set.has(todayN) ? todayN : set.has(todayN - 1) ? todayN - 1 : -1;
  if (cursor === -1) return 0;
  let streak = 0;
  while (set.has(cursor)) {
    streak++;
    cursor--;
  }
  return streak;
}

export function bestStreak(days: readonly string[]): number {
  const nums = [...new Set(days.map(dayNumber))].sort((a, b) => a - b);
  let best = 0;
  let run = 0;
  nums.forEach((n, i) => {
    run = i > 0 && n === nums[i - 1] + 1 ? run + 1 : 1;
    best = Math.max(best, run);
  });
  return best;
}

// ---- achievements ------------------------------------------------------------------------

interface AchievementDef {
  id: string;
  test: (p: Progress, r: GameRecord, level: number, streak: number) => boolean;
}

export const ACHIEVEMENTS: readonly AchievementDef[] = [
  { id: 'first_game', test: (p) => p.totalGames >= 1 },
  { id: 'first_line', test: (p) => p.totalLines >= 1 },
  { id: 'long_line_7', test: (p) => p.bestLine >= 7 },
  { id: 'long_line_9', test: (p) => p.bestLine >= 9 },
  { id: 'score_100', test: (p) => p.bestScore >= 100 },
  { id: 'score_250', test: (p) => p.bestScore >= 250 },
  { id: 'score_500', test: (p) => p.bestScore >= 500 },
  { id: 'score_1000', test: (p) => p.bestScore >= 1000 },
  { id: 'games_10', test: (p) => p.totalGames >= 10 },
  { id: 'games_50', test: (p) => p.totalGames >= 50 },
  { id: 'games_100', test: (p) => p.totalGames >= 100 },
  { id: 'lines_50', test: (p) => p.totalLines >= 50 },
  { id: 'lines_250', test: (p) => p.totalLines >= 250 },
  { id: 'streak_3', test: (_p, _r, _l, streak) => streak >= 3 },
  { id: 'streak_7', test: (_p, _r, _l, streak) => streak >= 7 },
  { id: 'marathon', test: (_p, r) => r.moves >= 150 },
  { id: 'level_5', test: (_p, _r, level) => level >= 5 },
  { id: 'level_10', test: (_p, _r, level) => level >= 10 },
  { id: 'daily_first', test: (_p, r) => r.mode === 'daily' },
  { id: 'all_modes', test: (p) => MODE_IDS.every((m) => p.gamesByMode[m] > 0) },
  { id: 'goal_first', test: (p) => p.bonusXp > 0 },
  { id: 'goal_day', test: (p) => p.goalDays.length >= 1 },
  { id: 'goals_7', test: (p) => bestStreak(p.goalDays) >= 7 },
];

const ACHIEVEMENT_IDS = new Set(ACHIEVEMENTS.map((a) => a.id));

export interface AppliedGame {
  progress: Progress;
  /** Achievements unlocked by this game. */
  unlocked: string[];
}

/** Folds one finished game into the profile. Pure: the input is not modified. */
export function applyGame(before: Progress, record: GameRecord): AppliedGame {
  const days = before.days.includes(dayKey(record.endedAt))
    ? [...before.days]
    : [...before.days, dayKey(record.endedAt)].sort().slice(-DAYS_LIMIT);

  const progress: Progress = {
    totalGames: before.totalGames + 1,
    completedGames: before.completedGames + (record.completed ? 1 : 0),
    totalScore: before.totalScore + record.score,
    totalLines: before.totalLines + record.lines,
    totalBalls: before.totalBalls + record.balls,
    totalMoves: before.totalMoves + record.moves,
    totalPlayMs: before.totalPlayMs + record.durationMs,
    bestScore: Math.max(before.bestScore, record.score),
    bestLine: Math.max(before.bestLine, record.maxLine),
    mostLinesInGame: Math.max(before.mostLinesInGame, record.lines),
    longestGameMoves: Math.max(before.longestGameMoves, record.moves),
    days,
    achievements: { ...before.achievements },
    bonusXp: before.bonusXp,
    goalDays: [...before.goalDays],
    gamesByMode: { ...before.gamesByMode },
    bestByMode: { ...before.bestByMode },
  };
  const mode = record.mode ?? 'classic';
  progress.gamesByMode[mode]++;
  progress.bestByMode[mode] = Math.max(progress.bestByMode[mode], record.score);

  return awardAchievements(progress, record);
}

/** Unlocks every achievement whose condition now holds; the input is not modified. */
export function awardAchievements(before: Progress, record: GameRecord): AppliedGame {
  const progress: Progress = { ...before, achievements: { ...before.achievements } };
  const level = levelInfo(xpOf(progress)).level;
  const streak = currentStreak(progress.days, dayKey(record.endedAt));
  const unlocked: string[] = [];
  for (const a of ACHIEVEMENTS) {
    if (progress.achievements[a.id] === undefined && a.test(progress, record, level, streak)) {
      progress.achievements[a.id] = record.endedAt;
      unlocked.push(a.id);
    }
  }
  return { progress, unlocked };
}

/** Adds the experience of newly completed goals and remembers a fully completed day. */
export function applyGoalBonus(before: Progress, day: string, bonus: GoalBonus): Progress {
  if (bonus.newlyDone.length === 0) return before;
  const goalDays = bonus.allDone && !before.goalDays.includes(day) ? [...before.goalDays, day].sort().slice(-DAYS_LIMIT) : before.goalDays;
  return { ...before, bonusXp: before.bonusXp + bonus.xp, goalDays };
}

/** Rebuilds the profile from a newest-first history (used for saves made before profiles existed). */
export function rebuildProgress(newestFirst: readonly GameRecord[]): Progress {
  let progress = emptyProgress();
  for (const record of [...newestFirst].reverse()) progress = applyGame(progress, record).progress;
  return progress;
}

// ---- persistence and charts --------------------------------------------------------------

const isCount = (v: unknown): v is number => typeof v === 'number' && Number.isInteger(v) && v >= 0;

/** Validates untrusted stored data; anything wrong falls back to defaults. */
export function sanitizeProgress(raw: unknown): Progress {
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) return emptyProgress();
  const r = raw as Record<string, unknown>;
  const base = emptyProgress();
  const out = { ...base };
  for (const key of [
    'totalGames',
    'completedGames',
    'totalScore',
    'totalLines',
    'totalBalls',
    'totalMoves',
    'totalPlayMs',
    'bestScore',
    'bestLine',
    'mostLinesInGame',
    'longestGameMoves',
  ] as const) {
    const v = r[key];
    out[key] = isCount(v) ? v : 0;
  }
  out.days = Array.isArray(r.days)
    ? r.days.filter((d): d is string => typeof d === 'string' && DAY_RE.test(d)).slice(-DAYS_LIMIT)
    : [];
  if (typeof r.achievements === 'object' && r.achievements !== null) {
    for (const [id, when] of Object.entries(r.achievements)) {
      if (ACHIEVEMENT_IDS.has(id) && isCount(when)) out.achievements[id] = when;
    }
  }
  out.bonusXp = isCount(r.bonusXp) ? r.bonusXp : 0;
  out.goalDays = Array.isArray(r.goalDays)
    ? r.goalDays.filter((d): d is string => typeof d === 'string' && DAY_RE.test(d)).slice(-DAYS_LIMIT)
    : [];
  for (const key of ['gamesByMode', 'bestByMode'] as const) {
    const raw = r[key];
    out[key] = zeroByMode();
    if (typeof raw === 'object' && raw !== null) {
      for (const m of MODE_IDS) {
        const v = (raw as Record<string, unknown>)[m];
        if (isCount(v)) out[key][m] = v;
      }
    }
  }
  return out;
}

/** The latest scores, oldest first, for a small trend chart. */
export function scoreTrend(newestFirst: readonly GameRecord[], count: number): number[] {
  return newestFirst
    .slice(0, count)
    .map((g) => g.score)
    .reverse();
}
