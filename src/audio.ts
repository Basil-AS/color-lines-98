class SoundManager {
  private enabled: boolean = true;
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
    this.playSound('sounds/classic/selectBall.mp3', 0.6);
  }

  playJump(): void {
    this.playSound('sounds/classic/jump.mp3', 0.6);
  }

  playEat(points: number): void {
    let index = 1;
    if (points >= 30) index = 5;
    else if (points >= 20) index = 4;
    else if (points >= 15) index = 3;
    else if (points >= 12) index = 2;
    this.playSound(`sounds/classic/eatScore_${index}.mp3`, 0.8);
  }

  playLose(): void {
    this.playSound('sounds/classic/Lose.mp3', 0.8);
  }

  playWin(): void {
    this.playSound('sounds/classic/Win.mp3', 0.8);
  }

  playClick(): void {
    this.playSound('sounds/classic/ButtonClick.mp3', 0.4);
  }
}

export const soundManager = new SoundManager();
