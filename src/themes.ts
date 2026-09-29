export const THEMES = ['modern', 'light', 'lines98', 'colorlines92'] as const;
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

/** How a theme sounds: recorded samples, or the beeps of a PC speaker. */
export type SoundProfile = 'sampled' | 'pcspeaker';

export function soundProfile(theme: Theme): SoundProfile {
  return theme === 'colorlines92' ? 'pcspeaker' : 'sampled';
}

/** Themes drawn with the original game's own sprites and window chrome. */
export function isRetroTheme(theme: Theme): boolean {
  return theme === 'lines98' || theme === 'colorlines92';
}
