import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

/**
 * Layout audit: on every theme and screen size, nothing overlaps, nothing sticks out of the screen and no label is
 * cut off, in the game screen, in every settings tab and in the other dialogs. Plus the look studio's balls stay
 * inside their cells whatever the ring, size and grid.
 */
const VIEWPORTS = [
  { name: 'small phone', width: 320, height: 568 },
  { name: 'phone', width: 390, height: 844 },
  { name: 'phone landscape', width: 844, height: 390 },
  { name: 'tablet', width: 820, height: 1180 },
  { name: 'desktop', width: 1440, height: 900 },
];
const ALL_THEMES = ['modern', 'light', 'material', 'neon', 'synthwave', 'ocean', 'paper', 'gameboy', 'terminal', 'contrast', 'clear', 'clearNight', 'retro98', 'spectrum', 'meadow', 'meadowClear', 'meadowRelief', 'orchid', 'custom', 'lines98', 'lines98plus', 'colorlines92'];
const DIALOG_THEMES = ['modern', 'light', 'neon', 'gameboy', 'terminal', 'meadowRelief', 'custom', 'lines98'];

interface Problem {
  kind: 'overlap' | 'outside' | 'clipped' | 'scroll';
  what: string;
}

/** Everything wrong with the layout inside `root` (or the whole page). */
async function audit(page: Page, rootSelector: string): Promise<Problem[]> {
  return page.evaluate((sel) => {
    const root = document.querySelector(sel) ?? document.body;
    const problems: { kind: 'overlap' | 'outside' | 'clipped' | 'scroll'; what: string }[] = [];
    const describe = (el: Element) => {
      const text = (el.getAttribute('aria-label') ?? el.textContent ?? '').trim().replace(/\s+/g, ' ').slice(0, 30);
      return `${el.tagName.toLowerCase()}${el.className && typeof el.className === 'string' ? '.' + el.className.split(' ')[0] : ''} "${text}"`;
    };
    const visible = (el: Element) => {
      const r = el.getBoundingClientRect();
      const s = getComputedStyle(el);
      return r.width > 1 && r.height > 1 && s.visibility !== 'hidden' && s.display !== 'none' && Number(s.opacity) > 0.05;
    };
    const inScroller = (el: Element) => {
      for (let p = el.parentElement; p && p !== document.body; p = p.parentElement) {
        const s = getComputedStyle(p);
        if (/(auto|scroll)/.test(s.overflowX) && p.scrollWidth > p.clientWidth + 1) return true;
      }
      return false;
    };
    const items = [...root.querySelectorAll('button, a[href], select, textarea, input:not([type=checkbox]), label, [role=tab], [role=menuitem], .stat-box, h1, h2, h3, h4, .led-number')]
      .filter((el) => visible(el) && !el.closest('.board-grid') && !el.closest('.vision-defs'));

    const d = document.documentElement;
    if (d.scrollWidth > window.innerWidth + 1) problems.push({ kind: 'scroll', what: `page scrolls sideways (${d.scrollWidth} > ${window.innerWidth})` });
    const dialog = document.querySelector('.modal-content');
    if (dialog && dialog.scrollWidth > dialog.clientWidth + 1) problems.push({ kind: 'scroll', what: `dialog scrolls sideways (${dialog.scrollWidth} > ${dialog.clientWidth})` });

    for (const el of items) {
      const r = el.getBoundingClientRect();
      if (!inScroller(el) && (r.left < -1 || r.right > window.innerWidth + 1)) problems.push({ kind: 'outside', what: `${describe(el)} spans ${Math.round(r.left)}..${Math.round(r.right)} of ${window.innerWidth}` });
      const s = getComputedStyle(el);
      if ((el.tagName === 'BUTTON' || el.tagName === 'A' || el.tagName === 'LABEL') && /(hidden|clip)/.test(s.overflowX) && el.scrollWidth > el.clientWidth + 2) {
        problems.push({ kind: 'clipped', what: `${describe(el)} is cut off (${el.scrollWidth} > ${el.clientWidth})` });
      }
    }

    for (let i = 0; i < items.length; i++) {
      for (let j = i + 1; j < items.length; j++) {
        const a = items[i];
        const b = items[j];
        if (a.contains(b) || b.contains(a)) continue;
        const ra = a.getBoundingClientRect();
        const rb = b.getBoundingClientRect();
        const w = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left);
        const h = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top);
        if (w > 3 && h > 3 && w * h > 24) problems.push({ kind: 'overlap', what: `${describe(a)} overlaps ${describe(b)} by ${Math.round(w)}x${Math.round(h)}` });
      }
    }
    return problems;
  }, rootSelector);
}

async function open(page: Page, theme: string, extra: Record<string, string> = {}) {
  await page.goto('/');
  await page.evaluate(
    ([t, more]) => {
      localStorage.clear();
      localStorage.setItem('colorlines_theme', t as string);
      localStorage.setItem('colorlines_lang', 'en');
      for (const [k, v] of Object.entries(more as Record<string, string>)) localStorage.setItem(k, v);
    },
    [theme, extra] as const,
  );
  await page.reload();
  await page.locator('.board-cell').first().waitFor();
  await page.waitForTimeout(250);
}

