/**
 * Touch feedback through the Vibration API (Chrome and Firefox on Android; not iOS or desktop, where the call does nothing).
 * Patterns are in milliseconds: buzz, pause, buzz...
 */
export type HapticKind = 'select' | 'move' | 'blocked' | 'clear' | 'bigClear' | 'combo' | 'danger' | 'gameOver' | 'record' | 'hint' | 'undo';

export const HAPTIC_PATTERNS: Record<HapticKind, number[]> = {
  select: [6],
  move: [10],
  blocked: [28, 40, 28],
  clear: [16, 30, 22],
  bigClear: [26, 36, 26, 36, 60],
  combo: [18, 24, 18, 24, 18, 24, 50],
  danger: [40, 90, 40],
  gameOver: [120, 70, 220],
  record: [40, 40, 40, 40, 40, 40, 220],
  hint: [8, 40, 8],
  undo: [12],
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
