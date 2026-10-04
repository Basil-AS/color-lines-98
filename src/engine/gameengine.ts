import { Telemetry } from './telemetry';
import { Board } from './board';
import { LineDetector } from './linedetector';
import type { ScoringSystem } from './linedetector';
import { ALL_COLORS } from './models';
import { isModeId } from './modes';
import type { ModeId } from './modes';
import { mixSeed, mulberry32 } from './rng';
import type { Rng } from './rng';
import type { BallColor, Point } from './models';
import { pointKey } from './models';
import { PathFinder } from './pathfinder';

export interface SpawnedBall {
  point: Point;
  color: BallColor;
}

export interface MoveResult {
  success: boolean;
  path?: Point[];
  clearedPoints: Point[];
  pointsEarned: number;
  spawnedBalls: SpawnedBall[];
  isGameOver: boolean;
  reason?: string;
}

export type { Rng } from './rng';

/** Serializable game state. The undo history is intentionally not persisted. */
export interface GameState {
  size: number;
  board: (BallColor | null)[][];
  score: number;
  nextColors: BallColor[];
  isGameOver: boolean;
  /** Optional so older saves still load: they are classic games. */
  mode?: ModeId;
  colors?: BallColor[];
  seed?: number;
  /** Optional so saves written before these counters existed still load. */
  /** Cells where the next balls will appear. Optional so older saves still load. */
  nextPoints?: Point[];
  moves?: number;
  linesCleared?: number;
  ballsCleared?: number;
  maxLine?: number;
  playMs?: number;
  /** Play data (see telemetry.ts); optional so older saves still load. */
  tel?: number[];
}

interface GameSnapshot {
  board: Board;
  score: number;
  nextColors: BallColor[];
  nextPoints: Point[];
  moves: number;
  linesCleared: number;
  ballsCleared: number;
  maxLine: number;
}

export class GameEngine {
  readonly size: number;
  readonly ballsPerSpawn: number;
  readonly minLineLength: number;
  readonly scoringSystem: ScoringSystem;

  board: Board;
  score: number = 0;
  bestScore: number = 0;
  moves: number = 0;
  linesCleared: number = 0;
  ballsCleared: number = 0;
  /** Length of the longest line cleared in this game. */
  maxLine: number = 0;
  /** Active play time in milliseconds, reported by the UI through addPlayTime. */
  playMs: number = 0;
  /** How the game is being played: decision times, hesitation, danger. */
  tel: Telemetry = new Telemetry();
  isGameOver: boolean = false;
  selectedPoint: Point | null = null;
  nextColors: BallColor[] = [];
  nextSpawnPoints: Point[] = [];

  private undoStack: GameSnapshot[] = [];
  private rng: Rng;
  private seed?: number;
  readonly colors: readonly BallColor[];
  mode: ModeId = 'classic';

  constructor(
    size = 9,
    ballsPerSpawn = 3,
    minLineLength = 5,
    scoringSystem: ScoringSystem = 'gamos',
    rng: Rng = Math.random,
    options: { colors?: readonly BallColor[]; mode?: ModeId; seed?: number } = {}
  ) {
    this.rng = rng;
    this.colors = options.colors ?? ALL_COLORS;
    this.mode = options.mode ?? 'classic';
    this.seed = options.seed;
    this.size = size;
    this.ballsPerSpawn = ballsPerSpawn;
    this.minLineLength = minLineLength;
    this.scoringSystem = scoringSystem;
    this.board = new Board(size);
    this.startNewGame();
  }

  startNewGame(): void {
    this.board.clear();
    this.moves = 0;
    this.reseedTurn();
    this.score = 0;
    this.moves = 0;
    this.linesCleared = 0;
    this.ballsCleared = 0;
    this.maxLine = 0;
    this.playMs = 0;
    this.tel = new Telemetry();
    this.isGameOver = false;
    this.selectedPoint = null;
    this.undoStack = [];

    this.generateNextColors();

    // Spawn 5 initial balls; the original never starts with a finished line, so re-deal if one appears.
    for (let attempt = 0; attempt < 50; attempt++) {
      this.board.clear();
      const emptyCells = this.shuffle(this.board.getEmptyCells());
      const initialCount = Math.min(5, emptyCells.length);
      for (let i = 0; i < initialCount; i++) {
        this.board.set(emptyCells[i].x, emptyCells[i].y, this.randomColor());
      }
      if (LineDetector.findLines(this.board, this.minLineLength, this.scoringSystem).matchedPoints.length === 0) break;
    }

    this.generateNextColors();
    this.planSpawnPoints();
  }

