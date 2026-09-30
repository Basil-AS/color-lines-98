import { useEffect, useState, useSyncExternalStore } from 'react';
import { installKind } from './install';
import { clearInstallPrompt, onInstallPrompt, savedInstallPrompt } from './earlyInstall';
import type { InstallKind } from './install';

function isStandalone(): boolean {
  const iosStandalone = (navigator as Navigator & { standalone?: boolean }).standalone === true;
  return iosStandalone || (window.matchMedia?.('(display-mode: standalone)').matches ?? false);
}

/** Tracks whether and how the app can be installed in this browser. */
export function useInstall(): { kind: InstallKind; install: () => Promise<void> } {
  // Reads the event kept since page load and follows later changes; nothing can be missed between render and effect.
  const promptEvent = useSyncExternalStore((notify) => onInstallPrompt(() => notify()), savedInstallPrompt);
  const [installed, setInstalled] = useState(isStandalone);

  useEffect(() => {
    const onInstalled = () => setInstalled(true);
    window.addEventListener('appinstalled', onInstalled);
    return () => window.removeEventListener('appinstalled', onInstalled);
  }, []);

  const kind = installKind({
    userAgent: navigator.userAgent,
    standalone: installed,
    hasPrompt: promptEvent !== null,
    touchPoints: navigator.maxTouchPoints,
  });

  const install = async () => {
    if (!promptEvent) return;
    await promptEvent.prompt();
    await promptEvent.userChoice;
    clearInstallPrompt();
  };

  return { kind, install };
}
