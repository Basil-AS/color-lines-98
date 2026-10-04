import { useState, useEffect, useLayoutEffect, useCallback, useRef } from 'react';
import type { KeyboardEvent } from 'react';
import confetti from 'canvas-confetti';
import {
  BarChart3,
  Download,
  MonitorDown,
  HelpCircle,
  RotateCcw,
  Settings,
  Trophy,
  Lightbulb,
  Undo2,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { GameEngine } from './engine/gameengine';
import { MODES, MODE_IDS } from './engine/modes';
import { buildBackup, historyToCsv, mergeHalls, mergeHistories, mergeProgress } from './backup';
import type { Backup } from './backup';
import { downloadText } from './download';
import { addGame, mergeLedgersWithHistory } from './ledger';
import type { Ledger } from './ledger';
import type { ModeId } from './engine/modes';
import { findHint, type Hint } from './engine/hint';
import { createEngine, remainingMs, todayKey } from './modes';
import { dailyGoals, evaluateGoals, goalBonus } from './goals';
import type { Goal } from './goals';
import type { BallColor, Point } from './engine/models';
import { pointsEqual } from './engine/models';
import { soundManager } from './audio';
import {
  colorName,
  resolveLanguage,
  translate,
} from './i18n';
import type { Language, MessageKey } from './i18n';
import {
  clearHistory,
  clearProgress,
  loadBestScore,
  loadGame,
  loadHistory,
  loadLedger,
  saveLedger,
  clearLedger,
  loadGoalsDone,
  loadLanguagePref,
  loadMode,
  loadProgress,
  loadHall,
  loadPlayerName,
  loadShowNext,
  loadEffects,
  loadVibration,
  loadSpawnPreview,
  loadTheme,
  saveBestScore,
  saveGame,
  saveHistory,
  saveGoalsDone,
  saveLanguagePref,
  saveMode,
  saveProgress,
  saveHall,
  savePlayerName,
  saveShowNext,
  saveEffects,
  saveVibration,
  saveSpawnPreview,
  saveTheme,
} from './storage';
import type { LanguagePref, Theme } from './storage';
import { addRecord, isNewRecord, recordFromEngine, summarize } from './stats';
import type { GameRecord } from './stats';
import { applyGame, applyGoalBonus, awardAchievements, currentStreak, dayKey, levelInfo, rebuildProgress, xpOf } from './progress';
import type { Progress } from './progress';
import { defaultSpawnPreview, soundProfile, soundVoice, usesSprites } from './themes';
import { BoardFx, travelMs } from './effects';
import type { EffectsLevel, MoveFx } from './effects';
import { haptic, setHapticsEnabled } from './haptics';
import { DosScreen } from './dos/DosScreen';
import type { DosWindow } from './dos/DosScreen';
import type { Effect } from './dos/scene';
import { insertScore, kingOf } from './dos/hall';
import { dosNames } from './dos/names';
import { InstallDialog } from './components/InstallDialog';
import { LedNumber } from './components/LedNumber';
import { useInstall } from './pwa/useInstall';
import { GameOverDialog } from './components/GameOverDialog';
import { GoalsDialog } from './components/GoalsDialog';
import { WinMenu } from './components/WinMenu';
import { GoalsPanel } from './components/GoalsPanel';
import { NewGameDialog } from './components/NewGameDialog';
import { ColorDots } from './components/ColorDots';
import { HelpDialog } from './components/HelpDialog';
import { SettingsDialog } from './components/SettingsDialog';
import { StatsDialog } from './components/StatsDialog';
import './App.css';

const BOARD_SIZE = 9;
const GITHUB_REPO_URL = 'https://github.com/Basil-AS/color-lines-98';
/** A stable name on every release, so this link always downloads the newest APK. */
const LATEST_APK_URL = `${GITHUB_REPO_URL}/releases/latest/download/ColorLines.apk`;

function prefersReducedMotion(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function browserLanguages(): readonly string[] {
  return navigator.languages?.length ? navigator.languages : [navigator.language];
}

const clock = (): number => Date.now();
const perfNow = (): number => performance.now();

function applyDocumentLanguage(lang: Language): void {
  document.documentElement.lang = lang;
  document.title = translate(lang, 'app.docTitle');
}

const THEME_COLORS: Record<Theme, string> = {
  modern: '#121217',
  light: '#e9ecf5',
  material: '#fef7ff',
  neon: '#07060f',
  synthwave: '#140a2e',
  ocean: '#04222f',
  paper: '#efe3c8',
  gameboy: '#8bac0f',
  terminal: '#120900',
  contrast: '#000000',
  lines98: '#008080',
  lines98plus: '#dde5ef',
  colorlines92: '#000000',
};

function applyDocumentTheme(theme: Theme): void {
  document.body.className = `theme-${theme}`;
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[theme]);
}

function withRecord(history: readonly GameRecord[], engine: GameEngine, completed: boolean) {
  return addRecord(history, recordFromEngine(engine, completed, Date.now()));
}

function goalLabel(lang: Language, goal: Goal): string {
  const target = goal.type === 'efficiency' ? goal.target.toFixed(1) : String(Math.round(goal.target));
  return translate(lang, `goals.${goal.type}` as MessageKey, { target });
}

function formatClock(ms: number): string {
  const total = Math.ceil(ms / 1000);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}

function ballThemeClass(theme: Theme): string {
  if (theme === 'lines98' || theme === 'lines98plus') return '';
  if (theme === 'colorlines92') return 'ball-dos';
  if (theme === 'neon') return 'ball-neon';
  if (theme === 'contrast') return 'ball-contrast';
  if (theme === 'gameboy' || theme === 'terminal') return 'ball-contrast ball-symbols';
  if (theme === 'synthwave') return 'ball-neon ball-synth';
  return 'ball-modern';
}

function getSpriteUrl(color: BallColor): string {
  const map: Record<BallColor, string> = {
    red: 'sprites/classic/lineBall_10_0.png',
    green: 'sprites/classic/lineBall_10_1.png',
    blue: 'sprites/classic/lineBall_10_2.png',
    cyan: 'sprites/classic/lineBall_10_3.png',
    magenta: 'sprites/classic/lineBall_10_4.png',
    yellow: 'sprites/classic/lineBall_10_5.png',
    brown: 'sprites/classic/lineBall_10_6.png',
  };
  const base = import.meta.env.BASE_URL.replace(/\/$/, '');
  return `${base}/${map[color]}`;
}

const HINTS_PER_GAME = 3;

export default function App() {
  const [engine, setEngine] = useState<GameEngine>(() => loadGame() ?? createEngine(loadMode()));
  const [hint, setHint] = useState<Hint | null>(null);
  const [hintsLeft, setHintsLeft] = useState(HINTS_PER_GAME);
  const [today, setToday] = useState(() => todayKey());
  const [newGameMode, setNewGameMode] = useState<ModeId | null>(null);
  const [version, setVersion] = useState(0); // Bumped after every engine mutation to re-render
  const [theme, setTheme] = useState<Theme>(loadTheme);
  const [langPref, setLangPref] = useState<LanguagePref>(loadLanguagePref);
  const [soundEnabled, setSoundEnabled] = useState(() => soundManager.isEnabled);
  const [dialog, setDialog] = useState<'help' | 'stats' | 'settings' | 'newgame' | 'goals' | null>(null);
  const [history, setHistory] = useState(loadHistory);
  const [progress, setProgress] = useState<Progress>(() => {
    const saved = loadProgress();
    const past = loadHistory();
    // Saves made before profiles existed: rebuild the profile from the recorded games.
    return saved.totalGames === 0 && past.length > 0 ? rebuildProgress(past) : saved;
  });
  const [spawnStored, setSpawnStored] = useState<boolean | null>(loadSpawnPreview);
  const spawnPreview = spawnStored ?? defaultSpawnPreview(theme);
  const [ledger, setLedger] = useState<Ledger>(loadLedger);
  const [hall, setHall] = useState(loadHall);
  const [showNext, setShowNext] = useState(loadShowNext);
  const [effectsLevel, setEffectsLevel] = useState<EffectsLevel>(() => loadEffects(prefersReducedMotion()));
  const [vibration, setVibration] = useState(loadVibration);
  const boardRef = useRef<HTMLElement>(null);
  const fxRef = useRef<BoardFx | null>(null);
  const pendingFx = useRef<MoveFx | null>(null);
  const comboRef = useRef(0);
  const dangerWarned = useRef(false);
  const [dosWindow, setDosWindow] = useState<DosWindow>('none');
  const [effects, setEffects] = useState<Effect[]>([]);
  const [coronationStart, setCoronationStart] = useState<number | null>(() =>
    engine.score > kingOf(loadHall()).score ? -1e9 : null
  );
  const [playerName, setPlayerName] = useState(loadPlayerName);
  const [lastResult, setLastResult] = useState<{ xp: number; levelUp: number | null; unlocked: string[]; goals: string[]; goalXp: number }>({
    xp: 0,
    levelUp: null,
    unlocked: [],
    goals: [],
    goalXp: 0,
  });
  const lastActionAt = useRef(0);
  const [goalsDone, setGoalsDone] = useState<string[]>(() => loadGoalsDone(todayKey()));
  const [bestScore, setBestScore] = useState(() =>
    Math.max(loadBestScore(), summarize(loadHistory()).bestScore)
  );
  const [bestAtGameStart, setBestAtGameStart] = useState(bestScore);
  const [announcement, setAnnouncement] = useState('');
  const [focusCell, setFocusCell] = useState<Point>({ x: 0, y: 0 });
  const cellRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const lang = resolveLanguage(langPref, browserLanguages());
  const t = (key: MessageKey, params?: Record<string, string | number>) =>
    translate(lang, key, params);

  // Re-render and persist the game after the engine has been mutated by a handler.
  const commit = useCallback(() => {
    setVersion((v) => v + 1);
    saveGame(engine);
  }, [engine]);

  // Counts active time between actions; long pauses (a forgotten tab) are capped.
  const trackTime = () => {
    const now = clock();
    engine.noteAction(lastActionAt.current > 0 ? Math.min(now - lastActionAt.current, 60_000) : null);
    lastActionAt.current = now;
  };

  const recordGame = (completed: boolean) => {
    const record = withRecord(history, engine, completed)[0];
    const nextHistory = addRecord(history, record);
    const applied = applyGame(progress, record);
    let nextProgress = applied.progress;
    let unlocked = applied.unlocked;

    // Daily goals: the experience of every newly completed goal, once.
    const day = dayKey(record.endedAt);
    const goals = dailyGoals(nextHistory, day);
    const todays = nextHistory.filter((g) => dayKey(g.endedAt) === day);
    const nowDone = evaluateGoals(goals, todays).filter((g) => g.done).map((g) => g.goal.id);
    const alreadyDone = day === today ? goalsDone : loadGoalsDone(day);
    const bonus = goalBonus(alreadyDone, nowDone, goals.length);
    let reachedLabels: string[] = [];
    if (bonus.newlyDone.length > 0) {
      nextProgress = applyGoalBonus(nextProgress, day, bonus);
      const extra = awardAchievements(nextProgress, record);
      nextProgress = extra.progress;
      unlocked = [...unlocked, ...extra.unlocked];
      saveGoalsDone(day, nowDone);
      if (day === today) setGoalsDone(nowDone);
      reachedLabels = goals.filter((g) => bonus.newlyDone.includes(g.id)).map((g) => goalLabel(lang, g));
    }

    const nextLedger = addGame(ledger, record);
    setLedger(nextLedger);
    saveLedger(nextLedger);
    setHistory(nextHistory);
    saveHistory(nextHistory);
    setProgress(nextProgress);
    saveProgress(nextProgress);
    if (completed) {
      const nextHall = insertScore(hall, { name: playerName || t('dos.defaultName'), score: record.score, at: record.endedAt });
      setHall(nextHall);
      saveHall(nextHall);
    }
    const before = levelInfo(xpOf(progress)).level;
    const after = levelInfo(xpOf(nextProgress)).level;
    const reward = {
      xp: xpOf(nextProgress) - xpOf(progress),
      levelUp: after > before ? after : null,
      unlocked,
      goals: reachedLabels,
      goalXp: bonus.xp,
    };
    setLastResult(reward);
    return reward;
  };

  const changeTheme = (newTheme: Theme) => {
    setTheme(newTheme);
    saveTheme(newTheme);
  };

  const changeLanguage = (pref: LanguagePref) => {
    setLangPref(pref);
    saveLanguagePref(pref);
  };

  const toggleSound = () => {
    setSoundEnabled(soundManager.toggle());
  };

  const changePlayerName = (name: string) => {
    setPlayerName(name.slice(0, 12));
    savePlayerName(name);
  };

  const togglePreview = () => {
    const next = !spawnPreview;
    setSpawnStored(next);
    saveSpawnPreview(next);
  };

  const changeEffects = (level: EffectsLevel) => {
    setEffectsLevel(level);
    saveEffects(level);
  };

  const changeVibration = (on: boolean) => {
    setVibration(on);
    saveVibration(on);
    if (on) haptic('select');
  };

  const toggleNext = () => {
    const next = !showNext;
    setShowNext(next);
    saveShowNext(next);
  };

  const handleCellClick = (x: number, y: number) => {
    if (hint) setHint(null);
    setFocusCell({ x, y });
    if (engine.isGameOver) return;
    trackTime();

    const clickedPoint: Point = { x, y };

    if (engine.board.get(x, y)) {
      if (engine.selectedPoint && pointsEqual(engine.selectedPoint, clickedPoint)) {
        engine.unselect();
      } else {
        engine.select(clickedPoint);
        soundManager.play('select');
        haptic('select');
        fxRef.current?.ripple(clickedPoint, 'rgba(255,255,255,0.9)');
      }
      commit();
      return;
    }

    if (!engine.selectedPoint) return;

    const from = engine.selectedPoint;
    const before = engine.board.copy();
    const res = engine.moveBall(from, clickedPoint);
    if (!res.success) {
      soundManager.play('blocked');
      haptic('blocked');
      fxRef.current?.deny(clickedPoint);
      setAnnouncement(t('announce.noPath'));
      return;
    }

    engine.unselect();
    soundManager.play('jump');

    // Remember what appeared and what burst, so the 1992 screen can animate it.
    const start = perfNow();
    const movedColor = before.get(from.x, from.y);
    const fx: Effect[] = res.spawnedBalls.map((s) => ({
      kind: 'spawn',
      x: s.point.x,
      y: s.point.y,
      color: s.color,
      start,
    }));
    for (const p of res.clearedPoints) {
      const color = pointsEqual(p, clickedPoint)
        ? movedColor
        : (before.get(p.x, p.y) ?? res.spawnedBalls.find((s) => pointsEqual(s.point, p))?.color ?? null);
      if (color) fx.push({ kind: 'burst', x: p.x, y: p.y, color, start });
    }
    setEffects(fx);

    // The look with a living board (not the 1992 picture) plays the move out; the sounds follow its timing.
    const animated = effectsLevel !== 'off' && !dos && fxRef.current !== null;
    const free = engine.board.getEmptyCells().length;
    const lag = animated ? travelMs(res.path ? res.path.length - 1 : 0) : 0;
    const after = (ms: number, fn: () => void) => (ms > 0 ? void window.setTimeout(fn, ms) : fn());
    const cleared = res.clearedPoints.length > 0;
    comboRef.current = cleared ? comboRef.current + 1 : 0;
    if (animated) {
      pendingFx.current = {
        path: res.path ?? [from, clickedPoint],
        color: movedColor ?? 'red',
        spawned: res.spawnedBalls.map((b) => ({ point: b.point, color: b.color })),
        cleared: fx.filter((f) => f.kind === 'burst').map((f) => ({ point: { x: f.x, y: f.y }, color: f.color })),
        points: res.pointsEarned,
        combo: comboRef.current,
        free,
      };
    }
    haptic(cleared ? (res.pointsEarned >= 28 ? 'bigClear' : 'clear') : 'move');
    if (engine.score > kingOf(hall).score && coronationStart === null) {
      setCoronationStart(start);
      if (!res.isGameOver) window.setTimeout(() => soundManager.play('crown'), 450 + lag);
    }

    if (cleared) {
      // Clearing a line is a free turn, so chains are natural: each link sounds higher.
      after(lag, () => soundManager.play('eat', res.pointsEarned));
      if (comboRef.current >= 2) {
        after(lag + 180, () => {
          soundManager.play('combo', comboRef.current);
          haptic('combo');
        });
      }
      setAnnouncement(
        comboRef.current >= 2
          ? t('announce.combo', { n: comboRef.current, points: res.pointsEarned, score: engine.score })
          : t('announce.lineCleared', { points: res.pointsEarned, score: engine.score })
      );
      if (res.pointsEarned >= 18 && !prefersReducedMotion() && effectsLevel === 'full') {
        confetti({ particleCount: 50 + res.pointsEarned * 2, spread: 60, origin: { y: 0.6 } });
      }
    } else {
      setAnnouncement(t('announce.score', { score: engine.score }));
    }
    if (res.spawnedBalls.length > 0) after(lag + 60, () => soundManager.play('pop'));

    // A nearly full board: one warning each time it gets that tight.
    if (!res.isGameOver && free <= 8 && !dangerWarned.current) {
      dangerWarned.current = true;
      after(lag + 350, () => {
        soundManager.play('danger');
        haptic('danger');
      });
    } else if (free > 12) {
      dangerWarned.current = false;
    }

    if (engine.score > bestScore) {
      setBestScore(engine.score);
      saveBestScore(engine.score);
    }

    if (res.isGameOver) finishGame();
    commit();
  };

  /** The game just ended (board full or time up): sound, result, record and the follow-up rewards. */
  const finishGame = () => {
    const newBest = isNewRecord(engine.score, bestAtGameStart);
    soundManager.play(newBest ? 'record' : 'lose');
    haptic(newBest ? 'record' : 'gameOver');
    if (newBest) window.setTimeout(() => fxRef.current?.celebrate(), 300);
    setAnnouncement(t('announce.gameOver', { score: engine.score }));
    const reward = recordGame(true);
    // The progress rewards follow the result after a short pause so the sounds do not blur together.
    if (reward.levelUp !== null) window.setTimeout(() => soundManager.play('levelUp'), 1100);
    else if (reward.unlocked.length > 0 || reward.goals.length > 0) window.setTimeout(() => soundManager.play('achievement'), 1100);
  };

  const handleCellKeyDown = (e: KeyboardEvent<HTMLButtonElement>, x: number, y: number) => {
    const delta: Record<string, Point> = {
      ArrowLeft: { x: -1, y: 0 },
      ArrowRight: { x: 1, y: 0 },
      ArrowUp: { x: 0, y: -1 },
      ArrowDown: { x: 0, y: 1 },
    };
    const step = delta[e.key];
    if (!step) return;
    e.preventDefault();
    const nx = Math.min(BOARD_SIZE - 1, Math.max(0, x + step.x));
    const ny = Math.min(BOARD_SIZE - 1, Math.max(0, y + step.y));
    setFocusCell({ x: nx, y: ny });
    cellRefs.current[ny * BOARD_SIZE + nx]?.focus();
  };

  /** Selects the ball worth moving and marks where to put it; three a game. */
  const handleHint = () => {
    if (engine.isGameOver || hintsLeft <= 0) return;
    const h = findHint(engine.board);
    if (!h) {
      setAnnouncement(t('hint.none'));
      return;
    }
    engine.select(h.from);
    engine.noteHint();
    setHint(h);
    setHintsLeft((n) => n - 1);
    soundManager.play('hint');
    haptic('hint');
    fxRef.current?.ripple(h.to, 'rgba(255,214,0,0.95)');
    setAnnouncement(t(h.clears ? 'hint.clear' : 'hint.build', { n: h.value }));
    commit();
  };

  const handleUndo = () => {
    setHint(null);
    trackTime();
    if (engine.undo()) {
      soundManager.play('click');
      haptic('undo');
      setAnnouncement(t('announce.undone', { score: engine.score }));
      commit();
    }
  };

  /** Starts a game in the given mode; a game in progress is recorded as unfinished. */
  const startGame = (mode: ModeId) => {
    if (!engine.isGameOver && engine.moves > 0) recordGame(false);
    const next = createEngine(mode);
    setHint(null);
    setHintsLeft(HINTS_PER_GAME);
    setEngine(next);
    saveMode(mode);
    saveGame(next);
    setToday(todayKey());
    // The first decision of a game is timed from the moment the board appears.
    lastActionAt.current = clock();
    setEffects([]);
    comboRef.current = 0;
    dangerWarned.current = false;
    setCoronationStart(null);
    setBestAtGameStart(bestScore);
    setDialog(null);
    setNewGameMode(null);
    soundManager.play('start');
    setAnnouncement(t('announce.newGame'));
    setVersion((v) => v + 1);
  };

  /** Every "new game" button opens the dialog: it names the mode and warns before a game is thrown away. */
  const requestNewGame = () => {
    setNewGameMode(null);
    setDialog('newgame');
  };

  const handleNewGame = requestNewGame;

  const playDaily = () => {
    setNewGameMode('daily');
    setDialog('newgame');
  };

  const closeDialog = useCallback(() => setDialog(null), []);
  const [statsNow, setStatsNow] = useState(0);
  const openStats = () => {
    setStatsNow(clock());
    setDialog('stats');
  };

  const handleClearHistory = () => {
    clearHistory();
    clearProgress();
    clearLedger();
    setHistory([]);
    setLedger({});
    setProgress(loadProgress());
  };

  /** Saves the results to a file: the whole backup or just the games as a spreadsheet. */
  const handleExport = (kind: 'json' | 'csv') => {
    const stamp = new Date().toISOString().slice(0, 10);
    if (kind === 'csv') {
      downloadText(`color-lines-games-${stamp}.csv`, historyToCsv(history), 'text/csv');
      return;
    }
    const backup = buildBackup({
      exportedAt: clock(),
      app: { platform: 'web', version: __APP_VERSION__ },
      history,
      ledger,
      progress,
      hall,
      settings: { theme, language: langPref, playerName, soundEnabled, spawnPreview: spawnStored, showNext, mode: engine.mode, effects: effectsLevel, vibration },
    });
    downloadText(`color-lines-backup-${stamp}.json`, JSON.stringify(backup, null, 1), 'application/json');
  };

  /** Merge adds what the file holds and loses nothing; replace makes this device match the file. */
  const handleImport = (backup: Backup, how: 'merge' | 'replace') => {
    const nextHistory = how === 'merge' ? mergeHistories(history, backup.history) : backup.history;
    const nextProgress = how === 'merge' ? mergeProgress(progress, backup.progress, nextHistory) : backup.progress;
    const nextHall = how === 'merge' ? mergeHalls(hall, backup.hall) : backup.hall;
    const nextLedger = how === 'merge' ? mergeLedgersWithHistory(ledger, backup.ledger, nextHistory) : backup.ledger;
    setHistory(nextHistory);
    saveHistory(nextHistory);
    setProgress(nextProgress);
    saveProgress(nextProgress);
    setHall(nextHall);
    saveHall(nextHall);
    setLedger(nextLedger);
    saveLedger(nextLedger);
    const best = Math.max(bestScore, nextProgress.bestScore);
    setBestScore(best);
    saveBestScore(best);
    if (how === 'replace') {
      const s = backup.settings;
      if (s.theme) {
        setTheme(s.theme);
        saveTheme(s.theme);
      }
      if (s.language) {
        setLangPref(s.language);
        saveLanguagePref(s.language);
      }
      if (s.playerName !== undefined) {
        setPlayerName(s.playerName);
        savePlayerName(s.playerName);
      }
      if (s.soundEnabled !== undefined) {
        soundManager.setSoundEnabled(s.soundEnabled);
        setSoundEnabled(s.soundEnabled);
      }
      if (s.spawnPreview !== undefined && s.spawnPreview !== null) {
        setSpawnStored(s.spawnPreview);
        saveSpawnPreview(s.spawnPreview);
      }
      if (s.showNext !== undefined) {
        setShowNext(s.showNext);
        saveShowNext(s.showNext);
      }
      if (s.effects !== undefined) changeEffects(s.effects);
      if (s.vibration !== undefined) changeVibration(s.vibration);
    }
    setAnnouncement(t(how === 'merge' ? 'data.done.merge' : 'data.done.replace'));
  };

  useEffect(() => {
    applyDocumentTheme(theme);
  }, [theme]);

  // The installed app's "New game" shortcut opens the dialog (it never restarts on its own).
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('new') !== '1') return;
    setDialog('newgame');
    params.delete('new');
    const rest = params.toString();
    window.history.replaceState(null, '', window.location.pathname + (rest ? `?${rest}` : '') + window.location.hash);
  }, []);

  const dos = theme === 'colorlines92';

  // The living board: effects are drawn over the board element of every look except the 1992 picture.
  const themeRef = useRef(theme);
  themeRef.current = theme;
  useEffect(() => {
    const root = boardRef.current;
    if (!root) return;
    const fx = new BoardFx(root, (color) => {
      const el = document.createElement('div');
      el.setAttribute('aria-hidden', 'true');
      el.className = `ball ${ballThemeClass(themeRef.current)} color-${color}`;
      if (usesSprites(themeRef.current)) {
        const img = document.createElement('img');
        img.alt = '';
        img.className = 'ball-classic';
        img.src = getSpriteUrl(color);
        el.appendChild(img);
      }
      return el;
    });
    fx.setLevel(effectsLevel);
    fxRef.current = fx;
    return () => {
      fx.destroy();
      fxRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [theme, dos]);
  useEffect(() => {
    fxRef.current?.setLevel(effectsLevel);
  }, [effectsLevel, theme]);
  useEffect(() => {
    setHapticsEnabled(vibration);
  }, [vibration]);
  useLayoutEffect(() => {
    const pending = pendingFx.current;
    pendingFx.current = null;
    if (pending) fxRef.current?.run(pending);
    fxRef.current?.setDanger(!engine.isGameOver && engine.board.getEmptyCells().length <= 10);
  }, [version, engine]);

  // Blitz: count the active time down while the game is visible and no window is open.
  const timed = MODES[engine.mode].timeLimitMs !== null && !engine.isGameOver;
  useEffect(() => {
    if (!timed || dialog !== null) return;
    let last = performance.now();
    const timer = window.setInterval(() => {
      const now = performance.now();
      if (!document.hidden) engine.addPlayTime(now - last);
      last = now;
      if (remainingMs(engine.mode, engine) === 0) {
        window.clearInterval(timer);
        engine.endGame();
        finishGame();
      }
      commit();
    }, 250);
    return () => window.clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timed, dialog, engine]);

  // A new day brings new goals.
  useEffect(() => {
    const onVisible = () => {
      const now = todayKey();
      if (!document.hidden && now !== today) {
        setToday(now);
        setGoalsDone(loadGoalsDone(now));
      }
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [today]);
  const install = useInstall();
  const [showInstallHelp, setShowInstallHelp] = useState(false);

  // The original keys: F1 help, F2 sound, F3 next, F4 restart.
  useEffect(() => {
    if (!dos) return;
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (dialog !== null || engine.isGameOver) return;
      const action: Record<string, () => void> = {
        F1: () => setDosWindow((w) => (w === 'help' ? 'none' : 'help')),
        F2: toggleSound,
        F3: toggleNext,
        F4: handleNewGame,
      };
      const run = action[e.key];
      if (!run) return;
      e.preventDefault();
      run();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  useEffect(() => {
    soundManager.setProfile(soundProfile(theme), soundVoice(theme));
  }, [theme]);

  useEffect(() => {
    applyDocumentLanguage(lang);
  }, [lang]);

  const reachableCells = engine.getReachableCells();
  const hintTitle = t('btn.hint', { n: hintsLeft });
  const newRecord = engine.isGameOver && isNewRecord(engine.score, bestAtGameStart);
  const incoming = new Map<string, BallColor>();
  if (spawnPreview && !engine.isGameOver) {
    engine.nextSpawnPoints.forEach((p, i) => {
      const color = engine.nextColors[i];
      if (color) incoming.set(`${p.x},${p.y}`, color);
    });
  }
  const cellText = (
    x: number,
    y: number,
    color: BallColor | null,
    sel: boolean,
    reach: boolean,
    coming: BallColor | undefined
  ) =>
    t('cell.description', {
      row: y + 1,
      col: x + 1,
      content: color ? t('cell.ball', { color: colorName(lang, color) }) : t('cell.empty'),
      state: `${sel ? t('cell.selected') : ''}${reach ? t('cell.reachable') : ''}${
        coming ? t('cell.incoming', { color: colorName(lang, coming) }) : ''
      }`,
    });
  const sprites = usesSprites(theme);

  const cellButtons = (visual: boolean) =>
    Array.from({ length: BOARD_SIZE }).map((_, y) =>
      Array.from({ length: BOARD_SIZE }).map((_, x) => {
        const color = engine.board.get(x, y);
        const isSelected = engine.selectedPoint?.x === x && engine.selectedPoint?.y === y;
        const isReachable = !color && reachableCells.has(`${x},${y}`);
        const coming = color ? undefined : incoming.get(`${x},${y}`);
        const isFocusStop = focusCell.x === x && focusCell.y === y;

        return (
          <button
            type="button"
            key={`${x}-${y}`}
            ref={(el) => {
              cellRefs.current[y * BOARD_SIZE + x] = el;
            }}
            tabIndex={isFocusStop ? 0 : -1}
            aria-label={cellText(x, y, color, isSelected, isReachable, coming)}
            aria-pressed={color ? isSelected : undefined}
            className={`board-cell ${isSelected ? 'selected' : ''} ${isReachable ? 'reachable' : ''} ${hint && hint.to.x === x && hint.to.y === y ? 'hint-target' : ''}`}
            onClick={() => handleCellClick(x, y)}
            onKeyDown={(e) => handleCellKeyDown(e, x, y)}
          >
            {visual && color && (
              <div
                aria-hidden="true"
                className={`ball ${isSelected ? 'selected-ball' : ''} ${ballThemeClass(
                  theme
                )} color-${color}`}
              >
                {sprites && <img src={getSpriteUrl(color)} alt="" className="ball-classic" />}
              </div>
            )}
            {visual && coming && (
              <div
                aria-hidden="true"
                className={`ball ball-preview ${ballThemeClass(theme)} color-${coming}`}
              >
                {sprites && <img src={getSpriteUrl(coming)} alt="" className="ball-classic" />}
              </div>
            )}
          </button>
        );
      })
    );

  const installButton =
    install.kind === 'prompt' || install.kind === 'ios' || install.kind === 'safari-mac' || install.kind === 'manual' ? (
      <button
        type="button"
        className="ctrl-btn ctrl-link"
        onClick={() => (install.kind === 'prompt' ? void install.install() : setShowInstallHelp(true))}
      >
        <MonitorDown size={16} aria-hidden="true" />
        <span>{t('install.button')}</span>
      </button>
    ) : null;

  // Goals: fixed for the day from the player's earlier results; progress includes the game in play.
  const goals = dailyGoals(history, today);
  const todaysGames = history.filter((g) => dayKey(g.endedAt) === today);
  const liveGame = !engine.isGameOver && engine.moves > 0 ? [withRecord(history, engine, false)[0]] : [];
  const goalProgress = evaluateGoals(goals, [...liveGame, ...todaysGames]);
  const doneCount = goalProgress.filter((g) => g.done).length;
  const goalStreak = currentStreak(progress.goalDays, today);
  const dailyToday = todaysGames.filter((g) => g.mode === 'daily');
  const dailyBest = dailyToday.length === 0 ? null : Math.max(...dailyToday.map((g) => g.score));
  const timeLeft = remainingMs(engine.mode, engine);

  const modeStrip = (
    <div className="mode-strip">
      <button type="button" className="strip-btn" onClick={requestNewGame} title={t('mode.stripHint')}>
        {t('mode.label')}: {t(`mode.${engine.mode}` as MessageKey)} <ColorDots mode={engine.mode} />
        <span className="sr-only">{t('mode.colors', { n: MODES[engine.mode].colors })}</span>
      </button>
      {timeLeft !== null && (
        <span className={`strip-timer ${timeLeft < 20_000 ? 'low' : ''}`} role="timer" aria-label={t('mode.timeLeft', { time: formatClock(timeLeft) })}>
          {formatClock(timeLeft)}
        </span>
      )}
      <button type="button" className="strip-btn" onClick={() => setDialog('goals')}>
        {t('goals.button', { done: doneCount, total: goals.length })}
      </button>
    </div>
  );

  const sidePanel = (
    <aside className="side-panel" aria-label={t('goals.title')}>
      <GoalsPanel lang={lang} goals={goalProgress} goalStreak={goalStreak} dailyBest={dailyBest} onPlayDaily={playDaily} />
    </aside>
  );

  const dialogs = (
    <>
      {showInstallHelp && (install.kind === 'ios' || install.kind === 'safari-mac' || install.kind === 'manual') && (
        <InstallDialog lang={lang} kind={install.kind} onClose={() => setShowInstallHelp(false)} />
      )}
      {dialog === 'newgame' && (
        <NewGameDialog
          lang={lang}
          current={newGameMode ?? engine.mode}
          inProgress={!engine.isGameOver && engine.moves > 0 ? { score: engine.score, moves: engine.moves } : null}
          stats={Object.fromEntries(MODE_IDS.map((id) => [id, { games: progress.gamesByMode[id], best: progress.bestByMode[id] }])) as Record<ModeId, { games: number; best: number }>}
          onStart={startGame}
          onClose={closeDialog}
        />
      )}
      {dialog === 'goals' && (
        <GoalsDialog
          lang={lang}
          goals={goalProgress}
          goalStreak={goalStreak}
          dailyBest={dailyBest}
          onPlayDaily={playDaily}
          onClose={closeDialog}
        />
      )}
      {engine.isGameOver && dialog !== 'newgame' && dialog !== 'goals' && (
        <GameOverDialog
          lang={lang}
          score={engine.score}
          best={Math.max(engine.score, bestScore)}
          newRecord={newRecord}
          xpGained={lastResult.xp}
          levelUp={lastResult.levelUp}
          unlocked={lastResult.unlocked}
          goalsReached={lastResult.goals}
          goalXp={lastResult.goalXp}
          onPlayAgain={() => startGame(engine.mode)}
          onChangeMode={requestNewGame}
        />
      )}
      {!engine.isGameOver && dialog === 'help' && <HelpDialog lang={lang} onClose={closeDialog} />}
      {!engine.isGameOver && dialog === 'stats' && (
        <StatsDialog
          lang={lang}
          history={history}
          progress={progress}
          ledger={ledger}
          now={statsNow}
          onExport={handleExport}
          onImport={handleImport}
          onClear={handleClearHistory}
          onClose={closeDialog}
        />
      )}
      {!engine.isGameOver && dialog === 'settings' && (
        <SettingsDialog
          lang={lang}
          theme={theme}
          onTheme={changeTheme}
          langPref={langPref}
          onLangPref={changeLanguage}
          soundEnabled={soundEnabled}
          onToggleSound={toggleSound}
          spawnPreview={spawnPreview}
          onTogglePreview={togglePreview}
          playerName={playerName}
          onPlayerName={changePlayerName}
          effects={effectsLevel}
          onEffects={changeEffects}
          vibration={vibration}
          onVibration={changeVibration}
          onClose={closeDialog}
        />
      )}
    </>
  );

  const footerNode = (
    <footer className="footer-row">
      <span className="footer-credit">{t('app.tagline')}</span>
      <div className="footer-tools">
        {installButton}
        <a
          href={LATEST_APK_URL}
          download="ColorLines.apk"
          rel="noreferrer"
          className="ctrl-btn ctrl-link"
          title={t('btn.apk')}
        >
          <Download size={16} aria-hidden="true" />
          <span>APK</span>
        </a>
        <a
          href={GITHUB_REPO_URL}
          target="_blank"
          rel="noreferrer"
          className="ctrl-btn ctrl-link"
          title={t('btn.github')}
          aria-label={t('btn.github')}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
          </svg>
          <span>GitHub</span>
        </a>
      </div>
    </footer>
  );

  // The captions of the 1992 screen stay in English, like the original, whatever the language of the app.
  const names = dosNames({
    kingName: hall.length > 0 ? hall[0].name : translate('en', 'dos.defaultKing'),
    crowned: coronationStart !== null,
    playerName,
    pretenderLabel: translate('en', 'dos.pretender'),
    defaultPlayerName: translate('en', 'dos.defaultName'),
  });

  const dosView = (
    <div className="game-window dos-window">
      <div className="sr-only" role="status" aria-live="polite">
        {announcement}
      </div>
      <DosScreen
        state={{
          cells: Array.from({ length: 81 }, (_, i) => engine.board.get(i % 9, Math.floor(i / 9))),
          selected: engine.selectedPoint,
          next: engine.nextColors,
          showNext,
          score: engine.score,
          kingScore: kingOf(hall).score,
          soundOn: soundEnabled,
          effects,
          coronationStart,
        }}
        // The dethroned king keeps his name and record; the crowned pretender is the player.
        kingName={names.king}
        pretenderName={names.pretender}
        // The picture is the original: English captions, buttons and windows in every app language.
        lang="en"
        window={dosWindow}
        hall={hall}
        labels={{ help: t('dos.help'), sound: t('dos.sound'), next: t('dos.next'), restart: t('dos.restart') }}
        onButton={(id) => {
          if (id === 'help') setDosWindow((w) => (w === 'help' ? 'none' : 'help'));
          else if (id === 'sound') toggleSound();
          else if (id === 'next') toggleNext();
          else handleNewGame();
        }}
        onCloseWindow={() => setDosWindow('none')}
      >
        <div className="dos-grid" role="group" aria-label={t('board.label')}>
          {cellButtons(false)}
        </div>
      </DosScreen>
      {modeStrip}
      <div className="dos-tools">
        <button type="button" className="ctrl-btn" onClick={handleUndo} disabled={!engine.canUndo} title={t('btn.undo')} aria-label={t('btn.undo')}>
          <Undo2 size={20} aria-hidden="true" />
        </button>
        <button type="button" className="ctrl-btn" onClick={handleHint} disabled={engine.isGameOver || hintsLeft <= 0} title={hintTitle} aria-label={hintTitle}>
          <Lightbulb size={20} aria-hidden="true" />
        </button>
        <button type="button" className="ctrl-btn" onClick={() => setDosWindow((w) => (w === 'top10' ? 'none' : 'top10'))} title={t('dos.topTen')} aria-label={t('dos.topTen')} aria-pressed={dosWindow === 'top10'}>
          <Trophy size={20} aria-hidden="true" />
        </button>
        <button type="button" className="ctrl-btn" onClick={openStats} title={t('btn.stats')} aria-label={t('btn.stats')}>
          <BarChart3 size={20} aria-hidden="true" />
        </button>
        <button type="button" className="ctrl-btn" onClick={() => setDialog('settings')} title={t('btn.settings')} aria-label={t('btn.settings')}>
          <Settings size={20} aria-hidden="true" />
        </button>
      </div>
      {footerNode}
    </div>
  );

  if (dos) {
    return (
      <div className={`app-container theme-${theme} has-side`}>
        {dosView}
        {sidePanel}
        {dialogs}
      </div>
    );
  }

  return (
    <div className={`app-container theme-${theme} has-side`}>
      <div className="game-window">
        <div className="sr-only" role="status" aria-live="polite">
          {announcement}
        </div>

        {theme === 'lines98' && (
          <>
            <div className="win98-titlebar">
              <span>{t('app.name')}</span>
              <div className="win98-titlebar-buttons">
                <button
                  type="button"
                  className="win98-btn"
                  onClick={() => setDialog('help')}
                  aria-label={t('btn.help')}
                >
                  ?
                </button>
              </div>
            </div>
            <WinMenu
              label={t('menu.game')}
              groups={[
                {
                  label: t('menu.game'),
                  items: [
                    { label: t('newgame.title'), onSelect: requestNewGame },
                    { label: t('btn.undo'), onSelect: handleUndo, disabled: !engine.canUndo },
                    { label: hintTitle, onSelect: handleHint, disabled: engine.isGameOver || hintsLeft <= 0 },
                    ...MODE_IDS.map((id, i) => ({
                      label: t(`mode.${id}` as MessageKey),
                      checked: engine.mode === id,
                      separatorBefore: i === 0,
                      onSelect: () => {
                        setNewGameMode(id);
                        setDialog('newgame');
                      },
                    })),
                    { label: t('settings.sound'), checked: soundEnabled, onSelect: toggleSound, separatorBefore: true },
                    { label: t('btn.settings'), onSelect: () => setDialog('settings') },
                  ],
                },
                {
                  label: t('menu.score'),
                  items: [
                    { label: t('btn.stats'), onSelect: openStats },
                    { label: t('goals.title'), onSelect: () => setDialog('goals') },
                  ],
                },
                {
                  label: t('menu.help'),
                  items: [{ label: t('btn.help'), onSelect: () => setDialog('help') }],
                },
              ]}
            />
          </>
        )}

        {theme === 'lines98' ? (
          <header className="hud-header l98-header">
            {/* Like the original: best score, the next balls and the score on one black LED panel. */}
            <div className="l98-panel">
              <div className="l98-cell" role="group" aria-label={t('hud.best')}>
                <LedNumber value={Math.max(engine.score, bestScore)} />
              </div>
              <div
                className="l98-next"
                role="img"
                aria-label={t('next.label', {
                  colors: engine.nextColors.map((c) => colorName(lang, c)).join(', '),
                })}
              >
                {engine.nextColors.map((color, idx) => (
                  <div key={idx} className={`ball ball-mini color-${color}`}>
                    <img src={getSpriteUrl(color)} alt="" className="ball-classic" />
                  </div>
                ))}
              </div>
              <div className="l98-cell" role="group" aria-label={t('hud.score')}>
                <LedNumber value={engine.score} />
              </div>
            </div>
          </header>
        ) : (
        <header className="hud-header">
          <div className="hud-top-row">
            <h1 className="game-title">{t('app.name')}</h1>
            <div className="hud-stats">
              <div className="stat-box">
                <div className="stat-label">{t('hud.score')}</div>
                <div className="stat-value">{sprites ? <LedNumber value={engine.score} /> : engine.score}</div>
              </div>
              <div className="stat-box">
                <div className="stat-label">{t('hud.best')}</div>
                <div className="stat-value">
                  {sprites ? <LedNumber value={Math.max(engine.score, bestScore)} /> : Math.max(engine.score, bestScore)}
                </div>
              </div>
            </div>
          </div>

          <div className="hud-action-row">
            <div
              className="next-balls-preview"
              role="img"
              aria-label={t('next.label', {
                colors: engine.nextColors.map((c) => colorName(lang, c)).join(', '),
              })}
            >
              <span className="next-balls-label" aria-hidden="true">
                {t('hud.next')}
              </span>
              <div className="next-balls-list" aria-hidden="true">
                {engine.nextColors.map((color, idx) => (
                  <div key={idx} className={`ball ball-mini ${ballThemeClass(theme)} color-${color}`}>
                    {sprites && <img src={getSpriteUrl(color)} alt="" className="ball-classic" />}
                  </div>
                ))}
              </div>
            </div>

            <div className="controls-group">
              <button
                type="button"
                className="ctrl-btn"
                onClick={handleUndo}
                disabled={!engine.canUndo}
                title={t('btn.undo')}
                aria-label={t('btn.undo')}
              >
                <Undo2 size={20} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={handleHint}
                disabled={engine.isGameOver || hintsLeft <= 0}
                title={hintTitle}
                aria-label={hintTitle}
              >
                <Lightbulb size={20} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={handleNewGame}
                title={t('btn.newGame')}
                aria-label={t('btn.newGame')}
              >
                <RotateCcw size={20} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={openStats}
                title={t('btn.stats')}
                aria-label={t('btn.stats')}
              >
                <BarChart3 size={20} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={toggleSound}
                title={soundEnabled ? t('btn.mute') : t('btn.unmute')}
                aria-label={soundEnabled ? t('btn.mute') : t('btn.unmute')}
                aria-pressed={!soundEnabled}
              >
                {soundEnabled ? (
                  <Volume2 size={20} aria-hidden="true" />
                ) : (
                  <VolumeX size={20} aria-hidden="true" />
                )}
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={() => setDialog('settings')}
                title={t('btn.settings')}
                aria-label={t('btn.settings')}
              >
                <Settings size={20} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={() => setDialog('help')}
                title={t('btn.help')}
                aria-label={t('btn.help')}
              >
                <HelpCircle size={20} aria-hidden="true" />
              </button>
            </div>
          </div>
        </header>
        )}

        {modeStrip}

        <main className="board-container" ref={boardRef}>
          <div className="board-grid" role="group" aria-label={t('board.label')}>
            {cellButtons(true)}
          </div>
        </main>

        {footerNode}
      </div>

      {sidePanel}
      {dialogs}
    </div>
  );
}
