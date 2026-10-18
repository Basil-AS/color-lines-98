import { ensureContrast, inkOn, isDark, mix, normalizeHex, withAlpha } from './colorlab';
import type { BallColor } from './engine/models';
import type { Theme } from './themes';
import { THEMES, knownTheme } from './themes';

/**
 * The look studio: every theme is a starting point, and on top of it the player can switch the depth of the balls,
 * their contrast, the markers on them, the colour set, the size and the style of the grid. Choices are kept per
 * theme; a choice that is not set follows the theme. The retro looks keep their original art and take none of it.
 */
export const VOLUMES = ['flat', 'soft', 'glossy'] as const;
export const CONTRASTS = ['normal', 'high', 'max'] as const;
export const SHAPE_SETS = ['off', 'shapes', 'digits'] as const;
export const PALETTES = ['classic', 'vivid', 'distinct', 'soft', 'custom'] as const;
export const BALL_SIZES = ['compact', 'normal', 'large'] as const;
export const GRIDS = ['plain', 'none', 'thin', 'bold', 'checker', 'checkerLines', 'dots'] as const;
export const CELL_SHAPES = ['square', 'rounded', 'round'] as const;

export type Volume = (typeof VOLUMES)[number];
export type Contrast = (typeof CONTRASTS)[number];
export type ShapeSet = (typeof SHAPE_SETS)[number];
export type PaletteId = (typeof PALETTES)[number];
export type BallSize = (typeof BALL_SIZES)[number];
export type GridStyle = (typeof GRIDS)[number];
export type CellShape = (typeof CELL_SHAPES)[number];

export interface Look {
  volume: Volume;
  contrast: Contrast;
  shapes: ShapeSet;
  palette: PaletteId;
  ballSize: BallSize;
  grid: GridStyle;
  cells: CellShape;
}
/** What the player has changed; a missing key follows the theme. */
export type LookOverrides = Partial<Look>;
export type LookKey = keyof Look;

export const LOOK_OPTIONS: { [K in LookKey]: readonly Look[K][] } = {
  volume: VOLUMES,
  contrast: CONTRASTS,
  shapes: SHAPE_SETS,
  palette: PALETTES,
  ballSize: BALL_SIZES,
  grid: GRIDS,
  cells: CELL_SHAPES,
};
export const LOOK_KEYS = Object.keys(LOOK_OPTIONS) as LookKey[];

/** The balls of the original games and of the DOS look are artwork, so the studio leaves them alone. */
export const isStudioTheme = (theme: Theme): boolean => theme !== 'lines98' && theme !== 'lines98plus' && theme !== 'colorlines92';

export const BALL_COLORS: readonly BallColor[] = ['red', 'green', 'blue', 'cyan', 'magenta', 'yellow', 'brown'];

/** Themes by collection, in the order of the picker. Every theme is in exactly one. */
export const THEME_FAMILIES: readonly { id: string; themes: readonly Theme[] }[] = [
  { id: 'modern', themes: ['modern', 'light', 'material', 'ocean', 'paper'] },
  { id: 'vivid', themes: ['clear', 'clearNight', 'spectrum', 'meadow', 'orchid'] },
  { id: 'neon', themes: ['neon', 'synthwave', 'retro98'] },
  { id: 'mono', themes: ['gameboy', 'terminal', 'contrast'] },
  { id: 'retro', themes: ['lines98', 'lines98plus', 'colorlines92'] },
  { id: 'own', themes: ['custom'] },
];

export interface BallTone {
  base: string;
  hi: string;
  lo: string;
}

function tone(base: string): BallTone {
  // Highlight and shade stay close to the colour itself, so a ball looks lit rather than whitened or dirtied.
  return { base, hi: mix(base, '#ffffff', 0.34), lo: mix(base, '#000000', 0.32) };
}

const toneSet = (colors: Record<BallColor, string>): Record<BallColor, BallTone> => ({
  red: tone(colors.red),
  green: tone(colors.green),
  blue: tone(colors.blue),
  cyan: tone(colors.cyan),
  magenta: tone(colors.magenta),
  yellow: tone(colors.yellow),
  brown: tone(colors.brown),
});

