import { expect, test } from '@playwright/test';

test('is installable and works offline, including the original artwork', async ({ browser }) => {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto('/');
  await page.evaluate(() => navigator.serviceWorker.ready.then(() => 1));
  await page.waitForTimeout(2500);

  const manifest = await page.evaluate(() =>
    fetch((document.querySelector('link[rel=manifest]') as HTMLLinkElement).href).then((r) => r.json())
  );
  expect(manifest.display).toBe('standalone');
  expect(manifest.icons.length).toBeGreaterThanOrEqual(4);

  await context.setOffline(true);
  await page.reload();
  await expect(page.locator('.board-cell')).toHaveCount(81);

  await page.evaluate(() => localStorage.setItem('colorlines_theme', 'colorlines92'));
  await page.reload();
  await expect(page.locator('.dos-screen')).toBeVisible();
  const width = await page.evaluate(
    () =>
      new Promise<number>((resolve) => {
        const img = new Image();
        img.onload = () => resolve(img.naturalWidth);
        img.onerror = () => resolve(0);
        img.src = './originals/colorlines1992/sheet.png';
      })
  );
  expect(width).toBe(640);
  await context.close();
});
