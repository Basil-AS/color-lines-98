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
      play: vi.fn(),
    },
  };
});

import App from '../src/App';
import { GameEngine } from '../src/engine/gameengine';
import { soundManager } from '../src/audio';
import { loadHistory, saveGame } from '../src/storage';
import { THEMES } from '../src/themes';

function setLanguages(languages: string[]) {
  Object.defineProperty(window.navigator, 'languages', { value: languages, configurable: true });
  Object.defineProperty(window.navigator, 'language', { value: languages[0], configurable: true });
}

const storedHistory = () => loadHistory();
const cells = () => screen.getAllByRole('button', { name: /^(Row|Ряд) \d+, (column|столбец) \d+/ });
const ballCells = () => cells().filter((c) => /(\w+ ball|шар)(,|$)/.test(c.getAttribute('aria-label')!) && !/empty|пусто/.test(c.getAttribute('aria-label')!));
const incomingCells = () => cells().filter((c) => /will appear|появится/.test(c.getAttribute('aria-label')!));
const reachableCells = () => cells().filter((c) => /reachable|можно дойти/.test(c.getAttribute('aria-label')!));

/** Opens the New game dialog and starts (optionally picking a mode). */
async function startNewGame(user: ReturnType<typeof userEvent.setup>, mode?: RegExp) {
  await user.click(screen.getByRole('button', { name: 'New game' }));
  if (mode) await user.click(screen.getByRole('radio', { name: mode }));
  await user.click(screen.getByRole('button', { name: /^Start/ }));
}

async function playOneMove(user: ReturnType<typeof userEvent.setup>) {
  await user.click(ballCells()[0]);
  await user.click(reachableCells()[0]);
}

