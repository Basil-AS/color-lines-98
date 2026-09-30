import type { GameEngine } from './engine/gameengine';
import { isModeId } from './engine/modes';
import type { ModeId } from './engine/modes';

export const HISTORY_LIMIT = 1000;

export interface GameRecord {
  score: number;
  /** Epoch milliseconds when the game ended. */
  endedAt: number;
  moves: number;
  /** Lines cleared. */
  lines: number;
  /** Balls cleared. */
  balls: number;
  /** false when the player abandoned the game with "new game". */
  completed: boolean;
  /** Length of the longest line cleared. */
  maxLine: number;
  /** Active play time in milliseconds. */
  durationMs: number;
  /** Which mode was played. */
  mode: ModeId;
}

export interface StatsSummary {
  gamesPlayed: number;
  completedGames: number;
  bestScore: number;
  averageScore: number;
  totalScore: number;
  totalMoves: number;
  totalLines: number;
  lastScore: number | null;
  bestRecord: GameRecord | null;
}

export function recordFromEngine(engine: GameEngine, completed: boolean, now: number): GameRecord {
  return {
    score: engine.score,
    endedAt: now,
    moves: engine.moves,
    lines: engine.linesCleared,
    balls: engine.ballsCleared,
    completed,
    maxLine: engine.maxLine,
    durationMs: engine.playMs,
    mode: engine.mode,
  };
}

/** Newest first, capped at HISTORY_LIMIT. */
export function addRecord(history: readonly GameRecord[], record: GameRecord): GameRecord[] {
  return [record, ...history].slice(0, HISTORY_LIMIT);
}

export function summarize(history: readonly GameRecord[]): StatsSummary {
  let totalScore = 0;
  let totalMoves = 0;
  let totalLines = 0;
  let completedGames = 0;
  let bestRecord: GameRecord | null = null;
  for (const r of history) {
    totalScore += r.score;
    totalMoves += r.moves;
    totalLines += r.lines;
    if (r.completed) completedGames++;
    if (bestRecord === null || r.score > bestRecord.score) bestRecord = r;
  }
  return {
    gamesPlayed: history.length,
    completedGames,
    bestScore: bestRecord?.score ?? 0,
    averageScore: history.length === 0 ? 0 : Math.round(totalScore / history.length),
    totalScore,
    totalMoves,
    totalLines,
    lastScore: history[0]?.score ?? null,
    bestRecord,
  };
}

export function isNewRecord(score: number, previousBest: number): boolean {
  return score > 0 && score > previousBest;
}

const isCount = (v: unknown): v is number => typeof v === 'number' && Number.isInteger(v) && v >= 0;

/** Validates untrusted stored data; malformed entries are dropped. */
export function sanitizeHistory(raw: unknown): GameRecord[] {
  if (!Array.isArray(raw)) return [];
  const out: GameRecord[] = [];
  for (const item of raw) {
    if (typeof item !== 'object' || item === null) continue;
    const r = item as Partial<GameRecord>;
    const maxLine = r.maxLine === undefined ? 0 : r.maxLine;
    const durationMs = r.durationMs === undefined ? 0 : r.durationMs;
    const mode = r.mode === undefined ? 'classic' : r.mode;
    if (
      isModeId(mode) &&
      isCount(maxLine) &&
      isCount(durationMs) &&
      isCount(r.score) &&
      isCount(r.endedAt) &&
      isCount(r.moves) &&
      isCount(r.lines) &&
      isCount(r.balls) &&
      typeof r.completed === 'boolean'
    ) {
      out.push({
        score: r.score,
        endedAt: r.endedAt,
        moves: r.moves,
        lines: r.lines,
        balls: r.balls,
        completed: r.completed,
        maxLine,
        durationMs,
        mode,
      });
    }
  }
  return out.slice(0, HISTORY_LIMIT);
}
