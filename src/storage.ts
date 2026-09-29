import { GameEngine } from './engine/gameengine';
import { sanitizeProgress } from './progress';
import type { Progress } from './progress';
import { sanitizeHistory } from './stats';
import type { GameRecord } from './stats';

export const THEMES = ['modern', 'classic98', 'retro92'] as const;
export type Theme = (typeof THEMES)[number];

const THEME_KEY = 'colorlines_theme';
const BEST_KEY = 'colorlines_best_score';
const GAME_KEY = 'colorlines_game';
const HISTORY_KEY = 'colorlines_history';
const LANG_KEY = 'colorlines_lang';
const PROGRESS_KEY = 'colorlines_progress';
const PREVIEW_KEY = 'colorlines_spawn_preview';

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
  const raw = read(THEME_KEY);
  return (THEMES as readonly string[]).includes(raw ?? '') ? (raw as Theme) : 'modern';
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

/** Show small balls on the board where the next balls will appear. On by default. */
export function loadSpawnPreview(): boolean {
  return read(PREVIEW_KEY) !== 'false';
}

export function saveSpawnPreview(enabled: boolean): void {
  write(PREVIEW_KEY, String(enabled));
}