beforeEach(() => {
  vi.mocked(soundManager.play).mockClear();
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
    await startNewGame(user);
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

describe('hint', () => {
  it('selects a ball, limits itself to three per game and resets on a new game', async () => {
    const user = userEvent.setup();
    render(<App />);
    const hint = () => screen.getByRole('button', { name: /^Hint/ });
    expect(hint()).toHaveAccessibleName('Hint (3 left)');
    await user.click(hint());
    expect(hint()).toHaveAccessibleName('Hint (2 left)');
    expect(reachableCells().length).toBeGreaterThan(0); // the suggested ball is selected
    await user.click(hint());
    await user.click(hint());
    expect(hint()).toBeDisabled();
    await startNewGame(user);
    expect(hint()).toHaveAccessibleName('Hint (3 left)');
  });
});

describe('the name of the game', () => {
  it('is Color Lines in the tab title and the page in every language', () => {
    for (const lang of ['en', 'ru']) {
      cleanup();
      localStorage.clear();
      setLanguages([lang]);
      render(<App />);
      expect(document.title).toMatch(/^Color Lines/);
      expect(document.title).not.toMatch(/[А-Яа-я]/);
    }
  });
});

describe('consistency between themes', () => {
  const ACTIONS: [string, RegExp][] = [
    ['new game', /^(New game|F4: Restart)/],
    ['undo', /^Undo move$/],
    ['hint', /^Hint \(\d left\)$/],
    ['statistics', /^Statistics$/],
    ['settings', /^Settings$/],
  ];

  for (const theme of THEMES) {
    it(`${theme}: shows a full board and offers every action`, async () => {
      const user = userEvent.setup();
      localStorage.setItem('colorlines_theme', theme);
      render(<App />);
      expect(cells()).toHaveLength(81);
      const menubar = screen.queryByRole('menubar');
      for (const [what, name] of ACTIONS) {
        let found = screen.queryByRole('button', { name }) !== null;
        if (!found && menubar) {
          // The Windows look keeps its actions in drop-down menus.
          for (const trigger of within(menubar).getAllByRole('menuitem')) {
            await user.click(trigger);
            if (screen.queryByRole('menuitem', { name }) !== null) found = true;
            await user.keyboard('{Escape}');
            if (found) break;
          }
        }
        expect(found, `${theme} lacks "${what}"`).toBe(true);
      }
    });
  }
});

describe('sound events', () => {
  it('plays select, jump and start for the basic actions', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(ballCells()[0]);
    expect(soundManager.play).toHaveBeenCalledWith('select');
    await user.click(reachableCells()[0]);
    expect(soundManager.play).toHaveBeenCalledWith('jump');
    await startNewGame(user);
    expect(soundManager.play).toHaveBeenCalledWith('start');
  });

  it('plays the "blocked" sound for a move that cannot be made', async () => {
    const engine = new GameEngine(9, 3, 5, 'gamos', () => 0.5);
    engine.board.clear();
    // A ball walled in by four others in the corner.
    engine.board.set(0, 0, 'red');
    engine.board.set(1, 0, 'green');
    engine.board.set(0, 1, 'green');
    engine.board.set(5, 5, 'blue');
    saveGame(engine);
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Row 1, column 1,/ }));
    await user.click(screen.getByRole('button', { name: /^Row 9, column 9, empty/ }));
    expect(soundManager.play).toHaveBeenCalledWith('blocked');
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
    expect(document.title).toContain('Color Lines');
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
  it.each(['modern', 'light', 'material', 'neon', 'contrast', 'lines98', 'colorlines92'])('applies and remembers the %s theme', async (theme) => {
    const user = userEvent.setup();
    const first = render(<App />);
    await user.click(screen.getByRole('button', { name: 'Settings' }));
    const group = screen.getByRole('group', { name: 'Theme' });
    const names: Record<string, RegExp> = {
      modern: /Modern dark/,
      light: /Modern light/,
      material: /^Material/,
      neon: /Neon/,
      contrast: /High contrast/,
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
    const user = userEvent.setup();
    render(<App />);
    expect(screen.getByRole('menubar', { name: 'Game' })).toBeInTheDocument();
    // Real drop-down menus: open, use, close with Escape and get the focus back.
    await user.click(screen.getByRole('menuitem', { name: 'Game' }));
    expect(screen.getByRole('menu', { name: 'Game' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    await user.click(screen.getByRole('menuitem', { name: 'Score' }));
    await user.click(screen.getByRole('menuitem', { name: 'Statistics' }));
    expect(screen.getByRole('dialog', { name: 'Statistics' })).toBeInTheDocument();
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
    await startNewGame(user);
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    expect(screen.getByText('unfinished')).toBeInTheDocument();
  });

  it('makes deleting the history a deliberate two-step action', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    await startNewGame(user);
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
    await user.click(screen.getByRole('button', { name: 'Clear history' }));

    // The warning says what is lost; the safe choice has the focus; "Delete" is locked until acknowledged.
    expect(screen.getByText(/This deletes all 1 saved games/)).toBeInTheDocument();
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByRole('button', { name: 'Delete' })).toBeDisabled();
    await user.keyboard('{Enter}'); // Enter on the focused Cancel must not delete anything
    expect(screen.getByText('unfinished')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Clear history' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByText('unfinished')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Clear history' }));
    await user.click(screen.getByRole('checkbox', { name: /cannot be undone/ }));
    expect(screen.getByRole('button', { name: 'Delete' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: 'Delete' }));
    expect(screen.queryByText('unfinished')).not.toBeInTheDocument();
    expect(localStorage.getItem('colorlines_history2')).toBe(null);
    expect(localStorage.getItem('colorlines_progress')).toBe(null);
  });
});

