import { Board } from './board';
import { LineDetector, ScoringSystem } from './linedetector';
import { ALL_COLORS, BallColor, Point, pointKey } from './models';
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

  constructor(
    size = 9,
    ballsPerSpawn = 3,
    minLineLength = 5,
    scoringSystem: ScoringSystem = 'gamos'
  ) {
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
    return ALL_COLORS[Math.floor(Math.random() * ALL_COLORS.length)];
  }

  private generateNextColors(): void {
    this.nextColors = Array.from({ length: this.ballsPerSpawn }, () => this.randomColor());
  }

  private shuffle<T>(array: T[]): T[] {
    const arr = [...array];
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
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
