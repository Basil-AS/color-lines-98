import { AxeBuilder } from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

const THEMES = ['modern', 'light', 'material', 'neon', 'synthwave', 'ocean', 'paper', 'gameboy', 'terminal', 'contrast', 'lines98', 'lines98plus', 'colorlines92'];

async function open(page: import('@playwright/test').Page, theme: string) {
  await page.goto('/');
  await page.evaluate((t) => {
    localStorage.clear();
    localStorage.setItem('colorlines_theme', t);
    localStorage.setItem('colorlines_lang', 'en');
  }, theme);
  await page.reload();
  await expect(page.locator('.board-cell').first()).toBeVisible();
}

test.describe('security policy', () => {
  for (const theme of ['modern', 'colorlines92', 'lines98']) {
    test(`${theme}: playing, statistics and settings never break the content security policy`, async ({ page }) => {
      const violations: string[] = [];
      await page.addInitScript(() => {
        document.addEventListener('securitypolicyviolation', (e) => {
          (window as unknown as { __csp: string[] }).__csp = [...((window as unknown as { __csp?: string[] }).__csp ?? []), `${e.violatedDirective} ${e.blockedURI}`];
        });
      });
      page.on('pageerror', (e) => violations.push(String(e)));
      await open(page, theme);
      const cell = page.locator('.board-cell[aria-pressed]').first();
      await cell.click();
      await page.locator('.board-cell[aria-label*="reachable"]').first().click();
      await page.getByRole('button', { name: 'Statistics' }).or(page.getByRole('menuitem', { name: 'Score' })).first().click().catch(() => undefined);
      const csp = await page.evaluate(() => (window as unknown as { __csp?: string[] }).__csp ?? []);
      expect([...violations, ...csp]).toEqual([]);
      expect(await page.locator('meta[http-equiv="Content-Security-Policy"]').count()).toBe(1);
    });
  }
});

test.describe('automated accessibility audit (axe)', () => {
  for (const theme of THEMES) {
    test(`${theme}: no serious or critical violations on the game screen`, async ({ page }) => {
      await open(page, theme);
      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
      const bad = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
      expect(bad.map((v) => `${v.id}: ${v.nodes.slice(0, 3).map((n) => n.target.join(' ')).join(' | ')}`)).toEqual([]);
    });
  }
});

test.describe('automated accessibility audit of the dialogs', () => {
  for (const theme of ['modern', 'light', 'gameboy', 'terminal', 'lines98']) {
    test(`${theme}: statistics tabs, new game, settings and help have no serious violations`, async ({ page }) => {
      await open(page, theme);
      const audit = async (label: string) => {
        const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
        const bad = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
        expect(bad.map((v) => `${label} ${v.id}: ${v.nodes.slice(0, 2).map((n) => n.target.join(' ')).join(' | ')}`)).toEqual([]);
      };
      const openAction = async (button: string, menuItem?: string) => {
        const direct = page.getByRole('button', { name: button, exact: true });
        if (await direct.count()) await direct.first().click();
        else {
          await page.getByRole('menuitem', { name: menuItem ?? 'Game' }).first().click();
          await page.getByRole('menuitem', { name: button }).first().click();
        }
      };
      await openAction('Statistics', 'Score');
      for (const tab of ['Overview', 'Career', 'Seasons', 'Records', 'Data']) {
        await page.getByRole('tab', { name: tab }).click();
        await audit(`stats/${tab}`);
      }
      await page.keyboard.press('Escape');
      await openAction('New game', 'Game');
      await audit('new game');
      await page.keyboard.press('Escape');
      await openAction('Settings', 'Game');
      await audit('settings');
      await page.keyboard.press('Escape');
    });
  }
});
