import type { BallColor, Point } from './models';

export class Board {
  readonly size: number;
  private grid: (BallColor | null)[][];

  constructor(size = 9) {
    this.size = size;
    this.grid = Array.from({ length: size }, () => Array(size).fill(null));
  }

  isInside(x: number, y: number): boolean {
    return x >= 0 && x < this.size && y >= 0 && y < this.size;
  }

  get(x: number, y: number): BallColor | null {
    if (!this.isInside(x, y)) return null;
    return this.grid[y][x];
  }

  set(x: number, y: number, color: BallColor | null): void {
    if (!this.isInside(x, y)) {
      throw new Error(`Coordinates (${x}, ${y}) out of bounds`);
    }
    this.grid[y][x] = color;
  }

  isEmpty(x: number, y: number): boolean {
    return this.isInside(x, y) && this.grid[y][x] === null;
  }

  getEmptyCells(): Point[] {
    const list: Point[] = [];
    for (let y = 0; y < this.size; y++) {
      for (let x = 0; x < this.size; x++) {
        if (this.grid[y][x] === null) {
          list.push({ x, y });
        }
      }
    }
    return list;
  }

  clear(): void {
    for (let y = 0; y < this.size; y++) {
      for (let x = 0; x < this.size; x++) {
        this.grid[y][x] = null;
      }
    }
  }

  copy(): Board {
    const newBoard = new Board(this.size);
    for (let y = 0; y < this.size; y++) {
      for (let x = 0; x < this.size; x++) {
        newBoard.set(x, y, this.grid[y][x]);
      }
    }
    return newBoard;
  }
}
