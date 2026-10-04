import type { BallColor, Point } from './engine/models';

/**
 * The life of the board: balls that travel along their path and land with a squash, bursts of sparks when a line
 * goes, shaking on a big one, floating points, rippling taps, a nervous glow when the board is nearly full and a ball
 * that now and then wiggles on its own. Everything is drawn over the board (DOM and one canvas) and never changes the
 * game; with effects off nothing here runs.
 */
export type EffectsLevel = 'off' | 'calm' | 'full';
export const EFFECT_LEVELS: readonly EffectsLevel[] = ['off', 'calm', 'full'];

export const isEffectsLevel = (v: unknown): v is EffectsLevel => typeof v === 'string' && (EFFECT_LEVELS as readonly string[]).includes(v);

export interface MoveFx {
  path: Point[];
  color: BallColor;
  spawned: { point: Point; color: BallColor }[];
  cleared: { point: Point; color: BallColor }[];
  points: number;
  /** Consecutive clearing moves, this one included. */
  combo: number;
  /** Free cells left after the move. */
  free: number;
}

export const SPARK_COLORS: Record<BallColor, string> = {
  red: '#ff5252',
  green: '#4caf50',
  blue: '#448aff',
  cyan: '#18ffff',
  magenta: '#e040fb',
  yellow: '#ffeb3b',
  brown: '#a1663a',
};

const SIZE = 9;

/** How long the ball takes to get there: 45 ms a cell, never less than 150 ms or more than 460 ms. */
export const travelMs = (steps: number): number => Math.min(460, Math.max(150, steps * 45));

/** Particles for a burst: more for a bigger clear, capped for slow phones. */
export const sparkCount = (points: number, level: EffectsLevel): number => Math.min(level === 'full' ? 26 : 12, 8 + Math.round(points / 4));

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  max: number;
  size: number;
  color: string;
  gravity: number;
  ring?: boolean;
}

export class BoardFx {
  private canvas: HTMLCanvasElement | null = null;
  private particles: Particle[] = [];
  private frame = 0;
  private last = 0;
  private idleTimer = 0;
  private timers = new Set<number>();
  private level: EffectsLevel = 'full';

  private readonly root: HTMLElement;
  private readonly makeBall: (color: BallColor) => HTMLElement;

  constructor(root: HTMLElement, makeBall: (color: BallColor) => HTMLElement) {
    this.root = root;
    this.makeBall = makeBall;
  }

  setLevel(level: EffectsLevel): void {
    this.level = level;
    if (level === 'off') this.clear();
    else this.startIdle();
  }

  destroy(): void {
    this.clear();
    window.clearTimeout(this.idleTimer);
    this.canvas?.remove();
    this.canvas = null;
  }

  private clear(): void {
    this.particles = [];
    cancelAnimationFrame(this.frame);
    this.frame = 0;
    this.timers.forEach((t) => window.clearTimeout(t));
    this.timers.clear();
    this.root.querySelectorAll('.fx-ghost, .fx-pop').forEach((n) => n.remove());
    const ctx = this.canvas?.getContext('2d');
    if (ctx && this.canvas) ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
  }

  private later(ms: number, fn: () => void): void {
    const id = window.setTimeout(() => {
      this.timers.delete(id);
      fn();
    }, ms);
    this.timers.add(id);
  }

  private grid(): HTMLElement | null {
    return this.root.querySelector('.board-grid');
  }

  /** The centre of a cell and its width, in pixels of the board container. */
  private cell(p: Point): { x: number; y: number; size: number } | null {
    const grid = this.grid();
    const el = grid?.children[p.y * SIZE + p.x] as HTMLElement | undefined;
    if (!el) return null;
    const r = el.getBoundingClientRect();
    const o = this.root.getBoundingClientRect();
    return { x: r.left - o.left + r.width / 2, y: r.top - o.top + r.height / 2, size: r.width };
  }

  private ballAt(p: Point): HTMLElement | null {
    const grid = this.grid();
    return (grid?.children[p.y * SIZE + p.x]?.querySelector('.ball:not(.ball-preview)') as HTMLElement | null) ?? null;
  }

  // ---- canvas -------------------------------------------------------------------------------

  private ensureCanvas(): CanvasRenderingContext2D | null {
    if (!this.canvas) {
      const c = document.createElement('canvas');
      c.className = 'fx-canvas';
      c.setAttribute('aria-hidden', 'true');
      this.root.appendChild(c);
      this.canvas = c;
    }
    const c = this.canvas;
    const rect = this.root.getBoundingClientRect();
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = Math.round(rect.width * dpr);
    const h = Math.round(rect.height * dpr);
    if (c.width !== w || c.height !== h) {
      c.width = w;
      c.height = h;
    }
    const ctx = c.getContext('2d');
    ctx?.setTransform(dpr, 0, 0, dpr, 0, 0);
    return ctx;
  }