describe('starting a new game', () => {
  it('warns before throwing away a game in progress and puts "Keep playing" in focus', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    const before = cells().map((c) => c.getAttribute('aria-label'));
    await user.click(screen.getByRole('button', { name: 'New game' }));
    expect(screen.getByRole('alert')).toHaveTextContent(/will be counted as unfinished/);
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Keep playing' }));
    await user.keyboard('{Enter}');
    expect(screen.queryByRole('dialog', { name: 'New game' })).not.toBeInTheDocument();
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(before);
    expect(localStorage.getItem('colorlines_history2')).toBe(null);
  });

  it('does not warn when no move was made yet, and Escape closes the dialog', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'New game' })).not.toBeInTheDocument();
  });

  it('offers the four modes and remembers the choice', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    expect(screen.getAllByRole('radio').map((r) => r.textContent?.split(/(?<=[a-z])(?=[A-Z0-9])/)[0])).toHaveLength(4);
    await user.click(screen.getByRole('radio', { name: /Easy/ }));
    await user.click(screen.getByRole('button', { name: /^Start/ }));
    expect(localStorage.getItem('colorlines_mode')).toBe('easy');
    expect(screen.getByRole('button', { name: /Mode: Easy/ })).toBeInTheDocument();
    const colors = new Set(ballCells().map((c) => /(\w+) ball/.exec(c.getAttribute('aria-label')!)![1]));
    for (const c of colors) expect(['red', 'green', 'blue', 'yellow', 'magenta']).toContain(c);
  });

  it('starts the same daily challenge every time on the same day', async () => {
    const user = userEvent.setup();
    render(<App />);
    await startNewGame(user, /Daily challenge/);
    const first = cells().map((c) => c.getAttribute('aria-label'));
    await startNewGame(user, /Daily challenge/);
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(first);
  });

  it('shows a countdown in Blitz', async () => {
    const user = userEvent.setup();
    render(<App />);
    await startNewGame(user, /Blitz/);
    expect(screen.getByRole('timer')).toHaveTextContent(/^[0-3]:\d\d$/);
    expect(screen.getByRole('timer')).toHaveAccessibleName(/Time left/);
  });
});

