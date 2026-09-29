import { existsSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { modernNotes } from '../src/modernsound';
import { pcSpeakerNotes } from '../src/pcspeaker';
import { sampleFor } from '../src/samples';
import { ALL_SOUND_KINDS } from './soundkinds';

describe('every look can play every event', () => {
  it.each(ALL_SOUND_KINDS)('%s has a sample that exists, PC-speaker notes and soft notes', (kind) => {
    for (const points of [0, 10, 12, 15, 20, 30, 60]) {
      const sample = sampleFor(kind, points);
      expect(existsSync('public/' + sample.path)).toBe(true);
      expect(sample.volume).toBeGreaterThan(0);
      expect(sample.volume).toBeLessThanOrEqual(1);
    }
    expect(pcSpeakerNotes(kind, 20).length).toBeGreaterThan(0);
    expect(modernNotes(kind, 20).length).toBeGreaterThan(0);
  });

  it('picks a louder, richer sample for bigger clears', () => {
    const paths = [10, 12, 15, 20, 30].map((p) => sampleFor('eat', p).path);
    expect(new Set(paths).size).toBe(5);
  });

  it('gives the fanfare to a new record and the coronation', () => {
    expect(pcSpeakerNotes('record').length).toBeGreaterThanOrEqual(6);
    expect(pcSpeakerNotes('crown')).toEqual(pcSpeakerNotes('record'));
    expect(sampleFor('record').path).not.toBe(sampleFor('lose').path);
  });
});