  private spawnParticles(list: Particle[]): void {
    this.particles.push(...list);
    if (this.frame === 0) {
      this.last = performance.now();
      this.frame = requestAnimationFrame(this.tick);
    }
  }

  private tick = (now: number): void => {
    const ctx = this.ensureCanvas();
    const dt = Math.min(0.05, (now - this.last) / 1000);
    this.last = now;
    if (!ctx || !this.canvas) return;
    ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    this.particles = this.particles.filter((p) => (p.life += dt) < p.max);
    for (const p of this.particles) {
      const k = 1 - p.life / p.max;
      p.vy += p.gravity * dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      ctx.globalAlpha = Math.max(0, k);
      if (p.ring) {
        ctx.strokeStyle = p.color;
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * (1 + (1 - k) * 1.4), 0, Math.PI * 2);
        ctx.stroke();
      } else {
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, Math.max(0.5, p.size * (0.4 + k * 0.6)), 0, Math.PI * 2);
        ctx.fill();
      }
    }
    ctx.globalAlpha = 1;
    this.frame = this.particles.length > 0 ? requestAnimationFrame(this.tick) : 0;
    if (this.frame === 0) ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
  };

  // ---- effects ------------------------------------------------------------------------------

  /** A ring from the touched cell. */
  ripple(p: Point, color = '#ffffff'): void {
    if (this.level === 'off') return;
    const c = this.cell(p);
    if (!c) return;
    this.spawnParticles([{ x: c.x, y: c.y, vx: 0, vy: 0, life: 0, max: 0.4, size: c.size * 0.25, color, gravity: 0, ring: true }]);
  }

  /** A refused move: the cell flinches. */
  deny(p: Point): void {
    if (this.level === 'off') return;
    const el = this.grid()?.children[p.y * SIZE + p.x] as HTMLElement | undefined;
    if (!el) return;
    el.classList.remove('cell-deny');
    void el.offsetWidth;
    el.classList.add('cell-deny');
    this.later(400, () => el.classList.remove('cell-deny'));
  }

  /** Plays everything for a finished move; call it right after the board was redrawn with the new position. */
  run(fx: MoveFx): void {
    if (this.level === 'off' || fx.path.length === 0) return;
    const full = this.level === 'full';
    const to = fx.path[fx.path.length - 1];
    const from = fx.path[0];
    const dur = travelMs(fx.path.length - 1);
    const clearedAtTarget = fx.cleared.some((c) => c.point.x === to.x && c.point.y === to.y);

    // The ball travels; its seat is empty until it arrives.
    const seat = this.ballAt(to);
    if (seat) seat.style.visibility = 'hidden';
    const start = this.cell(from);
    if (start) {
      const ghost = this.makeBall(fx.color);
      this.place(ghost, start, 'fx-ghost');
      const centres = fx.path.map((p) => this.cell(p) ?? start);
      const keyframes = centres.map((c, i) => ({ transform: `translate(${c.x - start.x}px, ${c.y - start.y}px) ${i === 0 || i === centres.length - 1 ? 'scale(1)' : 'scale(0.92)'}`, offset: i / (centres.length - 1 || 1) }));
      if (keyframes.length === 1) keyframes.push({ ...keyframes[0], offset: 1 });
      // Browsers without the Web Animations API (and test environments) just show the ball arriving.
      const anim = typeof ghost.animate === 'function' ? ghost.animate(keyframes, { duration: dur, easing: 'ease-in-out', fill: 'forwards' }) : null;
      if (full) {
        // A trail of fading dots along the way.
        centres.forEach((c, i) =>
          this.later((dur * i) / centres.length, () => this.spawnParticles([{ x: c.x, y: c.y, vx: 0, vy: 0, life: 0, max: 0.35, size: c.size * 0.12, color: SPARK_COLORS[fx.color], gravity: 0 }]))
        );
      }
      if (anim) void anim.finished.catch(() => undefined).finally(() => ghost.remove());
      else this.later(dur, () => ghost.remove());
    }

    // Balls about to burn stay where they were until the ball arrives.
    const ghosts: HTMLElement[] = [];
    for (const c of fx.cleared) {
      if (c.point.x === to.x && c.point.y === to.y) continue;
      const at = this.cell(c.point);
      if (!at) continue;
      const g = this.makeBall(c.color);
      this.place(g, at, 'fx-ghost');
      ghosts.push(g);
    }

    this.later(dur, () => {
      if (seat) {
        seat.style.visibility = '';
        if (!clearedAtTarget) this.squash(seat);
      }
      ghosts.forEach((g) => g.remove());
      this.burst(fx);
    });

    // New balls pop in once the mover has landed.
    for (const s of fx.spawned) {
      const b = this.ballAt(s.point);
      if (!b) continue;
      b.style.animationDelay = `${dur + 60}ms`;
      b.classList.add('ball-pop');
      this.later(dur + 700, () => {
        b.classList.remove('ball-pop');
        b.style.animationDelay = '';
      });
    }
  }

  private place(el: HTMLElement, c: { x: number; y: number; size: number }, className: string): void {
    el.classList.add(className);
    el.style.position = 'absolute';
    el.style.left = `${c.x}px`;
    el.style.top = `${c.y}px`;
    el.style.width = `${c.size * 0.82}px`;
    el.style.height = `${c.size * 0.82}px`;
    el.style.margin = `${-c.size * 0.41}px 0 0 ${-c.size * 0.41}px`;
    el.style.pointerEvents = 'none';
    this.root.appendChild(el);
  }

  private squash(el: HTMLElement): void {
    el.classList.remove('ball-land');
    void el.offsetWidth;
    el.classList.add('ball-land');
    this.later(380, () => el.classList.remove('ball-land'));
  }

  private burst(fx: MoveFx): void {
    if (fx.cleared.length === 0) return;
    const full = this.level === 'full';
    const per = sparkCount(fx.points, this.level);
    const list: Particle[] = [];
    let cx = 0;
    let cy = 0;
    for (const c of fx.cleared) {
      const at = this.cell(c.point);
      if (!at) continue;
      cx += at.x;
      cy += at.y;
      for (let i = 0; i < per / 2 + 3; i++) {
        const a = Math.random() * Math.PI * 2;
        const v = 40 + Math.random() * (full ? 150 : 90);
        list.push({ x: at.x, y: at.y, vx: Math.cos(a) * v, vy: Math.sin(a) * v - 30, life: 0, max: 0.45 + Math.random() * 0.35, size: 2 + Math.random() * 3, color: SPARK_COLORS[c.color], gravity: 260 });
      }
      list.push({ x: at.x, y: at.y, vx: 0, vy: 0, life: 0, max: 0.35, size: at.size * 0.3, color: '#ffffff', gravity: 0, ring: true });
    }
    this.spawnParticles(list);
    if (!full) return;

    const n = fx.cleared.length;
    cx /= n;
    cy /= n;
    this.floatText(`+${fx.points}`, cx, cy, fx.points >= 28 ? 'fx-pop-big' : '');
    if (fx.combo >= 2) this.later(140, () => this.floatText(`×${fx.combo}`, cx, cy - 34, 'fx-pop-combo'));
    if (fx.points >= 18 || fx.combo >= 2) this.shake(fx.points >= 42 ? 'big' : 'small');
  }

  private floatText(text: string, x: number, y: number, extra: string): void {
    const el = document.createElement('div');
    el.className = `fx-pop ${extra}`;
    el.textContent = text;
    el.setAttribute('aria-hidden', 'true');
    el.style.left = `${x}px`;
    el.style.top = `${y}px`;
    this.root.appendChild(el);
    this.later(1100, () => el.remove());
  }

  private shake(size: 'small' | 'big'): void {
    const grid = this.grid();
    if (!grid) return;
    const cls = size === 'big' ? 'board-shake-big' : 'board-shake';
    grid.classList.remove('board-shake', 'board-shake-big');
    void grid.offsetWidth;
    grid.classList.add(cls);
    this.later(500, () => grid.classList.remove(cls));
  }

  /** Rain of sparks over the whole board, for a record or a finished game. */
  celebrate(): void {
    if (this.level !== 'full') return;
    const o = this.root.getBoundingClientRect();
    const list: Particle[] = [];
    const palette = Object.values(SPARK_COLORS);
    for (let i = 0; i < 70; i++) {
      list.push({ x: Math.random() * o.width, y: -10, vx: (Math.random() - 0.5) * 60, vy: 40 + Math.random() * 120, life: Math.random() * 0.3, max: 1.4 + Math.random() * 0.8, size: 2 + Math.random() * 3, color: palette[i % palette.length], gravity: 120 });
    }
    this.spawnParticles(list);
  }

  /** A nervous glow while the board is nearly full. */
  setDanger(on: boolean): void {
    this.root.classList.toggle('board-danger', on && this.level !== 'off');
  }

  // ---- idle life ----------------------------------------------------------------------------

  private startIdle(): void {
    window.clearTimeout(this.idleTimer);
    if (this.level !== 'full') return;
    const schedule = () => {
      this.idleTimer = window.setTimeout(() => {
        this.wiggleOne();
        schedule();
      }, 3500 + Math.random() * 4500);
    };
    schedule();
  }

  private wiggleOne(): void {
    if (this.level !== 'full' || document.hidden) return;
    const balls = [...this.root.querySelectorAll<HTMLElement>('.board-grid .ball:not(.ball-preview):not(.selected-ball)')];
    if (balls.length === 0) return;
    const b = balls[Math.floor(Math.random() * balls.length)];
    b.classList.add('ball-wiggle');
    this.later(900, () => b.classList.remove('ball-wiggle'));
  }
}
