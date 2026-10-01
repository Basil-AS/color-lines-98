import { beforeEach, describe, expect, it } from 'vitest';
import { GameEngine } from '../src/engine/gameengine';
import { defaultSpawnPreview } from '../src/themes';
import {
  clearGame,
  clearHall,
  clearHistory,
  clearProgress,
  loadBestScore,
  loadGame,
  loadHistory,
  loadHall,
  loadGoalsDone,
  loadLanguagePref,
  loadMode,
  loadPlayerName,
  loadProgress,
  loadShowNext,
  loadSpawnPreview,
  loadTheme,
  saveBestScore,
  saveGame,
  saveHistory,
  saveHall,
  saveGoalsDone,
  saveLanguagePref,
  saveMode,
  savePlayerName,
  saveProgress,
  saveShowNext,
  saveSpawnPreview,
  saveTheme,
} from '../src/storage';

class MemoryStorage {
  private data = new Map<string, string>();
  getItem(k: string) { return this.data.get(k) ?? null; }
  setItem(k: string, v: string) { this.data.set(k, v); }
  removeItem(k: string) { this.data.delete(k); }
  clear() { this.data.clear(); }
}

let store: MemoryStorage;

beforeEach(() => {
  store = new MemoryStorage();
  Object.defineProperty(globalThis, 'localStorage', { value: store, configurable: true });
});

describe('theme', () => {
  it('defaults to modern and ignores unknown values', () => {
    expect(loadTheme()).toBe('modern');
    store.setItem('colorlines_theme', 'glitter');
    expect(loadTheme()).toBe('modern');
  });

  it('round-trips every theme', () => {
    for (const theme of ['modern', 'light', 'material', 'neon', 'contrast', 'lines98', 'colorlines92'] as const) {
      saveTheme(theme);
      expect(loadTheme()).toBe(theme);
    }
  });

  it('migrates the theme names used before the redesign', () => {
    store.setItem('colorlines_theme', 'classic98');
    expect(loadTheme()).toBe('lines98');
    store.setItem('colorlines_theme', 'retro92');
    expect(loadTheme()).toBe('colorlines92');
  });
});

describe('best score', () => {
  it.each(['abc', '-4', '1.5', '', 'NaN'])('falls back to 0 for %j', (raw) => {
    store.setItem('colorlines_best_score', raw);
    expect(loadBestScore()).toBe(0);
  });

  it('round-trips', () => {
    saveBestScore(250);
    expect(loadBestScore()).toBe(250);
  });
});

describe('saved game', () => {
  it('returns null when nothing is saved or data is corrupt', () => {
    expect(loadGame()).toBeNull();
    store.setItem('colorlines_game', '{not json');
    expect(loadGame()).toBeNull();
    store.setItem('colorlines_game', JSON.stringify({ size: 3 }));
    expect(loadGame()).toBeNull();
  });

  it('restores a saved game and clears it', () => {
    const engine = new GameEngine();
    saveGame(engine);
    expect(loadGame()!.getState()).toEqual(engine.getState());
    clearGame();
    expect(loadGame()).toBeNull();
  });
});

describe('unavailable storage', () => {
  it('does not throw when storage access is denied', () => {
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      get() { throw new Error('SecurityError'); },
    });
    expect(loadTheme()).toBe('modern');
    expect(loadBestScore()).toBe(0);
    expect(loadGame()).toBeNull();
    expect(() => saveTheme('modern')).not.toThrow();
    expect(() => saveGame(new GameEngine())).not.toThrow();
  });
});

describe('history storage', () => {
  const record = { score: 42, endedAt: 1_700_000_000_000, moves: 9, lines: 2, balls: 10, completed: true, maxLine: 5, durationMs: 90_000, mode: 'classic' as const };

  it('is empty by default and after corrupt data', () => {
    expect(loadHistory()).toEqual([]);
    store.setItem('colorlines_history', '{oops');
    expect(loadHistory()).toEqual([]);
  });

  it('round-trips and clears', () => {
    saveHistory([record]);
    expect(loadHistory()).toEqual([record]);
    clearHistory();
    expect(loadHistory()).toEqual([]);
  });

  it('never throws when storage is denied', () => {
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      get() { throw new Error('SecurityError'); },
    });
    expect(loadHistory()).toEqual([]);
    expect(() => saveHistory([record])).not.toThrow();
    expect(() => clearHistory()).not.toThrow();
  });
});

