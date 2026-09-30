import { describe, expect, it } from 'vitest';
import { existsSync, readFileSync } from 'node:fs';
import { buildServiceWorker, precacheList } from '../pwa/precache';
import { installKind } from '../src/pwa/install';

const manifest = JSON.parse(readFileSync('public/manifest.webmanifest', 'utf8'));

describe('web app manifest', () => {
  it('has what installers need', () => {
    expect(manifest.name).toBe('Color Lines');
    expect(manifest.short_name).toBeTruthy();
    expect(manifest.display).toBe('standalone');
    expect(manifest.start_url).toBe('./');
    expect(manifest.scope).toBe('./');
    expect(manifest.theme_color).toMatch(/^#[0-9a-f]{6}$/i);
    expect(manifest.background_color).toMatch(/^#[0-9a-f]{6}$/i);
    expect(manifest.categories).toContain('games');
  });

  it('lists icons that exist, including 192, 512 and maskable', () => {
    const sizes = manifest.icons.map((i: { sizes: string; purpose?: string }) => `${i.sizes}/${i.purpose ?? 'any'}`);
    expect(sizes).toEqual(expect.arrayContaining(['192x192/any', '512x512/any', '192x192/maskable', '512x512/maskable']));
    for (const icon of manifest.icons) expect(existsSync('public/' + icon.src.replace(/^\.\//, ''))).toBe(true);
  });

  it('lists store screenshots that exist, for wide and narrow screens', () => {
    const forms = manifest.screenshots.map((s: { form_factor: string }) => s.form_factor);
    expect(forms).toEqual(expect.arrayContaining(['wide', 'narrow']));
    for (const shot of manifest.screenshots) expect(existsSync('public/' + shot.src.replace(/^\.\//, ''))).toBe(true);
  });

  it('is linked from index.html together with the iOS and Windows hints', () => {
    const html = readFileSync('index.html', 'utf8');
    expect(html).toContain('rel="manifest"');
    expect(html).toContain('rel="apple-touch-icon"');
    expect(html).toContain('apple-mobile-web-app-capable');
    expect(html).toContain('msapplication-TileColor');
    expect(html).toContain('viewport-fit=cover');
  });
});

describe('precache list', () => {
  it('keeps the shell and assets, drops the worker itself and source maps', () => {
    const list = precacheList(['index.html', 'assets/a-1.js', 'assets/a-1.js.map', 'sw.js', 'icons/icon-192.png', '.DS_Store']);
    expect(list).toEqual(['./', './assets/a-1.js', './icons/icon-192.png', './index.html']);
  });

  it('always lists the app shell, even when the HTML is not in the input yet', () => {
    expect(precacheList(['assets/a.js'])).toEqual(expect.arrayContaining(['./', './index.html']));
  });

  it('is stable regardless of input order', () => {
    expect(precacheList(['b.js', 'a.js', 'index.html'])).toEqual(precacheList(['index.html', 'a.js', 'b.js']));
  });
});

describe('generated service worker', () => {
  const sw = buildServiceWorker(['./', './index.html', './assets/x.js']);

  it('embeds the file list and a version that changes with the content', () => {
    expect(sw).toContain('"./assets/x.js"');
    expect(sw).not.toBe(buildServiceWorker(['./', './index.html', './assets/y.js']));
  });

  it('gets a new cache version when only the file contents change', () => {
    const urls = ['./', './index.html'];
    expect(buildServiceWorker(urls, 'aaa')).not.toBe(buildServiceWorker(urls, 'bbb'));
    expect(buildServiceWorker(urls, 'aaa')).toBe(buildServiceWorker(urls, 'aaa'));
  });

  it('handles install, activate and fetch, and is valid JavaScript', () => {
    for (const ev of ['install', 'activate', 'fetch']) expect(sw).toContain(`addEventListener('${ev}'`);
    expect(() => new Function(sw)).not.toThrow();
  });
});

describe('installKind', () => {
  const chrome = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36';
  const iphone = 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1';
  const macSafari = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15';
  const ipadDesktopUa = macSafari;

  it('is "installed" when already running as an app', () => {
    expect(installKind({ userAgent: chrome, standalone: true, hasPrompt: true, touchPoints: 0 })).toBe('installed');
  });

  it('offers the browser prompt when one is available', () => {
    expect(installKind({ userAgent: chrome, standalone: false, hasPrompt: true, touchPoints: 0 })).toBe('prompt');
  });

  it('explains "Add to Home Screen" on iPhone and iPad', () => {
    expect(installKind({ userAgent: iphone, standalone: false, hasPrompt: false, touchPoints: 5 })).toBe('ios');
    expect(installKind({ userAgent: ipadDesktopUa, standalone: false, hasPrompt: false, touchPoints: 5 })).toBe('ios');
  });

  it('explains "Add to Dock" in Safari on a Mac', () => {
    expect(installKind({ userAgent: macSafari, standalone: false, hasPrompt: false, touchPoints: 0 })).toBe('safari-mac');
  });

  it('explains the browser menu where Chrome has no prompt to offer (dismissed, or not yet)', () => {
    expect(installKind({ userAgent: chrome, standalone: false, hasPrompt: false, touchPoints: 0 })).toBe('manual');
  });

  it('offers nothing in a browser that cannot install at all', () => {
    expect(installKind({ userAgent: 'SomeOldBrowser/1.0', standalone: false, hasPrompt: false, touchPoints: 0 })).toBe('none');
  });
});
