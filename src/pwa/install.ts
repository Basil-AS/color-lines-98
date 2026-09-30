export type InstallKind = 'installed' | 'prompt' | 'ios' | 'safari-mac' | 'manual' | 'none';

export interface InstallEnv {
  userAgent: string;
  /** Running as an installed app (display-mode: standalone or iOS navigator.standalone). */
  standalone: boolean;
  /** The browser fired beforeinstallprompt (Chrome, Edge, Samsung Internet, Android). */
  hasPrompt: boolean;
  touchPoints: number;
}

/** What the "Install" button should do in this browser. */
export function installKind(env: InstallEnv): InstallKind {
  if (env.standalone) return 'installed';
  if (env.hasPrompt) return 'prompt';
  const ua = env.userAgent;
  const iPhoneOrIPad = /iPhone|iPad|iPod/.test(ua);
  // iPadOS reports itself as a Mac, but has a touch screen.
  const iPadAsMac = /Macintosh/.test(ua) && env.touchPoints > 1;
  if (iPhoneOrIPad || iPadAsMac) return 'ios';
  const safariOnMac = /Macintosh/.test(ua) && /Safari/.test(ua) && !/Chrome|Chromium|Edg|Firefox/.test(ua);
  if (safariOnMac) return 'safari-mac';
  // Chrome, Edge, Samsung Internet and Firefox can all install from their menu even when they offer no prompt
  // (it was already dismissed, or the browser only shows it after some use), so the button explains where.
  return /Chrome|Chromium|Edg|SamsungBrowser|Firefox|OPR/.test(ua) ? 'manual' : 'none';
}
