import { HALL_SIZE, sanitizeHall } from './dos/hall';
import type { HallEntry } from './dos/hall';
import { ledgerFromHistory, sanitizeLedger } from './ledger';
import type { Ledger } from './ledger';
import { DAYS_LIMIT, emptyProgress, rebuildProgress, sanitizeProgress } from './progress';
import type { Progress } from './progress';
import { localTime } from './engine/telemetry';
import { sanitizeHistory } from './stats';
import type { GameRecord } from './stats';
import { isModeId } from './engine/modes';
import type { ModeId } from './engine/modes';
import { THEMES } from './themes';
import type { Theme } from './themes';

/** A file with everything worth moving between devices; the same format is written by the web app and by Android. */
export const BACKUP_FORMAT = 'color-lines-backup';
export const BACKUP_VERSION = 2;
/** Files bigger than this are not read: a real backup is a few megabytes even after years. */
export const BACKUP_MAX_BYTES = 64 * 1024 * 1024;

export interface BackupSettings {
  theme?: Theme;
  language?: 'auto' | 'en' | 'ru';
  playerName?: string;
  soundEnabled?: boolean;
  spawnPreview?: boolean | null;
  showNext?: boolean;
  mode?: ModeId;
}

export interface Backup {
  format: typeof BACKUP_FORMAT;
  version: number;
  exportedAt: number;
  app: { platform: string; version: string };
  history: GameRecord[];
  ledger: Ledger;
  progress: Progress;
  hall: HallEntry[];
  settings: BackupSettings;
}

export function buildBackup(data: Omit<Backup, 'format' | 'version'>): Backup {
  return { format: BACKUP_FORMAT, version: BACKUP_VERSION, ...data };
}

export type ParsedBackup = { ok: true; backup: Backup } | { ok: false; error: 'empty' | 'tooBig' | 'notJson' | 'notBackup' | 'newer' };

function sanitizeSettings(raw: unknown): BackupSettings {
  if (typeof raw !== 'object' || raw === null) return {};
  const r = raw as Record<string, unknown>;
  const out: BackupSettings = {};
  if (typeof r.theme === 'string' && (THEMES as readonly string[]).includes(r.theme)) out.theme = r.theme as Theme;
  if (r.language === 'auto' || r.language === 'en' || r.language === 'ru') out.language = r.language;
  if (typeof r.playerName === 'string') out.playerName = r.playerName.slice(0, 12);
  if (typeof r.soundEnabled === 'boolean') out.soundEnabled = r.soundEnabled;
  if (typeof r.spawnPreview === 'boolean' || r.spawnPreview === null) out.spawnPreview = r.spawnPreview;
  if (typeof r.showNext === 'boolean') out.showNext = r.showNext;
  if (isModeId(r.mode)) out.mode = r.mode;
  return out;
}

/** A profile from an older or other-platform file may lack the per-mode counters: derive them from the games. */
function withModeCounters(progress: Progress, raw: unknown, history: readonly GameRecord[]): Progress {
  const has = (key: string) => typeof raw === 'object' && raw !== null && typeof (raw as Record<string, unknown>)[key] === 'object';
  if (has('gamesByMode') && has('bestByMode')) return progress;
  const derived = rebuildProgress(history);
  return { ...progress, gamesByMode: has('gamesByMode') ? progress.gamesByMode : derived.gamesByMode, bestByMode: has('bestByMode') ? progress.bestByMode : derived.bestByMode };
}

/** Reads a backup file; nothing from it is trusted, every part goes through the same validation as stored data. */
export function parseBackup(text: string): ParsedBackup {
  if (text.trim() === '') return { ok: false, error: 'empty' };
  if (text.length > BACKUP_MAX_BYTES) return { ok: false, error: 'tooBig' };
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    return { ok: false, error: 'notJson' };
  }
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) return { ok: false, error: 'notBackup' };
  const r = raw as Record<string, unknown>;
  if (r.format !== BACKUP_FORMAT || typeof r.version !== 'number') return { ok: false, error: 'notBackup' };
  if (r.version > BACKUP_VERSION) return { ok: false, error: 'newer' };
  // Newest first whatever the order in the file.
  const history = sanitizeHistory(r.history).sort((a, b) => b.endedAt - a.endedAt);
  // The file's own ledger is trusted over one rebuilt in this device's time zone (games near midnight would double up).
  const ledger = r.ledger === undefined ? ledgerFromHistory(history) : sanitizeLedger(r.ledger);
  const app = (typeof r.app === 'object' && r.app !== null ? r.app : {}) as Record<string, unknown>;
  return {
    ok: true,
    backup: {
      format: BACKUP_FORMAT,
      version: BACKUP_VERSION,
      exportedAt: typeof r.exportedAt === 'number' && Number.isFinite(r.exportedAt) ? r.exportedAt : 0,
      app: { platform: typeof app.platform === 'string' ? app.platform.slice(0, 20) : 'unknown', version: typeof app.version === 'string' ? app.version.slice(0, 20) : '' },
      history,
      ledger,
      progress: r.progress === undefined ? rebuildProgress(history) : withModeCounters(sanitizeProgress(r.progress), r.progress, history),
      hall: sanitizeHall(r.hall),
      settings: sanitizeSettings(r.settings),
    },
  };
}

const gameKey = (g: GameRecord) => `${g.endedAt}:${g.score}:${g.moves}:${g.mode}`;

