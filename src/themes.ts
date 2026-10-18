export const THEMES = [
  'modern',
  'material',
  'neon',
  'synthwave',
  'ocean',
  'paper',
  'gameboy',
  'terminal',
  'contrast',
  'clear',
  'retro98',
  'spectrum',
  'meadow',
  'orchid',
  'custom',
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
  clearnight: 'clear',
  // Light and night are no longer looks of their own: the day/night switch picks them.
  light: 'modern',
  clearNight: 'clear',
  // The three meadow looks became one: the clearer colours are the default, the relief is a choice in the studio.
  meadowClear: 'meadow',
  meadowRelief: 'meadow',
};

/** A saved theme name, current or from an older version; null when it is neither. */
export function knownTheme(raw: unknown): Theme | null {
  if (typeof raw !== 'string') return null;
  if ((THEMES as readonly string[]).includes(raw)) return raw as Theme;
  return LEGACY_THEMES[raw] ?? null;
}

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
export type VoiceId = 'soft' | 'marimba' | 'arcade' | 'saw' | 'glass' | 'wood' | 'chip' | 'teletype' | 'beep' | 'clarity' | 'retro' | 'spectrum' | 'meadow' | 'orchid' | 'studio';

const VOICES: Record<Theme, VoiceId> = {
  modern: 'soft',
  material: 'marimba',
  neon: 'arcade',
  synthwave: 'saw',
  ocean: 'glass',
  paper: 'wood',
  gameboy: 'chip',
  terminal: 'teletype',
  contrast: 'beep',
  clear: 'clarity',
  retro98: 'retro',
  spectrum: 'spectrum',
  meadow: 'meadow',
  orchid: 'orchid',
  custom: 'studio',
  lines98: 'soft',
  lines98plus: 'soft',
  colorlines92: 'soft',
};

export function soundVoice(theme: Theme): VoiceId {
  return VOICES[theme];
}

/** The ball style class a theme draws its balls with; the look studio can still restyle them. */
export function ballThemeClass(theme: Theme): string {
  if (theme === 'lines98' || theme === 'lines98plus') return '';
  if (theme === 'colorlines92') return 'ball-dos';
  if (theme === 'neon') return 'ball-neon';
  if (theme === 'contrast' || theme === 'clear' || theme === 'retro98' || theme === 'spectrum' || theme === 'meadow' || theme === 'orchid') return 'ball-contrast';
  if (theme === 'gameboy' || theme === 'terminal') return 'ball-contrast ball-symbols';
  if (theme === 'synthwave') return 'ball-neon ball-synth';
  return 'ball-modern';
}

/** Day or night: a fixed choice, or follow the device. */
export const COLOR_SCHEMES = ['auto', 'day', 'night'] as const;
export type ColorScheme = (typeof COLOR_SCHEMES)[number];
export type Shade = 'day' | 'night';

export function parseColorScheme(raw: unknown): ColorScheme | null {
  return (COLOR_SCHEMES as readonly unknown[]).includes(raw) ? (raw as ColorScheme) : null;
}

/** What the saved scheme meant before there was one: the old light and night looks were a fixed choice. */
export function schemeOfLegacyTheme(raw: unknown): ColorScheme | null {
  if (raw === 'light') return 'day';
  if (raw === 'clearNight' || raw === 'clearnight') return 'night';
  return null;
}

export function resolveShade(scheme: ColorScheme, systemDark: boolean): Shade {
  if (scheme === 'auto') return systemDark ? 'night' : 'day';
  return scheme;
}

/** Looks whose colours are the look itself (a hand-set palette, a monochrome screen, an original game): day and night do not change them. */
const FIXED_SHADE: readonly Theme[] = ['custom', 'gameboy', 'terminal', 'lines98', 'colorlines92'];

export function followsShade(theme: Theme): boolean {
  return !FIXED_SHADE.includes(theme);
}

/** The shade a look is drawn in when nothing is switched. */
const NIGHT_LOOKS: readonly Theme[] = ['modern', 'neon', 'synthwave', 'ocean', 'contrast', 'retro98'];

export function nativeShade(theme: Theme): Shade {
  return NIGHT_LOOKS.includes(theme) ? 'night' : 'day';
}

/**
 * The stylesheet class for a look in a shade, and whether a generic day/night recolouring has to be laid on it:
 * Modern and Clear have a real sheet for the other shade, the rest are recoloured from their accent.
 */
export function shadeStyle(theme: Theme, shade: Shade): { css: string; flip: Shade | null } {
  if (!followsShade(theme)) return { css: theme, flip: null };
  if (theme === 'modern') return { css: shade === 'day' ? 'light' : 'modern', flip: null };
  if (theme === 'clear') return { css: shade === 'night' ? 'clearNight' : 'clear', flip: null };
  return { css: theme, flip: shade === nativeShade(theme) ? null : shade };
}
