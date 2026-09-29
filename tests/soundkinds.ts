import type { SoundKind } from '../src/pcspeaker';

/** Every event the game can play; the compiler checks that none is missing. */
const table: Record<SoundKind, true> = {
  select: true,
  jump: true,
  eat: true,
  lose: true,
  win: true,
  click: true,
  blocked: true,
  start: true,
  record: true,
  crown: true,
  levelUp: true,
  achievement: true,
};

export const ALL_SOUND_KINDS = Object.keys(table) as SoundKind[];