describe('goals', () => {
  it('shows three goals built from the results and how many are done', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Goals 0\/3/ }));
    const dialog = screen.getByRole('dialog', { name: "Today's goals" });
    expect(within(dialog).getAllByRole('progressbar')).toHaveLength(3);
    expect(within(dialog).getByText(/Score \d+ points in one game/)).toBeInTheDocument();
    expect(within(dialog).getByText(/built from your own recent results/)).toBeInTheDocument();
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

  it('records the finished game, unlocks achievements and shows result on demand', async () => {
    almostFullGame();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Row 1, column 3,/ }));
    await user.click(screen.getByRole('button', { name: /^Row 1, column 2, empty/ }));

    // No modal opens by itself
    expect(screen.queryByRole('dialog', { name: 'Game over' })).not.toBeInTheDocument();
    expect(storedHistory()).toHaveLength(1);
    expect(JSON.parse(localStorage.getItem('colorlines_progress')!).totalGames).toBe(1);

    // The HUD shows the Result button with an XP badge
    const resultBtn = screen.getByRole('button', { name: /^Result/ });
    expect(resultBtn).toBeInTheDocument();
    expect(resultBtn).toHaveTextContent(/\+\d+/);

    // Opening Result on demand shows the dialog with achievements
    await user.click(resultBtn);
    const dialog = await screen.findByRole('dialog', { name: 'Game over' });
    expect(within(dialog).getByText('Final score')).toBeInTheDocument();
    expect(within(dialog).getByText('First steps')).toBeInTheDocument();

    // The dialog is dismissable with Escape
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Game over' })).not.toBeInTheDocument();

    // It reopens on demand and "Play again" starts a fresh game in the same mode
    await user.click(screen.getByRole('button', { name: /^Result/ }));
    const reopened = await screen.findByRole('dialog', { name: 'Game over' });
    await user.click(within(reopened).getByRole('button', { name: 'Play again' }));
    expect(screen.queryByRole('dialog', { name: 'Game over' })).not.toBeInTheDocument();
    expect(ballCells()).toHaveLength(5);
    // A finished game is recorded once; starting the next one does not add an "abandoned" entry.
    expect(storedHistory()).toHaveLength(1);
  });

  it('no dialog after game over: HUD offers New game and Result with badge', async () => {
    almostFullGame();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Row 1, column 3,/ }));
    await user.click(screen.getByRole('button', { name: /^Row 1, column 2, empty/ }));

    // No modal opens by itself
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Result/ })).toBeInTheDocument();
    const newGameBtns = screen.getAllByRole('button', { name: 'New game' });
    expect(newGameBtns.length).toBeGreaterThanOrEqual(1);
  });

  it('New game after game over restarts same mode immediately without dialog', async () => {
    almostFullGame();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Row 1, column 3,/ }));
    await user.click(screen.getByRole('button', { name: /^Row 1, column 2, empty/ }));

    // Click "New game" button directly
    const newGameBtn = screen.getAllByRole('button', { name: 'New game' })[0];
    await user.click(newGameBtn);

    // No confirmation dialog or mode picker opens
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    // Fresh board started
    expect(ballCells()).toHaveLength(5);
    expect(storedHistory()).toHaveLength(1);
  });

  it('Result dialog dismiss: dismissable via Escape, Close button and backdrop click', async () => {
    almostFullGame();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: /^Row 1, column 3,/ }));
    await user.click(screen.getByRole('button', { name: /^Row 1, column 2, empty/ }));

    // Open Result dialog
    await user.click(screen.getByRole('button', { name: /^Result/ }));
    let dialog = await screen.findByRole('dialog', { name: 'Game over' });

    // Dismiss with Close button
    await user.click(within(dialog).getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('dialog', { name: 'Game over' })).not.toBeInTheDocument();

    // Reopen and dismiss with Escape
    await user.click(screen.getByRole('button', { name: /^Result/ }));
    dialog = await screen.findByRole('dialog', { name: 'Game over' });
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Game over' })).not.toBeInTheDocument();

    // Reopen and dismiss with backdrop click
    await user.click(screen.getByRole('button', { name: /^Result/ }));
    await screen.findByRole('dialog', { name: 'Game over' });
    const backdrop = document.querySelector('.modal-overlay') as HTMLElement;
    await user.click(backdrop);
    expect(screen.queryByRole('dialog', { name: 'Game over' })).not.toBeInTheDocument();
  });

  it('folded mode list: in-progress New game shows current mode and unfolds on Change mode', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);

    await user.click(screen.getByRole('button', { name: 'New game' }));
    expect(screen.getByRole('dialog', { name: 'New game' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/unfinished/);

    // Mode list is folded: shows current mode and Change mode button, but no radio list
    expect(screen.queryAllByRole('radio')).toHaveLength(0);
    expect(screen.getByText('Classic')).toBeInTheDocument();
    const changeBtn = screen.getByRole('button', { name: 'Change mode' });
    expect(changeBtn).toBeInTheDocument();

    // Clicking Change mode unfolds the radio list
    await user.click(changeBtn);
    expect(screen.getAllByRole('radio')).toHaveLength(4);
    await user.click(screen.getByRole('radio', { name: /Easy/ }));
    await user.click(screen.getByRole('button', { name: /^Start/ }));
    expect(localStorage.getItem('colorlines_mode')).toBe('easy');
  });
});

