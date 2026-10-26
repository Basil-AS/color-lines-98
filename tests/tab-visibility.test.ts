// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { revealTabHorizontally } from '../src/components/tabVisibility';

describe('revealTabHorizontally', () => {
  it('moves the tab rail to reveal a selected tab without scrolling its vertical dialog', () => {
    const dialog = document.createElement('div');
    const rail = document.createElement('div');
    const tab = document.createElement('button');
    dialog.append(rail);
    rail.append(tab);
    dialog.scrollTop = 73;
    rail.scrollLeft = 0;
    Object.defineProperty(rail, 'scrollWidth', { value: 360 });
    Object.defineProperty(rail, 'clientWidth', { value: 120 });
    rail.getBoundingClientRect = () => ({ left: 20, right: 140, top: 10, bottom: 40, width: 120, height: 30, x: 20, y: 10, toJSON: () => ({}) });
    tab.getBoundingClientRect = () => {
      const left = 190 - rail.scrollLeft;
      return { left, right: left + 90, top: 10, bottom: 40, width: 90, height: 30, x: left, y: 10, toJSON: () => ({}) };
    };

    revealTabHorizontally(tab);

    expect(rail.scrollLeft).toBe(140);
    expect(dialog.scrollTop).toBe(73);
    expect(tab.getBoundingClientRect().right).toBe(140);
  });

  it('moves left only when a selected tab is clipped at the rail start', () => {
    const rail = document.createElement('div');
    const tab = document.createElement('button');
    rail.append(tab);
    rail.scrollLeft = 40;
    rail.getBoundingClientRect = () => ({ left: 20, right: 140, top: 10, bottom: 40, width: 120, height: 30, x: 20, y: 10, toJSON: () => ({}) });
    tab.getBoundingClientRect = () => ({ left: -5, right: 45, top: 10, bottom: 40, width: 50, height: 30, x: -5, y: 10, toJSON: () => ({}) });

    revealTabHorizontally(tab);

    expect(rail.scrollLeft).toBe(15);
  });
});
