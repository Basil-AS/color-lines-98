// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EFFECT_LEVELS, isEffectsLevel, sparkCount, travelMs } from '../src/effects';
import { HAPTIC_PATTERNS, haptic, setHapticsEnabled } from '../src/haptics';
import { loadEffects, loadVibration, saveEffects, saveVibration } from '../src/storage';
import { parseBackup } from '../src/backup';
import { usesSprites, soundProfile, soundVoice, THEMES } from '../src/themes';

afterEach(() => {
  localStorage.clear();
  setHapticsEnabled(true);
  vi.restoreAllMocks();
});

describe('effects', () => {
  it('times a travel by its length, within limits', () => {
    expect(travelMs(1)).toBe(150);
    expect(travelMs(6)).toBe(270);
    expect(travelMs(40)).toBe(460);
  });

  it('gives bigger clears more sparks, but never too many', () => {
    expect(sparkCount(10, 'full')).toBeLessThan(sparkCount(42, 'full'));
    expect(sparkCount(1000, 'full')).toBe(26);
    expect(sparkCount(1000, 'calm')).toBe(12);
  });

  it('knows its levels and remembers the choice', () => {
    expect(EFFECT_LEVELS).toEqual(['off', 'calm', 'full']);
    expect(isEffectsLevel('calm')).toBe(true);
    expect(isEffectsLevel('wild')).toBe(false);
    expect(loadEffects(false)).toBe('full');
    expect(loadEffects(true)).toBe('calm');
    saveEffects('off');
    expect(loadEffects(false)).toBe('off');
    localStorage.setItem('colorlines_effects', 'wild');
    expect(loadEffects(false)).toBe('full');
  });

  it('keeps vibration on by default', () => {
    expect(loadVibration()).toBe(true);
    saveVibration(false);
    expect(loadVibration()).toBe(false);
  });

  it('travels through backups', () => {
    const file = JSON.stringify({ format: 'color-lines-backup', version: 2, history: [], settings: { effects: 'calm', vibration: false } });
    const parsed = parseBackup(file);
    expect(parsed.ok && parsed.backup.settings).toMatchObject({ effects: 'calm', vibration: false });
    const bad = parseBackup(JSON.stringify({ format: 'color-lines-backup', version: 2, settings: { effects: 'x', vibration: 'y' } }));
    expect(bad.ok && bad.backup.settings).toEqual({});
  });
});

describe('haptics', () => {
  it('vibrates with the pattern when enabled and supported', () => {
    const spy = vi.fn();
    Object.defineProperty(navigator, 'vibrate', { value: spy, configurable: true });
    haptic('blocked');
    expect(spy).toHaveBeenCalledWith(HAPTIC_PATTERNS.blocked);
    setHapticsEnabled(false);
    spy.mockClear();
    haptic('blocked');
    expect(spy).not.toHaveBeenCalled();
  });

  it('does nothing where the browser cannot vibrate', () => {
    Object.defineProperty(navigator, 'vibrate', { value: undefined, configurable: true });
    expect(() => haptic('record')).not.toThrow();
  });

  it('has positive durations and total duration <= 900ms for every pattern', () => {
    for (const [kind, pattern] of Object.entries(HAPTIC_PATTERNS)) {
      expect(pattern.length, `pattern for ${kind} should not be empty`).toBeGreaterThan(0);
      for (const d of pattern) {
        expect(d, `durations in ${kind} must be positive`).toBeGreaterThan(0);
      }
      const total = pattern.reduce((acc, cur) => acc + cur, 0);
      expect(total, `total duration of ${kind} must be <= 900ms`).toBeLessThanOrEqual(900);
    }
  });
});

describe('98 Modern', () => {
  it('shows the Lines 98 sprites with the samples of Lines 98', () => {
    expect(THEMES).toContain('lines98plus');
    expect(usesSprites('lines98plus')).toBe(true);
    expect(usesSprites('modern')).toBe(false);
    expect(soundProfile('lines98plus')).toBe('sampled');
    expect(soundVoice('lines98plus')).toBe('soft');
  });
});
