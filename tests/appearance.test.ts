// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import {
  BALL_COLORS,
  DEFAULT_CUSTOM,
  LOOK_KEYS,
  LOOK_OPTIONS,
  PALETTE_COLORS,
  THEME_FAMILIES,
  ballVariables,
  customPreset,
  customVariables,
  decodeLook,
  effectiveLook,
  encodeLook,
  isStudioTheme,
  lookAttributes,
  paletteTones,
  sanitizeCustom,
  sanitizeLookStore,
  themeDefaults,
} from '../src/appearance';
import { applyLook } from '../src/applyLook';
import { contrastRatio, luminance } from '../src/colorlab';
import { THEMES } from '../src/themes';
import { ballWarnings } from '../src/components/LookBuilder';
import { parseBackup } from '../src/backup';

describe('theme families', () => {
  it('list every theme exactly once', () => {
    const all = THEME_FAMILIES.flatMap((f) => [...f.themes]);
    expect([...all].sort()).toEqual([...THEMES].sort());
    expect(new Set(all).size).toBe(all.length);
  });
});

describe('look overrides', () => {
  it('follow the theme until something is chosen', () => {
    expect(lookAttributes('modern', undefined)).toEqual({});
    expect(lookAttributes('modern', {})).toEqual({});
  });

  it('turn on the studio only for depth and colour choices', () => {
    expect(lookAttributes('modern', { volume: 'flat' })).toMatchObject({ 'data-studio': '1', 'data-volume': 'flat' });
    expect(lookAttributes('modern', { palette: 'distinct' })['data-studio']).toBe('1');
    const overlays = lookAttributes('modern', { contrast: 'max', shapes: 'digits', ballSize: 'large', grid: 'dots', cells: 'round' });
    expect(overlays).toEqual({ 'data-contrast': 'max', 'data-shapes': 'digits', 'data-ballsize': 'large', 'data-grid': 'dots', 'data-cells': 'round' });
  });

  it('leave the original games alone', () => {
    for (const theme of ['lines98', 'lines98plus', 'colorlines92'] as const) {
      expect(isStudioTheme(theme)).toBe(false);
      expect(lookAttributes(theme, { volume: 'flat', grid: 'none' })).toEqual({});
      expect(ballVariables(theme, { palette: 'vivid' }, DEFAULT_CUSTOM)).toEqual({});
    }
  });

  it('merge a choice over the theme defaults', () => {
    expect(effectiveLook('modern', undefined)).toEqual(themeDefaults('modern'));
    expect(effectiveLook('modern', { volume: 'flat' }).volume).toBe('flat');
    expect(effectiveLook('gameboy', undefined).shapes).toBe('shapes');
  });

  it('drops anything that is not a known option', () => {
    const store = sanitizeLookStore({ modern: { volume: 'glossy', contrast: 'insane', nope: 1 }, bogus: { volume: 'flat' }, neon: 'x', light: {} });
    expect(store).toEqual({ modern: { volume: 'glossy' } });
    expect(sanitizeLookStore(null)).toEqual({});
    expect(sanitizeLookStore([])).toEqual({});
  });

  it('have at least two options for every key', () => {
    for (const key of LOOK_KEYS) expect(LOOK_OPTIONS[key].length).toBeGreaterThanOrEqual(2);
  });
});

