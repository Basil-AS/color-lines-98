import { describe, expect, it } from 'vitest';
import { ALL_COLORS } from '../src/engine/models';
import {
  colorName,
  formatDate,
  messages,
  resolveLanguage,
  translate,
  translatePlural,
} from '../src/i18n';

describe('resolveLanguage', () => {
  it('honours an explicit preference over the browser', () => {
    expect(resolveLanguage('ru', ['en-US'])).toBe('ru');
    expect(resolveLanguage('en', ['ru-RU'])).toBe('en');
  });

  it('picks the first supported browser language when set to auto', () => {
    expect(resolveLanguage('auto', ['ru-RU', 'en-US'])).toBe('ru');
    expect(resolveLanguage('auto', ['en-GB', 'ru'])).toBe('en');
    expect(resolveLanguage('auto', ['de-DE', 'ru-RU'])).toBe('ru');
    expect(resolveLanguage('auto', ['RU'])).toBe('ru');
  });

  it('falls back to English when nothing is supported or the list is empty', () => {
    expect(resolveLanguage('auto', ['de-DE', 'fr'])).toBe('en');
    expect(resolveLanguage('auto', [])).toBe('en');
  });
});

describe('dictionaries', () => {
  const enKeys = Object.keys(messages.en).sort();
  const ruKeys = Object.keys(messages.ru).sort();
  const placeholders = (s: string) => [...s.matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort();

  it('have exactly the same keys', () => {
    expect(ruKeys).toEqual(enKeys);
  });

  it.each(enKeys)('%s is non-empty and keeps the same placeholders in both languages', (key) => {
    const en = messages.en[key as keyof typeof messages.en];
    const ru = messages.ru[key as keyof typeof messages.ru];
    expect(en.trim()).not.toBe('');
    expect(ru.trim()).not.toBe('');
    expect(placeholders(ru)).toEqual(placeholders(en));
  });

  it('contains Cyrillic text in the Russian dictionary', () => {
    expect(messages.ru['hud.score']).toMatch(/[а-я]/i);
  });
});

describe('translate', () => {
  it('interpolates parameters', () => {
    expect(translate('en', 'announce.score', { score: 120 })).toBe('Score 120');
    expect(translate('ru', 'announce.score', { score: 120 })).toBe('Счёт 120');
  });

  it('interpolates every occurrence and leaves unknown placeholders visible', () => {
    expect(translate('en', 'announce.score', {})).toContain('{score}');
  });
});

describe('translatePlural', () => {
  it('uses English one/other', () => {
    expect(translatePlural('en', 'plural.moves', 1)).toBe('1 move');
    expect(translatePlural('en', 'plural.moves', 5)).toBe('5 moves');
    expect(translatePlural('en', 'plural.moves', 0)).toBe('0 moves');
  });

  it('uses Russian one/few/many forms', () => {
    const f = (n: number) => translatePlural('ru', 'plural.moves', n);
    expect(f(1)).toBe('1 ход');
    expect(f(2)).toBe('2 хода');
    expect(f(4)).toBe('4 хода');
    expect(f(5)).toBe('5 ходов');
    expect(f(11)).toBe('11 ходов');
    expect(f(21)).toBe('21 ход');
    expect(f(22)).toBe('22 хода');
    expect(f(100)).toBe('100 ходов');
  });

  it('pluralises balls and games in Russian', () => {
    expect(translatePlural('ru', 'plural.balls', 5)).toBe('5 шаров');
    expect(translatePlural('ru', 'plural.balls', 2)).toBe('2 шара');
    expect(translatePlural('ru', 'plural.games', 3)).toBe('3 партии');
    expect(translatePlural('ru', 'plural.games', 12)).toBe('12 партий');
  });
});

describe('colorName', () => {
  it.each(ALL_COLORS)('names %s in both languages', (color) => {
    expect(colorName('en', color)).not.toBe('');
    expect(colorName('ru', color)).toMatch(/[а-я]/i);
  });
});

describe('formatDate', () => {
  it('formats using the language locale', () => {
    const t = Date.UTC(2026, 8, 29, 12, 0, 0);
    expect(formatDate('en', t)).toMatch(/2026/);
    expect(formatDate('ru', t)).toMatch(/2026/);
    expect(formatDate('ru', t)).not.toBe(formatDate('en', t));
  });
});
