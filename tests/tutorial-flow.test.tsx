// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
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
import { saveGame } from '../src/storage';

function setLanguages(languages: string[]) {
  Object.defineProperty(window.navigator, 'languages', { value: languages, configurable: true });
  Object.defineProperty(window.navigator, 'language', { value: languages[0], configurable: true });
}

const cells = () => screen.getAllByRole('button', { name: /^(Row|Ряд) \d+, (column|столбец) \d+/ });
const cellAt = (x: number, y: number) => cells()[y * 9 + x];
const ballCells = () =>
  cells().filter(
    (c) =>
      /(\w+ ball|шар)(,|$)/.test(c.getAttribute('aria-label')!) &&
      !/empty|пусто/.test(c.getAttribute('aria-label')!)
  );
const reachableCells = () =>
  cells().filter((c) => /reachable|можно дойти/.test(c.getAttribute('aria-label')!));

async function playOneMove(user: ReturnType<typeof userEvent.setup>) {
  await user.click(ballCells()[0]);
  await user.click(reachableCells()[0]);
}

beforeEach(() => {
  vi.mocked(soundManager.play).mockClear();
  localStorage.clear();
  setLanguages(['en-US']);
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

describe('tutorial flow', () => {
  it('Help -> Take the tutorial shows Step 1 of 7, first step text and hint-target on (2,4)', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: 'Rules and info' }));
    await user.click(screen.getByRole('button', { name: 'Take the tutorial' }));

    const banner = document.querySelector('section.tutorial-banner');
    expect(banner).toBeInTheDocument();
    expect(within(banner as HTMLElement).getByText('Training')).toBeInTheDocument();
    expect(within(banner as HTMLElement).getByText('Step 1 of 7')).toBeInTheDocument();
    expect(within(banner as HTMLElement).getByRole('status')).toHaveTextContent(
      'Tap a ball to pick it up. Start with the highlighted one.'
    );

    // cell (2, 4): x = 2, y = 4 -> index = 4 * 9 + 2 = 38
    const cell24 = cellAt(2, 4);
    expect(cell24).toHaveClass('hint-target');
    expect(cell24.getAttribute('aria-label')).toMatch(/red ball/);

    const targets = document.querySelectorAll('.board-cell.hint-target');
    expect(targets).toHaveLength(1);
    expect(targets[0]).toBe(cell24);
  });

  it('Skip the tutorial removes banner, restores previous board and leaves localStorage unchanged', async () => {
    const engine = new GameEngine(9, 3, 5, 'classic', () => 0.5);
    saveGame(engine);
    localStorage.setItem('colorlines_best_score', '300');
    localStorage.setItem('colorlines_progress', JSON.stringify({ totalGames: 3, xp: 150 }));
    localStorage.setItem(
      'colorlines_history2',
      JSON.stringify([
        {
          score: 300,
          endedAt: 1700000000000,
          moves: 10,
          lines: 2,
          balls: 10,
          completed: true,
          maxLine: 5,
          durationMs: 20000,
          mode: 'classic',
        },
      ])
    );
    localStorage.setItem(
      'colorlines_hall',
      JSON.stringify([{ name: 'Champion', score: 300, at: 1700000000000 }])
    );

    const user = userEvent.setup();
    render(<App />);

    const snapshotBefore = Object.fromEntries(Object.entries(localStorage));
    const boardBefore = cells().map((c) => c.getAttribute('aria-label'));

    await user.click(screen.getByRole('button', { name: 'Rules and info' }));
    await user.click(screen.getByRole('button', { name: 'Take the tutorial' }));
    expect(document.querySelector('section.tutorial-banner')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Skip the tutorial' }));

    expect(document.querySelector('section.tutorial-banner')).not.toBeInTheDocument();
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(boardBefore);

    const snapshotAfter = Object.fromEntries(Object.entries(localStorage));
    expect(snapshotAfter).toEqual(snapshotBefore);

    for (const key of [
      'colorlines_game',
      'colorlines_history2',
      'colorlines_progress',
      'colorlines_best_score',
      'colorlines_hall',
    ]) {
      expect(localStorage.getItem(key)).toBe(snapshotBefore[key]);
    }
  });

  it('drives the scripted solution by clicking cells to completion and returns to original game', () => {
    vi.useFakeTimers();
    render(<App />);

    const originalBoardLabels = cells().map((c) => c.getAttribute('aria-label'));

    fireEvent.click(screen.getByRole('button', { name: 'Rules and info' }));
    fireEvent.click(screen.getByRole('button', { name: 'Take the tutorial' }));

    const banner = () => document.querySelector('section.tutorial-banner');
    expect(banner()).toBeInTheDocument();
    expect(within(banner() as HTMLElement).getByText('Step 1 of 7')).toBeInTheDocument();

    // click (2,4) -> step 2
    fireEvent.click(cellAt(2, 4));
    expect(within(banner() as HTMLElement).getByText('Step 2 of 7')).toBeInTheDocument();
    expect(within(banner() as HTMLElement).getByRole('status')).toHaveTextContent(
      'Now tap an empty cell: the ball goes there along a free path.'
    );

    // click empty (2,2) -> after 1100ms delay step 3 with 'Continue' button
    fireEvent.click(cellAt(2, 2));
    expect(within(banner() as HTMLElement).getByText('Step 2 of 7')).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(1100);
    });

    expect(within(banner() as HTMLElement).getByText('Step 3 of 7')).toBeInTheDocument();
    const continueBtn = within(banner() as HTMLElement).getByRole('button', { name: 'Continue' });
    expect(continueBtn).toBeInTheDocument();

    // Continue -> step 4
    fireEvent.click(continueBtn);
    expect(within(banner() as HTMLElement).getByText('Step 4 of 7')).toBeInTheDocument();
    expect(within(banner() as HTMLElement).getByRole('status')).toHaveTextContent(
      'Line up five balls of one colour'
    );

    // click (7,2) then (5,5) -> after delay step 5
    fireEvent.click(cellAt(7, 2));
    fireEvent.click(cellAt(5, 5));

    act(() => {
      vi.advanceTimersByTime(1100);
    });

    expect(within(banner() as HTMLElement).getByText('Step 5 of 7')).toBeInTheDocument();
    expect(within(banner() as HTMLElement).getByRole('status')).toHaveTextContent(
      'The line is gone and scored points. A cleared line gives a free turn: no new balls appear. Make any move.'
    );

    // make any legal move (green ball at (0,0) to empty (0,1)) -> step 6
    fireEvent.click(cellAt(0, 0));
    fireEvent.click(cellAt(0, 1));

    act(() => {
      vi.advanceTimersByTime(1100);
    });

    expect(within(banner() as HTMLElement).getByText('Step 6 of 7')).toBeInTheDocument();
    expect(within(banner() as HTMLElement).getByRole('status')).toHaveTextContent(
      'A ball cannot pass through other balls.'
    );

    // click (4,4) then (0,0) -> after delay step 7 showing 'Start playing'
    fireEvent.click(cellAt(4, 4));
    fireEvent.click(cellAt(0, 0));

    act(() => {
      vi.advanceTimersByTime(1100);
    });

    expect(within(banner() as HTMLElement).getByText('Step 7 of 7')).toBeInTheDocument();
    expect(within(banner() as HTMLElement).getByRole('status')).toHaveTextContent(
      'The game ends when the board is full.'
    );
    const startPlayingBtn = within(banner() as HTMLElement).getByRole('button', {
      name: 'Start playing',
    });
    expect(startPlayingBtn).toBeInTheDocument();
    expect(
      within(banner() as HTMLElement).queryByRole('button', { name: 'Skip the tutorial' })
    ).not.toBeInTheDocument();

    // Start playing returns to the original game
    fireEvent.click(startPlayingBtn);
    expect(banner()).not.toBeInTheDocument();
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(originalBoardLabels);
  });

  it('wrong move in the line step shows Not quite after 1300ms and restores the line board', () => {
    vi.useFakeTimers();
    render(<App />);

    fireEvent.click(screen.getByRole('button', { name: 'Rules and info' }));
    fireEvent.click(screen.getByRole('button', { name: 'Take the tutorial' }));

    const banner = () => document.querySelector('section.tutorial-banner');

    // advance to step 4 (line)
    fireEvent.click(cellAt(2, 4));
    fireEvent.click(cellAt(2, 2));
    act(() => {
      vi.advanceTimersByTime(1100);
    });
    fireEvent.click(within(banner() as HTMLElement).getByRole('button', { name: 'Continue' }));
    expect(within(banner() as HTMLElement).getByText('Step 4 of 7')).toBeInTheDocument();

    const lineBoardLabels = cells().map((c) => c.getAttribute('aria-label'));

    // Make a move that does not clear a line: move green ball at (0,0) to empty (0,1)
    fireEvent.click(cellAt(0, 0));
    fireEvent.click(cellAt(0, 1));

    // Wait 1300ms retry delay
    act(() => {
      vi.advanceTimersByTime(1300);
    });

    expect(within(banner() as HTMLElement).getByRole('status')).toHaveTextContent(/^Not quite/);
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(lineBoardLabels);
    expect(cellAt(7, 2)).toHaveClass('hint-target');
    expect(cellAt(5, 5)).toHaveClass('hint-target');
  });

  it('hint button does nothing during the tutorial', async () => {
    const user = userEvent.setup();
    render(<App />);

    const hintBtn = screen.getByRole('button', { name: /^Hint/ });
    expect(hintBtn).toHaveAccessibleName('Hint (3 left)');

    await user.click(screen.getByRole('button', { name: 'Rules and info' }));
    await user.click(screen.getByRole('button', { name: 'Take the tutorial' }));
    expect(document.querySelector('section.tutorial-banner')).toBeInTheDocument();

    vi.mocked(soundManager.play).mockClear();
    await user.click(hintBtn);

    expect(hintBtn).toHaveAccessibleName('Hint (3 left)');
    expect(soundManager.play).not.toHaveBeenCalledWith('hint');

    // Only step 1 hint target (2,4) is present
    const targets = document.querySelectorAll('.board-cell.hint-target');
    expect(targets).toHaveLength(1);
    expect(targets[0]).toBe(cellAt(2, 4));
  });

  it('new game during the tutorial opens dialog about the original game and removes banner', async () => {
    const user = userEvent.setup();
    render(<App />);

    // Play 1 move in original game so it has in-progress state
    await playOneMove(user);
    const originalBoardLabels = cells().map((c) => c.getAttribute('aria-label'));

    // Open and enter tutorial
    await user.click(screen.getByRole('button', { name: 'Rules and info' }));
    await user.click(screen.getByRole('button', { name: 'Take the tutorial' }));
    expect(document.querySelector('section.tutorial-banner')).toBeInTheDocument();

    // Click New game while in tutorial
    await user.click(screen.getByRole('button', { name: 'New game' }));

    // Banner is gone
    expect(document.querySelector('section.tutorial-banner')).not.toBeInTheDocument();

    // New game dialog is open with warning about the 1-move unfinished original game
    const dialog = screen.getByRole('dialog', { name: 'New game' });
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByRole('alert')).toHaveTextContent(/will be counted as unfinished/i);
    expect(within(dialog).getByRole('alert')).toHaveTextContent(/1 move/);

    // Dismissing returns to original game
    await user.click(within(dialog).getByRole('button', { name: 'Keep playing' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(cells().map((c) => c.getAttribute('aria-label'))).toEqual(originalBoardLabels);
  });

  it('can also be started from the New game dialog', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: 'New game' }));
    const dialog = screen.getByRole('dialog', { name: 'New game' });
    await user.click(within(dialog).getByRole('button', { name: 'Take the tutorial' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    const banner = document.querySelector('section.tutorial-banner');
    expect(banner).toBeInTheDocument();
    expect(within(banner as HTMLElement).getByText('Step 1 of 7')).toBeInTheDocument();
  });
});
