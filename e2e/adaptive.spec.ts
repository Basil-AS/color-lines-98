import { expect, test } from '@playwright/test';

const WINDOWS = [
  { width: 320, height: 568 },
  { width: 360, height: 640 },
  { width: 411, height: 891 },
  { width: 568, height: 280 },
  { width: 640, height: 320 },
  { width: 844, height: 400 },
  { width: 820, height: 1180 },
  { width: 1180, height: 820 },
  { width: 1440, height: 900 },
];

const MODERN_THEMES = ['modern', 'material', 'neon', 'synthwave', 'ocean', 'paper', 'gameboy', 'terminal', 'contrast', 'clear', 'retro98', 'spectrum', 'meadow', 'orchid', 'custom'];

for (const theme of ['modern', 'lines98', 'lines98plus', 'colorlines92']) {
  for (const fontSize of [24, 32]) {
    for (const viewport of [{ width: 568, height: 280 }, { width: 640, height: 320 }]) {
      test(`${theme} Russian ${fontSize}px text keeps short-window actions reachable at ${viewport.width}×${viewport.height}`, async ({ page }) => {
        await page.setViewportSize(viewport);
        await page.addInitScript(({ theme }) => {
          localStorage.setItem('colorlines_theme', theme);
          localStorage.setItem('colorlines_lang', 'ru');
        }, { theme });
        await page.goto('/');
        await page.evaluate((fontSize) => { document.documentElement.style.fontSize = `${fontSize}px`; }, fontSize);
        await expect(page.locator('.board-cell')).toHaveCount(81);
        await expect.poll(() => page.evaluate(() => ({
          horizontal: document.documentElement.scrollWidth > innerWidth,
          vertical: document.documentElement.scrollHeight > innerHeight + 1,
        }))).toEqual({ horizontal: false, vertical: false });
        const picture = page.locator(theme === 'colorlines92' ? '.dos-screen' : '.board-grid');
        const before = await picture.boundingBox();
        expect(before!.width).toBeGreaterThanOrEqual(120);
        expect(before!.y).toBeGreaterThanOrEqual(0);
        expect(before!.y + before!.height).toBeLessThanOrEqual(viewport.height);

        // Enlarged controls may use an internal rail scroller. Every action must
        // remain fully visible and receive events after scrolling to it.
        const actions = page.locator('.game-window button, .game-window a').filter({ visible: true });
        for (const action of await actions.all()) {
          if (await action.evaluate((el) => !!el.closest('.board-grid, .dos-screen'))) continue;
          await action.scrollIntoViewIfNeeded();
          const diagnostics = await action.evaluate((el) => {
            const r = el.getBoundingClientRect();
            const hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
            const label = el.querySelector<HTMLElement>('.control-label');
            const rail = el.closest<HTMLElement>('.controls-group');
            return {
              reachable: r.left >= 0 && r.right <= innerWidth && r.top >= 0 && r.bottom <= innerHeight
                && !!hit && (el === hit || el.contains(hit)) && el.scrollWidth <= el.clientWidth + 1,
              action: el.getAttribute('aria-label') ?? el.textContent?.trim(),
              viewport: { width: innerWidth, height: innerHeight },
              rect: { left: r.left, top: r.top, right: r.right, bottom: r.bottom },
              size: { scrollWidth: el.scrollWidth, clientWidth: el.clientWidth },
              label: label ? {
                text: label.textContent?.trim(),
                width: label.getBoundingClientRect().width,
                scrollWidth: label.scrollWidth,
                clientWidth: label.clientWidth,
                font: getComputedStyle(label).font,
                family: getComputedStyle(label).fontFamily,
              } : null,
              rail: rail ? {
                scrollTop: rail.scrollTop,
                scrollHeight: rail.scrollHeight,
                clientHeight: rail.clientHeight,
                primaryColumns: getComputedStyle(rail.querySelector('.primary-controls')!).gridTemplateColumns,
              } : null,
              hit: hit ? { tag: hit.tagName, className: (hit as HTMLElement).className, text: hit.textContent?.trim() } : null,
            };
          });
          expect(diagnostics.reachable, JSON.stringify(diagnostics)).toBe(true);
        }
        const after = await picture.boundingBox();
        for (const key of ['x', 'y', 'width', 'height'] as const) {
          // Sticky layout can round a fractional font metric to the next device pixel.
          expect(Math.abs(after![key] - before![key]), key).toBeLessThan(1);
        }
        const goals = page.getByRole('button', { name: /^Цели/ }).first();
        await goals.click();
        await expect(page.getByRole('dialog')).toBeVisible();
      });
    }
  }
}

