// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';

describe('the install prompt is kept from the start', () => {
  it('remembers an event that fired before anyone listened, and hands it to a late subscriber', async () => {
    const mod = await import('../src/pwa/earlyInstall');
    expect(mod.savedInstallPrompt()).toBeNull();
    const event = Object.assign(new Event('beforeinstallprompt', { cancelable: true }), {
      prompt: vi.fn(async () => undefined),
      userChoice: Promise.resolve({ outcome: 'accepted' as const }),
    });
    window.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    expect(mod.savedInstallPrompt()).toBe(event);
    const seen: unknown[] = [];
    const off = mod.onInstallPrompt((e) => seen.push(e));
    window.dispatchEvent(new Event('appinstalled'));
    expect(seen).toEqual([null]);
    expect(mod.savedInstallPrompt()).toBeNull();
    off();
  });
});