/**
 * Colour sets. Vivid keeps red and violet far apart; Distinct is built on the Okabe-Ito colour-blind safe palette
 * and spreads the balls over the whole lightness range, so they also differ in greyscale.
 */
export const PALETTE_COLORS: Record<Exclude<PaletteId, 'custom'>, Record<BallColor, string>> = {
  classic: { red: '#e53935', green: '#43a047', blue: '#1e88e5', cyan: '#00acc1', magenta: '#8e24aa', yellow: '#fdd835', brown: '#6d4c41' },
  vivid: { red: '#f0261b', green: '#2fbf4a', blue: '#2f7bff', cyan: '#00d8f0', magenta: '#8b3dff', yellow: '#ffd60a', brown: '#a9703a' },
  distinct: { red: '#d55e00', green: '#009e73', blue: '#0072b2', cyan: '#56b4e9', magenta: '#cc79a7', yellow: '#f0e442', brown: '#4a2f1b' },
  soft: { red: '#e0675f', green: '#6fb97a', blue: '#6f9de0', cyan: '#6cc7d6', magenta: '#a98bd6', yellow: '#eed26a', brown: '#a98467' },
};

export function paletteTones(palette: PaletteId, custom: Record<BallColor, string>): Record<BallColor, BallTone> {
  return toneSet(palette === 'custom' ? custom : PALETTE_COLORS[palette]);
}

/** The glyph on a ball: the shapes of the high-contrast looks, or a digit for each colour. */
export const SHAPE_GLYPHS: Record<BallColor, string> = { red: '●', green: '▲', blue: '■', cyan: '◆', magenta: '★', yellow: '✚', brown: '♥' };
export const DIGIT_GLYPHS: Record<BallColor, string> = { red: '1', green: '2', blue: '3', cyan: '4', magenta: '5', yellow: '6', brown: '7' };

export interface CustomTheme {
  dark: boolean;
  page: string;
  board: string;
  cell: string;
  accent: string;
  balls: Record<BallColor, string>;
}

/** The balls a fresh custom look starts with: Vivid, with a yellow dark enough to stand out on a light cell. */
const START_BALLS: Record<BallColor, string> = { ...PALETTE_COLORS.vivid, yellow: '#f2b705' };

export const DEFAULT_CUSTOM: CustomTheme = {
  dark: false,
  page: '#e8eef2',
  board: '#3f5566',
  cell: '#f2f6f8',
  accent: '#2a7ab0',
  balls: { ...START_BALLS },
};

export function sanitizeCustom(raw: unknown): CustomTheme {
  const out: CustomTheme = { ...DEFAULT_CUSTOM, balls: { ...DEFAULT_CUSTOM.balls } };
  if (typeof raw !== 'object' || raw === null) return out;
  const r = raw as Record<string, unknown>;
  if (typeof r.dark === 'boolean') out.dark = r.dark;
  out.page = normalizeHex(r.page, out.page);
  out.board = normalizeHex(r.board, out.board);
  out.cell = normalizeHex(r.cell, out.cell);
  out.accent = normalizeHex(r.accent, out.accent);
  const balls = typeof r.balls === 'object' && r.balls !== null ? (r.balls as Record<string, unknown>) : {};
  for (const color of BALL_COLORS) out.balls[color] = normalizeHex(balls[color], out.balls[color]);
  return out;
}

/** A whole starting look for the custom theme: a light or a dark surface with matching cells. */
export function customPreset(dark: boolean): CustomTheme {
  return dark
    ? { dark: true, page: '#14181d', board: '#0b0e11', cell: '#262d35', accent: '#4fd1a5', balls: { ...PALETTE_COLORS.vivid } }
    : { ...DEFAULT_CUSTOM, balls: { ...START_BALLS } };
}

