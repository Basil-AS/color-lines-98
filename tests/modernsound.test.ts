import { describe, expect, it } from 'vitest';
import { modernNotes } from '../src/modernsound';
import type { SoundKind } from '../src/pcspeaker';
import { soundProfile } from '../src/themes';

import { ALL_SOUND_KINDS } from './soundkinds';
const KINDS: SoundKind[] = ALL_SOUND_KINDS;

describe('modernNotes', () => {
  it.each(KINDS)('%s is a short, quiet, audible sound', (kind) => {
    const notes = modernNotes(kind, 30);
    expect(notes.length).toBeGreaterThan(0);
    for (const n of notes) {
      expect(n.freq).toBeGreaterThanOrEqual(100);
      expect(n.freq).toBeLessThanOrEqual(3000);
      expect(n.ms).toBeGreaterThan(0);
      expect(n.gain).toBeGreaterThan(0);
      expect(n.gain).toBeLessThanOrEqual(0.5);
      expect(['sine', 'triangle']).toContain(n.wave);
    }
    expect(notes.reduce((s, n) => s + n.ms, 0)).toBeLessThan(1600);
  });

  it('plays a longer run for bigger scores, capped', () => {
    const lengths = [10, 30, 60, 500].map((p) => modernNotes('eat', p).length);
    expect(lengths[1]).toBeGreaterThan(lengths[0]);
    expect(lengths[2]).toBeGreaterThan(lengths[1]);
    expect(lengths[3]).toBeLessThanOrEqual(8);
  });

  it('rises when winning and falls when losing', () => {
    const f = (k: SoundKind) => modernNotes(k, 0).map((n) => n.freq);
    expect(f('win')[0]).toBeLessThan(f('win').at(-1)!);
    expect(f('lose')[0]).toBeGreaterThan(f('lose').at(-1)!);
  });

  it('is deterministic', () => {
    expect(modernNotes('eat', 20)).toEqual(modernNotes('eat', 20));
  });
});

describe('sound profile per theme', () => {
  it('gives every look its own sound', () => {
    expect(soundProfile('modern')).toBe('modern');
    expect(soundProfile('light')).toBe('modern');
    for (const theme of ['material', 'neon', 'contrast'] as const) expect(soundProfile(theme)).toBe('modern');
    expect(soundProfile('lines98')).toBe('sampled');
    expect(soundProfile('colorlines92')).toBe('pcspeaker');
  });
});
