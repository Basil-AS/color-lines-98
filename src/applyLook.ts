import { ballVariables, customColorScheme, customPageColor, customVariables, lookAttributes } from './appearance';
import type { CustomTheme, LookOverrides } from './appearance';
import type { Theme } from './themes';

const ATTRIBUTES = ['data-studio', 'data-volume', 'data-contrast', 'data-shapes', 'data-ballsize', 'data-grid', 'data-cells'];
let applied: string[] = [];

/** Writes the look on the page: data attributes for the stylesheet and custom properties for the colours. */
export function applyLook(theme: Theme, overrides: LookOverrides | undefined, custom: CustomTheme, target: HTMLElement = document.body): void {
  for (const name of ATTRIBUTES) target.removeAttribute(name);
  for (const name of applied) target.style.removeProperty(name);
  applied = [];
  for (const [name, value] of Object.entries(lookAttributes(theme, overrides))) target.setAttribute(name, value);
  const vars = { ...(theme === 'custom' ? customVariables(custom) : {}), ...ballVariables(theme, overrides, custom) };
  for (const [name, value] of Object.entries(vars)) {
    target.style.setProperty(name, value);
    applied.push(name);
  }
  target.style.colorScheme = theme === 'custom' ? customColorScheme(custom) : '';
}

/** The browser bar colour of the theme, for the custom one it follows the page colour. */
export const customThemeColor = (custom: CustomTheme): string => customPageColor(custom);
