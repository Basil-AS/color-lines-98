import { useState, useEffect, useCallback, useRef } from 'react';
import type { KeyboardEvent } from 'react';
import confetti from 'canvas-confetti';
import {
  RotateCcw,
  Undo2,
  Volume2,
  VolumeX,
  HelpCircle,
  Download
} from 'lucide-react';
import { GameEngine } from './engine/gameengine';
import type { BallColor, Point } from './engine/models';
import { pointsEqual } from './engine/models';
import { soundManager } from './audio';
import {
  loadBestScore,
  loadGame,
  loadTheme,
  saveBestScore,
  saveGame,
  saveTheme,
} from './storage';
import type { Theme } from './storage';
import './App.css';

const BOARD_SIZE = 9;

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function describeCell(
  x: number,
  y: number,
  color: BallColor | null,
  selected: boolean,
  reachable: boolean
): string {
  const content = color ? `${color} ball` : 'empty';
  return `Row ${y + 1}, column ${x + 1}, ${content}${selected ? ', selected' : ''}${
    reachable ? ', reachable' : ''
  }`;
}

const GITHUB_REPO_URL = 'https://github.com/Basil-AS/color-lines-98';
const GITHUB_RELEASES_URL = 'https://github.com/Basil-AS/color-lines-98/releases';

