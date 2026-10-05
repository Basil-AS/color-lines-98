import { describe, expect, it } from 'vitest';
import { STEPS, judge, stepEngine, stepState } from '../src/tutorial';
import type { TutorialStep } from '../src/tutorial';

const step = (id: string): TutorialStep => STEPS.find((s) => s.id === id)!;
const rng = () => 0.5;

describe('tutorial steps', () => {
  it('starts on a board and ends with read-only steps', () => {
    expect(STEPS[0].setup).toBeDefined();
    expect(STEPS.at(-1)!.info).toBe(true);
    expect(new Set(STEPS.map((s) => s.id)).size).toBe(STEPS.length);
  });

  it('only builds valid boards, with the next balls on free cells', () => {
    for (const s of STEPS) {
      if (!s.setup) continue;
      const engine = stepEngine(s, rng);
      expect(engine, s.id).not.toBeNull();
      expect(engine!.nextColors).toHaveLength(3);
      for (const q of engine!.nextSpawnPoints) expect(engine!.board.get(q.x, q.y)).toBeNull();
      expect(stepState(s)!.isGameOver).toBe(false);
    }
  });

  it('shows hints on a ball and an empty cell', () => {
    for (const s of STEPS) {
      if (!s.setup || !s.from) continue;
      const engine = stepEngine(s, rng)!;
      expect(engine.board.get(s.from.x, s.from.y), s.id).not.toBeNull();
      if (s.to) expect(engine.board.get(s.to.x, s.to.y), s.id).toBeNull();
    }
  });
});

describe('the scripted moves work on the real rules', () => {
  it('the line step clears a line with the hinted move and gives a free turn', () => {
    const s = step('line');
    const engine = stepEngine(s, rng)!;
    const res = engine.moveBall(s.from!, s.to!);
    expect(res.success).toBe(true);
    expect(res.clearedPoints.length).toBeGreaterThanOrEqual(5);
    expect(res.spawnedBalls).toHaveLength(0);
  });

  it('the move step spawns the announced balls', () => {
    const s = step('select');
    const engine = stepEngine(s, rng)!;
    const next = [...engine.nextColors];
    const res = engine.moveBall(step('move').from!, step('move').to!);
    expect(res.success).toBe(true);
    expect(res.spawnedBalls.map((b) => b.color)).toEqual(next);
  });

  it('the boxed-in ball of the blocked step cannot move', () => {
    const s = step('blocked');
    const engine = stepEngine(s, rng)!;
    expect(engine.moveBall(s.from!, s.to!).success).toBe(false);
  });
});

describe('judging what the player does', () => {
  const move = (success: boolean, cleared = 0) => ({ kind: 'move' as const, success, cleared });

  it('select: any selection finishes it', () => {
    expect(judge(step('select'), { kind: 'select', point: { x: 6, y: 2 } })).toBe('done');
    expect(judge(step('select'), move(false))).toBe('ignore');
  });

  it('move and free: a successful move finishes them, a failed one does not', () => {
    for (const id of ['move', 'free']) {
      expect(judge(step(id), move(true))).toBe('done');
      expect(judge(step(id), move(false))).toBe('ignore');
    }
  });

  it('line: only a move that clears counts, any other move restarts the step', () => {
    expect(judge(step('line'), move(true, 5))).toBe('done');
    expect(judge(step('line'), move(true, 0))).toBe('retry');
    expect(judge(step('line'), move(false))).toBe('ignore');
  });

  it('blocked: trying and failing is the lesson, a move that works restarts it', () => {
    expect(judge(step('blocked'), move(false))).toBe('done');
    expect(judge(step('blocked'), move(true))).toBe('retry');
  });

  it('read-only steps ignore the board', () => {
    expect(judge(step('next'), move(true, 5))).toBe('ignore');
    expect(judge(step('end'), { kind: 'select', point: { x: 0, y: 0 } })).toBe('ignore');
  });
});
