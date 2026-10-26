import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

function colorAlpha(color: string): number {
  const slashAlpha = color.match(/\/\s*([\d.]+)%?\s*\)$/);
  if (slashAlpha) return Number(slashAlpha[1]);
  const rgbaAlpha = color.match(/^rgba\([^,]+,[^,]+,[^,]+,\s*([\d.]+)\)$/);
  if (rgbaAlpha) return Number(rgbaAlpha[1]);
  return color === 'transparent' ? 0 : 1;
}

const VIEWPORTS = [
  { name: 'portrait', width: 390, height: 844 },
  { name: 'narrow portrait', width: 320, height: 720 },
  { name: 'landscape', width: 844, height: 390 },
];

for (const viewport of VIEWPORTS) {
  test(`modern control deck has clear hierarchy and aligned stats in ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.addInitScript(() => {
      localStorage.setItem('colorlines_lang', 'en');
      localStorage.setItem('colorlines_theme', 'modern');
    });
    await page.goto('/');

    const board = page.locator('.board-grid');
    await expect(board).toBeVisible();
    const boardBefore = await board.boundingBox();
    expect(boardBefore).not.toBeNull();

    const primary = page.getByRole('button', { name: 'New game', exact: true });
    const utilityButtons = page.locator('.secondary-controls > .ctrl-btn');
    await expect(primary).toBeVisible();
    await expect(primary).toHaveAccessibleName('New game');
    await expect(utilityButtons).toHaveCount(4);

    const stats = await page.locator('.hud-stats').evaluate((element) => {
      const children = Array.from(element.children) as HTMLElement[];
      const bounds = children.map((child) => child.getBoundingClientRect());
      return {
        count: children.length,
        hasNextPreview: children.some((child) => child.matches('.next-balls-preview')),
        rowWidth: element.getBoundingClientRect().width,
        parentWidth: element.parentElement!.getBoundingClientRect().width,
        top: bounds.map((rect) => rect.top),
        widths: bounds.map((rect) => rect.width),
        labelsAboveValues: children.every((child) => {
          const label = child.querySelector('.stat-label, .next-balls-label')!.getBoundingClientRect();
          const value = child.querySelector('.stat-value, .next-balls-list')!.getBoundingClientRect();
          return label.bottom <= value.top;
        }),
        valueFontSizes: children.slice(0, 2).map((child) =>
          getComputedStyle(child.querySelector('.stat-value')!).fontSize,
        ),
      };
    });
    expect(stats.count, 'score, best and next preview should share one stats row').toBe(3);
    expect(stats.hasNextPreview).toBe(true);
    expect(stats.rowWidth).toBeGreaterThanOrEqual(200);
    expect(Math.abs(stats.parentWidth - stats.rowWidth)).toBeLessThanOrEqual(2);
    expect(Math.max(...stats.top) - Math.min(...stats.top)).toBeLessThanOrEqual(1);
    expect(Math.max(...stats.widths) - Math.min(...stats.widths)).toBeLessThanOrEqual(2);
    expect(stats.labelsAboveValues).toBe(true);
    expect(stats.valueFontSizes).toEqual(['24px', '24px']);

    if (viewport.name === 'landscape') {
      const gaps = await page.evaluate(() => {
        const mode = document.querySelector('.mode-strip')!.getBoundingClientRect();
        const footerTools = document.querySelector('.footer-tools')!.getBoundingClientRect();
        const stats = document.querySelector('.hud-stats')!.getBoundingClientRect();
        const actions = document.querySelector('.controls-group')!.getBoundingClientRect();
        return { header: actions.top - stats.bottom, footer: footerTools.top - mode.bottom };
      });
      expect(gaps.header, 'landscape actions should follow the HUD without a large spacer').toBeLessThanOrEqual(16);
      expect(gaps.footer, 'landscape footer actions should sit close to goals').toBeLessThanOrEqual(64);
    }

    const buttons = page.locator('.controls-group .ctrl-btn');
    await expect(buttons).toHaveCount(7);
    const accessibleNames = await buttons.evaluateAll((controls) =>
      controls.map((control) => control.getAttribute('aria-label')),
    );
    expect(accessibleNames.every((name) => !!name?.trim())).toBe(true);
    const accessibility = await new AxeBuilder({ page }).include('.game-window').analyze();
    expect(accessibility.violations).toEqual([]);

    const assertDialogAccessible = async () => {
      await expect(page.locator('.modal-content')).toBeVisible();
      const dialogAccessibility = await new AxeBuilder({ page }).include('.modal-content').analyze();
      expect(dialogAccessibility.violations).toEqual([]);
      await page.getByRole('button', { name: 'Close' }).click();
      await expect(page.locator('.modal-content')).toHaveCount(0);
    };
    await page.getByRole('button', { name: 'Rules and info', exact: true }).click();
    await assertDialogAccessible();
    await page.getByRole('button', { name: 'Settings', exact: true }).click();
    await assertDialogAccessible();
    await page.getByRole('button', { name: 'Statistics', exact: true }).click();
    await assertDialogAccessible();
    await page.getByRole('button', { name: 'New game', exact: true }).click();
    await assertDialogAccessible();
    await page.locator('.mode-strip .strip-btn').nth(1).click();
    await assertDialogAccessible();

    const measuredButtonStyles = await buttons.evaluateAll((controls) =>
      controls.map((button) => {
        const style = getComputedStyle(button);
        const label = button.querySelector('.control-label');
        return {
          className: button.className,
          background: style.backgroundColor,
          labelVisible: !!label && getComputedStyle(label).visibility !== 'hidden'
            && getComputedStyle(label).display !== 'none',
          height: button.getBoundingClientRect().height,
          borderWidth: parseFloat(style.borderTopWidth),
          borderColor: style.borderTopColor,
          borderRadius: parseFloat(style.borderTopLeftRadius),
        };
      }),
    );
    const buttonStyles = measuredButtonStyles.map((style) => ({
      ...style,
      borderAlpha: colorAlpha(style.borderColor),
    }));
    const primaryStyle = buttonStyles.find((style) => style.className.includes('control-new-game'));
    expect(primaryStyle, 'New Game should have an explicit primary-action style').toBeDefined();
    expect(primaryStyle!.background).not.toBe('rgba(0, 0, 0, 0)');
    expect(buttonStyles.filter((style) => !style.className.includes('control-new-game'))
      .every((style) => style.background !== primaryStyle!.background)).toBe(true);
    const utilityStyles = buttonStyles.filter((style) => !style.className.includes('control-new-game')
      && !style.className.includes('primary'));
    expect(utilityStyles.every((style) => style.borderWidth >= 1 && style.borderAlpha > 0)).toBe(true);
    expect(utilityStyles.every((style) => style.borderRadius >= 8)).toBe(true);
    expect(buttonStyles.every((style) => style.labelVisible)).toBe(true);
    expect(buttonStyles.every((style) => style.height >= 44)).toBe(true);

    const boardAfter = await board.boundingBox();
    expect(boardAfter).not.toBeNull();
    expect(boardAfter!.width).toBe(boardBefore!.width);
    expect(boardAfter!.height).toBe(boardBefore!.height);
  });
}

test('large-font modern controls split into clear primary and compact utility rails', async ({ page }) => {
  await page.setViewportSize({ width: 568, height: 280 });
  await page.addInitScript(() => {
    localStorage.setItem('colorlines_lang', 'ru');
    localStorage.setItem('colorlines_theme', 'modern');
  });
  await page.goto('/');

  for (const fontSize of [24, 32]) {
    await page.evaluate((size) => { document.documentElement.style.fontSize = `${size}px`; }, fontSize);
    await expect(page.locator('.primary-controls > .ctrl-btn')).toHaveCount(3);
    await expect(page.locator('.secondary-controls > .ctrl-btn')).toHaveCount(4);
    await expect(page.locator('.primary-controls .control-new-game')).toBeVisible();

    const labels = await page.locator('.controls-group .control-label').evaluateAll((elements) =>
      elements.map((label) => {
        const box = label.getBoundingClientRect();
        const style = getComputedStyle(label);
        const button = label.closest('button')!;
        return {
          text: label.textContent?.trim(),
          fits: label.scrollWidth <= label.clientWidth + 1,
          unbroken: style.overflowWrap !== 'anywhere' && style.wordBreak !== 'break-all',
          visible: box.width > 0 && box.height > 0,
          dimensions: {
            labelWidth: box.width,
            labelScrollWidth: label.scrollWidth,
            labelClientWidth: label.clientWidth,
            buttonWidth: button.getBoundingClientRect().width,
            buttonScrollWidth: button.scrollWidth,
            buttonClientWidth: button.clientWidth,
            font: style.font,
            fontFamily: style.fontFamily,
            gridColumns: getComputedStyle(button.parentElement!).gridTemplateColumns,
          },
        };
      }),
    );
    expect(labels).toHaveLength(7);
    const labelIssues = labels.filter((label) => !label.fits || !label.unbroken || !label.visible);
    expect(labels.every((label) => label.fits && label.unbroken && label.visible), `${fontSize}px labels: ${JSON.stringify(labelIssues)}`).toBe(true);
    expect(labels.some((label) => label.text === 'Звук')).toBe(true);
    expect(labels.some((label) => label.text === 'Подсказка')).toBe(true);

    const hint = page.locator('.primary-controls .ctrl-btn[aria-label^="Подсказка"]');
    await expect(hint).toHaveAccessibleName('Подсказка (осталось 3)');
    await expect(hint.locator('.control-label')).toHaveText('Подсказка');
    const sound = page.locator('.secondary-controls .ctrl-btn[aria-pressed]');
    await expect(sound).toHaveAccessibleName('Выключить звук');
    await expect(sound.locator('.control-label')).toHaveText('Звук');
  }
});
