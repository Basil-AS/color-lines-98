import type { BallColor, Point } from '../engine/models';
import {
  BOARD,
  BUTTONS,
  BUTTON_X,
  LABEL_Y,
  CELL,
  EMPTY_CELL,
  KING_POS,
  LCD,
  NEXT_SLOT,
  PRETENDER_POS,
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
  | { kind: 'fill'; x: number; y: number; w: number; h: number; color: string };

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
}

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
  let pretenderFrame = 0;
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
  for (const id of BUTTONS) draws.push(image('sheet', labelRect(id, lit[id]), BUTTON_X[id], LABEL_Y));

  return draws;
}

export { BOARD, CELL };