/** Adds the games of a backup to the current ones without duplicates; nothing already here is lost. */
export function mergeHistories(current: readonly GameRecord[], incoming: readonly GameRecord[]): GameRecord[] {
  const seen = new Map<string, number>();
  const out: GameRecord[] = [];
  for (const g of [...current, ...incoming]) {
    const key = gameKey(g);
    const at = seen.get(key);
    if (at !== undefined) {
      // The same game on both sides: keep the copy that carries the play data.
      if (!out[at].cog && g.cog) out[at] = g;
      continue;
    }
    seen.set(key, out.length);
    out.push(g);
  }
  return out.sort((a, b) => b.endedAt - a.endedAt);
}

export function mergeHalls(a: readonly HallEntry[], b: readonly HallEntry[]): HallEntry[] {
  const seen = new Set<string>();
  const all = [...a, ...b].filter((h) => {
    const key = `${h.name}:${h.score}:${h.at}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  return all.sort((x, y) => y.score - x.score || x.at - y.at).slice(0, HALL_SIZE);
}

/** The merged profile: rebuilt from the merged games, never below what either side had earned as bonus XP or goal days. */
export function mergeProgress(current: Progress, incoming: Progress, history: readonly GameRecord[]): Progress {
  const rebuilt = rebuildProgress(history);
  const base = history.length > 0 ? rebuilt : emptyProgress();
  const days = [...new Set([...current.days, ...incoming.days, ...base.days])].sort().slice(-DAYS_LIMIT);
  const achievements = { ...incoming.achievements, ...current.achievements };
  for (const [id, when] of Object.entries(base.achievements)) achievements[id] = Math.min(when, achievements[id] ?? when);
  return {
    ...base,
    totalGames: Math.max(base.totalGames, current.totalGames, incoming.totalGames),
    totalScore: Math.max(base.totalScore, current.totalScore, incoming.totalScore),
    totalLines: Math.max(base.totalLines, current.totalLines, incoming.totalLines),
    totalBalls: Math.max(base.totalBalls, current.totalBalls, incoming.totalBalls),
    totalMoves: Math.max(base.totalMoves, current.totalMoves, incoming.totalMoves),
    totalPlayMs: Math.max(base.totalPlayMs, current.totalPlayMs, incoming.totalPlayMs),
    completedGames: Math.max(base.completedGames, current.completedGames, incoming.completedGames),
    bestScore: Math.max(base.bestScore, current.bestScore, incoming.bestScore),
    bestLine: Math.max(base.bestLine, current.bestLine, incoming.bestLine),
    mostLinesInGame: Math.max(base.mostLinesInGame, current.mostLinesInGame, incoming.mostLinesInGame),
    longestGameMoves: Math.max(base.longestGameMoves, current.longestGameMoves, incoming.longestGameMoves),
    bonusXp: Math.max(current.bonusXp, incoming.bonusXp),
    goalDays: [...new Set([...current.goalDays, ...incoming.goalDays])].sort(),
    days,
    achievements,
    gamesByMode: Object.fromEntries(Object.keys(base.gamesByMode).map((m) => [m, Math.max(base.gamesByMode[m as ModeId], current.gamesByMode[m as ModeId], incoming.gamesByMode[m as ModeId])])) as Progress['gamesByMode'],
    bestByMode: Object.fromEntries(Object.keys(base.bestByMode).map((m) => [m, Math.max(base.bestByMode[m as ModeId], current.bestByMode[m as ModeId], incoming.bestByMode[m as ModeId])])) as Progress['bestByMode'],
  };
}

/** What a backup file holds, for the confirmation dialog. */
export function describeBackup(b: Backup): { games: number; first: number | null; last: number | null; exportedAt: number } {
  const times = b.history.map((g) => g.endedAt);
  return { games: Math.max(b.progress.totalGames, b.history.length), first: times.length ? Math.min(...times) : null, last: times.length ? Math.max(...times) : null, exportedAt: b.exportedAt };
}

const csvCell = (v: string | number | boolean) => {
  const s = String(v);
  // A leading = + - @ would be run as a formula by spreadsheets.
  const safe = /^[=+\-@]/.test(s) ? `'${s}` : s;
  return /[",\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
};

/** The games as a spreadsheet: one row per game, oldest first, with the play data (empty for games without it). */
export function historyToCsv(history: readonly GameRecord[]): string {
  const rows = [[
    'date', 'mode', 'score', 'moves', 'lines', 'balls', 'longest_line', 'play_seconds', 'completed',
    'local_hour', 'weekday', 'avg_decision_s', 'decision_spread_s', 'fast_moves', 'slow_moves', 'undos', 'hints', 'misses', 'clearing_moves', 'danger_moves', 'fewest_free_cells',
  ]];
  for (const g of [...history].reverse()) {
    const c = g.cog;
    const mean = c && c.tm > 0 ? c.think / c.tm : 0;
    const spread = c && c.tm > 1 ? Math.sqrt(Math.max(0, (c.thinkSq * 10_000) / c.tm - mean * mean)) : 0;
    const when = localTime(g.endedAt, c?.tz);
    const play: (string | number)[] = c
      ? [when.hour, when.weekday + 1, (mean / 1000).toFixed(2), (spread / 1000).toFixed(2), c.fast, c.slow, c.undo, c.hint, c.miss, c.clears, c.danger, c.minEmpty]
      : ['', '', '', '', '', '', '', '', '', '', '', ''];
    rows.push([new Date(g.endedAt).toISOString(), g.mode, g.score, g.moves, g.lines, g.balls, g.maxLine, Math.round(g.durationMs / 1000), g.completed, ...play].map(csvCell));
  }
  return rows.map((r) => r.join(',')).join('\n') + '\n';
}
