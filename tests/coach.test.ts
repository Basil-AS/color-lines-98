import { describe, expect, it } from 'vitest';
import { PathFinder } from '../src/engine/pathfinder';
import { STRATEGY_LESSONS, strategyEngine, judgeStrategy } from '../src/coach';

describe('strategic practice on the real engine', () => {
  it('offers six distinct, bilingual puzzles with playable solutions', () => {
    expect(STRATEGY_LESSONS).toHaveLength(6);
    expect(new Set(STRATEGY_LESSONS.map(l => l.id)).size).toBe(6);
    for (const lesson of STRATEGY_LESSONS) {
      const engine = strategyEngine(lesson);
      const result = engine.moveBall(lesson.from, lesson.to);
      expect(result.success, lesson.id).toBe(true);
      expect(result.clearedPoints.length, lesson.id).toBe(lesson.expectedCleared);
      expect(judgeStrategy(lesson, lesson.from, lesson.to, result.success, result.clearedPoints.length)).toBe('done');
      for (const language of ['en', 'ru'] as const) for (const value of Object.values(lesson.text[language])) expect(value.length).toBeGreaterThan(10);
    }
  });
  it('accepts extending either open end of the initial pair', () => {
    const lesson = STRATEGY_LESSONS[0];
    const target = { x: 1, y: 4 };
    const result = strategyEngine(lesson).moveBall(lesson.from, target);
    expect(result.success).toBe(true);
    expect(judgeStrategy(lesson, lesson.from, target, result.success, result.clearedPoints.length)).toBe('done');
  });
  it('accepts the other diagonal end and every safe corridor relocation', () => {
    const diagonal = STRATEGY_LESSONS.find(l => l.id === 'diagonal')!;
    const target = { x: 0, y: 0 };
    const clear = strategyEngine(diagonal).moveBall(diagonal.from, target);
    expect(clear.success).toBe(true);
    expect(judgeStrategy(diagonal, diagonal.from, target, clear.success, clear.clearedPoints.length)).toBe('done');
    const corridor = STRATEGY_LESSONS.find(l => l.id === 'corridor')!;
    for (let y = 0; y < 9; y++) for (let x = 0; x < 9; x++) {
      if (x === 4 || (x === 3 && y === 4) || (x === 5 && y === 4)) continue;
      if (corridor.setup.balls.some(([bx, by]) => bx === x && by === y)) continue;
      const to = { x, y };
      const engine = strategyEngine(corridor);
      const result = engine.moveBall(corridor.from, to);
      expect(result.success).toBe(true);
      expect(judgeStrategy(corridor, corridor.from, to, result.success, result.clearedPoints.length), `${x},${y}`).toBe('done');
      expect(PathFinder.findPath({ x: 3, y: 4 }, { x: 5, y: 4 }, engine.board)).not.toBeNull();
    }
  });
  it('rejects moving the blocker into either mouth of the passage', () => {
    const lesson = STRATEGY_LESSONS.find(l => l.id === 'corridor')!;
    for (const to of [{ x: 3, y: 4 }, { x: 5, y: 4 }]) {
      const result = strategyEngine(lesson).moveBall(lesson.from, to);
      expect(result.success).toBe(true);
      expect(judgeStrategy(lesson, lesson.from, to, result.success, result.clearedPoints.length)).toBe('retry');
    }
  });
  it('rejects safe but irrelevant moves and failed or misreported solutions', () => {
    for (const lesson of STRATEGY_LESSONS) {
      const engine = strategyEngine(lesson);
      const wrong = lesson.id === 'corridor' ? { x: 0, y: 0 } : { x: 8, y: 7 };
      const result = engine.moveBall(lesson.id === 'corridor' ? { x: 1, y: 4 } : lesson.from, wrong);
      expect(result.success, lesson.id).toBe(true);
      expect(judgeStrategy(lesson, lesson.id === 'corridor' ? { x: 1, y: 4 } : lesson.from, wrong, result.success, result.clearedPoints.length)).toBe('retry');
      expect(judgeStrategy(lesson, lesson.from, lesson.to, false, lesson.expectedCleared)).toBe('retry');
      expect(judgeStrategy(lesson, lesson.from, lesson.to, true, lesson.expectedCleared + 1)).toBe('retry');
    }
  });
  it('opens the sealed corridor and uses the announced spawn to finish a line', () => {
    const corridor = STRATEGY_LESSONS.find(l => l.id === 'corridor')!;
    const engine = strategyEngine(corridor);
    expect(PathFinder.findPath({ x: 1, y: 4 }, { x: 7, y: 4 }, engine.board)).toBeNull();
    engine.moveBall(corridor.from, corridor.to);
    expect(PathFinder.findPath({ x: 1, y: 4 }, { x: 7, y: 4 }, engine.board)).not.toBeNull();
    const preview = STRATEGY_LESSONS.find(l => l.id === 'preview')!;
    const planned = strategyEngine(preview);
    expect(planned.nextColors[0]).toBe('red');
    expect(planned.nextSpawnPoints[0]).toEqual({ x: 5, y: 4 });
    const result = planned.moveBall(preview.from, preview.to);
    expect(result.spawnedBalls).toContainEqual({ point: { x: 5, y: 4 }, color: 'red' });
    expect(result.clearedPoints).toHaveLength(5);
  });
  it('preserves open ends and clears both arms of a crossing', () => {
    const open = strategyEngine(STRATEGY_LESSONS[0]);
    open.moveBall(STRATEGY_LESSONS[0].from, STRATEGY_LESSONS[0].to);
    expect(open.board.get(1, 4)).toBeNull();
    expect(open.board.get(5, 4)).toBeNull();
    const cross = STRATEGY_LESSONS.find(l => l.id === 'cross')!;
    expect(strategyEngine(cross).moveBall(cross.from, cross.to).clearedPoints).toHaveLength(9);
  });
});
