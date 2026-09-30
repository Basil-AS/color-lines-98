import type { BallColor, Point } from '../engine/models';
import {
  BOARD,
  BUTTONS,
  BUTTON_X,
  BUTTON_Y,
  LABEL_Y,
  CELL,
  EMPTY_CELL,
  KING_POS,
  LCD,
  NEXT_SLOT,
  PRETENDER_POS,
  HELP_WINDOW,
  TOP_TEN_WINDOW,
  WINDOW_POS,
  ballRect,
  cellOrigin,
  glyphRect,
  kingRect,
  labelRect,
  pretenderRect,
} from './sprites';
import type { BallFrame, ButtonId, Rect } from './sprites';

export type Draw =
  | { kind: 'image'; img: 'layout' | 'sheet'; sx: number; sy: number; sw: number; sh: number; dx: number; dy: number }
  | { kind: 'fill'; x: number; y: number; w: number; h: number; color: string }
  | {
      kind: 'text';
      text: string;
      x: number;
      y: number;
      color: string;
      size: number;
      align: 'left' | 'center' | 'right';
      /** gothic: the names under the pillars; mono: pixel-like text; serif: the white captions. */
      font: 'gothic' | 'mono' | 'serif';
      shadow?: string;
    };

export interface Effect {
  kind: 'spawn' | 'burst';
  x: number;
  y: number;
  color: BallColor;
  /** Timestamp (ms) when the effect started. */
  start: number;
}

export interface DosState {
  cells: readonly (BallColor | null)[];
  selected: Point | null;
  next: readonly BallColor[];
  showNext: boolean;
  score: number;
  kingScore: number;
  soundOn: boolean;
  /** Current time in ms, for animations. */
  now: number;
  effects: readonly Effect[];
  /** When the pretender started taking the throne; null while the king still reigns. */
  coronationStart: number | null;
  /** Buttons currently held down (shown in green). */
  pressed?: readonly ButtonId[];
  /** The pictures carry English words; other languages get them covered and rewritten. */
  lang?: 'en' | 'ru';
  kingName?: string;
  pretenderName?: string;
}

const RU_BUTTONS: Record<ButtonId, string> = { help: 'ПОМОЩЬ', sound: 'ЗВУК', next: 'ДАЛЕЕ', restart: 'ЗАНОВО' };

export const SPAWN_FRAMES: readonly BallFrame[] = ['small', 'medium', 'wide'];
export const BURST_FRAMES: readonly BallFrame[] = ['burst1', 'burst2'];
export const SPAWN_STEP = 70;
export const BURST_STEP = 90;
export const CORONATION_STEP = 260;

const image = (img: 'layout' | 'sheet', r: Rect, dx: number, dy: number): Draw => ({
  kind: 'image',
  img,
  sx: r[0],
  sy: r[1],
  sw: r[2],
  sh: r[3],
  dx,
  dy,
});

function lcd(value: number, box: { x: number; y: number; w: number; h: number }): Draw[] {
  const text = String(Math.max(0, Math.floor(value))).slice(-LCD.chars);
  const out: Draw[] = [{ kind: 'fill', x: box.x, y: box.y, w: box.w, h: box.h, color: '#000000' }];
  const start = LCD.chars - text.length;
  [...text].forEach((ch, i) => {
    const g = glyphRect(ch);
    if (g) out.push(image('sheet', g, box.x + (start + i) * 9, box.y + 1));
  });
  return out;
}

function ballFrame(state: DosState, index: number, effect: Effect | undefined): BallFrame {
  const x = index % 9;
  const y = Math.floor(index / 9);
  if (state.selected && state.selected.x === x && state.selected.y === y) {
    return Math.floor(state.now / 140) % 2 === 0 ? 'full' : 'wide';
  }
  if (effect?.kind === 'spawn') {
    const step = Math.floor((state.now - effect.start) / SPAWN_STEP);
    return step < SPAWN_FRAMES.length ? SPAWN_FRAMES[Math.max(0, step)] : 'full';
  }
  return 'full';
}

