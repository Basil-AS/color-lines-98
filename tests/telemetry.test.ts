import { describe, expect, it } from 'vitest';
import { GameEngine } from '../src/engine/gameengine';
import { COG_KEYS, Telemetry, localTime, sanitizeCognition, timeZoneCode } from '../src/engine/telemetry';
import { mulberry32 } from '../src/engine/rng';

describe('Telemetry', () => {
  it('measures a decision as the time since the last move and keeps its spread', () => {
    const t = new Telemetry();
    t.action(1500);
    t.action(500);
    t.moved(1, 0, 70);
    t.action(12000);
    t.moved(2, 1, 60);
    expect(t.c.tm).toBe(2);
    expect(t.c.think).toBe(14000);
    expect(t.c.thinkMax).toBe(12000);
    expect(t.c.fast).toBe(0);
    expect(t.c.slow).toBe(1);
    expect(t.c.lat).toBe(1500 + 12000);
    expect(t.c.first).toBe(2000);
    expect(t.c.p1).toBe(14000);
    expect(t.c.clears).toBe(1);
  });

  it('does not count a move whose start time is unknown', () => {
    const t = new Telemetry();
    t.action(null);
    t.action(900);
    t.moved(1, 0, 70);
    expect(t.c.tm).toBe(0);
    t.action(900);
    t.moved(2, 0, 70);
    expect(t.c.tm).toBe(1);
  });

  it('buckets by game phase, notes danger and multi-line moves', () => {
    const t = new Telemetry();
    for (let m = 1; m <= 70; m++) {
      t.action(3000);
      t.moved(m, m === 5 ? 2 : 0, m >= 65 ? 10 : 50);
    }
    expect([t.c.n1, t.c.n2, t.c.n3]).toEqual([20, 40, 10]);
    expect(t.c.multi).toBe(1);
    expect(t.c.danger).toBe(6);
    expect(t.c.minEmpty).toBe(10);
  });

  it('clips a break to a minute and ignores nonsense', () => {
    const t = new Telemetry();
    t.action(9_999_999);
    t.moved(1, 0, 70);
    expect(t.c.thinkMax).toBe(60_000);
    t.action(-5);
    expect(() => t.moved(2, 0, 70)).not.toThrow();
  });

  it('round-trips through the saved-game array and ignores a malformed one', () => {
    const t = new Telemetry();
    t.action(2500);
    t.moved(1, 0, 70);
    t.undo();
    t.hint();
    t.miss();
    const copy = Telemetry.fromArray(JSON.parse(JSON.stringify(t.toArray())));
    expect(copy.c).toEqual({ ...t.c, tz: 0 });
    expect(Telemetry.fromArray([1, 2]).c.tm).toBe(0);
    expect(Telemetry.fromArray('x').c.minEmpty).toBe(81);
  });

  it('sanitises stored play data', () => {
    const good = new Telemetry().snapshot(112);
    expect(sanitizeCognition(good)).toEqual(good);
    expect(sanitizeCognition({ ...good, tm: -1 })).toBeUndefined();
    expect(sanitizeCognition({ ...good, tz: 'x' })).toBeUndefined();
    expect(sanitizeCognition(null)).toBeUndefined();
    expect(Object.keys(good)).toEqual([...COG_KEYS]);
  });

  it('reads the local hour in the stored zone', () => {
    // 2026-03-02 (a Monday) 22:30 UTC seen from UTC+3 is 01:30 on Tuesday.
    const at = Date.UTC(2026, 2, 2, 22, 30);
    expect(localTime(at, 100 + 12)).toEqual({ hour: 1, weekday: 1 });
    expect(localTime(at, 100)).toEqual({ hour: 22, weekday: 0 });
    expect(timeZoneCode(new Date(at))).toBeGreaterThan(0);
  });
});

describe('the engine feeds the telemetry', () => {
  it('counts moves, misses, undos and hints of a real game', () => {
    const e = new GameEngine(9, 3, 5, 'gamos', mulberry32(7));
    let moved = 0;
    for (let guard = 0; guard < 400 && moved < 5; guard++) {
      e.noteAction(2000);
      let done = false;
      for (let y = 0; y < 9 && !done; y++)
        for (let x = 0; x < 9 && !done; x++) {
          if (!e.board.get(x, y)) continue;
          for (let ty = 0; ty < 9 && !done; ty++)
            for (let tx = 0; tx < 9 && !done; tx++) {
              if (e.board.get(tx, ty)) continue;
              if (e.moveBall({ x, y }, { x: tx, y: ty }).success) {
                moved++;
                done = true;
              }
            }
        }
    }
    expect(e.tel.c.tm).toBe(moved);
    expect(e.tel.c.think).toBe(moved * 2000);
    e.noteHint();
    expect(e.undo()).toBe(true);
    expect(e.tel.c.hint).toBe(1);
    expect(e.tel.c.undo).toBe(1);
    const restored = GameEngine.fromState(JSON.parse(JSON.stringify(e.getState())));
    expect(restored?.tel.c.tm).toBe(moved);
    const old = e.getState();
    delete old.tel;
    expect(GameEngine.fromState(old)?.tel.c.tm).toBe(0);
  });
});
