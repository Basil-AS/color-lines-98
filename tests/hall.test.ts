import { describe, expect, it } from 'vitest';
import { DEFAULT_KING, HALL_SIZE, insertScore, kingOf, rankOf, sanitizeHall } from '../src/dos/hall';
import type { HallEntry } from '../src/dos/hall';

const e = (name: string, score: number, at = 1): HallEntry => ({ name, score, at });

describe('king', () => {
  it('is the original "Handicap" with 100 points until someone scores more', () => {
    expect(kingOf([])).toEqual(DEFAULT_KING);
    expect(DEFAULT_KING).toMatchObject({ name: 'Handicap', score: 100 });
    expect(kingOf([e('Ann', 500)]).name).toBe('Ann');
  });
});

describe('insertScore', () => {
  it('keeps the table sorted, best first', () => {
    let hall: HallEntry[] = [];
    for (const [n, s] of [['a', 30], ['b', 90], ['c', 60]] as const) hall = insertScore(hall, e(n, s));
    expect(hall.map((x) => x.score)).toEqual([90, 60, 30]);
  });

  it('puts a tie below the older result', () => {
    const hall = insertScore([e('old', 50, 1)], e('new', 50, 2));
    expect(hall.map((x) => x.name)).toEqual(['old', 'new']);
  });

  it('keeps only the top ten and does not mutate the input', () => {
    let hall: HallEntry[] = [];
    for (let i = 1; i <= HALL_SIZE + 3; i++) hall = insertScore(hall, e('p' + i, i * 10));
    expect(hall).toHaveLength(HALL_SIZE);
    expect(hall[0].score).toBe((HALL_SIZE + 3) * 10);
    const before = [e('x', 5)];
    insertScore(before, e('y', 6));
    expect(before).toHaveLength(1);
  });

  it('ignores a score of zero', () => {
    expect(insertScore([], e('zero', 0))).toEqual([]);
  });
});

describe('rankOf', () => {
  it('returns the 0-based place a score would take, or -1', () => {
    const hall = Array.from({ length: HALL_SIZE }, (_, i) => e('p', (HALL_SIZE - i) * 10));
    expect(rankOf(hall, 1000)).toBe(0);
    expect(rankOf(hall, 55)).toBe(HALL_SIZE - 5);
    expect(rankOf(hall, 5)).toBe(-1);
    expect(rankOf([], 10)).toBe(0);
    expect(rankOf([], 0)).toBe(-1);
  });
});

describe('sanitizeHall', () => {
  it('drops junk, trims long names and sorts', () => {
    const out = sanitizeHall([
      e('a', 10),
      { name: 'x'.repeat(50), score: 30, at: 2 },
      null,
      { name: 3, score: 1, at: 1 },
      { name: 'neg', score: -5, at: 1 },
      e('b', 20),
    ]);
    expect(out.map((x) => x.score)).toEqual([30, 20, 10]);
    expect(out[0].name.length).toBeLessThanOrEqual(12);
    expect(sanitizeHall('nope')).toEqual([]);
  });
});
