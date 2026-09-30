import { describe, expect, it } from 'vitest';
import { Board } from '../src/engine/board';
import { findHint } from '../src/engine/hint';

describe('findHint', () => {
  it('returns null on an empty board', () => {
    expect(findHint(new Board())).toBeNull();
  });

  it('finds the move that completes a line of five', () => {
    const b = new Board();
    for (let x = 0; x < 4; x++) b.set(x, 0, 'red');
    b.set(8, 8, 'red');
    const h = findHint(b)!;
    expect(h.clears).toBe(true);
    expect(h.from).toEqual({ x: 8, y: 8 });
    expect(h.to).toEqual({ x: 4, y: 0 });
  });

  it('builds a run when nothing can be cleared', () => {
    const b = new Board();
    b.set(0, 0, 'green');
    b.set(1, 0, 'green');
    b.set(8, 8, 'green');
    const h = findHint(b)!;
    expect(h.clears).toBe(false);
    expect(h.value).toBeGreaterThanOrEqual(3);
  });

  it('does not offer a move through a walled-in ball', () => {
    const b = new Board();
    b.set(0, 0, 'red');
    b.set(1, 0, 'blue');
    b.set(0, 1, 'blue');
    const h = findHint(b)!;
    expect(h.from).not.toEqual({ x: 0, y: 0 });
  });
});
