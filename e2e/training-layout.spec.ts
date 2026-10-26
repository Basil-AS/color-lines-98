import { expect, test } from '@playwright/test';

const VIEWPORTS = [
  { name: 'portrait', width: 390, height: 844 },
  { name: 'landscape', width: 844, height: 390 },
];

for (const viewport of VIEWPORTS) {
  for (const training of [
    { name: 'mechanics tutorial', entry: 'Take the tutorial', banner: '.tutorial-banner:not(.strategy-coach)' },
    { name: 'strategy coach', entry: 'Strategy coach', banner: '.strategy-coach' },
  ]) {
    test(`${training.name} preserves exact board geometry in ${viewport.name}`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await page.addInitScript(() => {
        localStorage.setItem('colorlines_lang', 'en');
        localStorage.setItem('colorlines_theme', 'modern');
      });
      await page.goto('/');
      const board = page.locator('.board-grid');
      await expect(board).toBeVisible();
      await expect(page.getByRole('button', { name: 'New game', exact: true })).toBeVisible();
      const before = await board.boundingBox();
      expect(before).not.toBeNull();

      await page.getByRole('button', { name: 'Rules and info', exact: true }).click();
      await page.getByRole('button', { name: training.entry }).click();
      await expect(page.locator(training.banner)).toBeVisible();
      const actionHeights = await page.locator(`${training.banner} .tutorial-actions .modal-btn`)
        .evaluateAll((buttons) => buttons.map((button) => button.getBoundingClientRect().height));
      expect(actionHeights.length).toBeGreaterThan(0);
      expect(actionHeights.every((height) => height >= 44)).toBe(true);
      const after = await board.boundingBox();
      expect(after).not.toBeNull();
      expect(after!.width, 'training must not resize the board width').toBe(before!.width);
      expect(after!.height, 'training must not resize the board height').toBe(before!.height);
      const bounds = await page.evaluate(() => {
        const instructions = document.querySelector('.tutorial-banner')!.getBoundingClientRect();
        const grid = document.querySelector('.board-grid')!.getBoundingClientRect();
        return !(instructions.left < grid.right && instructions.right > grid.left
          && instructions.top < grid.bottom && instructions.bottom > grid.top);
      });
      expect(bounds, 'training instructions must never cover playable cells').toBe(true);
      await page.getByRole('button', { name: training.name === 'strategy coach' ? 'Leave training' : 'Skip the tutorial' }).click();
      await expect(page.getByRole('button', { name: 'New game', exact: true })).toBeVisible();
    });
  }
}

test('Lines 98 keeps tutorial controls inside its legacy header', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.addInitScript(() => {
    localStorage.setItem('colorlines_lang', 'en');
    localStorage.setItem('colorlines_theme', 'lines98');
  });
  await page.goto('/');
  await expect(page.locator('.l98-panel')).toBeVisible();

  await page.getByRole('button', { name: 'Rules and info', exact: true }).first().click();
  await page.getByRole('button', { name: 'Take the tutorial' }).click();

  const banner = page.locator('.l98-header .tutorial-banner:not(.strategy-coach)');
  await expect(banner).toBeVisible();
  await expect(banner.getByRole('button', { name: 'Skip the tutorial' })).toBeVisible();
  await expect(page.locator('.l98-panel')).toBeVisible();
  await banner.getByRole('button', { name: 'Skip the tutorial' }).click();
  await expect(banner).toHaveCount(0);
  await expect(page.locator('.l98-panel')).toBeVisible();
});

test('Lines 98 lets the strategy coach progress and return to the saved game', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.addInitScript(() => {
    localStorage.setItem('colorlines_lang', 'en');
    localStorage.setItem('colorlines_theme', 'lines98');
  });
  await page.goto('/');

  await page.getByRole('button', { name: 'Rules and info', exact: true }).first().click();
  await page.getByRole('button', { name: 'Strategy coach' }).click();

  const banner = page.locator('.l98-header .strategy-coach');
  await expect(banner).toBeVisible();
  await expect(banner.getByRole('button', { name: 'Show a hint' })).toBeVisible();
  const cells = page.locator('.board-cell');
  await cells.nth(2 * 9 + 7).click();
  await cells.nth(4 * 9 + 4).click();
  await expect(banner.getByRole('button', { name: 'Continue' })).toBeVisible();
  await banner.getByRole('button', { name: 'Continue' }).click();
  await expect(banner).toBeVisible();
  await banner.getByRole('button', { name: 'Leave training' }).click();
  await expect(banner).toHaveCount(0);
  await expect(page.locator('.l98-panel')).toBeVisible();
});
