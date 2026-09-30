import { expect, test } from '@playwright/test';

test('plays a move, keeps the game after a reload and starts a new one', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(String(e)));
  await page.goto('/');
  await page.evaluate(() => localStorage.setItem('colorlines_lang', 'en'));
  await page.reload();

  const cells = page.locator('.board-cell');
  await expect(cells).toHaveCount(81);
  const balls = page.locator('.board-cell[aria-pressed]');
  await expect(balls).toHaveCount(5);

  await balls.first().click();
  await page.locator('.board-cell.reachable').first().click();
  const after = await cells.evaluateAll((els) => els.map((e) => e.getAttribute('aria-label')));

  await page.reload();
  expect(await page.locator('.board-cell').evaluateAll((els) => els.map((e) => e.getAttribute('aria-label')))).toEqual(after);

  await page.getByRole('button', { name: 'New game' }).click();
  await expect(page.locator('.board-cell[aria-pressed]')).toHaveCount(5);
  expect(errors).toEqual([]);
});

test('follows the browser language', async ({ browser }) => {
  const context = await browser.newContext({ locale: 'ru-RU' });
  const page = await context.newPage();
  await page.goto('/');
  await expect(page.locator('html')).toHaveAttribute('lang', 'ru');
  await expect(page).toHaveTitle(/Color Lines/);
  await expect(page.getByRole('group', { name: 'Игровое поле' })).toBeVisible();
  await context.close();
});

test('the keyboard moves the focus over the board', async ({ page }) => {
  await page.goto('/');
  await page.locator('.board-cell[tabindex="0"]').focus();
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowDown');
  const label = await page.evaluate(() => document.activeElement?.getAttribute('aria-label'));
  expect(label).toMatch(/^(Row 2, column 2|Ряд 2, столбец 2)/);
});