describe('long-term statistics and data files', () => {
  async function openStats(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: 'Statistics' }));
  }

  it('offers six tabs, reachable by keyboard, each with its own content', async () => {
    const user = userEvent.setup();
    render(<App />);
    await openStats(user);
    const tabs = screen.getAllByRole('tab');
    expect(tabs.map((x) => x.textContent)).toEqual(['Overview', 'Career', 'Mind', 'Seasons', 'Records', 'Data']);
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    tabs[0].focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('tab', { name: 'Career' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Next milestones')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Mind' }));
    expect(screen.getByText(/more games are needed/)).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Seasons' }));
    expect(screen.getByText(/Your typical month/)).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Records' }));
    expect(screen.getByText(/Play a few games/)).toBeInTheDocument();
  });

  it('writes a game into the permanent ledger when it ends', async () => {
    const user = userEvent.setup();
    render(<App />);
    await playOneMove(user);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    await user.click(screen.getByRole('button', { name: /^Start/ }));
    const stored = JSON.parse(localStorage.getItem('colorlines_ledger') ?? '{}') as Record<string, { games: number }>;
    expect(Object.values(stored).reduce((n, d) => n + d.games, 0)).toBe(1);
    await openStats(user);
    await user.click(screen.getByRole('tab', { name: 'Career' }));
    expect(screen.getAllByText('Days played').length).toBeGreaterThan(0);
    expect(screen.getByText(/Activity, last 26 weeks/)).toBeInTheDocument();
  });

  it('refuses a file that is not a backup and never replaces data without the checkbox', async () => {
    const user = userEvent.setup();
    localStorage.setItem('colorlines_history', JSON.stringify([{ score: 50, endedAt: Date.now(), moves: 9, lines: 1, balls: 5, completed: true, maxLine: 5, durationMs: 1000, mode: 'classic' }]));
    render(<App />);
    await openStats(user);
    await user.click(screen.getByRole('tab', { name: 'Data' }));
    const file = screen.getByTestId('backup-file');
    await user.upload(file, new File(['{"hello":1}'], 'x.json', { type: 'application/json' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('not a Color Lines backup');

    const good = {
      format: 'color-lines-backup', version: 1, exportedAt: Date.now(), app: { platform: 'android', version: '1' },
      history: [{ score: 900, endedAt: 1_700_000_000_000, moves: 40, lines: 6, balls: 30, completed: true, maxLine: 7, durationMs: 50_000, mode: 'classic' }],
      settings: { theme: 'neon' },
    };
    await user.upload(file, new File([JSON.stringify(good)], 'b.json', { type: 'application/json' }));
    expect(await screen.findByText(/Backup from/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Replace my data' }));
    const confirm = screen.getByRole('button', { name: 'Yes, replace' });
    expect(confirm).toBeDisabled();
    expect(storedHistory()).toHaveLength(1);
    await user.click(screen.getByRole('checkbox', { name: 'I understand, replace my data' }));
    await user.click(confirm);
    expect(storedHistory()[0].score).toBe(900);
    expect(localStorage.getItem('colorlines_theme')).toBe('neon');
  });

  it('adds a backup to the current data without losing any game', async () => {
    const user = userEvent.setup();
    const mine = { score: 50, endedAt: 1_600_000_000_000, moves: 9, lines: 1, balls: 5, completed: true, maxLine: 5, durationMs: 1000, mode: 'classic' };
    localStorage.setItem('colorlines_history', JSON.stringify([mine]));
    render(<App />);
    await openStats(user);
    await user.click(screen.getByRole('tab', { name: 'Data' }));
    const theirs = { ...mine, score: 700, endedAt: 1_700_000_000_000 };
    const backup = { format: 'color-lines-backup', version: 1, exportedAt: 1, app: {}, history: [theirs, mine] };
    await user.upload(screen.getByTestId('backup-file'), new File([JSON.stringify(backup)], 'b.json'));
    await user.click(await screen.findByRole('button', { name: 'Add to my data' }));
    const after = storedHistory() as { score: number }[];
    expect(after.map((g) => g.score)).toEqual([700, 50]);
    expect(screen.getAllByRole('status').map((e) => e.textContent).join(' ')).toContain('data added');
  });

  it('downloads the backup and the spreadsheet', async () => {
    const user = userEvent.setup();
    const made: string[] = [];
    const create = vi.fn((blob: Blob) => {
      made.push(`${blob.type}:${blob.size}`);
      return 'blob:x';
    });
    Object.assign(URL, { createObjectURL: create, revokeObjectURL: vi.fn() });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    render(<App />);
    await openStats(user);
    await user.click(screen.getByRole('tab', { name: 'Data' }));
    await user.click(screen.getByRole('button', { name: 'Save backup (JSON)' }));
    await user.click(screen.getByRole('button', { name: 'Save games as a spreadsheet (CSV)' }));
    expect(click).toHaveBeenCalledTimes(2);
    expect(made[0].startsWith('application/json:')).toBe(true);
    expect(made[1].startsWith('text/csv:')).toBe(true);
    click.mockRestore();
  });
});

describe('the installed app shortcut', () => {
  it('"?new=1" opens the new-game dialog with its warning and changes nothing by itself', async () => {
    const user = userEvent.setup();
    const engine = new GameEngine();
    saveGame(engine);
    render(<App />);
    await playOneMove(user);
    cleanup();
    window.history.replaceState(null, '', '/?new=1');
    render(<App />);
    expect(screen.getByRole('dialog', { name: 'New game' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('unfinished');
    expect(window.location.search).toBe('');
    expect(screen.getByRole('button', { name: 'Keep playing' })).toHaveFocus();
  });
});

describe('the captions of the 1992 screen', () => {
  it('stay in English even when the app is in Russian', async () => {
    localStorage.setItem('colorlines_theme', 'colorlines92');
    setLanguages(['ru']);
    render(<App />);
    const canvas = document.querySelector('canvas');
    expect(canvas).not.toBeNull();
    // The captions are painted on the canvas; their source is the English dictionary, never the Russian one.
    const { translate } = await import('../src/i18n');
    expect(translate('ru', 'dos.pretender')).toBe('Pretender');
    expect(translate('ru', 'dos.defaultKing')).toBe('Handicap');
    expect(translate('ru', 'dos.defaultName')).toBe('Player');
  });
});

describe('modes are explained and visible', () => {
  it('shows colours, clock, best result and what a switch does, and names the mode on the Start button', async () => {
    const user = userEvent.setup();
    localStorage.setItem(
      'colorlines_progress',
      JSON.stringify({ totalGames: 3, gamesByMode: { classic: 2, blitz: 1 }, bestByMode: { classic: 500, blitz: 120 } })
    );
    render(<App />);
    await playOneMove(user);
    await user.click(screen.getByRole('button', { name: 'New game' }));
    await user.click(screen.getByRole('button', { name: 'Change mode' }));
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(4);
    const dots = (r: HTMLElement) => r.querySelectorAll('.color-dot').length;
    expect(radios.map(dots)).toEqual([7, 5, 7, 7]);
    const [classic, easy, blitz] = radios;
    expect(classic).toHaveTextContent('7 colours, no time limit');
    expect(classic).toHaveTextContent('Best: 500 · games: 2');
    expect(easy).toHaveTextContent('5 colours');
    expect(blitz).toHaveTextContent('3 min');
    await user.click(blitz);
    expect(screen.getByText(/switching from Classic to Blitz/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Start: Blitz' })).toBeInTheDocument();
    // The game in progress is still there until Start is pressed.
    expect(screen.getByRole('alert')).toHaveTextContent('unfinished');
  });

  it('actually plays the colours it promises', async () => {
    const user = userEvent.setup();
    render(<App />);
    await startNewGame(user, /Easy/);
    const colorsOnBoard = () => new Set(ballCells().map((c) => /(\w+) ball/.exec(c.getAttribute('aria-label')!)?.[1]));
    for (let i = 0; i < 12; i++) await playOneMove(user).catch(() => undefined);
    expect([...colorsOnBoard()].every((c) => ['red', 'green', 'blue', 'yellow', 'magenta'].includes(c!))).toBe(true);
  });

  it('explains every mode, the hint and the goals in the help', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'Rules and info' }));
    for (const text of [/Classic: the original rules, all 7 colours/, /Easy: only 5 colours/, /Blitz: 3 minutes/, /Daily challenge: everyone gets the same balls/, /Hint: marks a ball/])
      expect(screen.getByText(text)).toBeInTheDocument();
  });
});
