import { describe, expect, it } from 'vitest';
import { pcSpeakerNotes } from '../src/pcspeaker';
import type { SoundKind } from '../src/pcspeaker';

const KINDS: SoundKind[] = ['select', 'jump', 'eat', 'lose', 'win', 'click'];

describe('pcSpeakerNotes', () => {
  it.each(KINDS)('%s is a short audible melody', (kind) => {
    const notes = pcSpeakerNotes(kind, 20);
    expect(notes.length).toBeGreaterThan(0);
    for (const n of notes) {
      expect(n.freq).toBeGreaterThanOrEqual(100);
      expect(n.freq).toBeLessThanOrEqual(5000);
      expect(n.ms).toBeGreaterThan(0);
    }
    const total = notes.reduce((sum, n) => sum + n.ms, 0);
    expect(total).toBeLessThan(1500);
  });

  it('plays a longer flourish for bigger scores', () => {
    const small = pcSpeakerNotes('eat', 10).length;
    const big = pcSpeakerNotes('eat', 60).length;
    expect(big).toBeGreaterThan(small);
  });

  it('is deterministic', () => {
    expect(pcSpeakerNotes('lose', 0)).toEqual(pcSpeakerNotes('lose', 0));
  });

  it('descends for losing and ascends for winning', () => {
    const lose = pcSpeakerNotes('lose', 0).map((n) => n.freq);
    const win = pcSpeakerNotes('win', 0).map((n) => n.freq);
    expect(lose[0]).toBeGreaterThan(lose[lose.length - 1]);
    expect(win[0]).toBeLessThan(win[win.length - 1]);
  });
});
