import { GameEngine } from './engine/gameengine';
import { MODES } from './engine/modes';
import type { ModeId } from './engine/modes';
import { ALL_COLORS } from './engine/models';
import type { BallColor } from './engine/models';
import { hashString } from './engine/rng';
import type { Rng } from './engine/rng';

/** Easy mode drops two colours, so lines are much easier to make. */
export const EASY_COLORS: BallColor[] = ['red', 'green', 'blue', 'yellow', 'magenta'];

export function dailySeed(dayKey: string): number {
  return hashString(`color-lines-daily:${dayKey}`);
}

export function todayKey(now = Date.now()): string {
  const d = new Date(now);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function createEngine(mode: ModeId, opts: { dayKey?: string; rng?: Rng } = {}): GameEngine {
  const def = MODES[mode];
  return new GameEngine(9, 3, 5, 'gamos', opts.rng ?? Math.random, {
    mode,
    colors: mode === 'easy' ? EASY_COLORS : ALL_COLORS,
    seed: def.seeded ? dailySeed(opts.dayKey ?? todayKey()) : undefined,
  });
}

/** Time left in a timed mode, or null when the mode has no clock. */
export function remainingMs(mode: ModeId, engine: GameEngine): number | null {
  const limit = MODES[mode].timeLimitMs;
  return limit === null ? null : Math.max(0, limit - engine.playMs);
}
