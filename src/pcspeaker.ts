/**
 * Everything the game can say: select/jump/eat/lose/click are the moves, `blocked` a refused move,
 * `start` a new game, `record` a new best score, `crown` the pretender taking the throne,
 * `levelUp` and `achievement` the progress rewards.
 */
export type SoundKind =
  | 'select'
  | 'jump'
  | 'eat'
  | 'lose'
  | 'win'
  | 'click'
  | 'blocked'
  | 'start'
  | 'record'
  | 'crown'
  | 'levelUp'
  | 'achievement';

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
    case 'blocked':
      return [{ freq: 196, ms: 70 }, { freq: 147, ms: 90 }];
    case 'start':
      return [NOTE.C5, NOTE.G5, NOTE.C6].map((freq) => ({ freq, ms: 70 }));
    case 'levelUp':
      return [NOTE.C5, NOTE.E5, NOTE.G5, NOTE.C6, NOTE.E6].map((freq) => ({ freq, ms: 75 }));
    case 'achievement':
      return [{ freq: NOTE.E6, ms: 60 }, { freq: NOTE.G6, ms: 110 }];
    // The trumpet fanfares of the original characters: a short triple, then the rising chord.
    case 'record':
    case 'crown':
      return [
        { freq: NOTE.C5, ms: 90 }, { freq: NOTE.C5, ms: 90 }, { freq: NOTE.C5, ms: 90 },
        { freq: NOTE.E5, ms: 160 }, { freq: NOTE.G5, ms: 120 }, { freq: NOTE.C6, ms: 320 },
      ];
  }
}
