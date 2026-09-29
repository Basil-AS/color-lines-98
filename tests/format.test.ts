import { describe, expect, it } from 'vitest';
import { formatDuration } from '../src/format';

describe('formatDuration', () => {
  it('formats seconds, minutes and hours', () => {
    expect(formatDuration('en', 0)).toBe('0 s');
    expect(formatDuration('en', 45_900)).toBe('45 s');
    expect(formatDuration('en', 60_000)).toBe('1 min');
    expect(formatDuration('en', 12 * 60_000 + 30_000)).toBe('12 min');
    expect(formatDuration('en', 65 * 60_000)).toBe('1 h 05 min');
    expect(formatDuration('en', 10 * 3600_000)).toBe('10 h 00 min');
  });

  it('uses Russian units', () => {
    expect(formatDuration('ru', 30_000)).toBe('30 с');
    expect(formatDuration('ru', 5 * 60_000)).toBe('5 мин');
    expect(formatDuration('ru', 125 * 60_000)).toBe('2 ч 05 мин');
  });

  it('treats negative or tiny values as zero', () => {
    expect(formatDuration('en', -500)).toBe('0 s');
    expect(formatDuration('en', 999)).toBe('0 s');
  });
});