/** Everything to draw for one frame of the 1992 screen, back to front. */
export function buildScene(state: DosState): Draw[] {
  const draws: Draw[] = [image('layout', [0, 0, 640, 350], 0, 0)];

  // Score displays: the king's record on the left, the player's score on the right.
  draws.push(...lcd(state.kingScore, LCD.king), ...lcd(state.score, LCD.player));

  // The three balls that will appear next (F3 hides them).
  for (let i = 0; i < 3; i++) {
    const dx = NEXT_SLOT.x + NEXT_SLOT.pitch * i;
    const color = state.next[i];
    draws.push(
      state.showNext && color
        ? image('sheet', ballRect(color, 'small'), dx, NEXT_SLOT.y)
        : image('layout', EMPTY_CELL, dx, NEXT_SLOT.y)
    );
  }

  // Balls on the board, then the bursts of balls that just disappeared.
  const effectAt = new Map<number, Effect>();
  for (const e of state.effects) effectAt.set(e.y * 9 + e.x, e);
  state.cells.forEach((color, index) => {
    if (!color) return;
    const [dx, dy] = cellOrigin(index % 9, Math.floor(index / 9));
    draws.push(image('sheet', ballRect(color, ballFrame(state, index, effectAt.get(index))), dx, dy));
  });
  for (const e of state.effects) {
    if (e.kind !== 'burst' || state.cells[e.y * 9 + e.x]) continue;
    const step = Math.floor((state.now - e.start) / BURST_STEP);
    if (step < 0 || step >= BURST_FRAMES.length) continue;
    const [dx, dy] = cellOrigin(e.x, e.y);
    draws.push(image('sheet', ballRect(e.color, BURST_FRAMES[step]), dx, dy));
  }

  // King and pretender; the crown moves over when the record falls.
  const dt = state.coronationStart === null ? -1 : state.now - state.coronationStart;
  let kingFrame = Math.floor(state.now / 700) % 2; // the gold on the crown glints
  // The pretender lifts his sword now and then, more eagerly the closer he gets to the record.
  const closing = state.kingScore > 0 && state.score >= state.kingScore * 0.75;
  const swordPeriod = closing ? 1800 : 4200;
  let pretenderFrame = state.now % swordPeriod >= swordPeriod - 650 ? 5 : 0;
  if (dt >= 0) {
    const step = Math.floor(dt / CORONATION_STEP);
    kingFrame = step === 0 ? 2 : step === 1 ? 3 : 4;
    pretenderFrame = Math.min(4, step + 1);
  }
  draws.push(image('sheet', kingRect(kingFrame), KING_POS.x, KING_POS.y));
  draws.push(image('sheet', pretenderRect(pretenderFrame), PRETENDER_POS.x, PRETENDER_POS.y));

  // Bottom buttons: SOUND and NEXT light up while on, HELP and RESTART while pressed.
  const lit: Record<ButtonId, boolean> = {
    help: state.pressed?.includes('help') ?? false,
    sound: state.soundOn,
    next: state.showNext,
    restart: state.pressed?.includes('restart') ?? false,
  };
  const ru = state.lang === 'ru';
  for (const id of BUTTONS) {
    if (!ru) {
      draws.push(image('sheet', labelRect(id, lit[id]), BUTTON_X[id], LABEL_Y));
      continue;
    }
    // Cover the English label with the black display and write the Russian word in the same colours.
    draws.push({ kind: 'fill', x: BUTTON_X[id], y: BUTTON_Y, w: 73, h: 13, color: '#000000' });
    draws.push({ kind: 'text', text: RU_BUTTONS[id], x: BUTTON_X[id] + 36, y: BUTTON_Y + 10, color: lit[id] ? '#00aa00' : '#555555', size: 11, align: 'center', font: 'mono' });
  }

  if (ru) {
    // "Next" and "Colors" are part of the layout picture: cover them and write "Далее" and "цвета".
    draws.push({ kind: 'fill', x: 208, y: 8, w: 46, h: 20, color: '#aaaaaa' });
    draws.push({ kind: 'fill', x: 388, y: 8, w: 60, h: 20, color: '#aaaaaa' });
    draws.push({ kind: 'text', text: 'Далее', x: 231, y: 24, color: '#ffffff', size: 15, align: 'center', font: 'serif', shadow: '#555555' });
    draws.push({ kind: 'text', text: 'цвета', x: 418, y: 24, color: '#ffffff', size: 15, align: 'center', font: 'serif', shadow: '#555555' });
  }

  if (state.kingName) draws.push({ kind: 'text', text: state.kingName, x: 88, y: 262, color: '#ffff55', size: 22, align: 'center', font: 'gothic' });
  if (state.pretenderName) draws.push({ kind: 'text', text: state.pretenderName, x: 541, y: 262, color: '#ffff55', size: 22, align: 'center', font: 'gothic' });

  return draws;
}

