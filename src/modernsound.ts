import type { SoundKind } from './pcspeaker';
import type { VoiceId } from './themes';

export interface ModernNote {
  freq: number;
  ms: number;
  wave: 'sine' | 'triangle' | 'square' | 'sawtooth';
  gain: number;
}

const n = (freq: number, ms: number, wave: 'sine' | 'triangle' = 'sine', gain = 0.22): ModernNote => ({ freq, ms, wave, gain });

// C major pentatonic: pleasant whatever order the notes come in.
const C5 = 523;
const D5 = 587;
const E5 = 659;
const G5 = 784;
const A5 = 880;
const C6 = 1047;
const D6 = 1175;
const E6 = 1319;

interface Voice {
  /** Replaces the wave of every note (null keeps the soft sine/triangle mix). */
  wave: ModernNote['wave'] | null;
  /** Frequency multiplier: 2 is an octave up. */
  pitch: number;
  /** Duration multiplier. */
  tempo: number;
  gain: number;
}

/**
 * What makes the looks sound different: the same melodies, played by different instruments. No voice plays below the
 * original pitch (phone speakers hardly reproduce tones under 300 Hz) and the square and saw voices are boosted to match
 * the loudness of the sine ones.
 */
export const VOICES: Record<VoiceId, Voice> = {
  soft: { wave: null, pitch: 1, tempo: 1, gain: 1 },
  bell: { wave: 'sine', pitch: 2, tempo: 0.8, gain: 1 },
  marimba: { wave: 'triangle', pitch: 1, tempo: 0.7, gain: 1.5 },
  arcade: { wave: 'square', pitch: 1, tempo: 0.75, gain: 0.45 },
  saw: { wave: 'sawtooth', pitch: 1, tempo: 1.25, gain: 0.5 },
  glass: { wave: 'sine', pitch: 1.5, tempo: 1.5, gain: 1 },
  wood: { wave: 'triangle', pitch: 1.5, tempo: 0.5, gain: 1.6 },
  chip: { wave: 'square', pitch: 2, tempo: 0.5, gain: 0.4 },
  teletype: { wave: 'square', pitch: 1.5, tempo: 0.4, gain: 0.4 },
  beep: { wave: 'square', pitch: 1.25, tempo: 1.6, gain: 0.45 },
};

/** The melody of an event in the voice of a look. */
export function voicedNotes(kind: SoundKind, points: number, voice: VoiceId): ModernNote[] {
  const v = VOICES[voice];
  return modernNotes(kind, points).map((note) => ({
    freq: Math.round(note.freq * v.pitch),
    ms: Math.max(20, Math.round(note.ms * v.tempo)),
    wave: v.wave ?? note.wave,
    gain: note.gain * v.gain,
  }));
}

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
    case 'blocked':
      return [n(220, 80, 'triangle', 0.14), n(196, 110, 'triangle', 0.12)];
    case 'start':
      return [n(G5, 70), n(C6, 70), n(E6, 110)];
    case 'levelUp':
      return [C5, E5, G5, C6, E6].map((f, i) => n(f, 90, 'sine', 0.2 + i * 0.01));
    case 'achievement':
      return [n(E6, 70, 'sine', 0.2), n(G5 * 2, 130, 'sine', 0.22)];
    case 'record':
    case 'crown':
      return [C5, E5, G5, C6, G5, C6, E6].map((f, i) => n(f, i === 6 ? 320 : 110, 'sine', 0.22));
  }
}