export default function App() {
  const [engine] = useState(() => loadGame() ?? new GameEngine(BOARD_SIZE, 3, 5, 'gamos'));
  const [, setVersion] = useState(0); // Bumped after every engine mutation to re-render
  const [theme, setTheme] = useState<Theme>(loadTheme);
  const [soundEnabled, setSoundEnabled] = useState(() => soundManager.isEnabled);
  const [showHelp, setShowHelp] = useState(false);
  const [bestScore, setBestScore] = useState(loadBestScore);
  const [announcement, setAnnouncement] = useState('');
  const [focusCell, setFocusCell] = useState<Point>({ x: 0, y: 0 });
  const cellRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // Re-render and persist the game after the engine has been mutated by a handler.
  const commit = useCallback(() => {
    setVersion((v) => v + 1);
    saveGame(engine);
  }, [engine]);

  const changeTheme = (newTheme: Theme) => {
    setTheme(newTheme);
    saveTheme(newTheme);
  };

  const toggleSound = () => {
    const next = soundManager.toggle();
    setSoundEnabled(next);
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
      setAnnouncement('No path to that cell');
      return;
    }

    engine.unselect();
    soundManager.playJump();

    if (res.clearedPoints.length > 0) {
      soundManager.playEat(res.pointsEarned);
      setAnnouncement(`Line cleared, plus ${res.pointsEarned} points. Score ${engine.score}`);
      if (res.pointsEarned >= 18 && !prefersReducedMotion()) {
        confetti({
          particleCount: 50 + res.pointsEarned * 2,
          spread: 60,
          origin: { y: 0.6 },
        });
      }
    } else {
      setAnnouncement(`Score ${engine.score}`);
    }

    if (res.isGameOver) {
      soundManager.playLose();
      setAnnouncement(`Game over. Final score ${engine.score}`);
    }

    if (engine.score > bestScore) {
      setBestScore(engine.score);
      saveBestScore(engine.score);
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
      setAnnouncement(`Move undone. Score ${engine.score}`);
      commit();
    }
  };

  const handleNewGame = () => {
    engine.startNewGame();
    soundManager.playClick();
    setAnnouncement('New game started');
    commit();
  };

  const reachableCells = engine.getReachableCells();

  const getSpriteUrl = (color: BallColor): string => {
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
  };

  useEffect(() => {
    if (!showHelp) return;
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') setShowHelp(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [showHelp]);

  useEffect(() => {
    document.body.className = `theme-${theme}`;
  }, [theme]);

  return (
    <div className={`app-container theme-${theme}`}>
      <div className="game-window">
        <div className="sr-only" role="status" aria-live="polite">
          {announcement}
        </div>
        {/* Win98 Window Titlebar */}
        {theme === 'classic98' && (
          <div className="win98-titlebar">
            <span>Color Lines 98</span>
            <div className="win98-titlebar-buttons">
              <button className="win98-btn" onClick={() => setShowHelp(true)} aria-label="Rules and info">?</button>
              <button className="win98-btn" onClick={handleNewGame} aria-label="New game">×</button>
            </div>
          </div>
        )}

        {/* HUD Header */}
        <header className="hud-header">
          <div className="hud-top-row">
            <h1 className="game-title">Color Lines 98</h1>
            <div className="hud-stats">
              <div className="stat-box">
                <div className="stat-label">Score</div>
                <div className="stat-value">{engine.score}</div>
              </div>
              <div className="stat-box">
                <div className="stat-label">Best</div>
                <div className="stat-value">{Math.max(engine.score, bestScore)}</div>
              </div>
            </div>
          </div>

          <div className="hud-action-row">
            <div className="next-balls-preview">
              <span className="next-balls-label">Next:</span>
              <div className="next-balls-list" role="img" aria-label={`Next balls: ${engine.nextColors.join(', ')}`}>
                {engine.nextColors.map((color, idx) => (
                  <div
                    key={idx}
                    className={`ball ball-mini ${
                      theme === 'classic98' ? '' : theme === 'retro92' ? 'ball-retro' : 'ball-modern'
                    } color-${color}`}
                  >
                    {theme === 'classic98' && (
                      <img src={getSpriteUrl(color)} alt="" className="ball-classic" />
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div className="controls-group">
              <button
                className="ctrl-btn"
                onClick={handleUndo}
                disabled={!engine.canUndo}
                title="Undo move"
                aria-label="Undo move"
              >
                <Undo2 size={18} />
              </button>
              <button
                className="ctrl-btn"
                onClick={handleNewGame}
                title="New Game"
                aria-label="New game"
              >
                <RotateCcw size={18} />
              </button>
              <button
                className="ctrl-btn"
                onClick={toggleSound}
                title={soundEnabled ? 'Mute sound' : 'Unmute sound'}
                aria-label={soundEnabled ? 'Mute sound' : 'Unmute sound'}
                aria-pressed={!soundEnabled}
              >
                {soundEnabled ? <Volume2 size={18} /> : <VolumeX size={18} />}
              </button>
              <button
                className="ctrl-btn"
                onClick={() => setShowHelp(true)}
                title="Rules & Info"
                aria-label="Rules and info"
              >
                <HelpCircle size={18} />
              </button>
            </div>
          </div>
        </header>

        {/* 9x9 Board */}
        <main className="board-container">
          <div className="board-grid" role="group" aria-label="Game board">
            {Array.from({ length: BOARD_SIZE }).map((_, y) =>
              Array.from({ length: BOARD_SIZE }).map((_, x) => {
                const color = engine.board.get(x, y);
                const isSelected =
                  engine.selectedPoint?.x === x && engine.selectedPoint?.y === y;
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
                    aria-label={describeCell(x, y, color, isSelected, isReachable)}
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
                        className={`ball ${
                          isSelected ? 'selected-ball' : ''
                        } ${
                          theme === 'classic98'
                            ? ''
                            : theme === 'retro92'
                            ? 'ball-retro'
                            : 'ball-modern'
                        } color-${color}`}
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

        {/* Footer & Themes */}
        <footer className="footer-row">
          <div className="theme-selector">
            <button
              className={`theme-opt-btn ${theme === 'modern' ? 'active' : ''}`}
              onClick={() => changeTheme('modern')}
              aria-pressed={theme === 'modern'}
            >
              Modern
            </button>
            <button
              className={`theme-opt-btn ${theme === 'classic98' ? 'active' : ''}`}
              onClick={() => changeTheme('classic98')}
              aria-pressed={theme === 'classic98'}
            >
              Win98
            </button>
            <button
              className={`theme-opt-btn ${theme === 'retro92' ? 'active' : ''}`}
              onClick={() => changeTheme('retro92')}
              aria-pressed={theme === 'retro92'}
            >
              DOS92
            </button>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <a
              href={GITHUB_RELEASES_URL}
              target="_blank"
              rel="noreferrer"
              className="ctrl-btn"
              title="Download Android APK"
              style={{ textDecoration: 'none', gap: '4px', padding: '4px 8px', fontSize: '0.75rem' }}
            >
              <Download size={14} /> APK
            </a>
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noreferrer"
              className="ctrl-btn"
              title="GitHub Repo"
              style={{ textDecoration: 'none', padding: '4px 8px', display: 'flex', alignItems: 'center' }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
              </svg>
            </a>
          </div>
        </footer>
      </div>

      {/* Game Over Modal */}
      {engine.isGameOver && (
        <div className="modal-overlay">
          <div className="modal-content" role="alertdialog" aria-modal="true" aria-labelledby="game-over-title">
            <h2 className="modal-title" id="game-over-title">Game Over!</h2>
            <p className="modal-text">
              Final Score: <strong>{engine.score}</strong>
              <br />
              Best Score: <strong>{bestScore}</strong>
            </p>
            <button className="modal-btn" onClick={handleNewGame} autoFocus>
              Play Again
            </button>
          </div>
        </div>
      )}

      {/* Help Modal */}
      {showHelp && (
        <div className="modal-overlay" onClick={() => setShowHelp(false)}>
          <div className="modal-content" role="dialog" aria-modal="true" aria-labelledby="help-title" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title" id="help-title">Rules & Scoring</h2>
            <p className="modal-text" style={{ textAlign: 'left', lineHeight: '1.5' }}>
              <strong>Objective:</strong> Align 5 or more balls of the same color horizontally, vertically, or diagonally.
              <br /><br />
              <strong>Scoring (Gamos 1992):</strong>
              <br />• 5 balls: 10 pts
              <br />• 6 balls: 12 pts
              <br />• 7 balls: 18 pts
              <br />• 8 balls: 28 pts
              <br />• 9 balls: 42 pts
              <br /><br />
              <strong>Movement:</strong> A ball can only move if there is a clear, unblocked path to the destination cell. Clearing a line gives a free turn: no new balls appear.
              <br /><br />
              <strong>Keyboard:</strong> arrow keys move between cells, Enter or Space selects a ball or moves it, Escape closes this window.
            </p>
            <button className="modal-btn" onClick={() => setShowHelp(false)} autoFocus>
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
