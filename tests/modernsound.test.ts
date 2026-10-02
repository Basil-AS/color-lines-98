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

describe('a voice for every look', () => {
  it('gives every theme a voice and every voice valid notes for every event', async () => {
    const { THEMES, soundVoice } = await import('../src/themes');
    const { voicedNotes } = await import('../src/modernsound');
    const { ALL_SOUND_KINDS: SOUND_KINDS } = await import('./soundkinds');
    for (const theme of THEMES) {
      const voice = soundVoice(theme);
      for (const kind of SOUND_KINDS) {
        const notes = voicedNotes(kind, 40, voice);
        expect(notes.length, `${theme}/${kind}`).toBeGreaterThan(0);
        for (const n of notes) {
          expect(n.freq).toBeGreaterThan(80);
          expect(n.freq).toBeLessThan(6000);
          expect(n.ms).toBeGreaterThan(0);
          expect(n.gain).toBeGreaterThan(0);
          expect(n.gain).toBeLessThan(0.5);
        }
      }
    }
  });

  it('makes the six new-style looks sound different from each other', async () => {
    const { THEMES, soundVoice, soundProfile } = await import('../src/themes');
    const { voicedNotes } = await import('../src/modernsound');
    const fingerprints = new Map<string, string>();
    for (const theme of THEMES.filter((t) => soundProfile(t) === 'modern')) {
      const fp = JSON.stringify(voicedNotes('eat', 30, soundVoice(theme)));
      expect(fingerprints.has(fp), `${theme} sounds like ${fingerprints.get(fp)}`).toBe(false);
      fingerprints.set(fp, theme);
    }
  });

  it('never plays a voice below the original pitch and keeps every voice loud enough to hear', async () => {
    const { VOICES, modernNotes, voicedNotes } = await import('../src/modernsound');
    const { ALL_SOUND_KINDS } = await import('./soundkinds');
    for (const [id, v] of Object.entries(VOICES)) expect(v.pitch, id).toBeGreaterThanOrEqual(1);
    for (const id of Object.keys(VOICES) as (keyof typeof VOICES)[]) {
      const peak = Math.max(...ALL_SOUND_KINDS.flatMap((k) => voicedNotes(k, 30, id).map((n) => n.gain)));
      const soft = Math.max(...ALL_SOUND_KINDS.flatMap((k) => modernNotes(k, 30).map((n) => n.gain)));
      expect(peak, id).toBeGreaterThan(soft * 0.3);
    }
  });
});
