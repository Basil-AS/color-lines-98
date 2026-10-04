import { GameEngine } from './engine/gameengine';
import { isModeId } from './engine/modes';
import type { ModeId } from './engine/modes';
import { sanitizeHall } from './dos/hall';
import type { HallEntry } from './dos/hall';
import { sanitizeProgress } from './progress';
import { ledgerFromHistory, sanitizeLedger } from './ledger';
import type { Ledger } from './ledger';
import type { Progress } from './progress';
import { isEffectsLevel } from './effects';
import type { EffectsLevel } from './effects';
import { compactHistory, expandHistory, sanitizeHistory } from './stats';
import type { GameRecord } from './stats';

import { parseTheme } from './themes';
import type { Theme } from './themes';

export { THEMES } from './themes';
export type { Theme } from './themes';

const THEME_KEY = 'colorlines_theme';
const BEST_KEY = 'colorlines_best_score';
const GAME_KEY = 'colorlines_game';
const HISTORY_KEY = 'colorlines_history';
/** The compact form (arrays); the first form (objects) is still read and replaced on the next save. */
const HISTORY_KEY_V2 = 'colorlines_history2';
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

function write(key: string, value: string): boolean {
  try {
    localStorage.setItem(key, value);
    return true;
  } catch {
    // Persistence is optional; see note above.
    return false;
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
  const compact = read(HISTORY_KEY_V2);
  const raw = compact ?? read(HISTORY_KEY);
  if (raw === null) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    return compact !== null ? expandHistory(parsed) : sanitizeHistory(parsed);
  } catch {
    return [];
  }
}

/** Set when the browser refused to store the games (its quota is full): the game list must be exported, see the data tab. */
let historyWriteFailed = false;

export function saveHistory(history: readonly GameRecord[]): void {
  historyWriteFailed = !write(HISTORY_KEY_V2, JSON.stringify(compactHistory(history)));
  if (!historyWriteFailed) {
    try {
      localStorage.removeItem(HISTORY_KEY);
    } catch {
      // See note above.
    }
  }
}

/** How much room the game list takes in this browser, and whether the last save failed. */
export function historyStorage(): { bytes: number; failed: boolean } {
  const raw = read(HISTORY_KEY_V2);
  return { bytes: raw === null ? 0 : raw.length * 2, failed: historyWriteFailed };
}

export function clearHistory(): void {
  try {
    localStorage.removeItem(HISTORY_KEY);
    localStorage.removeItem(HISTORY_KEY_V2);
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

const EFFECTS_KEY = 'colorlines_effects';
const VIBRATION_KEY = 'colorlines_vibration';

/** How lively the board is: off, calm or full. Calm when the system asks for reduced motion, full otherwise. */
export function loadEffects(reducedMotion: boolean): EffectsLevel {
  const raw = read(EFFECTS_KEY);
  return isEffectsLevel(raw) ? raw : reducedMotion ? 'calm' : 'full';
}

export function saveEffects(level: EffectsLevel): void {
  write(EFFECTS_KEY, level);
}

export function loadVibration(): boolean {
  return read(VIBRATION_KEY) !== 'false';
}

export function saveVibration(on: boolean): void {
  write(VIBRATION_KEY, String(on));
}
