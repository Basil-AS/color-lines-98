/**
 * Touch feedback through the Vibration API (Chrome and Firefox on Android; not iOS or desktop, where the call does nothing).
 * Patterns are in milliseconds: buzz, pause, buzz...
 */
export type HapticKind = 'select' | 'move' | 'blocked' | 'clear' | 'bigClear' | 'combo' | 'danger' | 'gameOver' | 'record' | 'hint' | 'undo';

export const HAPTIC_PATTERNS: Record<HapticKind, number[]> = {
  select: [14],
  move: [18],
  blocked: [40, 50, 40],
  clear: [22, 12, 22, 12, 26],
  bigClear: [20, 12, 24, 12, 28, 16, 50],
  combo: [16, 40, 16, 30, 18, 20, 20, 14, 24],
  danger: [60, 100, 60],
  gameOver: [80, 15, 80, 15, 80, 15, 70],
  record: [25, 30, 25, 30, 25, 45, 40, 10, 60, 10, 100],
  hint: [12, 45, 12],
  undo: [10, 10, 16],
};

let enabled = true;

export function setHapticsEnabled(on: boolean): void {
  enabled = on;
}

export function haptic(kind: HapticKind, scale = 1): void {
  if (!enabled || typeof navigator === 'undefined' || typeof navigator.vibrate !== 'function') return;
  try {
    const pattern = HAPTIC_PATTERNS[kind].map((v, i) => (i % 2 === 0 ? Math.round(v * scale) : v));
    navigator.vibrate(pattern);
  } catch {
    // Vibration may be blocked (no user gesture yet, a battery saver); the game goes on without it.
  }
}

export function hapticsSupported(): boolean {
  return typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function';
}
