import { GameEngine } from './engine/gameengine';

export const THEMES = ['modern', 'classic98', 'retro92'] as const;
export type Theme = (typeof THEMES)[number];

const THEME_KEY = 'colorlines_theme';
const BEST_KEY = 'colorlines_best_score';
const GAME_KEY = 'colorlines_game';

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
