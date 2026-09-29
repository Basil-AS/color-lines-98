export type SoundKind = 'select' | 'jump' | 'eat' | 'lose' | 'win' | 'click';

export interface Beep {
  freq: number;
  ms: number;
}

const NOTE = { C5: 523, D5: 587, E5: 659, G5: 784, A5: 880, C6: 1047, E6: 1319, G6: 1568 };

/** The beeps a PC speaker would play for a game event (used by the DOS theme). */
export function pcSpeakerNotes(kind: SoundKind, points = 0): Beep[] {
  switch (kind) {
    case 'select':
      return [{ freq: NOTE.A5, ms: 25 }];
    case 'jump':
      return [
        { freq: 440, ms: 20 },
        { freq: 660, ms: 20 },
      ];
    case 'click':
      return [{ freq: 330, ms: 20 }];
    case 'eat': {
      const ladder = [NOTE.C5, NOTE.E5, NOTE.G5, NOTE.C6, NOTE.E6, NOTE.G6, NOTE.C6 * 2];
      const count = Math.min(ladder.length, 3 + Math.floor(Math.max(0, points) / 12));
      return ladder.slice(0, count).map((freq) => ({ freq, ms: 45 }));
    }
    case 'lose':
      return [NOTE.A5, NOTE.G5, NOTE.E5, NOTE.C5, 262].map((freq) => ({ freq, ms: 140 }));
    case 'win':
      return [NOTE.C5, NOTE.E5, NOTE.G5, NOTE.C6].map((freq) => ({ freq, ms: 110 }));
  }
}