/** Every custom property the interface reads, worked out from the handful of colours the builder asks for. */
export function customVariables(custom: CustomTheme): Record<string, string> {
  const dark = custom.dark;
  const toward = dark ? '#ffffff' : '#000000';
  const away = dark ? '#000000' : '#ffffff';
  const panel = mix(custom.page, away, dark ? 0.04 : 0.55);
  const text = dark ? '#eef1f4' : '#1d242b';
  const accent = ensureContrast(custom.accent, panel, 3);
  const stat = mix(custom.page, toward, dark ? 0.12 : 0.06);
  const ctrl = mix(panel, toward, dark ? 0.1 : 0.06);
  const card = mix(panel, toward, 0.04);
  // Muted text sits on the panel, the stat boxes, the controls and the cards: it must read on every one of them.
  const muted = [panel, stat, ctrl, card].reduce((c, bg) => ensureContrast(c, bg, 4.5), mix(text, custom.page, 0.35));
  return {
    '--page-bg': custom.page,
    '--panel-bg': panel,
    '--panel-border': mix(panel, toward, 0.16),
    '--panel-shadow': dark ? '0 16px 40px rgba(0, 0, 0, 0.5)' : `0 12px 30px ${withAlpha(custom.board, 0.18)}`,
    '--text-main': text,
    '--text-muted': muted,
    '--accent': accent,
    '--accent-glow': withAlpha(accent, 0.3),
    '--on-accent': inkOn(accent),
    '--board-bg': custom.board,
    '--cell-bg': custom.cell,
    '--cell-hover': mix(custom.cell, toward, 0.08),
    '--cell-reachable-dot': accent,
    '--stat-bg': stat,
    '--stat-label': muted,
    '--stat-value': text,
    '--title-color': text,
    '--ctrl-bg': ctrl,
    '--ctrl-hover': mix(ctrl, toward, 0.08),
    '--input-bg': ctrl,
    '--card-bg': card,
    '--danger': dark ? '#ff6b6b' : '#b4232c',
  };
}

export const customColorScheme = (custom: CustomTheme): 'dark' | 'light' => (custom.dark ? 'dark' : 'light');
export const customPageColor = (custom: CustomTheme): string => custom.page;
export const surfaceIsDark = (custom: CustomTheme): boolean => isDark(custom.page);

export type LookStore = Partial<Record<Theme, LookOverrides>>;

function sanitizeOverrides(raw: unknown): LookOverrides {
  const out: LookOverrides = {};
  if (typeof raw !== 'object' || raw === null) return out;
  const r = raw as Record<string, unknown>;
  for (const key of LOOK_KEYS) {
    const options = LOOK_OPTIONS[key] as readonly unknown[];
    if (options.includes(r[key])) (out as Record<string, unknown>)[key] = r[key];
  }
  return out;
}

export function sanitizeLookStore(raw: unknown): LookStore {
  const out: LookStore = {};
  if (typeof raw !== 'object' || raw === null) return out;
  const r = raw as Record<string, unknown>;
  for (const theme of THEMES) {
    const overrides = sanitizeOverrides(r[theme]);
    if (Object.keys(overrides).length > 0) out[theme] = overrides;
  }
  // Choices kept for the meadow looks that were folded into Meadow: the relief look is the soft depth, and the
  // choices already made on Meadow itself win.
  for (const legacy of ['meadowClear', 'meadowRelief'] as const) {
    if (r[legacy] === undefined) continue;
    const merged: LookOverrides = { ...(legacy === 'meadowRelief' ? { volume: 'soft' as const } : {}), ...sanitizeOverrides(r[legacy]), ...(out.meadow ?? {}) };
    if (Object.keys(merged).length > 0) out.meadow = merged;
  }
  return out;
}

/** What a theme looks like before the player changes anything. */
export function themeDefaults(theme: Theme): Look {
  const base: Look = { volume: 'soft', contrast: 'normal', shapes: 'off', palette: 'vivid', ballSize: 'normal', grid: 'plain', cells: 'rounded' };
  switch (theme) {
    case 'modern': case 'material': case 'ocean': case 'paper': case 'light':
      return { ...base, volume: 'glossy', palette: 'classic' };
    case 'neon': case 'synthwave':
      return { ...base, volume: 'glossy', palette: 'vivid' };
    case 'contrast': case 'clear': case 'clearNight': case 'retro98': case 'spectrum': case 'meadow': case 'orchid':
      return { ...base, volume: 'flat', contrast: 'high', cells: 'square', grid: 'plain' };
    case 'gameboy': case 'terminal':
      return { ...base, volume: 'flat', contrast: 'high', shapes: 'shapes', cells: 'square' };
    default:
      return base;
  }
}

