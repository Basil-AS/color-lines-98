import { GameEngine } from './engine/gameengine';
import { isModeId } from './engine/modes';
import type { ModeId } from './engine/modes';
import { sanitizeHall } from './dos/hall';
import type { HallEntry } from './dos/hall';
import { sanitizeProgress } from './progress';
import { ledgerFromHistory, sanitizeLedger } from './ledger';
import type { Ledger } from './ledger';
import type { Progress } from './progress';
import { sanitizeHistory } from './stats';
import type { GameRecord } from './stats';

import { parseTheme } from './themes';
import type { Theme } from './themes';

export { THEMES } from './themes';
export type { Theme } from './themes';

const THEME_KEY = 'colorlines_theme';
const BEST_KEY = 'colorlines_best_score';
const GAME_KEY = 'colorlines_game';
const HISTORY_KEY = 'colorlines_history';
const LANG_KEY = 'colorlines_lang';
const PROGRESS_KEY = 'colorlines_progress';
const PREVIEW_KEY = 'colorlines_spawn_preview';
const HALL_KEY = 'colorlines_hall';
const NAME_KEY = 'colorlines_player_name';
const NEXT_KEY = 'colorlines_show_next';
const MODE_KEY = 'colorlines_mode';
const GOALS_KEY = 'colorlines_goals_done';
const LEDGER_KEY = 'colorlines_ledger';

export const LANGUAGE_PREFS = ['auto', 'en', 'ru'] as const;
export type LanguagePref = (typeof LANGUAGE_PREFS)[number];

// Storage can be missing or throw (private mode, blocked site data); the game
// must still work, so reads fall back to defaults and writes are best-effort.
function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Persistence is optional; see note above.
  }
}

export function loadTheme(): Theme {
  return parseTheme(read(THEME_KEY));
}

export function saveTheme(theme: Theme): void {
  write(THEME_KEY, theme);
}

export function loadBestScore(): number {
  const raw = read(BEST_KEY);
  if (raw === null || !/^\d+$/.test(raw)) return 0;
  return Number(raw);
}

export function saveBestScore(score: number): void {
  write(BEST_KEY, String(score));
}

export function loadGame(): GameEngine | null {
  const raw = read(GAME_KEY);
  if (raw === null) return null;
  try {
    return GameEngine.fromState(JSON.parse(raw));
  } catch {
    return null;
  }
}

export function saveGame(engine: GameEngine): void {
  write(GAME_KEY, JSON.stringify(engine.getState()));
}

export function clearGame(): void {
  try {
    localStorage.removeItem(GAME_KEY);
  } catch {
    // See note above.
  }
}

export function loadHistory(): GameRecord[] {
  const raw = read(HISTORY_KEY);
  if (raw === null) return [];
  try {
    return sanitizeHistory(JSON.parse(raw));
  } catch {
    return [];
  }
}

export function saveHistory(history: readonly GameRecord[]): void {
  write(HISTORY_KEY, JSON.stringify(history));
}

export function clearHistory(): void {
  try {
    localStorage.removeItem(HISTORY_KEY);
  } catch {
    // See note above.
  }
}

export function loadLanguagePref(): LanguagePref {
  const raw = read(LANG_KEY);
  return (LANGUAGE_PREFS as readonly string[]).includes(raw ?? '') ? (raw as LanguagePref) : 'auto';
}

export function saveLanguagePref(pref: LanguagePref): void {
  write(LANG_KEY, pref);
}

export function loadProgress(): Progress {
  const raw = read(PROGRESS_KEY);
  if (raw === null) return sanitizeProgress(null);
  try {
    return sanitizeProgress(JSON.parse(raw));
  } catch {
    return sanitizeProgress(null);
  }
}

export function saveProgress(progress: Progress): void {
  write(PROGRESS_KEY, JSON.stringify(progress));
}

export function clearProgress(): void {
  try {
    localStorage.removeItem(PROGRESS_KEY);
  } catch {
    // See note above.
  }
}

/** The player's explicit choice for marking the spawn cells, or null to use the theme default. */
export function loadSpawnPreview(): boolean | null {
  const raw = read(PREVIEW_KEY);
  return raw === 'true' ? true : raw === 'false' ? false : null;
}

export function saveSpawnPreview(enabled: boolean): void {
  write(PREVIEW_KEY, String(enabled));
}

export function loadHall(): HallEntry[] {
  const raw = read(HALL_KEY);
  if (raw === null) return [];
  try {
    return sanitizeHall(JSON.parse(raw));
  } catch {
    return [];
  }
}

export function saveHall(hall: readonly HallEntry[]): void {
  write(HALL_KEY, JSON.stringify(hall));
}

export function clearHall(): void {
  try {
    localStorage.removeItem(HALL_KEY);
  } catch {
    // See note above.
  }
}

export function loadPlayerName(): string {
  return (read(NAME_KEY) ?? '').slice(0, 12);
}

export function savePlayerName(name: string): void {
  write(NAME_KEY, name.slice(0, 12));
}

/** F3 "NEXT" of the 1992 screen: show the upcoming colours. On by default. */
export function loadShowNext(): boolean {
  return read(NEXT_KEY) !== 'false';
}

export function saveShowNext(show: boolean): void {
  write(NEXT_KEY, String(show));
}

/** The mode of the last game the player started. */
export function loadMode(): ModeId {
  const raw = read(MODE_KEY);
  return isModeId(raw) ? raw : 'classic';
}

export function saveMode(mode: ModeId): void {
  write(MODE_KEY, mode);
}

export interface GoalsDone {
  day: string;
  ids: string[];
}

/** Goals completed today; older days are ignored. */
export function loadGoalsDone(day: string): string[] {
  const raw = read(GOALS_KEY);
  if (raw === null) return [];
  try {
    const parsed = JSON.parse(raw) as Partial<GoalsDone>;
    if (parsed.day !== day || !Array.isArray(parsed.ids)) return [];
    return parsed.ids.filter((id): id is string => typeof id === 'string');
  } catch {
    return [];
  }
}

export function saveGoalsDone(day: string, ids: readonly string[]): void {
  write(GOALS_KEY, JSON.stringify({ day, ids }));
}

/** The permanent day ledger; built from the game history the first time (installs that predate it). */
export function loadLedger(): Ledger {
  const raw = read(LEDGER_KEY);
  if (raw !== null) {
    try {
      return sanitizeLedger(JSON.parse(raw));
    } catch {
      // Keep the unreadable value aside: the rebuild below covers only the latest games, and the next save would
      // overwrite the permanent ledger with it.
      write(LEDGER_KEY + '_corrupt', raw);
    }
  }
  return ledgerFromHistory(loadHistory());
}

export function saveLedger(ledger: Ledger): void {
  write(LEDGER_KEY, JSON.stringify(ledger));
}

export function clearLedger(): void {
  try {
    localStorage.removeItem(LEDGER_KEY);
  } catch {
    // See note above.
  }
}
