import { expect, test } from '@playwright/test';

test('real Chromium touch swipes navigate Settings and Statistics tabs without capturing vertical intent', async ({ browser }) => {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 1,
    hasTouch: true,
    isMobile: true,
  });
  const page = await context.newPage();
  await page.addInitScript(() => { localStorage.setItem('colorlines_lang', 'en'); });
  await page.goto('/');

  const cdp = await context.newCDPSession(page);
  let touchId = 1;
  const swipe = async (target: ReturnType<typeof page.locator>, dx: number, dy: number) => {
    const rect = await target.boundingBox();
    expect(rect, 'touch target should be visible').not.toBeNull();
    const x = rect!.x + rect!.width / 2;
    const y = rect!.y + rect!.height / 2;
    const id = touchId++;
    const point = (px: number, py: number) => ({ x: px, y: py, id, radiusX: 1, radiusY: 1, force: 1 });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point(x, y)] });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [point(x + dx, y + dy)] });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
    await page.waitForTimeout(50);
  };

  const dialog = page.getByRole('dialog');
  await page.getByRole('button', { name: 'Settings', exact: true }).click();
  await expect(dialog).toHaveAccessibleName('Settings');
  const title = dialog.locator('.modal-title');
  await swipe(title, -110, 0);
  await expect(page.getByRole('button', { name: 'Balls', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await swipe(title, 110, 0);
  await expect(page.getByRole('button', { name: 'Themes', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await swipe(title, 0, 110);
  await expect(page.getByRole('button', { name: 'Themes', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: 'Close', exact: true }).click();

  await page.getByRole('button', { name: 'Statistics', exact: true }).click();
  await expect(dialog).toHaveAccessibleName('Statistics');
  const statsTitle = dialog.locator('.modal-title');
  await expect(page.getByRole('tab', { name: 'Overview' })).toHaveAttribute('aria-selected', 'true');
  await swipe(statsTitle, -110, 0);
  await expect(page.getByRole('tab', { name: 'Career' })).toHaveAttribute('aria-selected', 'true');
  for (let index = 0; index < 4; index += 1) await swipe(statsTitle, -110, 0);
  const dataTab = page.getByRole('tab', { name: 'Data' });
  await expect(dataTab).toHaveAttribute('aria-selected', 'true');
  const visibleInTabRail = await dataTab.evaluate((tab) => {
    const rail = tab.parentElement!.getBoundingClientRect();
    const rect = tab.getBoundingClientRect();
    return rect.left >= rail.left && rect.right <= rail.right && rect.width > 0;
  });
  expect(visibleInTabRail, 'the selected last tab should be scrolled into view horizontally').toBe(true);

  await context.close();
});