test('primary controls adapt to wide fallback-font metrics at 32px without shrinking the board', async ({ page }) => {
  const viewport = { width: 568, height: 280 };
  await page.setViewportSize(viewport);
  await page.addInitScript(() => {
    localStorage.setItem('colorlines_theme', 'modern');
    localStorage.setItem('colorlines_lang', 'ru');
  });
  await page.goto('/');
  await page.evaluate(() => { document.documentElement.style.fontSize = '32px'; });
  // Georgia with added tracking is a deterministic stress case for wider
  // platform font metrics. The shipped fonts and default 32px test stay intact.
  await page.addStyleTag({
    content: '.adaptive-game .primary-controls .control-label { font-family: Georgia, serif !important; letter-spacing: .1em !important; }',
  });

  const board = page.locator('.board-grid');
  const boardBefore = await board.boundingBox();
  expect(boardBefore).not.toBeNull();
  const labels = await page.locator('.primary-controls .control-label').evaluateAll((elements) =>
    elements.map((label) => ({
      text: label.textContent?.trim(),
      scrollWidth: label.scrollWidth,
      clientWidth: label.clientWidth,
      buttonWidth: label.closest('button')!.getBoundingClientRect().width,
    })),
  );
  expect(labels).toHaveLength(3);
  expect(labels.every((label) => label.scrollWidth <= label.clientWidth + 1), JSON.stringify(labels)).toBe(true);

  const buttons = page.locator('.primary-controls .ctrl-btn');
  for (const button of await buttons.all()) {
    await button.scrollIntoViewIfNeeded();
    const diagnostics = await button.evaluate((element) => {
      const rect = element.getBoundingClientRect();
      const hit = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
      return {
        reachable: rect.left >= 0 && rect.right <= innerWidth && rect.top >= 0 && rect.bottom <= innerHeight
          && !!hit && (element === hit || element.contains(hit))
          && element.scrollWidth <= element.clientWidth + 1,
        name: element.getAttribute('aria-label'),
        button: { rect: [rect.left, rect.top, rect.right, rect.bottom], scrollWidth: element.scrollWidth, clientWidth: element.clientWidth },
        label: [...element.querySelectorAll<HTMLElement>('.control-label')].map((label) => ({
          text: label.textContent?.trim(),
          scrollWidth: label.scrollWidth,
          clientWidth: label.clientWidth,
          font: getComputedStyle(label).font,
        })),
        columns: getComputedStyle(element.parentElement!).gridTemplateColumns,
        hit: hit ? { tag: hit.tagName, className: (hit as HTMLElement).className } : null,
      };
    });
    expect(diagnostics.reachable, JSON.stringify(diagnostics)).toBe(true);
  }
  const boardAfter = await board.boundingBox();
  expect(boardAfter).not.toBeNull();
  expect(boardAfter!.width).toBe(boardBefore!.width);
  expect(boardAfter!.height).toBe(boardBefore!.height);
});

