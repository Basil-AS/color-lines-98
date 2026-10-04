import type { SoundKind } from './pcspeaker';

export interface Sample {
  path: string;
  volume: number;
}

/** Which recorded sample (Lines 98) plays for a game event. */
export function sampleFor(kind: SoundKind, points = 0): Sample {
  const s = (file: string, volume: number): Sample => ({ path: `sounds/classic/${file}.mp3`, volume });
  switch (kind) {
    case 'select':
      return s('selectBall', 0.6);
    case 'jump':
      return s('jump', 0.6);
    case 'eat': {
      let index = 1;
      if (points >= 30) index = 5;
      else if (points >= 20) index = 4;
      else if (points >= 15) index = 3;
      else if (points >= 12) index = 2;
      return s(`eatScore_${index}`, 0.8);
    }
    case 'lose':
      return s('Lose', 0.8);
    case 'win':
    case 'crown':
      return s('Win', 0.8);
    case 'click':
      return s('ButtonClick', 0.4);
    case 'blocked':
      return s('ButtonClick', 0.25);
    case 'start':
      return s('start', 0.6);
    case 'levelUp':
      return s('levelUp', 0.7);
    case 'record':
      return s('fireworks', 0.7);
    case 'achievement':
      return s('jumpBonus', 0.7);
    case 'combo':
      return s(`eatScore_${Math.min(5, 2 + Math.max(0, points))}`, 0.8);
    case 'danger':
      return s('ButtonClick', 0.3);
    case 'tick':
      return s('ButtonClick', 0.2);
    case 'hint':
      return s('selectBall', 0.5);
    case 'pop':
      return s('selectBall', 0.25);
  }
}
