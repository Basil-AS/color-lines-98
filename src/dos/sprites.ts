import type { BallColor } from '../engine/models';

/**
 * Coordinates of the original Color Lines (Gamos, 1992) artwork, taken from its `lines.lib`
 * archive: `layout.png` is the empty game screen, `sheet.png` holds every sprite.
 * The DOS screen is 640x350 with pixels that are 1.37 times taller than wide.
 */
export type Rect = readonly [x: number, y: number, w: number, h: number];

export const SCREEN = { w: 640, h: 350 } as const;
export const SHEET = { w: 640, h: 350 } as const;

export const BOARD = { x: 171, y: 61 } as const;
export const CELL = { w: 34, h: 24 } as const;

export function cellOrigin(cx: number, cy: number): readonly [number, number] {
  return [BOARD.x + cx * CELL.w, BOARD.y + cy * CELL.h];
}

/** Row of each colour in the ball table of the sheet. */
const BALL_ROW: Record<BallColor, number> = {
  green: 0,
  red: 1,
  magenta: 2,
  cyan: 3,
  brown: 4,
  yellow: 5,
  blue: 6,
};

export type BallFrame = 'full' | 'small' | 'medium' | 'wide' | 'burst1' | 'burst2';
const BALL_COLUMN: Record<BallFrame, number> = { full: 0, small: 1, medium: 2, wide: 3, burst1: 4, burst2: 5 };

/** A ball on its grey cell tile (34x24), in one of its animation frames. */
export function ballRect(color: BallColor, frame: BallFrame): Rect {
  return [44 + 34 * BALL_COLUMN[frame], 171 + 24 * BALL_ROW[color], CELL.w, CELL.h];
}

/** A cell without a ball (taken from the empty board of the layout). */
export const EMPTY_CELL: Rect = [BOARD.x, BOARD.y, CELL.w, CELL.h];

/**
 * King frames, 2 per row: 0/1 crowned (blinking), 2/3 crown fading away, 4 crownless,
 * 5 crownless with the arm out, 7 crownless and crouching.
 */
export function kingRect(index: number): Rect {
  return [478 + 73 * (index % 2), 1 + 74 * Math.floor(index / 2), 72, 73];
}
export const KING_POS = { x: 51, y: 72 } as const;

/** Pretender frames: 0 plain, 1-3 the crown appearing, 4 big crown, 5 sword raised. */
export function pretenderRect(index: number): Rect {
  return [249 + 51 * (index % 4), 170 + 48 * Math.floor(index / 4), 50, 47];
}
export const PRETENDER_POS = { x: 516, y: 156 } as const;

/**
 * The pretender's pillar grows with the score: it starts as the short pedestal of the original screen and reaches the
 * top of the king's pillar when the record is beaten. All numbers are rows/columns of the 640x350 layout picture.
 */
export const TOWER = {
  /** The column that moves: the pretender, his legs and the top of the pedestal (x, width), from the first row to PEDESTAL_TOP + 4. */
  x: 516,
  w: 68,
  top: 156,
  /** First row of the pedestal body and the first row of the fixed foot ring under it. */
  bodyTop: 219,
  footTop: 232,
  /** Rows of the plain pedestal body that are stretched to build the pillar. */
  stretch: { sy: 224, sh: 4 },
  /** How far the pretender can climb: from the pedestal top to the top of the king's pillar. */
  maxRise: 82,
} as const;

/** Pixels the pretender has climbed for a score against the record (0 at no score, maxRise once the record falls). */
export function towerRise(score: number, kingScore: number): number {
  if (kingScore <= 0) return TOWER.maxRise;
  const share = Math.max(0, Math.min(1, score / kingScore));
  return Math.round(TOWER.maxRise * share);
}

export const LCD = {
  king: { x: 55, y: 11, w: 73, h: 11 },
  player: { x: 507, y: 11, w: 73, h: 11 },
  chars: 8,
} as const;

export const NEXT_SLOT = { x: 272, y: 5, pitch: 34 } as const;

/** The four bottom buttons of the original screen: F1 help, F2 sound, F3 next, F4 restart. */
export const BUTTONS = ['help', 'sound', 'next', 'restart'] as const;
export type ButtonId = (typeof BUTTONS)[number];
export const BUTTON_X: Record<ButtonId, number> = { help: 139, sound: 272, next: 406, restart: 539 };
export const BUTTON_Y = 321;
/** The label text sits a little lower inside its 13px black box. */
export const LABEL_Y = BUTTON_Y + 2;

/** Label sprite of a button: grey (idle/off) or green (pressed/on). */
export function labelRect(button: ButtonId, lit: boolean): Rect {
  const index = BUTTONS.indexOf(button) * 2 + (lit ? 1 : 0);
  return [74 * index, 340, 73, 10];
}

/** The 9x10 green bitmap font of the sheet. */
export function glyphRect(ch: string): Rect | null {
  const code = ch.charCodeAt(0);
  let row: number;
  let first: number;
  if (code >= 32 && code <= 64) [row, first] = [304, 32]; // the first cell is a blank
  else if (code >= 65 && code <= 96) [row, first] = [316, 65];
  else if (code >= 97 && code <= 122) [row, first] = [329, 97];
  else return null;
  return [318 + 9 * (code - first), row, 9, 9];
}

/** Windows drawn on top of the board. */
export const TOP_TEN_WINDOW: Rect = [0, 0, 238, 166];
export const HELP_WINDOW: Rect = [240, 0, 236, 166];
export const WINDOW_POS = { x: 206, y: 86 } as const;