describe('language preference', () => {
  it('defaults to auto and ignores unknown values', () => {
    expect(loadLanguagePref()).toBe('auto');
    store.setItem('colorlines_lang', 'klingon');
    expect(loadLanguagePref()).toBe('auto');
  });

  it('round-trips', () => {
    saveLanguagePref('ru');
    expect(loadLanguagePref()).toBe('ru');
  });
});

describe('progress storage', () => {
  it('starts empty, round-trips, clears and survives corrupt data', async () => {
    const { applyGame, emptyProgress } = await import('../src/progress');
    expect(loadProgress()).toEqual(emptyProgress());
    const played = applyGame(emptyProgress(), {
      score: 60, endedAt: new Date(2026, 8, 29, 12).getTime(), moves: 20, lines: 2, balls: 10,
      completed: true, maxLine: 5, durationMs: 1000, mode: 'classic',
    }).progress;
    saveProgress(played);
    expect(loadProgress()).toEqual(played);
    store.setItem('colorlines_progress', '{oops');
    expect(loadProgress()).toEqual(emptyProgress());
    saveProgress(played);
    clearProgress();
    expect(loadProgress()).toEqual(emptyProgress());
  });

  it('never throws when storage is denied', () => {
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      get() { throw new Error('SecurityError'); },
    });
    expect(() => loadProgress()).not.toThrow();
    expect(() => saveProgress(loadProgress())).not.toThrow();
    expect(() => clearProgress()).not.toThrow();
  });
});

describe('spawn preview setting', () => {
  it('is unset until chosen, then remembers the choice', () => {
    expect(loadSpawnPreview()).toBeNull();
    saveSpawnPreview(false);
    expect(loadSpawnPreview()).toBe(false);
    saveSpawnPreview(true);
    expect(loadSpawnPreview()).toBe(true);
    store.setItem('colorlines_spawn_preview', 'maybe');
    expect(loadSpawnPreview()).toBeNull();
  });

  it('defaults per theme: on except for the 1992 DOS look, which never marked the cells', () => {
    expect(defaultSpawnPreview('modern')).toBe(true);
    expect(defaultSpawnPreview('light')).toBe(true);
    expect(defaultSpawnPreview('lines98')).toBe(true);
    expect(defaultSpawnPreview('colorlines92')).toBe(false);
  });
});

describe('hall of fame, player name and NEXT toggle', () => {
  it('round-trips the hall and survives corrupt data', () => {
    expect(loadHall()).toEqual([]);
    saveHall([{ name: 'Ann', score: 300, at: 5 }]);
    expect(loadHall()).toEqual([{ name: 'Ann', score: 300, at: 5 }]);
    store.setItem('colorlines_hall', '{oops');
    expect(loadHall()).toEqual([]);
    saveHall([{ name: 'Ann', score: 300, at: 5 }]);
    clearHall();
    expect(loadHall()).toEqual([]);
  });

  it('keeps the player name (at most 12 characters)', () => {
    expect(loadPlayerName()).toBe('');
    savePlayerName('Alexander the Great');
    expect(loadPlayerName()).toBe('Alexander th');
  });

  it('shows the next balls by default and remembers F3', () => {
    expect(loadShowNext()).toBe(true);
    saveShowNext(false);
    expect(loadShowNext()).toBe(false);
  });
});

describe('mode and goals storage', () => {
  it('remembers the last mode and ignores unknown values', () => {
    expect(loadMode()).toBe('classic');
    saveMode('blitz');
    expect(loadMode()).toBe('blitz');
    store.setItem('colorlines_mode', 'chaos');
    expect(loadMode()).toBe('classic');
  });

  it('keeps the goals done today and forgets other days', () => {
    expect(loadGoalsDone('2026-09-30')).toEqual([]);
    saveGoalsDone('2026-09-30', ['a', 'b']);
    expect(loadGoalsDone('2026-09-30')).toEqual(['a', 'b']);
    expect(loadGoalsDone('2026-10-01')).toEqual([]);
    store.setItem('colorlines_goals_done', '{oops');
    expect(loadGoalsDone('2026-09-30')).toEqual([]);
  });
});

describe('a corrupt ledger', () => {
  it('is kept aside instead of being lost when the ledger is rebuilt from the games', async () => {
    const { loadLedger } = await import('../src/storage');
    localStorage.setItem('colorlines_ledger', '{broken');
    expect(loadLedger()).toEqual({});
    expect(localStorage.getItem('colorlines_ledger_corrupt')).toBe('{broken');
  });
});
