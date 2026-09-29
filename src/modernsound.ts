import type { SoundKind } from './pcspeaker';

export interface ModernNote {
  freq: number;
  ms: number;
  wave: 'sine' | 'triangle';
  gain: number;
}

const n = (freq: number, ms: number, wave: ModernNote['wave'] = 'sine', gain = 0.22): ModernNote => ({ freq, ms, wave, gain });

// C major pentatonic: pleasant whatever order the notes come in.
const C5 = 523;
const D5 = 587;
const E5 = 659;
const G5 = 784;
const A5 = 880;
const C6 = 1047;
const D6 = 1175;
const E6 = 1319;

/** The soft synthesised sounds of the modern themes (the older looks keep their own sounds). */
export function modernNotes(kind: SoundKind, points = 0): ModernNote[] {
  switch (kind) {
    case 'select':
      return [n(C6, 70, 'sine', 0.2)];
    case 'jump':
      return [n(C5, 45), n(E5, 45), n(G5, 60)];
    case 'click':
      return [n(700, 35, 'triangle', 0.16)];
    case 'eat': {
      const ladder = [C5, D5, E5, G5, A5, C6, D6, E6];
      const count = Math.min(ladder.length, 3 + Math.floor(Math.max(0, points) / 12));
      return ladder.slice(0, count).map((f, i) => n(f, 75, 'sine', 0.2 + i * 0.01));
    }
    case 'lose':
      return [440, 349, 294, 220].map((f) => n(f, 190, 'triangle', 0.2));
    case 'win':
      return [C5, E5, G5, C6, E6].map((f) => n(f, 130, 'sine', 0.22));
  }
}
