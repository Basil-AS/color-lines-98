import type { SoundProfile } from './themes';

import { modernNotes } from './modernsound';
import { pcSpeakerNotes } from './pcspeaker';
import { sampleFor } from './samples';
import type { SoundKind } from './pcspeaker';

class SoundManager {
  private enabled: boolean = true;
  private profile: SoundProfile = 'sampled';
  private context: AudioContext | null = null;
  private audioCache: Map<string, HTMLAudioElement> = new Map();

  constructor() {
    const saved = localStorage.getItem('colorlines_sound_enabled');
    if (saved !== null) {
      this.enabled = saved === 'true';
    }
  }

  get isEnabled(): boolean {
    return this.enabled;
  }

  setSoundEnabled(enabled: boolean): void {
    this.enabled = enabled;
    localStorage.setItem('colorlines_sound_enabled', String(enabled));
  }

  toggle(): boolean {
    this.setSoundEnabled(!this.enabled);
    return this.enabled;
  }

  setProfile(profile: SoundProfile): void {
    this.profile = profile;
  }

  /** Plays synthesised notes: square beeps for the PC speaker, soft tones for the modern themes. */
  private synth(kind: SoundKind, points = 0): void {
    if (!this.enabled) return;
    try {
      this.context ??= new AudioContext();
      const ctx = this.context;
      void ctx.resume();
      let t = ctx.currentTime;
      const notes =
        this.profile === 'pcspeaker'
          ? pcSpeakerNotes(kind, points).map((b) => ({ freq: b.freq, ms: b.ms, wave: 'square' as const, gain: 0.06 }))
          : modernNotes(kind, points);
      for (const note of notes) {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = note.wave;
        osc.frequency.value = note.freq;
        const end = t + note.ms / 1000;
        if (note.wave === 'square') {
          gain.gain.value = note.gain;
        } else {
          // A quick attack and a smooth decay keep the soft tones free of clicks.
          gain.gain.setValueAtTime(0.0001, t);
          gain.gain.linearRampToValueAtTime(note.gain, t + 0.008);
          gain.gain.exponentialRampToValueAtTime(0.0001, end);
        }
        osc.connect(gain).connect(ctx.destination);
        osc.start(t);
        osc.stop(end + 0.02);
        t = end;
      }
    } catch {
      // Audio can be unavailable (no output device, blocked before a gesture); the game goes on silently.
    }
  }

  private playSound(path: string, volume = 0.7): void {
    if (!this.enabled) return;
    try {
      let audio = this.audioCache.get(path);
      if (!audio) {
        // Use relative path so it works with Vite base './'
        const base = import.meta.env.BASE_URL.replace(/\/$/, '');
        const fullPath = `${base}/${path.replace(/^\//, '')}`;
        audio = new Audio(fullPath);
        this.audioCache.set(path, audio);
      }
      audio.currentTime = 0;
      audio.volume = volume;
      audio.play().catch(() => {
        // Autoplay may be blocked before first user gesture
      });
    } catch {
      // Ignore audio error
    }
  }

  /** Plays a game event in the voice of the current look: samples, PC-speaker beeps or soft tones. */
  play(kind: SoundKind, points = 0): void {
    if (this.profile !== 'sampled') {
      this.synth(kind, points);
      return;
    }
    const sample = sampleFor(kind, points);
    this.playSound(sample.path, sample.volume);
  }
}

export const soundManager = new SoundManager();