describe('colour sets', () => {
  it('keep red and violet far apart in vivid and distinct', () => {
    for (const set of ['vivid', 'distinct'] as const) {
      const { red, magenta } = PALETTE_COLORS[set];
      expect(contrastRatio(red, magenta)).toBeGreaterThan(1.05);
    }
    expect(PALETTE_COLORS.vivid.red).toBe('#f0261b');
    expect(PALETTE_COLORS.vivid.magenta).toBe('#8b3dff');
  });

  it('spread the distinct set over lightness so it also differs in greyscale', () => {
    const lum = BALL_COLORS.map((c) => luminance(PALETTE_COLORS.distinct[c])).sort((a, b) => a - b);
    for (let i = 1; i < lum.length; i++) expect(lum[i] - lum[i - 1]).toBeGreaterThan(0.01);
  });

  it('build a tone for every colour', () => {
    const tones = paletteTones('vivid', DEFAULT_CUSTOM.balls);
    for (const c of BALL_COLORS) {
      expect(tones[c].base).toMatch(/^#[0-9a-f]{6}$/);
      expect(luminance(tones[c].hi)).toBeGreaterThan(luminance(tones[c].lo));
    }
  });

  it('write the colours as custom properties only when the studio draws the balls', () => {
    expect(ballVariables('modern', undefined, DEFAULT_CUSTOM)).toEqual({});
    const vars = ballVariables('modern', { palette: 'distinct' }, DEFAULT_CUSTOM);
    expect(vars['--bs-red']).toBe('#d55e00');
    expect(vars['--bs-yellow-ink']).toBe('#000000');
    expect(vars['--bs-blue-ink']).toBe('#ffffff');
    expect(Object.keys(vars)).toHaveLength(BALL_COLORS.length * 5);
  });
});

describe('custom theme', () => {
  it('falls back to defaults for anything invalid', () => {
    expect(sanitizeCustom(null)).toEqual(DEFAULT_CUSTOM);
    const c = sanitizeCustom({ dark: true, page: 'zzz', accent: '#ABC', balls: { red: '#123456', green: 7 } });
    expect(c.dark).toBe(true);
    expect(c.page).toBe(DEFAULT_CUSTOM.page);
    expect(c.accent).toBe('#aabbcc');
    expect(c.balls.red).toBe('#123456');
    expect(c.balls.green).toBe(DEFAULT_CUSTOM.balls.green);
  });

  it('derives readable text and accent for light and dark surfaces', () => {
    for (const dark of [false, true]) {
      const v = customVariables(customPreset(dark));
      expect(contrastRatio(v['--text-main'], v['--panel-bg'])).toBeGreaterThan(7);
      expect(contrastRatio(v['--text-muted'], v['--panel-bg'])).toBeGreaterThanOrEqual(4.5);
      expect(contrastRatio(v['--accent'], v['--panel-bg'])).toBeGreaterThanOrEqual(3);
      expect(contrastRatio(v['--on-accent'], v['--accent'])).toBeGreaterThan(4);
    }
  });

  it('always draws its balls through the studio', () => {
    expect(lookAttributes('custom', undefined)).toMatchObject({ 'data-studio': '1', 'data-volume': 'soft', 'data-contrast': 'normal' });
    expect(ballVariables('custom', undefined, { ...DEFAULT_CUSTOM, balls: { ...DEFAULT_CUSTOM.balls, red: '#010203' } })['--bs-red']).toBe('#010203');
  });

  it('warns about similar and faint colours', () => {
    const same = { ...DEFAULT_CUSTOM.balls, magenta: '#f0281c' };
    expect(ballWarnings(same, '#ffffff').similar.some(([a, b]) => a === 'red' && b === 'magenta')).toBe(true);
    expect(ballWarnings(DEFAULT_CUSTOM.balls, DEFAULT_CUSTOM.cell)).toEqual({ similar: [], faint: [] });
    const dark = customPreset(true);
    expect(ballWarnings(dark.balls, dark.cell)).toEqual({ similar: [], faint: [] });
    expect(ballWarnings({ ...DEFAULT_CUSTOM.balls, yellow: '#f4f4f4' }, '#f2f6f8').faint).toContain('yellow');
  });
});

describe('look codes', () => {
  it('survive a round trip', () => {
    const code = encodeLook({ theme: 'custom', look: { volume: 'glossy', grid: 'bold' }, custom: customPreset(true) });
    expect(code.startsWith('CL3:')).toBe(true);
    expect(decodeLook(code)).toEqual({ theme: 'custom', look: { volume: 'glossy', grid: 'bold' }, custom: customPreset(true) });
  });

  it('carry the custom colours only for the custom theme', () => {
    const decoded = decodeLook(encodeLook({ theme: 'modern', look: { contrast: 'high' }, custom: customPreset(true) }));
    expect(decoded).toEqual({ theme: 'modern', look: { contrast: 'high' }, custom: undefined });
  });

  it('refuse text that is not a look code', () => {
    expect(decodeLook('')).toBeNull();
    expect(decodeLook('hello')).toBeNull();
    expect(decodeLook('CL3:!!!')).toBeNull();
    expect(decodeLook(`CL3:${btoa('{"theme":"nope"}')}`)).toBeNull();
    expect(decodeLook(`CL3:${'A'.repeat(5000)}`)).toBeNull();
  });

  it('clean a hostile code before it reaches the store', () => {
    const body = btoa(JSON.stringify({ theme: 'modern', look: { volume: 'flat', grid: '<script>' }, custom: 5 }));
    expect(decodeLook(`CL3:${body}`)).toEqual({ theme: 'modern', look: { volume: 'flat' }, custom: undefined });
  });
});

describe('applying a look to the page', () => {
  it('sets attributes and variables, and clears them again', () => {
    const el = document.createElement('div');
    applyLook('modern', { volume: 'flat', grid: 'thin' }, DEFAULT_CUSTOM, el);
    expect(el.getAttribute('data-studio')).toBe('1');
    expect(el.getAttribute('data-grid')).toBe('thin');
    expect(el.style.getPropertyValue('--bs-red')).toBe('#e53935');
    applyLook('light', undefined, DEFAULT_CUSTOM, el);
    expect(el.hasAttribute('data-studio')).toBe(false);
    expect(el.hasAttribute('data-grid')).toBe(false);
    expect(el.style.getPropertyValue('--bs-red')).toBe('');
  });

  it('paints the page for the custom theme', () => {
    const el = document.createElement('div');
    applyLook('custom', undefined, customPreset(true), el);
    expect(el.style.getPropertyValue('--page-bg')).toBe('#14181d');
    expect(el.style.colorScheme).toBe('dark');
    applyLook('modern', undefined, customPreset(true), el);
    expect(el.style.getPropertyValue('--page-bg')).toBe('');
  });
});

describe('backups', () => {
  it('keep the looks and the custom colours', () => {
    const text = JSON.stringify({
      format: 'color-lines-backup',
      version: 2,
      exportedAt: 1,
      app: { platform: 'web', version: '3.0.0' },
      history: [],
      ledger: {},
      progress: {},
      hall: [],
      settings: { theme: 'custom', looks: { modern: { volume: 'flat', evil: 'x' } }, custom: { dark: true, page: '#101010' } },
    });
    const parsed = parseBackup(text);
    expect(parsed.ok).toBe(true);
    if (parsed.ok) {
      expect(parsed.backup.settings.theme).toBe('custom');
      expect(parsed.backup.settings.looks).toEqual({ modern: { volume: 'flat' } });
      expect(parsed.backup.settings.custom?.page).toBe('#101010');
    }
  });
});
