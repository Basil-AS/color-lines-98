// @vitest-environment jsdom
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('canvas-confetti', () => ({ default: vi.fn() }));
vi.mock('../src/audio', () => {
  let enabled = true;
  return {
    soundManager: {
      get isEnabled() {
        return enabled;
      },
      toggle: () => (enabled = !enabled),
      setProfile: vi.fn(),
      playSelect: vi.fn(),
      playJump: vi.fn(),
      playEat: vi.fn(),
      playLose: vi.fn(),
      playWin: vi.fn(),
      playClick: vi.fn(),
    },
  };
});

import App from '../src/App';
import { GameEngine } from '../src/engine/gameengine';
import { saveGame } from '../src/storage';

function setLanguages(languages: string[]) {
  Object.defineProperty(window.navigator, 'languages', { value: languages, configurable: true });
  Object.defineProperty(window.navigator, 'language', { value: languages[0], configurable: true });
}

const cells = () => screen.getAllByRole('button', { name: /^(Row|Ряд) \d+, (column|столбец) \d+/ });
const ballCells = () => cells().filter((c) => /(\w+ ball|шар)(,|$)/.test(c.getAttribute('aria-label')!) && !/empty|пусто/.test(c.getAttribute('aria-label')!));
const incomingCells = () => cells().filter((c) => /will appear|появится/.test(c.getAttribute('aria-label')!));
const reachableCells = () => cells().filter((c) => /reachable|можно дойти/.test(c.getAttribute('aria-label')!));

async function playOneMove(user: ReturnType<typeof userEvent.setup>) {
  await user.click(ballCells()[0]);
  await user.click(reachableCells()[0]);
}

beforeEach(() => {
  localStorage.clear();
  setLanguages(['en-US']);
});
afterEach(() => cleanup());

describe('board', () => {
  it('shows a full board, five balls and three announced cells', () => {
    render(<App />);
    expect(cells()).toHaveLength(81);
    expect(ballCells()).toHaveLength(5);
    expect(incomingCells()).toHaveLength(3);
    expect(screen.getByRole('group', { name: 'Game board' })).toBeInTheDocument();
  });

  it('selects a ball, shows where it can go and deselects it again', async () => {
    const user = userEvent.setup();
    render(<App />);
    const ball = ballCells()[0];
    await user.click(ball);
    expect(ball).toHaveAttribute('aria-pressed', 'true');
    expect(reachableCells().length).toBeGreaterThan(0);
    await user.click(ball);
    expect(ball).toHaveAttribute('aria-pressed', 'false');
    expect(reachableCells()).toHaveLength(0);
  });

  it('moves a ball, spawns new ones and announces new cells again', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    expect(ballCells().length).toBeGreaterThanOrEqual(3);
    expect(incomingCells()).toHaveLength(3);
    expect(screen.getByRole('status')).toHaveTextContent(/Score \d+|Line cleared/);
  });

  it('starts from scratch on "New game" (the old board must not stay)', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    expect(ballCells()).toHaveLength(5);
    expect(screen.getByRole('button', { name: 'Undo move' })).toBeDisabled();
  });

  it('undoes a move', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    await user.click(screen.getByRole('button', { name: 'Undo move' }));
    expect(ballCells()).toHaveLength(5);
  });

  it('supports the keyboard: arrows move focus, Enter selects', async () => {
    const user = userEvent.setup();
    render(<App />);
    cells()[0].focus();
    await user.keyboard('{ArrowRight}{ArrowDown}');
    expect(document.activeElement).toBe(cells()[10]);
    await user.keyboard('{ArrowLeft}{ArrowUp}{ArrowUp}{ArrowLeft}');
    expect(document.activeElement).toBe(cells()[0]);
  });

  it('resumes a saved game after a reload', async () => {
    const user = userEvent.setup();
    const first = render(<App />);
    await playOneMove(user);
    const before = cells().map((c) => c.getAttribute('aria-label'));
    first.unmount();
    render(<App />);
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(before);
  });
});

describe('spawn preview setting', () => {
  it('can be switched off, is remembered and can be switched on again', async () => {
    const user = userEvent.setup();
    const first = render(<App />);
    await user.click(screen.getByRole('button', { name: 'Settings' }));
    const toggle = screen.getByRole('switch', { name: /where new balls will appear/i });
    expect(toggle).toBeChecked();
    await user.click(toggle);
    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(incomingCells()).toHaveLength(0);
    first.unmount();

    render(<App />);
    expect(incomingCells()).toHaveLength(0);
    await user.click(screen.getByRole('button', { name: 'Settings' }));
    await user.click(screen.getByRole('switch', { name: /where new balls will appear/i }));
    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(incomingCells()).toHaveLength(3);
  });
});

describe('language', () => {
  it('follows the browser language by default', () => {
    setLanguages(['ru-RU', 'en-US']);
    render(<App />);
    expect(screen.getByRole('group', { name: 'Игровое поле' })).toBeInTheDocument();
    expect(document.documentElement.lang).toBe('ru');
    expect(document.title).toContain('Цветные линии');
  });

  it('falls back to English for unsupported languages', () => {
    setLanguages(['de-DE']);
    render(<App />);
    expect(screen.getByRole('group', { name: 'Game board' })).toBeInTheDocument();
    expect(document.documentElement.lang).toBe('en');
  });

  it('can be changed in the settings and is remembered', async () => {
    const user = userEvent.setup();
    const first = render(<App />);
    await user.click(screen.getByRole('button', { name: 'Settings' }));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'ru');
    expect(screen.getByRole('group', { name: 'Игровое поле' })).toBeInTheDocument();
    expect(localStorage.getItem('colorlines_lang')).toBe('ru');
    first.unmount();
    render(<App />);
    expect(screen.getByRole('group', { name: 'Игровое поле' })).toBeInTheDocument();
  });
});

