export const THEMES = [
  'modern',
  'light',
  'material',
  'neon',
  'synthwave',
  'ocean',
  'paper',
  'gameboy',
  'terminal',
  'contrast',
  'lines98',
  'lines98plus',
  'colorlines92',
] as const;
export type Theme = (typeof THEMES)[number];

export const DEFAULT_THEME: Theme = 'modern';

/** Names used before the four current themes existed. */
const LEGACY_THEMES: Record<string, Theme> = {
  classic98: 'lines98',
  retro92: 'colorlines92',
};

export function parseTheme(raw: string | null): Theme {
  if (raw === null) return DEFAULT_THEME;
  if ((THEMES as readonly string[]).includes(raw)) return raw as Theme;
  return LEGACY_THEMES[raw] ?? DEFAULT_THEME;
}

/** How a theme sounds: recorded samples (Lines 98), PC-speaker beeps (DOS) or soft synthesis (modern). */
export type SoundProfile = 'sampled' | 'pcspeaker' | 'modern';

export function soundProfile(theme: Theme): SoundProfile {
  if (theme === 'colorlines92') return 'pcspeaker';
  return theme === 'lines98' || theme === 'lines98plus' ? 'sampled' : 'modern';
}

/** Themes whose balls are the sprites of Lines 98. */
export function usesSprites(theme: Theme): boolean {
  return theme === 'lines98' || theme === 'lines98plus';
}

/** Themes drawn with the original game's own sprites and window chrome. */
export function isRetroTheme(theme: Theme): boolean {
  return theme === 'lines98' || theme === 'colorlines92';
}

/**
 * Small balls on the board where the next ones will appear: Lines 98 for Windows does exactly that, the
 * 1992 DOS original only shows the next colours in a panel, so the DOS look has the markers off.
 */
export function defaultSpawnPreview(theme: Theme): boolean {
  return theme !== 'colorlines92';
}

/** Every look has its own voice; the two retro ones have theirs in samples and PC-speaker beeps. */
export type VoiceId = 'soft' | 'bell' | 'marimba' | 'arcade' | 'saw' | 'glass' | 'wood' | 'chip' | 'teletype' | 'beep';

const VOICES: Record<Theme, VoiceId> = {
  modern: 'soft',
  light: 'bell',
  material: 'marimba',
  neon: 'arcade',
  synthwave: 'saw',
  ocean: 'glass',
  paper: 'wood',
  gameboy: 'chip',
  terminal: 'teletype',
  contrast: 'beep',
  lines98: 'soft',
  lines98plus: 'soft',
  colorlines92: 'soft',
};

export function soundVoice(theme: Theme): VoiceId {
  return VOICES[theme];
}
