import { expect, test } from '@playwright/test';

const THEMES = ['modern', 'light', 'material', 'neon', 'contrast', 'lines98', 'lines98plus', 'colorlines92'] as const;
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
        // On wide screens the goals panel sits next to the game: the two together are what gets centred.
        const boxes = [document.querySelector('.game-window'), document.querySelector('.side-panel')]
          .filter((el): el is Element => el !== null)
          .map((el) => el.getBoundingClientRect())
          .filter((r) => r.width > 0);
        const left = Math.min(...boxes.map((r) => r.left));
        const right = Math.max(...boxes.map((r) => r.right));
        const d = document.documentElement;
        return { left, right: window.innerWidth - right, overflow: d.scrollWidth > window.innerWidth };
      });
      expect(Math.abs(m.left - m.right)).toBeLessThanOrEqual(2);
      expect(m.overflow).toBe(false);
      await context.close();
    });
  }
}