const RETRO = new Set(['lines98', 'lines98plus', 'colorlines92']);

async function openAction(page: Page, theme: string, button: string, menuItem: string) {
  const direct = page.getByRole('button', { name: button, exact: true });
  if (!RETRO.has(theme) && (await direct.count())) {
    await direct.first().click();
    return;
  }
  await page.getByRole('menuitem', { name: menuItem }).first().click();
  await page.getByRole('menuitem', { name: button }).first().click();
}

test.describe('game screen layout', () => {
  for (const theme of ALL_THEMES) {
    for (const vp of VIEWPORTS) {
      test(`${theme} on ${vp.name}: nothing overlaps, sticks out or is cut off`, async ({ browser }) => {
        const context = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
        const page = await context.newPage();
        await open(page, theme);
        expect(await audit(page, 'body')).toEqual([]);
        await context.close();
      });
    }
  }
});

test.describe('dialogs layout', () => {
  for (const theme of DIALOG_THEMES) {
    for (const vp of VIEWPORTS) {
      test(`${theme} on ${vp.name}: settings tabs, statistics and new game`, async ({ browser }) => {
        const context = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
        const page = await context.newPage();
        await open(page, theme);
        const found: string[] = [];
        const check = async (label: string) => {
          for (const p of await audit(page, '.modal-content')) found.push(`${label}: ${p.kind} ${p.what}`);
        };

        await openAction(page, theme, 'Settings', 'Game');
        for (const tab of ['Themes', 'Balls', 'Board', 'Builder', 'Game']) {
          await page.getByRole('button', { name: new RegExp(`^${tab}$`) }).click();
          await check(`settings/${tab}`);
        }
        await page.keyboard.press('Escape');

        await openAction(page, theme, 'Statistics', 'Score');
        for (const tab of ['Overview', 'Career', 'Seasons', 'Records', 'Data']) {
          await page.getByRole('tab', { name: tab }).click();
          await check(`stats/${tab}`);
        }
        await page.keyboard.press('Escape');

        await openAction(page, theme, 'New game', 'Game');
        await check('new game');
        expect(found).toEqual([]);
        await context.close();
      });
    }
  }
});

test.describe('look studio keeps balls inside their cells', () => {
  const COMBOS = [
    { ballSize: 'large', contrast: 'max', grid: 'bold', cells: 'round', shapes: 'digits', volume: 'glossy' },
    { ballSize: 'large', contrast: 'high', grid: 'none', cells: 'square', shapes: 'shapes', volume: 'flat' },
    { ballSize: 'compact', contrast: 'max', grid: 'dots', cells: 'rounded', shapes: 'off', volume: 'soft' },
    { ballSize: 'normal', contrast: 'normal', grid: 'checker', cells: 'round', shapes: 'digits', volume: 'soft' },
  ];
  for (const theme of ['modern', 'clearNight', 'meadow', 'custom']) {
    for (const vp of [VIEWPORTS[0], VIEWPORTS[1], VIEWPORTS[4]]) {
      test(`${theme} on ${vp.name}`, async ({ browser }) => {
        const context = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
        const page = await context.newPage();
        for (const combo of COMBOS) {
          await open(page, theme, { colorlines_look: JSON.stringify({ [theme]: combo }) });
          const bad = await page.evaluate(() => {
            const out: string[] = [];
            for (const cell of document.querySelectorAll('.board-cell')) {
              for (const ball of cell.querySelectorAll('.ball')) {
                const s = getComputedStyle(ball);
                const outline = s.outlineStyle === 'none' ? 0 : parseFloat(s.outlineWidth) + Math.max(0, parseFloat(s.outlineOffset) || 0);
                const rb = ball.getBoundingClientRect();
                const rc = cell.getBoundingClientRect();
                const cs = getComputedStyle(cell);
                // A round cell clips what pokes out of its circle: a round ball with its ring must fit the cell's width.
                const round = parseFloat(cs.borderTopLeftRadius) >= rc.width / 2 - 1;
                const half = Math.max(rb.width, rb.height) / 2 + outline;
                if (round) {
                  if (half > rc.width / 2 + 0.5) out.push(`ball ${Math.round(half * 2)}px does not fit round cell ${Math.round(rc.width)}px`);
                } else if (rb.left - outline < rc.left - 0.5 || rb.right + outline > rc.right + 0.5 || rb.top - outline < rc.top - 0.5 || rb.bottom + outline > rc.bottom + 0.5) {
                  out.push(`ball ${Math.round(rb.width)}px + ring ${outline}px sticks out of cell ${Math.round(rc.width)}px`);
                }
              }
            }
            return out.slice(0, 3);
          });
          expect(bad, JSON.stringify(combo)).toEqual([]);
        }
        await context.close();
      });
    }
  }
});
