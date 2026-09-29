/** The "Top Ten" of the original game: the best result is the reigning king. */
export interface HallEntry {
  name: string;
  score: number;
  /** Epoch milliseconds. */
  at: number;
}

export const HALL_SIZE = 10;
export const NAME_LIMIT = 12;
export const DEFAULT_KING: HallEntry = { name: 'Handicap', score: 100, at: 0 };

export function kingOf(hall: readonly HallEntry[]): HallEntry {
  return hall[0] ?? DEFAULT_KING;
}

/** Position (0 = king) a score would take in the table, or -1 when it does not make the top ten. */
export function rankOf(hall: readonly HallEntry[], score: number): number {
  if (score <= 0) return -1;
  const place = hall.filter((h) => h.score >= score).length;
  return place < HALL_SIZE ? place : -1;
}

/** Adds a result; on equal scores the older one stays above. */
export function insertScore(hall: readonly HallEntry[], entry: HallEntry): HallEntry[] {
  if (entry.score <= 0) return [...hall];
  const place = rankOf(hall, entry.score);
  if (place === -1) return [...hall];
  const clean = { ...entry, name: entry.name.slice(0, NAME_LIMIT) };
  return [...hall.slice(0, place), clean, ...hall.slice(place)].slice(0, HALL_SIZE);
}

export function sanitizeHall(raw: unknown): HallEntry[] {
  if (!Array.isArray(raw)) return [];
  const out: HallEntry[] = [];
  for (const item of raw) {
    if (typeof item !== 'object' || item === null) continue;
    const r = item as Partial<HallEntry>;
    if (
      typeof r.name === 'string' &&
      typeof r.score === 'number' && Number.isInteger(r.score) && r.score >= 0 &&
      typeof r.at === 'number' && Number.isInteger(r.at) && r.at >= 0
    ) {
      out.push({ name: r.name.slice(0, NAME_LIMIT), score: r.score, at: r.at });
    }
  }
  return out.sort((a, b) => b.score - a.score || a.at - b.at).slice(0, HALL_SIZE);
}
