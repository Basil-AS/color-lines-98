/**
 * Chrome fires "beforeinstallprompt" once, soon after the page loads, which can be before React has mounted. This module
 * is imported first and keeps the event, so the Install button still works when it appears afterwards.
 */
export interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

let saved: BeforeInstallPromptEvent | null = null;
const listeners = new Set<(e: BeforeInstallPromptEvent | null) => void>();

export function savedInstallPrompt(): BeforeInstallPromptEvent | null {
  return saved;
}

export function onInstallPrompt(listener: (e: BeforeInstallPromptEvent | null) => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function clearInstallPrompt(): void {
  saved = null;
  listeners.forEach((l) => l(null));
}

if (typeof window !== 'undefined') {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    saved = e as BeforeInstallPromptEvent;
    listeners.forEach((l) => l(saved));
  });
  window.addEventListener('appinstalled', clearInstallPrompt);
}