for (const viewport of WINDOWS) {
  test(`board and all game controls fit ${viewport.width}×${viewport.height}`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await page.addInitScript(() => {
      localStorage.setItem('colorlines_lang', 'en');
      localStorage.setItem('colorlines_theme', 'modern');
    });
    await page.goto('/');
    await expect(page.locator('.board-cell')).toHaveCount(81);
    const geometry = await page.evaluate(() => {
      const board = document.querySelector('.board-grid')!.getBoundingClientRect();
      const controls = [...document.querySelectorAll('.game-window button, .game-window a')]
        .filter((el) => !el.closest('.board-grid'))
        .map((el) => ({ label: el.getAttribute('aria-label') ?? el.textContent, rect: el.getBoundingClientRect() }))
        .filter(({ rect }) => rect.width > 0 && rect.height > 0);
      return {
        board: { width: board.width, height: board.height, top: board.top, bottom: board.bottom },
        outside: controls.filter(({ rect }) => rect.left < 0 || rect.top < 0 || rect.right > innerWidth || rect.bottom > innerHeight).map(({ label }) => label),
        overflow: document.documentElement.scrollWidth > innerWidth || document.documentElement.scrollHeight > innerHeight + 1,
      };
    });
    expect(geometry.outside, 'every game action stays in the viewport').toEqual([]);
    expect(geometry.overflow, 'playing needs no page scroll').toBe(false);
    expect(Math.abs(geometry.board.width - geometry.board.height)).toBeLessThan(1);
    expect(geometry.board.top).toBeGreaterThanOrEqual(0);
    expect(geometry.board.bottom).toBeLessThanOrEqual(viewport.height);
    const wide = viewport.width >= 960 || viewport.width / viewport.height >= 1.45;
    const availableSquare = wide
      ? Math.min(viewport.height - 32, viewport.width - 310)
      : Math.min(viewport.width - 26, viewport.height - 265);
    expect(geometry.board.width, 'the board uses the available short dimension').toBeGreaterThanOrEqual(availableSquare - 8);
    await page.getByRole('button', { name: 'Settings', exact: true }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
  });
}