  /** Chooses the cells where the next balls will appear, so the UI can preview them. */
  private planSpawnPoints(): void {
    const empty = this.shuffle(this.board.getEmptyCells());
    this.nextSpawnPoints = empty.slice(0, Math.min(this.ballsPerSpawn, empty.length));
  }

  /** Keeps the still-free planned cells and replaces those the player just occupied. */
  private repairSpawnPlan(): void {
    const kept = this.nextSpawnPoints.filter((p) => this.board.isEmpty(p.x, p.y));
    const used = new Set(kept.map(pointKey));
    const empty = this.board.getEmptyCells();
    const want = Math.min(this.ballsPerSpawn, empty.length);
    if (kept.length < want) {
      for (const c of this.shuffle(empty)) {
        if (kept.length >= want) break;
        if (!used.has(pointKey(c))) kept.push(c);
      }
    }
    this.nextSpawnPoints = kept;
  }

  private randomColor(): BallColor {
    return this.colors[Math.floor(this.rng() * this.colors.length)];
  }

  private generateNextColors(): void {
    this.nextColors = Array.from({ length: this.ballsPerSpawn }, () => this.randomColor());
  }

  private shuffle<T>(array: T[]): T[] {
    const arr = [...array];
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(this.rng() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  getState(): GameState {
    const board: (BallColor | null)[][] = [];
    for (let y = 0; y < this.size; y++) {
      const row: (BallColor | null)[] = [];
      for (let x = 0; x < this.size; x++) {
        row.push(this.board.get(x, y));
      }
      board.push(row);
    }
    return {
      size: this.size,
      board,
      score: this.score,
      nextColors: [...this.nextColors],
      nextPoints: this.nextSpawnPoints.map((p) => ({ ...p })),
      isGameOver: this.isGameOver,
      mode: this.mode,
      colors: [...this.colors],
      ...(this.seed === undefined ? {} : { seed: this.seed }),
      moves: this.moves,
      linesCleared: this.linesCleared,
      ballsCleared: this.ballsCleared,
      maxLine: this.maxLine,
      playMs: this.playMs,
      tel: this.tel.toArray()
    };
  }

  /** Rebuilds an engine from untrusted data; returns null when it is not a valid state. */
  static fromState(data: unknown, rng: Rng = Math.random): GameEngine | null {
    if (typeof data !== 'object' || data === null) return null;
    const s = data as Partial<GameState>;
    const isColor = (v: unknown): v is BallColor =>
      typeof v === 'string' && (ALL_COLORS as string[]).includes(v);

    if (s.size !== 9 || !Array.isArray(s.board) || s.board.length !== s.size) return null;
    for (const row of s.board) {
      if (!Array.isArray(row) || row.length !== s.size) return null;
      if (!row.every((c) => c === null || isColor(c))) return null;
    }
    if (typeof s.score !== 'number' || !Number.isInteger(s.score) || s.score < 0) return null;
    if (typeof s.isGameOver !== 'boolean') return null;
    const counter = (v: unknown): number | null =>
      v === undefined ? 0 : typeof v === 'number' && Number.isInteger(v) && v >= 0 ? v : null;
    const moves = counter(s.moves);
    const linesCleared = counter(s.linesCleared);
    const ballsCleared = counter(s.ballsCleared);
    const maxLine = counter(s.maxLine);
    const playMs = counter(s.playMs);
    if (moves === null || linesCleared === null || ballsCleared === null) return null;
    if (maxLine === null || playMs === null) return null;

    const mode = s.mode === undefined ? 'classic' : s.mode;
    if (!isModeId(mode)) return null;
    let colors: readonly BallColor[] = ALL_COLORS;
    if (s.colors !== undefined) {
      if (!Array.isArray(s.colors) || s.colors.length < 3 || !s.colors.every(isColor)) return null;
      if (new Set(s.colors).size !== s.colors.length) return null;
      colors = s.colors;
    }
    if (s.seed !== undefined && !(typeof s.seed === 'number' && Number.isInteger(s.seed) && s.seed >= 0)) return null;
    const engine = new GameEngine(s.size, 3, 5, 'gamos', rng, { colors, mode, seed: s.seed });
    if (!Array.isArray(s.nextColors) || s.nextColors.length !== engine.ballsPerSpawn) return null;
    if (!s.nextColors.every(isColor)) return null;

    let nextPoints: Point[] | null = null;
    if (s.nextPoints !== undefined) {
      if (!Array.isArray(s.nextPoints)) return null;
      const seen = new Set<string>();
      let emptyCount = 0;
      for (const row of s.board) for (const c of row) if (c === null) emptyCount++;
      if (s.nextPoints.length !== Math.min(engine.ballsPerSpawn, emptyCount)) return null;
      for (const p of s.nextPoints) {
        const q = p as Partial<Point> | null;
        if (!q || !Number.isInteger(q.x) || !Number.isInteger(q.y)) return null;
        const x = q.x as number;
        const y = q.y as number;
        if (x < 0 || y < 0 || x >= s.size || y >= s.size) return null;
        if (s.board[y][x] !== null || seen.has(pointKey({ x, y }))) return null;
        seen.add(pointKey({ x, y }));
      }
      nextPoints = s.nextPoints.map((p) => ({ x: p.x, y: p.y }));
    }

    engine.board.clear();
    s.board.forEach((row, y) => row.forEach((c, x) => engine.board.set(x, y, c)));
    engine.score = s.score;
    engine.nextColors = [...s.nextColors];
    engine.isGameOver = s.isGameOver;
    engine.moves = moves;
    engine.linesCleared = linesCleared;
    engine.ballsCleared = ballsCleared;
    engine.maxLine = maxLine;
    engine.playMs = playMs;
    engine.tel = Telemetry.fromArray(s.tel);
    engine.undoStack = [];
    engine.reseedTurn();
    if (nextPoints) engine.nextSpawnPoints = nextPoints;
    else engine.planSpawnPoints();
    return engine;
  }

  selectCell(point: Point): boolean {
    if (this.isGameOver) return false;
    if (!this.board.isInside(point.x, point.y)) return false;

    const color = this.board.get(point.x, point.y);
    if (color) {
      this.selectedPoint = point;
      return true;
    }

    if (this.selectedPoint) {
      const res = this.moveBall(this.selectedPoint, point);
      if (res.success) {
        this.selectedPoint = null;
      }
      return res.success;
    }

    return false;
  }

  select(point: Point): void {
    if (this.board.get(point.x, point.y)) {
      this.selectedPoint = point;
    }
  }

  unselect(): void {
    this.selectedPoint = null;
  }

  getReachableCells(): Set<string> {
    if (!this.selectedPoint) return new Set();
    return PathFinder.getReachableCells(this.selectedPoint, this.board);
  }

  moveBall(from: Point, to: Point): MoveResult {
    if (this.isGameOver) {
      return { success: false, clearedPoints: [], pointsEarned: 0, spawnedBalls: [], isGameOver: true, reason: 'Game over' };
    }

    const movingColor = this.board.get(from.x, from.y);
    if (!movingColor) {
      return { success: false, clearedPoints: [], pointsEarned: 0, spawnedBalls: [], isGameOver: false, reason: 'No ball at source' };
    }

    if (!this.board.isEmpty(to.x, to.y)) {
      return { success: false, clearedPoints: [], pointsEarned: 0, spawnedBalls: [], isGameOver: false, reason: 'Target not empty' };
    }

    const path = PathFinder.findPath(from, to, this.board);
    if (!path) {
      this.tel.miss();
      return { success: false, clearedPoints: [], pointsEarned: 0, spawnedBalls: [], isGameOver: false, reason: 'Path blocked' };
    }

    // Save snapshot for undo
    this.saveUndoSnapshot();

    // Execute move
    const linesBefore = this.linesCleared;
    this.moves++;
    this.reseedTurn();
    this.board.set(from.x, from.y, null);
    this.board.set(to.x, to.y, movingColor);
    this.repairSpawnPlan();

    // 1. Check lines
    const match = LineDetector.findLines(this.board, this.minLineLength, this.scoringSystem);

    if (match.matchedPoints.length > 0) {
      for (const p of match.matchedPoints) {
        this.board.set(p.x, p.y, null);
      }
      this.score += match.score;
      this.linesCleared += match.lines.length;
      this.ballsCleared += match.matchedPoints.length;
      this.noteLongestLine(match.lines);
      if (this.score > this.bestScore) {
        this.bestScore = this.score;
      }

      this.tel.moved(this.moves, this.linesCleared - linesBefore, this.board.getEmptyCells().length);
      return {
        success: true,
        path,
        clearedPoints: match.matchedPoints,
        pointsEarned: match.score,
        spawnedBalls: [],
        isGameOver: false
      };
    }

    // 2. Spawn 3 new balls
    const spawned: SpawnedBall[] = [];
    const count = Math.min(this.nextColors.length, this.nextSpawnPoints.length);

    for (let i = 0; i < count; i++) {
      const pt = this.nextSpawnPoints[i];
      const color = this.nextColors[i];
      this.board.set(pt.x, pt.y, color);
      spawned.push({ point: pt, color });
    }

    // 3. Check if spawned balls completed any lines
    const postMatch = LineDetector.findLines(this.board, this.minLineLength, this.scoringSystem);
    let postScore = 0;
    if (postMatch.matchedPoints.length > 0) {
      for (const p of postMatch.matchedPoints) {
        this.board.set(p.x, p.y, null);
      }
      postScore = postMatch.score;
      this.score += postScore;
      this.linesCleared += postMatch.lines.length;
      this.ballsCleared += postMatch.matchedPoints.length;
      this.noteLongestLine(postMatch.lines);
      if (this.score > this.bestScore) {
        this.bestScore = this.score;
      }
    }

    // 4. Update next turn colors and where they will appear
    this.generateNextColors();
    this.planSpawnPoints();

    if (this.board.getEmptyCells().length === 0) {
      this.isGameOver = true;
    }

    this.tel.moved(this.moves, this.linesCleared - linesBefore, this.board.getEmptyCells().length);
    return {
      success: true,
      path,
      clearedPoints: postMatch.matchedPoints,
      pointsEarned: postScore,
      spawnedBalls: spawned,
      isGameOver: this.isGameOver
    };
  }

  private noteLongestLine(lines: Point[][]): void {
    for (const line of lines) this.maxLine = Math.max(this.maxLine, line.length);
  }

  /** Ends the game right now (a timed mode ran out of time). */
  endGame(): void {
    this.isGameOver = true;
    this.selectedPoint = null;
    this.undoStack = [];
  }

  /** In seeded games every turn draws from a generator that depends only on the seed and the move number. */
  private reseedTurn(): void {
    if (this.seed !== undefined) this.rng = mulberry32(mixSeed(this.seed, this.moves));
  }

  /** Adds active play time; negative or non-finite values are ignored. */
  addPlayTime(ms: number): void {
    if (Number.isFinite(ms) && ms > 0) this.playMs += Math.round(ms);
  }

  private saveUndoSnapshot(): void {
    if (this.undoStack.length >= 20) {
      this.undoStack.shift();
    }
    this.undoStack.push({
      board: this.board.copy(),
      score: this.score,
      nextColors: [...this.nextColors],
      nextPoints: this.nextSpawnPoints.map((p) => ({ ...p })),
      moves: this.moves,
      linesCleared: this.linesCleared,
      ballsCleared: this.ballsCleared,
      maxLine: this.maxLine
    });
  }

  get canUndo(): boolean {
    return this.undoStack.length > 0 && !this.isGameOver;
  }

  undo(): boolean {
    if (!this.canUndo) return false;
    const snap = this.undoStack.pop()!;
    this.board = snap.board.copy();
    this.score = snap.score;
    this.moves = snap.moves;
    this.reseedTurn();
    this.linesCleared = snap.linesCleared;
    this.ballsCleared = snap.ballsCleared;
    this.maxLine = snap.maxLine;
    this.nextColors = [...snap.nextColors];
    this.nextSpawnPoints = snap.nextPoints.map((p) => ({ ...p }));
    this.selectedPoint = null;
    this.isGameOver = false;
    this.tel.undo();
    return true;
  }

  /** The UI reports every touch on the board with the time since the previous one (null when unknown). */
  noteAction(deltaMs: number | null): void {
    if (deltaMs !== null) this.addPlayTime(deltaMs);
    this.tel.action(deltaMs);
  }

  noteHint(): void {
    this.tel.hint();
  }
}
