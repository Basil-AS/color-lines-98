import { describe, expect, it } from 'vitest';
import { contrastRatio, ensureContrast, inkOn, isDark, isHex, luminance, mix, normalizeHex, toHex, toRgb, withAlpha } from '../src/colorlab';

describe('colorlab', () => {
  it('reads and writes hex colours', () => {
    expect(toRgb('#ff8000')).toEqual([255, 128, 0]);
    expect(toHex([255, 128, 0])).toBe('#ff8000');
    expect(toHex([300, -4, 0.4])).toBe('#ff0000');
  });

  it('normalises short, upper-case and invalid values', () => {
    expect(normalizeHex('#ABC', '#000000')).toBe('#aabbcc');
    expect(normalizeHex('FFAA00', '#000000')).toBe('#ffaa00');
    expect(normalizeHex('nope', '#123456')).toBe('#123456');
    expect(normalizeHex(42, '#123456')).toBe('#123456');
    expect(isHex('#abc')).toBe(true);
    expect(isHex('red')).toBe(false);
  });

  it('mixes between two colours', () => {
    expect(mix('#000000', '#ffffff', 0.5)).toBe('#808080');
    expect(mix('#102030', '#ffffff', 0)).toBe('#102030');
    expect(mix('#102030', '#ffffff', 2)).toBe('#ffffff');
  });

  it('computes WCAG luminance and contrast', () => {
    expect(luminance('#000000')).toBe(0);
    expect(luminance('#ffffff')).toBeCloseTo(1, 5);
    expect(contrastRatio('#000000', '#ffffff')).toBeCloseTo(21, 0);
    expect(contrastRatio('#777777', '#777777')).toBe(1);
    expect(isDark('#101418')).toBe(true);
    expect(isDark('#f0f0f0')).toBe(false);
  });

  it('picks readable ink', () => {
    expect(inkOn('#ffd60a')).toBe('#000000');
    expect(inkOn('#0072b2')).toBe('#ffffff');
  });

  it('moves a colour until it reaches the contrast it needs', () => {
    const fixed = ensureContrast('#8a8a8a', '#ffffff', 4.5);
    expect(contrastRatio(fixed, '#ffffff')).toBeGreaterThanOrEqual(4.5);
    expect(ensureContrast('#000000', '#ffffff', 4.5)).toBe('#000000');
    const onDark = ensureContrast('#303030', '#101010', 4.5);
    expect(contrastRatio(onDark, '#101010')).toBeGreaterThanOrEqual(4.5);
  });

  it('writes an rgba string', () => {
    expect(withAlpha('#ff0000', 0.5)).toBe('rgba(255, 0, 0, 0.5)');
    expect(withAlpha('#ff0000', 4)).toBe('rgba(255, 0, 0, 1)');
  });
});
