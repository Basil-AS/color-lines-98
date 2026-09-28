import { beforeEach, describe, expect, it } from 'vitest';
import { GameEngine } from '../src/engine/gameengine';
import {
  clearGame,
  loadBestScore,
  loadGame,
  loadTheme,
  saveBestScore,
  saveGame,
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