/** The look a theme actually has: its defaults with the player's choices on top. */
export function effectiveLook(theme: Theme, overrides: LookOverrides | undefined): Look {
  return { ...themeDefaults(theme), ...(overrides ?? {}) };
}

/**
 * Attributes for the page. A theme is left to its own stylesheet until the player changes the depth or the colours
 * (`data-studio`); the other choices are small overlays and apply on their own.
 */
export function lookAttributes(theme: Theme, overrides: LookOverrides | undefined): Record<string, string> {
  if (!isStudioTheme(theme)) return {};
  const o = overrides ?? {};
  const look = effectiveLook(theme, o);
  const attrs: Record<string, string> = {};
  const studio = theme === 'custom' || o.volume !== undefined || o.palette !== undefined;
  if (studio) {
    attrs['data-studio'] = '1';
    attrs['data-volume'] = look.volume;
  }
  if (o.contrast !== undefined || theme === 'custom') attrs['data-contrast'] = look.contrast;
  if (o.shapes !== undefined) attrs['data-shapes'] = look.shapes;
  if (o.ballSize !== undefined) attrs['data-ballsize'] = look.ballSize;
  if (o.grid !== undefined) attrs['data-grid'] = look.grid;
  if (o.cells !== undefined) attrs['data-cells'] = look.cells;
  return attrs;
}

/** The ball colours as custom properties (`--bs-red`, `--bs-red-hi`, ...), only when the studio draws the balls. */
export function ballVariables(theme: Theme, overrides: LookOverrides | undefined, custom: CustomTheme): Record<string, string> {
  if (!isStudioTheme(theme)) return {};
  const look = effectiveLook(theme, overrides);
  const studio = theme === 'custom' || overrides?.volume !== undefined || overrides?.palette !== undefined;
  if (!studio) return {};
  const palette = theme === 'custom' && overrides?.palette === undefined ? 'custom' : look.palette;
  const tones = paletteTones(palette, custom.balls);
  const out: Record<string, string> = {};
  for (const color of BALL_COLORS) {
    out[`--bs-${color}`] = tones[color].base;
    out[`--bs-${color}-hi`] = tones[color].hi;
    out[`--bs-${color}-lo`] = tones[color].lo;
    const ink = inkOn(tones[color].base);
    out[`--bs-${color}-ink`] = ink;
    out[`--bs-${color}-halo`] = ink === '#000000' ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)';
  }
  return out;
}

/** A short text that carries a look from one device to another and back. */
export interface LookShare {
  theme: Theme;
  look: LookOverrides;
  custom?: CustomTheme;
}

export function encodeLook(share: LookShare): string {
  const body = JSON.stringify({ v: 1, theme: share.theme, look: share.look, custom: share.theme === 'custom' ? share.custom : undefined });
  return `CL3:${btoa(unescape(encodeURIComponent(body)))}`;
}

export function decodeLook(code: string): LookShare | null {
  const text = code.trim();
  if (!text.startsWith('CL3:') || text.length > 4000) return null;
  try {
    const raw = JSON.parse(decodeURIComponent(escape(atob(text.slice(4))))) as Record<string, unknown>;
    const theme = knownTheme(raw.theme);
    if (theme === null) return null;
    const look = sanitizeOverrides(raw.look);
    // A code made on the relief meadow keeps its soft depth.
    if (raw.theme === 'meadowRelief' && look.volume === undefined) look.volume = 'soft';
    return { theme, look, custom: theme === 'custom' ? sanitizeCustom(raw.custom) : undefined };
  } catch {
    return null;
  }
}
