export type InstallKind = 'installed' | 'prompt' | 'ios' | 'safari-mac' | 'none';

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
  return safariOnMac ? 'safari-mac' : 'none';
}
