export const MODE_IDS = ['classic', 'easy', 'blitz', 'daily'] as const;
export type ModeId = (typeof MODE_IDS)[number];

export interface ModeDef {
  /** How many of the seven colours are in play. */
  colors: number;
  /** Active play time after which the game ends, or null. */
  timeLimitMs: number | null;
  /** Everyone gets the same game on the same day. */
  seeded: boolean;
}

export const MODES: Record<ModeId, ModeDef> = {
  classic: { colors: 7, timeLimitMs: null, seeded: false },
  easy: { colors: 5, timeLimitMs: null, seeded: false },
  blitz: { colors: 7, timeLimitMs: 180_000, seeded: false },
  daily: { colors: 7, timeLimitMs: null, seeded: true },
};

export function isModeId(value: unknown): value is ModeId {
  return typeof value === 'string' && (MODE_IDS as readonly string[]).includes(value);
}
