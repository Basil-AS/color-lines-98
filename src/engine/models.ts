export type BallColor = 'red' | 'green' | 'blue' | 'cyan' | 'magenta' | 'yellow' | 'brown';

export const ALL_COLORS: BallColor[] = [
  'red',
  'green',
  'blue',
  'cyan',
  'magenta',
  'yellow',
  'brown'
];

export const COLOR_HEX: Record<BallColor, string> = {
  red: '#E53935',
  green: '#43A047',
  blue: '#1E88E5',
  cyan: '#00ACC1',
  magenta: '#8E24AA',
  yellow: '#FDD835',
  brown: '#6D4C41'
};

export interface Point {
  x: number;
  y: number;
}

export function pointsEqual(a: Point, b: Point): boolean {
  return a.x === b.x && a.y === b.y;
}

export function pointKey(p: Point): string {
  return `${p.x},${p.y}`;
}