describe('themes', () => {
  it.each(['modern', 'light', 'lines98', 'colorlines92'])('applies and remembers the %s theme', async (theme) => {
    const user = userEvent.setup();
    const first = render(<App />);
    await user.click(screen.getByRole('button', { name: 'Settings' }));
    const group = screen.getByRole('group', { name: 'Theme' });
    const names: Record<string, RegExp> = {
      modern: /Modern dark/,
      light: /Modern light/,
      lines98: /Lines 98/,
      colorlines92: /Color Lines 1992/,
    };
    await user.click(within(group).getByRole('button', { name: names[theme] }));
    expect(document.body.className).toBe(`theme-${theme}`);
    expect(localStorage.getItem('colorlines_theme')).toBe(theme);
    first.unmount();
    render(<App />);
    expect(document.body.className).toBe(`theme-${theme}`);
  });

  it('gives the Windows theme its own title bar and menu', async () => {
    localStorage.setItem('colorlines_theme', 'lines98');
    render(<App />);
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });

  it('migrates the old theme names', () => {
    localStorage.setItem('colorlines_theme', 'classic98');
    render(<App />);
    expect(document.body.className).toBe('theme-lines98');
  });
});

describe('statistics', () => {
  it('starts empty at level 1 and lists an abandoned game afterwards', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    expect(screen.getByText('Level 1')).toBeInTheDocument();
    expect(screen.getAllByText(/No games yet/).length).toBeGreaterThan(0);
    await user.click(screen.getByRole('button', { name: 'Close' }));

    await playOneMove(user);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    expect(screen.getByText('unfinished')).toBeInTheDocument();
  });

  it('asks before deleting the history', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    await user.click(screen.getByRole('button', { name: 'Clear history' }));
    expect(screen.getByText('Delete all saved games?')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByText('unfinished')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear history' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));
    expect(screen.queryByText('unfinished')).not.toBeInTheDocument();
    expect(localStorage.getItem('colorlines_history')).toBe(null);
  });
});

describe('analysis', () => {
  const history = Array.from({ length: 25 }, (_, i) => ({
    score: 200 - i * 4, // newest first: the player is improving
    endedAt: Date.now() - i * 3_600_000,
    moves: 40,
    lines: 4,
    balls: 20,
    completed: true,
    maxLine: 5,
    durationMs: 120_000,
  }));

  it('shows the trend, charts and efficiency once there are enough games', async () => {
    localStorage.setItem('colorlines_history', JSON.stringify(history));
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    expect(screen.getByRole('region', { name: 'Analysis' })).toBeInTheDocument();
    expect(screen.getByText(/better than the 10 before/)).toBeInTheDocument();
    expect(screen.getByText(/points per move/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Games per day, last 14 days/ })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /How your scores are spread/ })).toBeInTheDocument();
  });

  it('asks for more games while the history is short', async () => {
    localStorage.setItem('colorlines_history', JSON.stringify(history.slice(0, 3)));
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    expect(screen.getByText(/Finish 17 more games/)).toBeInTheDocument();
  });
});

describe('dialogs', () => {
  it('closes with Escape and gives the focus back', async () => {
    const user = userEvent.setup();
    render(<App />);
    const opener = screen.getByRole('button', { name: 'Rules and info' });
    await user.click(opener);
    expect(screen.getByRole('dialog', { name: 'Rules & scoring' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.activeElement).toBe(opener);
  });

  it('keeps the focus inside an open dialog', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'Settings' }));
    const dialog = screen.getByRole('dialog');
    for (let i = 0; i < 12; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
  });
});

describe('end of a game', () => {
  function almostFullGame() {
    const engine = new GameEngine(9, 3, 5, 'gamos', () => 0.3);
    engine.board.clear();
    const palette = ['red', 'green', 'blue', 'cyan', 'magenta', 'yellow', 'brown'] as const;
    for (let y = 0; y < 9; y++) for (let x = 0; x < 9; x++) engine.board.set(x, y, palette[(x + 2 * y) % 7]);
    engine.board.set(0, 0, null);
    engine.board.set(1, 0, null);
    engine.board.set(8, 8, null);
    engine.nextSpawnPoints = [
      { x: 0, y: 0 },
      { x: 8, y: 8 },
      { x: 1, y: 0 },
    ];
    engine.nextColors = ['red', 'red', 'green'];
    saveGame(engine);
  }

  it('shows the result, records it and unlocks the first achievement', async () => {
    almostFullGame();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Row 1, column 3,/ }));
    await user.click(screen.getByRole('button', { name: /^Row 1, column 2, empty/ }));

    const dialog = await screen.findByRole('alertdialog', { name: 'Game over' });
    expect(within(dialog).getByText('Final score')).toBeInTheDocument();
    expect(within(dialog).getByText('First steps')).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('colorlines_history')!)).toHaveLength(1);
    expect(JSON.parse(localStorage.getItem('colorlines_progress')!).totalGames).toBe(1);

    // The dialog demands a choice: Escape does nothing, "Play again" starts a fresh game.
    await user.keyboard('{Escape}');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Play again' }));
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(ballCells()).toHaveLength(5);
    // A finished game is recorded once; starting the next one does not add an "abandoned" entry.
    expect(JSON.parse(localStorage.getItem('colorlines_history')!)).toHaveLength(1);
  });
});
