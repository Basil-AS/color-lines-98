import { expect, test } from '@playwright/test';

const THEMES = ['modern', 'light', 'lines98', 'colorlines92'] as const;
const VIEWPORTS = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'tablet', width: 820, height: 1180 },
  { name: 'phone', width: 390, height: 844 },
  { name: 'phone landscape', width: 844, height: 390 },
  { name: 'small phone', width: 320, height: 568 },
];

for (const theme of THEMES) {
  for (const vp of VIEWPORTS) {
    test(`${theme} is centred without horizontal scroll on ${vp.name}`, async ({ browser }) => {
      const context = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
      const page = await context.newPage();
      await page.goto('/');
      await page.evaluate((t) => localStorage.setItem('colorlines_theme', t), theme);
      await page.reload();
      await page.locator('.game-window').waitFor();
      await page.waitForTimeout(400);

      const m = await page.evaluate(() => {
        const r = document.querySelector('.game-window')!.getBoundingClientRect();
        const d = document.documentElement;
        return { left: r.left, right: window.innerWidth - r.right, overflow: d.scrollWidth > window.innerWidth };
      });
      expect(Math.abs(m.left - m.right)).toBeLessThanOrEqual(2);
      expect(m.overflow).toBe(false);
      await context.close();
    });
  }
}