test('large text retains the board and reachable actions in a wide window', async ({ page }) => {
  await page.setViewportSize({ width: 1180, height: 820 });
  await page.goto('/');
  await page.evaluate(() => { document.documentElement.style.fontSize = '24px'; });
  const board = await page.locator('.board-grid').boundingBox();
  expect(board!.width).toBeGreaterThan(600);
  await page.getByRole('button', { name: 'Settings', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
});

for (const scenario of [
  { name: 'Russian labels in short landscape', width: 568, height: 280, lang: 'ru', fontSize: '16px' },
  { name: 'enlarged text on a small phone', width: 320, height: 568, lang: 'en', fontSize: '24px' },
]) {
  test(scenario.name, async ({ page }) => {
    await page.setViewportSize(scenario);
    await page.addInitScript(({ lang }) => {
      localStorage.setItem('colorlines_lang', lang);
      localStorage.setItem('colorlines_theme', 'modern');
    }, scenario);
    await page.goto('/');
    await page.evaluate(({ fontSize }) => { document.documentElement.style.fontSize = fontSize; }, scenario);
    await expect(page.locator('.board-cell')).toHaveCount(81);
    await expect.poll(() => page.evaluate(() => {
      const board = document.querySelector('.board-grid')!.getBoundingClientRect();
      const elements = [...document.querySelectorAll('.game-window button, .game-window a, .game-title, .stat-box')];
      const outside = elements.filter((el) => {
        const r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && (r.left < 0 || r.top < 0 || r.right > innerWidth || r.bottom > innerHeight);
      });
      return outside.length === 0 && board.width >= 100 && board.bottom <= innerHeight && document.documentElement.scrollHeight <= innerHeight + 1;
    }), { message: 'text size and translated labels must not push controls or the board offscreen' }).toBe(true);
  });
}

for (const viewport of [{ width: 320, height: 568 }, { width: 844, height: 400 }]) {
  test(`tutorial keeps its board and exit reachable at ${viewport.width}×${viewport.height}`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await page.addInitScript(() => { localStorage.setItem('colorlines_lang', 'en'); });
    await page.goto('/');
    await page.getByRole('button', { name: 'Rules and info', exact: true }).click();
    await page.getByRole('button', { name: 'Take the tutorial' }).click();
    await expect(page.locator('.tutorial-banner')).toBeVisible();
    await expect.poll(() => page.evaluate(() => {
      const board = document.querySelector('.board-grid')!.getBoundingClientRect();
      const skip = [...document.querySelectorAll('button')].find((button) => button.textContent?.includes('Skip the tutorial'))!.getBoundingClientRect();
      return board.width >= 100 && board.top >= 0 && board.bottom <= innerHeight && skip.top >= 0 && skip.bottom <= innerHeight && document.documentElement.scrollHeight <= innerHeight + 1;
    })).toBe(true);
  });
}

test('every modern look keeps the short landscape board and actions in view', async ({ page }) => {
  await page.setViewportSize({ width: 568, height: 280 });
  await page.goto('/');
  for (const theme of MODERN_THEMES) {
    await page.evaluate((theme) => {
      localStorage.setItem('colorlines_theme', theme);
      localStorage.setItem('colorlines_lang', 'ru');
    }, theme);
    await page.reload();
    await expect(page.locator('.board-cell')).toHaveCount(81);
    const problems = await page.evaluate(() => {
      const board = document.querySelector('.board-grid')!.getBoundingClientRect();
      const outside = [...document.querySelectorAll('.game-window button, .game-window a')].filter((el) => {
        const r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && (r.left < 0 || r.top < 0 || r.right > innerWidth || r.bottom > innerHeight);
      });
      return { outside: outside.length, scrolls: document.documentElement.scrollHeight > innerHeight + 1, size: board.width, square: Math.abs(board.width - board.height) < 1 };
    });
    expect(problems.outside, theme).toBe(0);
    expect(problems.scrolls, theme).toBe(false);
    expect(problems.size, theme).toBeGreaterThanOrEqual(240);
    expect(problems.square, theme).toBe(true);
  }
});

for (const theme of ['colorlines92', 'lines98', 'lines98plus']) {
  for (const viewport of [{ width: 568, height: 280 }, { width: 640, height: 320 }, { width: 844, height: 400 }]) {
    test(`${theme} retains all artwork and reachable controls at ${viewport.width}×${viewport.height}`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.addInitScript((theme) => {
        localStorage.setItem('colorlines_theme', theme);
        localStorage.setItem('colorlines_lang', 'en');
      }, theme);
      await page.goto('/');
      await expect(page.locator('.board-cell')).toHaveCount(81);
      const problems = await page.evaluate(() => {
        const elements = [...document.querySelectorAll('.game-window button, .game-window a, .dos-screen, .board-grid')];
        return elements.filter((el) => {
          const r = el.getBoundingClientRect();
          return r.width > 0 && r.height > 0 && (r.left < 0 || r.top < 0 || r.right > innerWidth || r.bottom > innerHeight);
        }).map((el) => el.getAttribute('aria-label') ?? el.className);
      });
      expect(problems).toEqual([]);
      expect(await page.evaluate(() => document.documentElement.scrollHeight <= innerHeight + 1)).toBe(true);
      if (theme === 'colorlines92') {
        const screen = await page.locator('.dos-screen').boundingBox();
        expect(screen!.width / screen!.height).toBeCloseTo(4 / 3, 2);
      } else {
        await expect(page.locator('.ball-classic').first()).toBeVisible();
        if (theme === 'lines98') await expect(page.locator('.win98-titlebar')).toBeVisible();
      }
    });
  }
}

test('modern controls have visible names and rounded touch surfaces', async ({ page }) => {
  await page.setViewportSize({ width: 411, height: 891 });
  for (const theme of ['modern', 'clear', 'meadow', 'terminal']) {
    await page.addInitScript((theme) => {
      localStorage.setItem('colorlines_theme', theme);
      localStorage.setItem('colorlines_lang', 'en');
    }, theme);
    await page.goto('/');
    const labels = page.locator('.adaptive-game .controls-group .control-label');
    await expect(labels).toHaveCount(7);
    for (const label of await labels.all()) await expect(label).toBeVisible();
    const targets = await page.locator('.adaptive-game .controls-group button').evaluateAll((buttons) => buttons.map((button) => {
      const bounds = button.getBoundingClientRect();
      const style = getComputedStyle(button);
      return bounds.height >= 44 && bounds.width >= 44 && parseFloat(style.borderTopLeftRadius) >= 12;
    }));
    expect(targets, theme).toEqual(Array(7).fill(true));
  }
});

test('classic Windows controls retain their original presentation', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('colorlines_theme', 'lines98plus'));
  await page.goto('/');
  await expect(page.locator('.board-cell')).toHaveCount(81);
  await expect(page.locator('.control-label')).toHaveCount(0);
});
