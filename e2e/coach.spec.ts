import { expect, test } from '@playwright/test';

// Board coordinates are kept here rather than importing runtime source into the
// NodeNext Playwright project; each pair is exercised against the real app.
const LESSON_MOVES = [
  { from: [7, 2], to: [4, 4] },
  { from: [7, 2], to: [3, 4] },
  { from: [7, 2], to: [5, 5] },
  { from: [0, 0], to: [4, 4] },
  { from: [4, 4], to: [6, 6] },
  { from: [7, 2], to: [4, 4] },
] as const;

const VIEWPORTS = [
  { name: 'portrait', width: 390, height: 844 },
  { name: 'landscape', width: 844, height: 390 },
];

for (const viewport of VIEWPORTS) {
  test(`strategy coach solves all six real-board lessons in ${viewport.name}, then restores the party`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.addInitScript(() => {
      localStorage.setItem('colorlines_lang', 'en');
      localStorage.setItem('colorlines_theme', 'modern');
    });
    await page.goto('/');
    const cells = page.locator('.board-cell');
    await expect(cells).toHaveCount(81);

    // Make the saved party nontrivial before entering the isolated coach.
    const firstBall = page.locator('.board-cell[aria-pressed="false"]').first();
    await firstBall.click();
    await page.locator('.board-cell.reachable').first().click();
    const savedBoard = await cells.evaluateAll((items) => items.map((cell) => cell.getAttribute('aria-label')));
    const savedStorage = await page.evaluate(() => Object.fromEntries(Object.entries(localStorage)));

    await page.getByRole('button', { name: 'Rules and info', exact: true }).click();
    await page.getByRole('button', { name: 'Strategy coach' }).click();
    const banner = page.locator('section.strategy-coach');
    await expect(banner).toBeVisible();

    const geometry = async () => page.evaluate(() => {
      const banner = document.querySelector('section.strategy-coach')!.getBoundingClientRect();
      const board = document.querySelector('.board-grid')!.getBoundingClientRect();
      return {
        banner: { top: banner.top, bottom: banner.bottom, left: banner.left, right: banner.right },
        board: { top: board.top, bottom: board.bottom, left: board.left, right: board.right },
      };
    });
    const initial = await geometry();
    expect(initial.banner.top).toBeGreaterThanOrEqual(0);
    expect(initial.banner.bottom).toBeLessThanOrEqual(viewport.height);
    expect(initial.board.top).toBeGreaterThanOrEqual(0);
    expect(initial.board.bottom).toBeLessThanOrEqual(viewport.height);
    expect(initial.banner.left >= initial.board.right || initial.banner.right <= initial.board.left
      || initial.banner.bottom <= initial.board.top || initial.banner.top >= initial.board.bottom,
    'the coach dock and playable board must not overlap').toBe(true);

    const cell = (x: number, y: number) => cells.nth(y * 9 + x);
    for (let index = 0; index < LESSON_MOVES.length; index += 1) {
      const lesson = LESSON_MOVES[index];
      await expect(banner).toContainText(`Lesson ${index + 1} of 6`);

      if (index === 0) {
        // A wrong but legal move gives feedback and resets this lesson; progress is manual.
        const prompt = await banner.getByRole('status').innerText();
        await cell(lesson.from[0], lesson.from[1]).click();
        await cell(8, 7).click();
        await expect(banner.getByRole('status')).not.toHaveText(prompt);
        await expect(banner.getByRole('button', { name: 'Continue' })).toHaveCount(0);
      }

      await banner.getByRole('button', { name: 'Show a hint' }).click();
      await expect(banner.getByRole('button', { name: 'Hide hint' })).toBeVisible();
      await expect(page.locator('.board-cell.hint-target')).toHaveCount(2);
      await cell(lesson.from[0], lesson.from[1]).click();
      await cell(lesson.to[0], lesson.to[1]).click();
      await expect(banner.getByRole('status')).not.toHaveText('');
      const continuation = index === LESSON_MOVES.length - 1 ? 'Return to game' : 'Continue';
      await expect(banner.getByRole('button', { name: continuation })).toBeVisible();
      if (index < LESSON_MOVES.length - 1) {
        await banner.getByRole('button', { name: 'Continue' }).click();
      }
    }

    await banner.getByRole('button', { name: 'Return to game' }).click();
    await expect(banner).toHaveCount(0);
    await expect.poll(() => cells.evaluateAll((items) => items.map((cell) => cell.getAttribute('aria-label')))).toEqual(savedBoard);
    expect(await page.evaluate(() => Object.fromEntries(Object.entries(localStorage)))).toEqual(savedStorage);
  });
}
