/** Small colour maths for the look builder: everything is a `#rrggbb` string, nothing here touches the DOM. */
export type Rgb = [number, number, number];

const HEX = /^#?([0-9a-f]{6})$/i;
const SHORT_HEX = /^#?([0-9a-f]{3})$/i;

export function isHex(value: unknown): value is string {
  return typeof value === 'string' && (HEX.test(value) || SHORT_HEX.test(value));
}

/** `#abc` and `abc` become `#aabbcc`; anything that is not a colour becomes the fallback. */
export function normalizeHex(value: unknown, fallback: string): string {
  if (typeof value !== 'string') return fallback;
  const v = value.trim();
  const long = HEX.exec(v);
  if (long) return `#${long[1].toLowerCase()}`;
  const short = SHORT_HEX.exec(v);
  if (short) return `#${short[1].split('').map((c) => c + c).join('').toLowerCase()}`;
  return fallback;
}

export function toRgb(hex: string): Rgb {
  const h = normalizeHex(hex, '#000000').slice(1);
  return [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)];
}

export function toHex([r, g, b]: Rgb): string {
  const c = (n: number) => Math.round(Math.min(255, Math.max(0, n))).toString(16).padStart(2, '0');
  return `#${c(r)}${c(g)}${c(b)}`;
}

/** `amount` of `b` mixed into `a` (0 is a, 1 is b). */
export function mix(a: string, b: string, amount: number): string {
  const x = toRgb(a);
  const y = toRgb(b);
  const t = Math.min(1, Math.max(0, amount));
  return toHex([x[0] + (y[0] - x[0]) * t, x[1] + (y[1] - x[1]) * t, x[2] + (y[2] - x[2]) * t]);
}

/** WCAG relative luminance, 0 (black) to 1 (white). */
export function luminance(hex: string): number {
  const lin = (v: number) => {
    const s = v / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  };
  const [r, g, b] = toRgb(hex);
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
}

/** WCAG contrast ratio, 1 to 21. */
export function contrastRatio(a: string, b: string): number {
  const la = luminance(a);
  const lb = luminance(b);
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}

export const isDark = (hex: string): boolean => luminance(hex) < 0.36;

/** Black or white, whichever reads better on the colour. */
export function inkOn(hex: string): string {
  return contrastRatio(hex, '#000000') >= contrastRatio(hex, '#ffffff') ? '#000000' : '#ffffff';
}

/** Moves a colour towards black or white until it reaches the contrast ratio against the background. */
export function ensureContrast(fg: string, bg: string, ratio: number): string {
  if (contrastRatio(fg, bg) >= ratio) return fg;
  const target = isDark(bg) ? '#ffffff' : '#000000';
  for (let step = 1; step <= 20; step++) {
    const candidate = mix(fg, target, step / 20);
    if (contrastRatio(candidate, bg) >= ratio) return candidate;
  }
  return target;
}

export const withAlpha = (hex: string, alpha: number): string => {
  const [r, g, b] = toRgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${Math.min(1, Math.max(0, alpha))})`;
};
