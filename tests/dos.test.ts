import { describe, expect, it } from 'vitest';
import {
  BOARD,
  CELL,
  SCREEN,
  SHEET,
  LABEL_Y,
  ballRect,
  cellOrigin,
  glyphRect,
  kingRect,
  pretenderRect,
} from '../src/dos/sprites';
import { buildScene } from '../src/dos/scene';
import type { DosState } from '../src/dos/scene';
import { ALL_COLORS } from '../src/engine/models';

const inside = (r: readonly number[], w: number, h: number) =>
  r[0] >= 0 && r[1] >= 0 && r[0] + r[2] <= w && r[1] + r[3] <= h;

describe('sprite coordinates', () => {
  it('keeps every ball frame inside the sprite sheet', () => {
    for (const c of ALL_COLORS)
      for (const f of ['full', 'small', 'medium', 'wide', 'burst1', 'burst2'] as const)
        expect(inside(ballRect(c, f), SHEET.w, SHEET.h)).toBe(true);
  });

  it('keeps king and pretender frames inside the sheet', () => {
    for (let i = 0; i < 7; i++) expect(inside(kingRect(i), SHEET.w, SHEET.h)).toBe(true);
    for (let i = 0; i < 6; i++) expect(inside(pretenderRect(i), SHEET.w, SHEET.h)).toBe(true);
  });

  it('places the 9x9 board cells on the original 34x24 grid', () => {
    expect(cellOrigin(0, 0)).toEqual([BOARD.x, BOARD.y]);
    expect(cellOrigin(8, 8)).toEqual([BOARD.x + 8 * CELL.w, BOARD.y + 8 * CELL.h]);
    expect(BOARD.x + 9 * CELL.w).toBeLessThanOrEqual(SCREEN.w);
    expect(BOARD.y + 9 * CELL.h).toBeLessThanOrEqual(SCREEN.h);
  });

  it('finds glyphs for digits and letters', () => {
    for (const ch of '0123456789Aa! ') expect(inside(glyphRect(ch)!, SHEET.w, SHEET.h)).toBe(true);
    // Row one starts with a blank cell, so '!' is the second glyph and '0' the seventeenth.
    expect(glyphRect('!')![0]).toBe(318 + 9);
    expect(glyphRect('0')![0]).toBe(318 + 9 * 16);
    expect(glyphRect('☃')).toBeNull();
  });
});

function state(over: Partial<DosState> = {}): DosState {
  return {
    cells: Array<null>(81).fill(null),
    selected: null,
    next: ['red', 'green', 'blue'],
    showNext: true,
    score: 0,
    kingScore: 100,
    soundOn: true,
    now: 0,
    effects: [],
    coronationStart: null,
    ...over,
  };
}

const sheetDraws = (s: DosState) => buildScene(s).filter((d) => d.kind === 'image' && d.img === 'sheet');

describe('scene', () => {
  it('starts with the original layout as background', () => {
    const scene = buildScene(state());
    expect(scene[0]).toMatchObject({ kind: 'image', img: 'layout', dx: 0, dy: 0 });
  });

  it('draws one sheet tile for every ball on the board at its cell', () => {
    const cells = Array<null | 'red'>(81).fill(null);
    cells[0] = 'red';
    cells[80] = 'red';
    const draws = buildScene(state({ cells })).filter(
      (d) => d.kind === 'image' && d.img === 'sheet' && d.sw === CELL.w && d.sh === CELL.h && d.dy >= BOARD.y
    );
    expect(draws.map((d) => [d.dx, d.dy])).toEqual([cellOrigin(0, 0), cellOrigin(8, 8)]);
  });

  it('shows the next balls only while F3 "NEXT" is on', () => {
    const on = sheetDraws(state({ showNext: true })).length;
    const off = sheetDraws(state({ showNext: false })).length;
    expect(on).toBeGreaterThan(off);
  });

  it('squashes the selected ball to make it bounce', () => {
    const cells = Array<null | 'blue'>(81).fill(null);
    cells[10] = 'blue';
    const frames = [0, 150].map((now) =>
      buildScene(state({ cells, selected: { x: 1, y: 1 }, now })).filter(
        (d) => d.kind === 'image' && d.img === 'sheet' && d.dy === cellOrigin(1, 1)[1]
      )[0]
    );
    expect(frames[0].sx).not.toBe(frames[1].sx);
  });

  it('grows a newly spawned ball and bursts a cleared one', () => {
    const cells = Array<null | 'green'>(81).fill(null);
    cells[0] = 'green';
    const spawn = (now: number) =>
      buildScene(state({ cells, now, effects: [{ kind: 'spawn', x: 0, y: 0, color: 'green', start: 0 }] })).filter(
        (d) => d.kind === 'image' && d.img === 'sheet' && d.dy === BOARD.y && d.dx === BOARD.x
      )[0];
    expect(spawn(10).sx).not.toBe(spawn(1000).sx);

    const burst = buildScene(
      state({ now: 20, effects: [{ kind: 'burst', x: 2, y: 3, color: 'red', start: 0 }] })
    ).filter((d) => d.kind === 'image' && d.img === 'sheet' && d.dx === cellOrigin(2, 3)[0] && d.dy === cellOrigin(2, 3)[1]);
    expect(burst).toHaveLength(1);
    const gone = buildScene(
      state({ now: 5000, effects: [{ kind: 'burst', x: 2, y: 3, color: 'red', start: 0 }] })
    ).filter((d) => d.kind === 'image' && d.img === 'sheet' && d.dx === cellOrigin(2, 3)[0] && d.dy === cellOrigin(2, 3)[1]);
    expect(gone).toHaveLength(0);
  });

  it('draws both score displays with the original digits, right aligned', () => {
    const scene = buildScene(state({ score: 42, kingScore: 1234 }));
    const digits = scene.filter((d) => d.kind === 'image' && d.img === 'sheet' && d.dy < 20 && d.sw === 9);
    // "42" on the right display, "1234" on the left one.
    expect(digits.filter((d) => d.dx >= 507)).toHaveLength(2);
    expect(digits.filter((d) => d.dx < 200)).toHaveLength(4);
    const right = digits.filter((d) => d.dx >= 507).map((d) => d.dx);
    expect(Math.max(...right)).toBe(507 + 7 * 9);
  });

  it('crowns the pretender after the king is beaten', () => {
    const pretender = (now: number, start: number | null) =>
      buildScene(state({ now, coronationStart: start, score: 500 })).filter(
        (d) => d.kind === 'image' && d.img === 'sheet' && d.dx === 516
      )[0];
    const at = (now: number, start: number | null) => {
      const d = pretender(now, start);
      return `${d.sx},${d.sy}`;
    };
    expect(at(0, null)).not.toBe(at(5000, 0));
    expect(at(100, 0)).not.toBe(at(5000, 0));
  });

  it('greys the SOUND label when the sound is off', () => {
    const label = (soundOn: boolean) =>
      buildScene(state({ soundOn })).filter((d) => d.kind === 'image' && d.img === 'sheet' && d.dy === LABEL_Y && d.dx === 272)[0];
    expect(label(true).sx).not.toBe(label(false).sx);
  });
});
