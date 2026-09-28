import { beforeEach, describe, expect, it } from 'vitest';
import { GameEngine } from '../src/engine/gameengine';
import {
  clearGame,
  clearHistory,
  loadBestScore,
  loadGame,
  loadHistory,
  loadLanguagePref,
  loadTheme,
  saveBestScore,
  saveGame,
  saveHistory,
  saveLanguagePref,
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
    store.setItem('colorlines_theme', 'neon');
    expect(loadTheme()).toBe('modern');
  });

  it('round-trips a valid theme', () => {
    saveTheme('retro92');
    expect(loadTheme()).toBe('retro92');
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
  const record = { score: 42, endedAt: 1_700_000_000_000, moves: 9, lines: 2, balls: 10, completed: true };

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
