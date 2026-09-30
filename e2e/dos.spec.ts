import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await page.evaluate(() => {
    localStorage.setItem('colorlines_theme', 'colorlines92');
    localStorage.setItem('colorlines_lang', 'en');
  });
  await page.reload();
  await page.locator('.dos-screen').waitFor();
});

test('draws the original screen: grey frame, black LCD displays, the board and the king', async ({ page }) => {
  await page.waitForFunction(() => {
    const c = document.querySelector<HTMLCanvasElement>('.dos-canvas')!;
    const px = c.getContext('2d')!.getImageData(5, 5, 1, 1).data;
    return px[0] === 170; // the frame's EGA light grey
  });
  const px = await page.evaluate(() => {
    const ctx = document.querySelector<HTMLCanvasElement>('.dos-canvas')!.getContext('2d')!;
    const at = (x: number, y: number) => [...ctx.getImageData(x, y, 1, 1).data.slice(0, 3)];
    // The red king lives in the 72x73 frame at (51, 72): somewhere in it there must be red pixels.
    const data = ctx.getImageData(51, 72, 72, 73).data;
    let red = 0;
    for (let i = 0; i < data.length; i += 4) if (data[i] > 150 && data[i + 1] < 60 && data[i + 2] < 60) red++;
    return { frame: at(5, 5), lcd: at(58, 14), redPixelsOfTheKing: red, board: at(180, 70) };
  });
  expect(px.frame).toEqual([170, 170, 170]);
  expect(px.lcd).toEqual([0, 0, 0]);
  expect(px.board).toEqual([170, 170, 170]);
  expect(px.redPixelsOfTheKing).toBeGreaterThan(200);
  await expect(page.locator('.dos-grid .board-cell')).toHaveCount(81);
});

test('F1 opens the original Help window, F3 hides the next balls, F4 restarts', async ({ page }) => {
  await page.keyboard.press('F1');
  await expect(page.locator('.dos-window-close')).toBeVisible();
  await page.keyboard.press('F1');
  await expect(page.locator('.dos-window-close')).toHaveCount(0);

  await page.keyboard.press('F3');
  expect(await page.evaluate(() => localStorage.getItem('colorlines_show_next'))).toBe('false');
  await page.keyboard.press('F3');

  const balls = page.locator('.dos-grid .board-cell[aria-pressed]');
  await balls.first().click();
  await page.locator('.dos-grid .board-cell').nth(40).click();
  // F4 no longer restarts silently: it asks first.
  await page.keyboard.press('F4');
  await expect(page.getByRole('alert')).toContainText('unfinished');
  await page.getByRole('button', { name: 'Start', exact: true }).click();
  await expect(balls).toHaveCount(5);
});
