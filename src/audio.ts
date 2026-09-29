import type { SoundProfile } from './themes';

import { pcSpeakerNotes } from './pcspeaker';
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

  private beep(kind: SoundKind, points = 0): void {
    if (!this.enabled) return;
    try {
      this.context ??= new AudioContext();
      const ctx = this.context;
      void ctx.resume();
      let t = ctx.currentTime;
      for (const note of pcSpeakerNotes(kind, points)) {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'square';
        osc.frequency.value = note.freq;
        gain.gain.value = 0.06;
        osc.connect(gain).connect(ctx.destination);
        osc.start(t);
        osc.stop(t + note.ms / 1000);
        t += note.ms / 1000;
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

  playSelect(): void {
    if (this.profile === 'pcspeaker') return this.beep('select');
    this.playSound('sounds/classic/selectBall.mp3', 0.6);
  }

  playJump(): void {
    if (this.profile === 'pcspeaker') return this.beep('jump');
    this.playSound('sounds/classic/jump.mp3', 0.6);
  }

  playEat(points: number): void {
    if (this.profile === 'pcspeaker') return this.beep('eat', points);
    let index = 1;
    if (points >= 30) index = 5;
    else if (points >= 20) index = 4;
    else if (points >= 15) index = 3;
    else if (points >= 12) index = 2;
    this.playSound(`sounds/classic/eatScore_${index}.mp3`, 0.8);
  }

  playLose(): void {
    if (this.profile === 'pcspeaker') return this.beep('lose');
    this.playSound('sounds/classic/Lose.mp3', 0.8);
  }

  playWin(): void {
    if (this.profile === 'pcspeaker') return this.beep('win');
    this.playSound('sounds/classic/Win.mp3', 0.8);
  }

  playClick(): void {
    if (this.profile === 'pcspeaker') return this.beep('click');
    this.playSound('sounds/classic/ButtonClick.mp3', 0.4);
  }
}

export const soundManager = new SoundManager();
