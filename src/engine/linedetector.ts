import { Board } from './board';
import { Point, pointKey } from './models';

export interface LineMatchResult {
  lines: Point[][];
  matchedPoints: Point[];
  score: number;
}

export type ScoringSystem = 'gamos' | 'lines98';

const AXES: Point[] = [
  { x: 1, y: 0 },  // Horizontal
  { x: 0, y: 1 },  // Vertical
  { x: 1, y: 1 },  // Diagonal Down-Right (\)
  { x: 1, y: -1 }  // Diagonal Up-Right (/)
];

export class LineDetector {
  static readonly DEFAULT_MIN_LENGTH = 5;

  static calculateScore(lineLength: number, system: ScoringSystem = 'gamos'): number {
    if (lineLength < LineDetector.DEFAULT_MIN_LENGTH) return 0;
    if (system === 'gamos') {
      // Color Lines 1992 (Gamos): 2*L^2 - 20*L + 60
      return 2 * lineLength * lineLength - 20 * lineLength + 60;
    }
    // Lines 98 classic table
    switch (lineLength) {
      case 5: return 10;
      case 6: return 12;
      case 7: return 18;
      case 8: return 28;
      default: return 42 + (lineLength - 9) * 16;
    }
  }

  static findLines(
    board: Board,
    minLength = LineDetector.DEFAULT_MIN_LENGTH,
    system: ScoringSystem = 'gamos'
  ): LineMatchResult {
    const foundLines: Point[][] = [];
    const matchedKeys = new Set<string>();
    const matchedPoints: Point[] = [];
    const size = board.size;

    for (const axis of AXES) {
      const visitedInDirection: boolean[][] = Array.from({ length: size }, () =>
        Array(size).fill(false)
      );

      for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
          if (visitedInDirection[y][x]) continue;

          const startColor = board.get(x, y);
          if (!startColor) continue;

          const currentLine: Point[] = [];
          let cx = x;
          let cy = y;

          while (board.isInside(cx, cy) && board.get(cx, cy) === startColor) {
            currentLine.push({ x: cx, y: cy });
            visitedInDirection[cy][cx] = true;
            cx += axis.x;
            cy += axis.y;
          }

          if (currentLine.size ? currentLine.length >= minLength : currentLine.length >= minLength) {
            foundLines.push(currentLine);
            for (const pt of currentLine) {
              const k = pointKey(pt);
              if (!matchedKeys.has(k)) {
                matchedKeys.add(k);
                matchedPoints.push(pt);
              }
            }
          }
        }
      }
    }

    let totalScore = 0;
    for (const line of foundLines) {
      totalScore += LineDetector.calculateScore(line.length, system);
    }

    return {
      lines: foundLines,
      matchedPoints,
      score: totalScore
    };
  }
}
