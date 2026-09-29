import { translate } from './i18n';
import type { Language } from './i18n';

/** "45 s", "12 min" or "1 h 05 min", in the given language. */
export function formatDuration(lang: Language, ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  if (totalSeconds < 60) return translate(lang, 'time.s', { s: totalSeconds });
  const totalMinutes = Math.floor(totalSeconds / 60);
  if (totalMinutes < 60) return translate(lang, 'time.m', { m: totalMinutes });
  const h = Math.floor(totalMinutes / 60);
  const m = String(totalMinutes % 60).padStart(2, '0');
  return translate(lang, 'time.hm', { h, m });
}
