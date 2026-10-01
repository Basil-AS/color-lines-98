import { dayKey } from './progress';
import type { GameRecord } from './stats';

/**
 * The permanent day-by-day ledger. The game history keeps only the latest 1000 games, the ledger keeps one small entry
 * per day played for ever, so careers, seasons and heatmaps stay complete after years of play.
 */
export interface DayEntry {
  games: number;
  completed: number;
  score: number;
  best: number;
  moves: number;
  lines: number;
  playMs: number;
}

export type Ledger = Record<string, DayEntry>;

const emptyDay = (): DayEntry => ({ games: 0, completed: 0, score: 0, best: 0, moves: 0, lines: 0, playMs: 0 });

export function addGame(ledger: Ledger, record: GameRecord): Ledger {
  const key = dayKey(record.endedAt);
  const day = ledger[key] ?? emptyDay();
  return {
    ...ledger,
    [key]: {
      games: day.games + 1,
      completed: day.completed + (record.completed ? 1 : 0),
      score: day.score + record.score,
      best: Math.max(day.best, record.score),
      moves: day.moves + record.moves,
      lines: day.lines + record.lines,
      playMs: day.playMs + record.durationMs,
    },
  };
}

export function ledgerFromHistory(history: readonly GameRecord[]): Ledger {
  let ledger: Ledger = {};
  for (const record of history) ledger = addGame(ledger, record);
  return ledger;
}

/** Day by day, keeps whichever side saw more games (used when two devices or a backup are combined). */
export function mergeLedgers(a: Ledger, b: Ledger): Ledger {
  const out: Ledger = { ...a };
  for (const [key, entry] of Object.entries(b)) {
    if (!out[key] || entry.games > out[key].games) out[key] = entry;
  }
  return out;
}

/**
 * The ledger after combining two devices: the two ledgers, then every day re-counted from the merged games, taking the
 * fuller of each. Two devices that played on the same day keep both sets of games.
 */
export function mergeLedgersWithHistory(a: Ledger, b: Ledger, mergedHistory: readonly GameRecord[]): Ledger {
  return mergeLedgers(mergeLedgers(a, b), ledgerFromHistory(mergedHistory));
}

const DAY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const count = (v: unknown): number => (typeof v === 'number' && Number.isFinite(v) && v >= 0 ? Math.floor(v) : 0);

/** Validates untrusted stored or imported data. */
export function sanitizeLedger(raw: unknown): Ledger {
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) return {};
  const out: Ledger = {};
  for (const [key, value] of Object.entries(raw)) {
    if (!DAY_PATTERN.test(key) || typeof value !== 'object' || value === null) continue;
    const v = value as Record<string, unknown>;
    const entry: DayEntry = {
      games: count(v.games),
      completed: count(v.completed),
      score: count(v.score),
      best: count(v.best),
      moves: count(v.moves),
      lines: count(v.lines),
      playMs: count(v.playMs),
    };
    if (entry.games > 0) out[key] = { ...entry, completed: Math.min(entry.completed, entry.games) };
  }
  return out;
}
