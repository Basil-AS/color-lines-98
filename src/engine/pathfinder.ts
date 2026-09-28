import { Board } from './board';
import { Point, pointKey, pointsEqual } from './models';

const DIRECTIONS: Point[] = [
  { x: 1, y: 0 },
  { x: -1, y: 0 },
  { x: 0, y: 1 },
  { x: 0, y: -1 }
];

export class PathFinder {
  /**
   * BFS (Lee Algorithm) to find the shortest path from start to target.
   */
  static findPath(start: Point, target: Point, board: Board): Point[] | null {
    if (!board.isInside(start.x, start.y) || !board.isInside(target.x, target.y)) {
      return null;
    }
    if (pointsEqual(start, target)) {
      return [start];
    }
    if (!board.isEmpty(target.x, target.y)) {
      return null;
    }

    const queue: Point[] = [start];
    const visited = new Set<string>([pointKey(start)]);
    const cameFrom = new Map<string, Point>();

    while (queue.length > 0) {
      const current = queue.shift()!;
      if (pointsEqual(current, target)) {
        // Reconstruct path
        const path: Point[] = [];
        let curr: Point | undefined = target;
        while (curr) {
          path.push(curr);
          curr = cameFrom.get(pointKey(curr));
        }
        return path.reverse();
      }

      for (const dir of DIRECTIONS) {
        const next: Point = { x: current.x + dir.x, y: current.y + dir.y };
        const key = pointKey(next);

        if (board.isInside(next.x, next.y) && !visited.has(key)) {
          if (board.isEmpty(next.x, next.y) || pointsEqual(next, target)) {
            visited.add(key);
            cameFrom.set(key, current);
            queue.push(next);
          }
        }
      }
    }

    return null;
  }

  /**
   * Finds all empty cells reachable from the start point.
   */
  static getReachableCells(start: Point, board: Board): Set<string> {
    const reachable = new Set<string>();
    if (!board.isInside(start.x, start.y)) return reachable;

    const queue: Point[] = [start];
    const visited = new Set<string>([pointKey(start)]);

    while (queue.length > 0) {
      const current = queue.shift()!;
      for (const dir of DIRECTIONS) {
        const next: Point = { x: current.x + dir.x, y: current.y + dir.y };
        const key = pointKey(next);

        if (board.isInside(next.x, next.y) && !visited.has(key) && board.isEmpty(next.x, next.y)) {
          visited.add(key);
          reachable.add(key);
          queue.push(next);
        }
      }
    }

    return reachable;
  }
}
