/**
 * How a game was played, not only how it ended: decision times, hesitation, how close the board came to filling up.
 * Everything is a plain non-negative integer so it fits a compact record and survives export, import and old saves.
 * Both apps count the same things in the same way (see Telemetry.kt), which keeps the cognitive analysis comparable.
 */
export const COG_KEYS = [
  'tz', // local time zone of the device, in quarter hours from UTC + 100 (always positive)
  'tm', // moves with a measured decision time
  'think', // summed decision time of those moves, ms
  'thinkMax', // the longest decision, ms
  'thinkSq', // sum of squares of decision times in 0.1 s units: gives the spread without storing every move
  'fast', // decisions under FAST_MS
  'slow', // decisions over SLOW_MS
  'lat', // summed time from the board settling to the first touch, ms
  'first', // decision time of the first measured move, ms
  'p1', // summed decision time in moves 1..20
  'n1',
  'p2', // moves 21..60
  'n2',
  'p3', // moves 61+
  'n3',
  'undo',
  'hint',
  'miss', // touches on a cell the ball could not reach
  'clears', // moves that cleared at least one line
  'multi', // moves that cleared two or more lines at once
  'danger', // moves that left 12 or fewer free cells
  'minEmpty', // fewest free cells seen after a move
] as const;

export type CogKey = (typeof COG_KEYS)[number];
export type Cognition = Record<CogKey, number>;

export const FAST_MS = 2000;
export const SLOW_MS = 10000;
export const DANGER_CELLS = 12;
/** A decision longer than this is a break, not thinking, and is clipped. */
export const MAX_THINK_MS = 60_000;

export const emptyCognition = (): Cognition => {
  const c = {} as Cognition;
  for (const k of COG_KEYS) c[k] = 0;
  c.minEmpty = 81;
  return c;
};

export const timeZoneCode = (now: Date): number => Math.round(-now.getTimezoneOffset() / 15) + 100;

/** Minutes from UTC of a stored code. */
export const offsetMinutes = (tz: number): number => (tz - 100) * 15;

export class Telemetry {
  readonly c: Cognition = emptyCognition();
  /** Time since the last finished move, and whether it was measured. */
  private pend = 0;
  private lat = -1;
  private unknown = false;

  /** A touch on the board; `deltaMs` is the time since the previous touch, null when that is not known. */
  action(deltaMs: number | null): void {
    if (deltaMs === null || !Number.isFinite(deltaMs) || deltaMs < 0) {
      this.unknown = true;
      if (this.lat < 0) this.lat = 0;
      return;
    }
    const ms = Math.min(Math.round(deltaMs), MAX_THINK_MS);
    this.pend += ms;
    if (this.lat < 0) this.lat = ms;
  }

  /** A move was made. `lines` counts the lines it cleared, `empty` the free cells afterwards. */
  moved(moves: number, lines: number, empty: number): void {
    const c = this.c;
    if (!this.unknown && this.pend > 0) {
      const ms = this.pend;
      c.tm++;
      c.think += ms;
      c.thinkMax = Math.max(c.thinkMax, ms);
      c.thinkSq += Math.round((ms / 100) ** 2);
      if (ms < FAST_MS) c.fast++;
      if (ms > SLOW_MS) c.slow++;
      c.lat += Math.max(0, this.lat);
      if (c.tm === 1) c.first = ms;
      if (moves <= 20) {
        c.p1 += ms;
        c.n1++;
      } else if (moves <= 60) {
        c.p2 += ms;
        c.n2++;
      } else {
        c.p3 += ms;
        c.n3++;
      }
    }
    if (lines > 0) c.clears++;
    if (lines > 1) c.multi++;
    if (empty <= DANGER_CELLS) c.danger++;
    c.minEmpty = Math.min(c.minEmpty, empty);
    this.pend = 0;
    this.lat = -1;
    this.unknown = false;
  }

  undo(): void {
    this.c.undo++;
  }
  hint(): void {
    this.c.hint++;
  }
  miss(): void {
    this.c.miss++;
  }

  /** The numbers in COG_KEYS order, minus the time zone, for a saved game. */
  toArray(): number[] {
    return COG_KEYS.filter((k) => k !== 'tz').map((k) => this.c[k]);
  }

  static fromArray(raw: unknown): Telemetry {
    const t = new Telemetry();
    const keys = COG_KEYS.filter((k) => k !== 'tz');
    if (Array.isArray(raw) && raw.length === keys.length && raw.every((v) => typeof v === 'number' && Number.isInteger(v) && v >= 0)) {
      keys.forEach((k, i) => (t.c[k] = raw[i] as number));
    }
    return t;
  }

  snapshot(tz: number): Cognition {
    return { ...this.c, tz };
  }
}

/** Validates untrusted stored data: absent or malformed becomes undefined (the game simply has no play data). */
export function sanitizeCognition(raw: unknown): Cognition | undefined {
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) return undefined;
  const o = raw as Record<string, unknown>;
  const out = emptyCognition();
  for (const k of COG_KEYS) {
    const v = o[k];
    if (v === undefined && k === 'minEmpty') continue;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > 1e12) return undefined;
    out[k] = v;
  }
  if (out.tz > 400) return undefined;
  out.minEmpty = Math.min(out.minEmpty, 81);
  return out;
}

/** Local hour (0-23) and weekday (0 = Monday) of an instant seen from a stored time zone. */
export function localTime(at: number, tz: number | undefined): { hour: number; weekday: number } {
  const offset = tz === undefined ? -new Date(at).getTimezoneOffset() : offsetMinutes(tz);
  const d = new Date(at + offset * 60_000);
  return { hour: d.getUTCHours(), weekday: (d.getUTCDay() + 6) % 7 };
}
