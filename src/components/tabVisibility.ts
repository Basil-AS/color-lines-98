/** Reveal a selected tab by moving only its horizontal rail. */
export function revealTabHorizontally(tab: HTMLElement | null): void {
  const rail = tab?.parentElement;
  if (!tab || !rail) return;

  const tabRect = tab.getBoundingClientRect();
  const railRect = rail.getBoundingClientRect();
  if (tabRect.left < railRect.left) {
    rail.scrollLeft -= Math.ceil(railRect.left - tabRect.left);
  } else if (tabRect.right > railRect.right) {
    rail.scrollLeft += Math.ceil(tabRect.right - railRect.right);
  }
}
