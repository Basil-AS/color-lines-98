import { useState, useEffect, useCallback, useRef } from 'react';
import type { KeyboardEvent } from 'react';
import confetti from 'canvas-confetti';
import {
  BarChart3,
  Download,
  HelpCircle,
  RotateCcw,
  Undo2,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { GameEngine } from './engine/gameengine';
import type { BallColor, Point } from './engine/models';
import { pointsEqual } from './engine/models';
import { soundManager } from './audio';
import {
  LANGUAGES,
  colorName,
  resolveLanguage,
  translate,
} from './i18n';
import type { Language, MessageKey } from './i18n';
import {
  THEMES,
  clearHistory,
  loadBestScore,
  loadGame,
  loadHistory,
  loadLanguagePref,
  loadTheme,
  saveBestScore,
  saveGame,
  saveHistory,
  saveLanguagePref,
  saveTheme,
} from './storage';
import type { LanguagePref, Theme } from './storage';
import { addRecord, isNewRecord, recordFromEngine, summarize } from './stats';
import type { GameRecord } from './stats';
import { GameOverDialog } from './components/GameOverDialog';
import { HelpDialog } from './components/HelpDialog';
import { StatsDialog } from './components/StatsDialog';
import './App.css';

const BOARD_SIZE = 9;
const GITHUB_REPO_URL = 'https://github.com/Basil-AS/color-lines-98';
const GITHUB_RELEASES_URL = `${GITHUB_REPO_URL}/releases/latest`;

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function browserLanguages(): readonly string[] {
  return navigator.languages?.length ? navigator.languages : [navigator.language];
}

function applyDocumentLanguage(lang: Language): void {
  document.documentElement.lang = lang;
  document.title = translate(lang, 'app.docTitle');
}

function withRecord(history: readonly GameRecord[], engine: GameEngine, completed: boolean) {
  return addRecord(history, recordFromEngine(engine, completed, Date.now()));
}

function ballThemeClass(theme: Theme): string {
  return theme === 'classic98' ? '' : theme === 'retro92' ? 'ball-retro' : 'ball-modern';
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

export default function App() {
  const [engine] = useState(() => loadGame() ?? new GameEngine(BOARD_SIZE, 3, 5, 'gamos'));
  const [, setVersion] = useState(0); // Bumped after every engine mutation to re-render
  const [theme, setTheme] = useState<Theme>(loadTheme);
  const [langPref, setLangPref] = useState<LanguagePref>(loadLanguagePref);
  const [soundEnabled, setSoundEnabled] = useState(() => soundManager.isEnabled);
  const [dialog, setDialog] = useState<'help' | 'stats' | null>(null);
  const [history, setHistory] = useState(loadHistory);
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

  const recordGame = (completed: boolean) => {
    const next = withRecord(history, engine, completed);
    setHistory(next);
    saveHistory(next);
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

  const handleCellClick = (x: number, y: number) => {
    setFocusCell({ x, y });
    if (engine.isGameOver) return;

    const clickedPoint: Point = { x, y };

    if (engine.board.get(x, y)) {
      if (engine.selectedPoint && pointsEqual(engine.selectedPoint, clickedPoint)) {
        engine.unselect();
      } else {
        engine.select(clickedPoint);
        soundManager.playSelect();
      }
      commit();
      return;
    }

    if (!engine.selectedPoint) return;

    const res = engine.moveBall(engine.selectedPoint, clickedPoint);
    if (!res.success) {
      soundManager.playClick();
      setAnnouncement(t('announce.noPath'));
      return;
    }

    engine.unselect();
    soundManager.playJump();

    if (res.clearedPoints.length > 0) {
      soundManager.playEat(res.pointsEarned);
      setAnnouncement(t('announce.lineCleared', { points: res.pointsEarned, score: engine.score }));
      if (res.pointsEarned >= 18 && !prefersReducedMotion()) {
        confetti({ particleCount: 50 + res.pointsEarned * 2, spread: 60, origin: { y: 0.6 } });
      }
    } else {
      setAnnouncement(t('announce.score', { score: engine.score }));
    }

    if (engine.score > bestScore) {
      setBestScore(engine.score);
      saveBestScore(engine.score);
    }

    if (res.isGameOver) {
      soundManager.playLose();
      setAnnouncement(t('announce.gameOver', { score: engine.score }));
      recordGame(true);
    }
    commit();
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

  const handleUndo = () => {
    if (engine.undo()) {
      soundManager.playClick();
      setAnnouncement(t('announce.undone', { score: engine.score }));
      commit();
    }
  };

  const handleNewGame = () => {
    // Abandoning a game in progress still counts towards the history.
    if (!engine.isGameOver && engine.moves > 0) recordGame(false);
    engine.startNewGame();
    setBestAtGameStart(bestScore);
    soundManager.playClick();
    setAnnouncement(t('announce.newGame'));
    commit();
  };

  const closeDialog = useCallback(() => setDialog(null), []);

  const handleClearHistory = () => {
    clearHistory();
    setHistory([]);
  };

  useEffect(() => {
    document.body.className = `theme-${theme}`;
  }, [theme]);

  useEffect(() => {
    applyDocumentLanguage(lang);
  }, [lang]);

  const reachableCells = engine.getReachableCells();
  const newRecord = engine.isGameOver && isNewRecord(engine.score, bestAtGameStart);
  const cellText = (x: number, y: number, color: BallColor | null, sel: boolean, reach: boolean) =>
    t('cell.description', {
      row: y + 1,
      col: x + 1,
      content: color ? t('cell.ball', { color: colorName(lang, color) }) : t('cell.empty'),
      state: `${sel ? t('cell.selected') : ''}${reach ? t('cell.reachable') : ''}`,
    });

  return (
    <div className={`app-container theme-${theme}`}>
      <div className="game-window">
        <div className="sr-only" role="status" aria-live="polite">
          {announcement}
        </div>

        {theme === 'classic98' && (
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
        )}

        <header className="hud-header">
          <div className="hud-top-row">
            <h1 className="game-title">{t('app.name')}</h1>
            <div className="hud-stats">
              <div className="stat-box">
                <div className="stat-label">{t('hud.score')}</div>
                <div className="stat-value">{engine.score}</div>
              </div>
              <div className="stat-box">
                <div className="stat-label">{t('hud.best')}</div>
                <div className="stat-value">{Math.max(engine.score, bestScore)}</div>
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
                    {theme === 'classic98' && (
                      <img src={getSpriteUrl(color)} alt="" className="ball-classic" />
                    )}
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
                onClick={handleNewGame}
                title={t('btn.newGame')}
                aria-label={t('btn.newGame')}
              >
                <RotateCcw size={20} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="ctrl-btn"
                onClick={() => setDialog('stats')}
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
                onClick={() => setDialog('help')}
                title={t('btn.help')}
                aria-label={t('btn.help')}
              >
                <HelpCircle size={20} aria-hidden="true" />
              </button>
            </div>
          </div>
        </header>

        <main className="board-container">
          <div className="board-grid" role="group" aria-label={t('board.label')}>
            {Array.from({ length: BOARD_SIZE }).map((_, y) =>
              Array.from({ length: BOARD_SIZE }).map((_, x) => {
                const color = engine.board.get(x, y);
                const isSelected = engine.selectedPoint?.x === x && engine.selectedPoint?.y === y;
                const isReachable = !color && reachableCells.has(`${x},${y}`);
                const isFocusStop = focusCell.x === x && focusCell.y === y;

                return (
                  <button
                    type="button"
                    key={`${x}-${y}`}
                    ref={(el) => {
                      cellRefs.current[y * BOARD_SIZE + x] = el;
                    }}
                    tabIndex={isFocusStop ? 0 : -1}
                    aria-label={cellText(x, y, color, isSelected, isReachable)}
                    aria-pressed={color ? isSelected : undefined}
                    className={`board-cell ${isSelected ? 'selected' : ''} ${
                      isReachable ? 'reachable' : ''
                    }`}
                    onClick={() => handleCellClick(x, y)}
                    onKeyDown={(e) => handleCellKeyDown(e, x, y)}
                  >
                    {color && (
                      <div
                        aria-hidden="true"
                        className={`ball ${isSelected ? 'selected-ball' : ''} ${ballThemeClass(
                          theme
                        )} color-${color}`}
                      >
                        {theme === 'classic98' && (
                          <img src={getSpriteUrl(color)} alt="" className="ball-classic" />
                        )}
                      </div>
                    )}
                  </button>
                );
              })
            )}
          </div>
        </main>

        <footer className="footer-row">
          <div className="theme-selector" role="group" aria-label={t('theme.group')}>
            {THEMES.map((id) => (
              <button
                type="button"
                key={id}
                className={`theme-opt-btn ${theme === id ? 'active' : ''}`}
                onClick={() => changeTheme(id)}
                aria-pressed={theme === id}
              >
                {t(`theme.${id}` as MessageKey)}
              </button>
            ))}
          </div>

          <div className="footer-tools">
            <label className="lang-select">
              <span className="sr-only">{t('lang.label')}</span>
              <select
                value={langPref}
                onChange={(e) => changeLanguage(e.target.value as LanguagePref)}
              >
                <option value="auto">{t('lang.auto')}</option>
                {LANGUAGES.map((l) => (
                  <option key={l} value={l}>
                    {t(`lang.${l}` as MessageKey)}
                  </option>
                ))}
              </select>
            </label>
            <a
              href={GITHUB_RELEASES_URL}
              target="_blank"
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
              aria-label={`${t('btn.github')}: Basil-AS`}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
              </svg>
              <span>Basil-AS</span>
            </a>
          </div>
        </footer>
      </div>

      {engine.isGameOver && (
        <GameOverDialog
          lang={lang}
          score={engine.score}
          best={Math.max(engine.score, bestScore)}
          newRecord={newRecord}
          onPlayAgain={handleNewGame}
        />
      )}
      {!engine.isGameOver && dialog === 'help' && <HelpDialog lang={lang} onClose={closeDialog} />}
      {!engine.isGameOver && dialog === 'stats' && (
        <StatsDialog
          lang={lang}
          history={history}
          onClear={handleClearHistory}
          onClose={closeDialog}
        />
      )}
    </div>
  );
}
