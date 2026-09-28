import { Board } from './board';
import { LineDetector } from './linedetector';
import type { ScoringSystem } from './linedetector';
import { ALL_COLORS } from './models';
import type { BallColor, Point } from './models';
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

export type Rng = () => number;

/** Serializable game state. The undo history is intentionally not persisted. */
export interface GameState {
  size: number;
  board: (BallColor | null)[][];
  score: number;
  nextColors: BallColor[];
  isGameOver: boolean;
}

interface GameSnapshot {
  board: Board;
  score: number;
  nextColors: BallColor[];
}

export class GameEngine {
  readonly size: number;
  readonly ballsPerSpawn: number;
  readonly minLineLength: number;
  readonly scoringSystem: ScoringSystem;

  board: Board;
  score: number = 0;
  bestScore: number = 0;
  isGameOver: boolean = false;
  selectedPoint: Point | null = null;
  nextColors: BallColor[] = [];

  private undoStack: GameSnapshot[] = [];
  private readonly rng: Rng;

  constructor(
    size = 9,
    ballsPerSpawn = 3,
    minLineLength = 5,
    scoringSystem: ScoringSystem = 'gamos',
    rng: Rng = Math.random
  ) {
    this.rng = rng;
    this.size = size;
    this.ballsPerSpawn = ballsPerSpawn;
    this.minLineLength = minLineLength;
    this.scoringSystem = scoringSystem;
    this.board = new Board(size);
    this.startNewGame();
  }

  startNewGame(): void {
    this.board.clear();
    this.score = 0;
    this.isGameOver = false;
    this.selectedPoint = null;
    this.undoStack = [];

    this.generateNextColors();

    // Spawn 5 initial balls
    const emptyCells = this.shuffle(this.board.getEmptyCells());
    const initialCount = Math.min(5, emptyCells.length);
    for (let i = 0; i < initialCount; i++) {
      const color = this.randomColor();
      this.board.set(emptyCells[i].x, emptyCells[i].y, color);
    }

    this.generateNextColors();
  }

  private randomColor(): BallColor {
    return ALL_COLORS[Math.floor(this.rng() * ALL_COLORS.length)];
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
      isGameOver: this.isGameOver
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

    const engine = new GameEngine(s.size, 3, 5, 'gamos', rng);
    if (!Array.isArray(s.nextColors) || s.nextColors.length !== engine.ballsPerSpawn) return null;
    if (!s.nextColors.every(isColor)) return null;

    engine.board.clear();
    s.board.forEach((row, y) => row.forEach((c, x) => engine.board.set(x, y, c)));
    engine.score = s.score;
    engine.nextColors = [...s.nextColors];
    engine.isGameOver = s.isGameOver;
    engine.undoStack = [];
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
      return { success: false, clearedPoints: [], pointsEarned: 0, spawnedBalls: [], isGameOver: false, reason: 'Path blocked' };
    }

    // Save snapshot for undo
    this.saveUndoSnapshot();

    // Execute move
    this.board.set(from.x, from.y, null);
    this.board.set(to.x, to.y, movingColor);

    // 1. Check lines
    const match = LineDetector.findLines(this.board, this.minLineLength, this.scoringSystem);

    if (match.matchedPoints.length > 0) {
      for (const p of match.matchedPoints) {
        this.board.set(p.x, p.y, null);
      }
      this.score += match.score;
      if (this.score > this.bestScore) {
        this.bestScore = this.score;
      }

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
    const empty = this.shuffle(this.board.getEmptyCells());
    const spawned: SpawnedBall[] = [];
    const count = Math.min(this.nextColors.length, empty.length);

    for (let i = 0; i < count; i++) {
      const pt = empty[i];
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
      if (this.score > this.bestScore) {
        this.bestScore = this.score;
      }
    }

    // 4. Update next turn colors
    this.generateNextColors();

    if (this.board.getEmptyCells().length === 0) {
      this.isGameOver = true;
    }

    return {
      success: true,
      path,
      clearedPoints: postMatch.matchedPoints,
      pointsEarned: postScore,
      spawnedBalls: spawned,
      isGameOver: this.isGameOver
    };
  }

  private saveUndoSnapshot(): void {
    if (this.undoStack.length >= 20) {
      this.undoStack.shift();
    }
    this.undoStack.push({
      board: this.board.copy(),
      score: this.score,
      nextColors: [...this.nextColors]
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
    this.nextColors = [...snap.nextColors];
    this.selectedPoint = null;
    this.isGameOver = false;
    return true;
  }
}
