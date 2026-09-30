import { describe, expect, it } from 'vitest';
import { BACKUP_FORMAT, buildBackup, describeBackup, historyToCsv, mergeHalls, mergeHistories, mergeProgress, parseBackup } from '../src/backup';
import { ledgerFromHistory } from '../src/ledger';
import { rebuildProgress, emptyProgress } from '../src/progress';
import type { GameRecord } from '../src/stats';

const g = (score: number, at: number, over: Partial<GameRecord> = {}): GameRecord => ({
  score, endedAt: at, moves: 30, lines: 2, balls: 10, completed: true, maxLine: 5, durationMs: 60_000, mode: 'classic', ...over,
});

const history = [g(300, 3000), g(200, 2000), g(100, 1000, { mode: 'easy' })];
const make = () =>
  buildBackup({
    exportedAt: 5000,
    app: { platform: 'web', version: '1.5.0' },
    history,
    ledger: ledgerFromHistory(history),
    progress: rebuildProgress(history),
    hall: [{ name: 'Ann', score: 300, at: 3000 }],
    settings: { theme: 'neon', language: 'ru', playerName: 'Ann', soundEnabled: false, spawnPreview: null, showNext: true, mode: 'blitz' },
  });

describe('backup files', () => {
  it('round-trips through JSON without losing anything', () => {
    const parsed = parseBackup(JSON.stringify(make()));
    expect(parsed.ok).toBe(true);
    if (!parsed.ok) return;
    expect(parsed.backup.history).toEqual(history);
    expect(parsed.backup.settings).toEqual({ theme: 'neon', language: 'ru', playerName: 'Ann', soundEnabled: false, spawnPreview: null, showNext: true, mode: 'blitz' });
    expect(parsed.backup.progress.totalGames).toBe(3);
    expect(parsed.backup.hall).toEqual([{ name: 'Ann', score: 300, at: 3000 }]);
    expect(describeBackup(parsed.backup)).toEqual({ games: 3, first: 1000, last: 3000, exportedAt: 5000 });
  });

  it('refuses files that are not backups, with a reason', () => {
    expect(parseBackup('')).toEqual({ ok: false, error: 'empty' });
    expect(parseBackup('{oops')).toEqual({ ok: false, error: 'notJson' });
    expect(parseBackup('[1,2]')).toEqual({ ok: false, error: 'notBackup' });
    expect(parseBackup('{"format":"other","version":1}')).toEqual({ ok: false, error: 'notBackup' });
    expect(parseBackup(JSON.stringify({ format: BACKUP_FORMAT, version: 99 }))).toEqual({ ok: false, error: 'newer' });
    expect(parseBackup('x'.repeat(9 * 1024 * 1024))).toEqual({ ok: false, error: 'tooBig' });
  });

  it('never trusts the content: bad games, themes and names are dropped or cut', () => {
    const evil = {
      format: BACKUP_FORMAT, version: 1, exportedAt: 'soon',
      history: [{ score: -1 }, { score: 5, endedAt: 1, moves: 1, lines: 0, balls: 0, completed: true, mode: 'classic' }, 'x', null],
      settings: { theme: '<script>', language: 'xx', playerName: 'A'.repeat(100), soundEnabled: 'yes', mode: 'turbo' },
      hall: [{ name: 'x', score: 'lots', at: 1 }],
      progress: 'nope',
      ledger: { '2026-01-01': { games: 'many' } },
    };
    const p = parseBackup(JSON.stringify(evil));
    expect(p.ok).toBe(true);
    if (!p.ok) return;
    expect(p.backup.history).toHaveLength(1);
    expect(p.backup.settings).toEqual({ playerName: 'A'.repeat(12) });
    expect(p.backup.hall).toEqual([]);
    expect(p.backup.exportedAt).toBe(0);
    expect(p.backup.progress).toEqual(emptyProgress());
  });

  it('merges games without duplicates, newest first, and keeps everything from both sides', () => {
    const merged = mergeHistories([g(300, 3000), g(50, 500)], [g(300, 3000), g(400, 4000)]);
    expect(merged.map((x) => x.score)).toEqual([400, 300, 50]);
    expect(mergeHistories([], [])).toEqual([]);
  });

  it('merges halls by score and progress without going backwards', () => {
    const hall = mergeHalls([{ name: 'A', score: 10, at: 1 }], [{ name: 'A', score: 10, at: 1 }, { name: 'B', score: 20, at: 2 }]);
    expect(hall.map((h) => h.name)).toEqual(['B', 'A']);
    const a = { ...rebuildProgress([g(100, 1000)]), bonusXp: 50, goalDays: ['2026-01-01'] };
    const b = { ...rebuildProgress([g(900, 9000, { maxLine: 9 })]), bonusXp: 25, goalDays: ['2026-01-02'] };
    const all = mergeHistories([g(100, 1000)], [g(900, 9000, { maxLine: 9 })]);
    const m = mergeProgress(a, b, all);
    expect(m.bestScore).toBe(900);
    expect(m.bestLine).toBe(9);
    expect(m.bonusXp).toBe(50);
    expect(m.goalDays).toEqual(['2026-01-01', '2026-01-02']);
    expect(m.totalGames).toBe(2);
  });

  it('writes a spreadsheet that cannot run formulas', () => {
    const csv = historyToCsv([g(200, Date.UTC(2026, 0, 3), { mode: '=cmd' as never }), g(100, Date.UTC(2026, 0, 2))]);
    const lines = csv.trim().split('\n');
    expect(lines[0]).toBe('date,mode,score,moves,lines,balls,longest_line,play_seconds,completed');
    expect(lines).toHaveLength(3);
    // The history is newest first, the sheet reads oldest first.
    expect(lines[2].startsWith('2026-01-03T00:00:00.000Z,')).toBe(true);
    expect(lines[1].startsWith('2026-01-02T00:00:00.000Z,classic,100,')).toBe(true);
    expect(lines[2]).toContain("'=cmd");
  });

  it('reads a file written by the Android app', () => {
    // Produced by Backups.encode() in core-engine (Kotlin); the two apps must understand each other.
    const fromAndroid = "{\"format\":\"color-lines-backup\",\"version\":1,\"exportedAt\":1790000000000,\"app\":{\"platform\":\"android\",\"version\":\"1.5.0\"},\"history\":[{\"score\":300,\"endedAt\":1788436800000,\"moves\":50,\"lines\":4,\"balls\":20,\"completed\":true,\"maxLine\":6,\"durationMs\":90000,\"mode\":\"classic\"},{\"score\":120,\"endedAt\":1788350400000,\"moves\":20,\"lines\":1,\"balls\":5,\"completed\":false,\"maxLine\":5,\"durationMs\":30000,\"mode\":\"easy\"}],\"ledger\":{\"2026-09-02\":{\"games\":1,\"completed\":0,\"score\":120,\"best\":120,\"moves\":20,\"lines\":1,\"playMs\":30000},\"2026-09-03\":{\"games\":1,\"completed\":1,\"score\":300,\"best\":300,\"moves\":50,\"lines\":4,\"playMs\":90000}},\"progress\":{\"totalGames\":2,\"completedGames\":1,\"totalScore\":420,\"totalLines\":5,\"totalBalls\":25,\"totalMoves\":70,\"totalPlayMs\":120000,\"bestScore\":300,\"bestLine\":6,\"mostLinesInGame\":4,\"longestGameMoves\":50,\"days\":[\"2026-09-02\",\"2026-09-03\"],\"achievements\":{\"first_game\":1788350400000,\"first_line\":1788350400000,\"score_100\":1788350400000,\"score_250\":1788436800000}},\"hall\":[{\"name\":\"Ann\",\"score\":300,\"at\":1}],\"settings\":{\"theme\":\"neon\",\"language\":\"ru\",\"playerName\":\"Ann\",\"soundEnabled\":false,\"spawnPreview\":null,\"showNext\":true,\"mode\":\"blitz\"}}";
    const p = parseBackup(fromAndroid);
    expect(p.ok).toBe(true);
    if (!p.ok) return;
    expect(p.backup.app).toEqual({ platform: 'android', version: '1.5.0' });
    expect(p.backup.history.map((x) => [x.score, x.mode, x.completed])).toEqual([[300, 'classic', true], [120, 'easy', false]]);
    expect(p.backup.settings).toEqual({ theme: 'neon', language: 'ru', playerName: 'Ann', soundEnabled: false, spawnPreview: null, showNext: true, mode: 'blitz' });
    expect(p.backup.hall).toEqual([{ name: 'Ann', score: 300, at: 1 }]);
    expect(p.backup.progress.totalGames).toBe(2);
    expect(Object.keys(p.backup.ledger)).toHaveLength(2);
  });
});
