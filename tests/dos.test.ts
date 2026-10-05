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
  PRETENDER_POS,
  TOWER,
  towerRise,
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

  it('animates the pretender as well as the king', () => {
    const at = (now: number) =>
      buildScene(state({ now })).find((d) => d.kind === 'image' && d.img === 'sheet' && d.dx === PRETENDER_POS.x)!;
    const seen = new Set([0, 1000, 3600, 4000].map((now) => (at(now) as { sx: number; sy: number }).sx + ',' + (at(now) as { sy: number }).sy));
    expect(seen.size).toBeGreaterThan(1);
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

import { dosNames } from '../src/dos/names';

describe('who is called what', () => {
  const base = { kingName: 'Handicap', playerName: 'Ann', pretenderLabel: 'Pretender', defaultPlayerName: 'Player' };

  it('keeps the labels of the original until the king is beaten', () => {
    expect(dosNames({ ...base, crowned: false })).toEqual({ king: 'Handicap', pretender: 'Pretender' });
  });

  it('crowns the player: the pretender gets the player name, the dethroned king keeps his own', () => {
    expect(dosNames({ ...base, crowned: true })).toEqual({ king: 'Handicap', pretender: 'Ann' });
  });

  it('falls back to a default name when none was entered', () => {
    expect(dosNames({ ...base, playerName: '  ', crowned: true }).pretender).toBe('Player');
  });
});

import { buildWindow, wrapText } from '../src/dos/scene';

const texts = (draws: ReturnType<typeof buildScene>) => draws.filter((d) => d.kind === 'text') as Extract<ReturnType<typeof buildScene>[number], { kind: 'text' }>[];
const cyrillic = /[А-Яа-яЁё]/;

describe('localised 1992 screen', () => {
  const ru = (over: Partial<DosState> = {}) => state({ lang: 'ru', kingName: 'Handicap', pretenderName: 'Pretender', ...over });

  it('keeps the original artwork and only draws the names in English', () => {
    const t = texts(buildScene(state({ lang: 'en', kingName: 'Handicap', pretenderName: 'Pretender' })));
    expect(t.map((x) => x.text)).toEqual(['Handicap', 'Pretender']);
    expect(sheetDraws(state({ lang: 'en' })).some((d) => d.kind === 'image' && d.dy === LABEL_Y)).toBe(true);
  });

  it('puts Russian words over the English ones baked into the pictures', () => {
    const scene = buildScene(ru());
    const words = texts(scene).map((x) => x.text);
    expect(words).toEqual(expect.arrayContaining(['Далее', 'цвета', 'ПОМОЩЬ', 'ЗВУК', 'ДАЛЕЕ', 'ЗАНОВО', 'Handicap', 'Pretender']));
    // The English button labels are not drawn any more.
    expect(scene.some((d) => d.kind === 'image' && d.img === 'sheet' && d.dy === LABEL_Y)).toBe(false);
    // A cover hides "Next" and "Colors" of the layout picture before the Russian text goes on top.
    const fills = scene.filter((d) => d.kind === 'fill');
    expect(fills.some((f) => f.kind === 'fill' && f.x <= 215 && f.x + f.w >= 245)).toBe(true);
  });

  it('lights the Russian SOUND and NEXT labels like the English ones', () => {
    const colour = (on: boolean) => texts(buildScene(ru({ soundOn: on }))).find((x) => x.text === 'ЗВУК')!.color;
    expect(colour(true)).not.toBe(colour(false));
  });

  it('draws only Cyrillic letters or the shared digits in the Russian overlay texts (the captions stay English)', () => {
    for (const x of texts(buildScene(ru()))) {
      if (x.text === 'Handicap' || x.text === 'Pretender') continue;
      expect(x.text).toMatch(/^[А-Яа-яЁё0-9 ]+$/);
    }
  });
});

describe('wrapText', () => {
  it('wraps at word boundaries and never exceeds the width', () => {
    const lines = wrapText('Цель игры набрать больше очков чем король', 14);
    expect(lines.every((l) => l.length <= 14)).toBe(true);
    expect(lines.join(' ')).toBe('Цель игры набрать больше очков чем король');
  });

  it('keeps a word longer than a line whole', () => {
    expect(wrapText('Сверхдлинноесловопример', 8)).toEqual(['Сверхдлинноесловопример']);
    expect(wrapText('', 10)).toEqual([]);
  });
});

describe('windows over the board', () => {
  it('uses the original Help picture as it is in English', () => {
    const draws = buildWindow('help', 'en', []);
    expect(draws.filter((d) => d.kind === 'text')).toHaveLength(0);
    expect(draws[0]).toMatchObject({ kind: 'image', img: 'sheet' });
  });

  it('covers the English help text and writes it again in Russian, inside the window', () => {
    const draws = buildWindow('help', 'ru', []);
    const lines = draws.filter((d) => d.kind === 'text');
    expect(lines.length).toBeGreaterThanOrEqual(6);
    for (const l of lines) {
      expect(l.kind === 'text' && cyrillic.test(l.text)).toBe(true);
      if (l.kind === 'text') {
        expect(l.x).toBeGreaterThanOrEqual(206);
        expect(l.x).toBeLessThanOrEqual(206 + 238);
        expect(l.y).toBeLessThanOrEqual(86 + 166);
      }
    }
    expect(draws.some((d) => d.kind === 'fill')).toBe(true);
  });

  it('lists the Top Ten with names and scores, whatever the language of the names', () => {
    const hall = [
      { name: 'Ann', score: 900, at: 1 },
      { name: 'Иван', score: 700, at: 2 },
    ];
    const t = buildWindow('top10', 'en', hall).filter((d) => d.kind === 'text').map((d) => (d.kind === 'text' ? d.text : ''));
    expect(t).toEqual(expect.arrayContaining(['Ann', '900', 'Иван', '700']));
    const ruTitle = buildWindow('top10', 'ru', hall).filter((d) => d.kind === 'text').map((d) => (d.kind === 'text' ? d.text : ''));
    expect(ruTitle).toContain('Десятка лучших');
  });

  it('shows at most ten entries', () => {
    const hall = Array.from({ length: 15 }, (_, i) => ({ name: 'p' + i, score: 100 - i, at: i }));
    const names = buildWindow('top10', 'en', hall).filter((d) => d.kind === 'text' && d.align === 'left');
    expect(names).toHaveLength(10);
  });
});

describe('the pretender tower', () => {
  it('grows with the score and tops out at the king pillar', () => {
    expect(towerRise(0, 100)).toBe(0);
    expect(towerRise(50, 100)).toBe(TOWER.maxRise / 2);
    expect(towerRise(100, 100)).toBe(TOWER.maxRise);
    expect(towerRise(500, 100)).toBe(TOWER.maxRise);
    expect(towerRise(10, 0)).toBe(TOWER.maxRise);
  });

  it('draws the lifted figure higher and keeps the foot ring in place', () => {
    const low = buildScene(state({ score: 0 }));
    const high = buildScene(state({ score: 60 }));
    const pretender = (d: ReturnType<typeof buildScene>) =>
      d.filter((x) => x.kind === 'image' && x.img === 'sheet' && x.dx === PRETENDER_POS.x).pop();
    expect(pretender(high)!.dy).toBeLessThan(pretender(low)!.dy);
    const foot = high.filter((x) => x.kind === 'image' && x.img === 'layout' && x.sy === TOWER.footTop);
    expect(foot).toHaveLength(1);
    expect(foot[0]).toMatchObject({ dy: TOWER.footTop });
  });
});