export { BOARD, CELL };

/** Greedy word wrap; a word longer than the line stays whole. */
export function wrapText(text: string, maxChars: number): string[] {
  const lines: string[] = [];
  let line = '';
  for (const word of text.split(/\s+/).filter(Boolean)) {
    if (line === '') line = word;
    else if ((line + ' ' + word).length <= maxChars) line += ' ' + word;
    else {
      lines.push(line);
      line = word;
    }
  }
  if (line !== '') lines.push(line);
  return lines;
}

const RU_HELP =
  'Цель игры набрать больше очков, чем «король». Очки растут, когда вы выстраиваете по горизонтали, вертикали или диагонали линию из пяти и более шаров одного цвета. Линии строятся перемещением шаров по свободным клеткам. Желаем успеха!';

/** The Help or Top Ten window over the board, in the language of the player. */
export function buildWindow(kind: 'help' | 'top10', lang: 'en' | 'ru', hall: readonly { name: string; score: number }[]): Draw[] {
  const r = kind === 'help' ? HELP_WINDOW : TOP_TEN_WINDOW;
  const { x, y } = WINDOW_POS;
  const draws: Draw[] = [image('sheet', r, x, y)];

  if (kind === 'help' && lang === 'ru') {
    // Cover the title and the green English text; write them again in Russian.
    draws.push({ kind: 'fill', x: x + 88, y: y + 6, w: 60, h: 18, color: '#aaaaaa' });
    draws.push({ kind: 'text', text: 'Справка', x: x + 118, y: y + 20, color: '#ff5555', size: 14, align: 'center', font: 'serif', shadow: '#ffffff' });
    draws.push({ kind: 'fill', x: x + 18, y: y + 28, w: 200, h: 122, color: '#000000' });
    wrapText(RU_HELP, 26).forEach((line, i) => {
      draws.push({ kind: 'text', text: line, x: x + 118, y: y + 40 + i * 11, color: '#00aa00', size: 9, align: 'center', font: 'mono' });
    });
  }

  if (kind === 'top10') {
    if (lang === 'ru') {
      draws.push({ kind: 'fill', x: x + 70, y: y + 5, w: 100, h: 18, color: '#aaaaaa' });
      draws.push({ kind: 'text', text: 'Десятка лучших', x: x + 120, y: y + 20, color: '#ff5555', size: 13, align: 'center', font: 'serif', shadow: '#ffffff' });
      draws.push({ kind: 'fill', x: x + 12, y: y + 143, w: 92, h: 15, color: '#aaaaaa' });
      draws.push({ kind: 'text', text: 'Ваше имя', x: x + 16, y: y + 155, color: '#ff5555', size: 12, align: 'left', font: 'serif' });
    }
    hall.slice(0, 10).forEach((h, i) => {
      const line = y + 39 + i * 11.6;
      draws.push({ kind: 'text', text: h.name, x: x + 52, y: line, color: '#00aa00', size: 9, align: 'left', font: 'mono' });
      draws.push({ kind: 'text', text: String(h.score), x: x + 210, y: line, color: '#00aa00', size: 9, align: 'right', font: 'mono' });
    });
  }
  return draws;
}
