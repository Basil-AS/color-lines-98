import { useState } from 'react';
import { BALL_COLORS, customPreset, decodeLook, encodeLook, paletteTones } from '../appearance';
import type { CustomTheme, LookOverrides, LookShare } from '../appearance';
import { contrastRatio, normalizeHex, toRgb } from '../colorlab';
import { colorName, translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import { effectiveLook } from '../appearance';
import type { Theme } from '../themes';
import type { BallColor } from '../engine/models';

interface BuilderProps {
  lang: Language;
  theme: Theme;
  custom: CustomTheme;
  overrides: LookOverrides | undefined;
  onCustom: (custom: CustomTheme) => void;
  onUseCustom: () => void;
  onApplyShare: (share: LookShare) => void;
}

/** Straight-line distance between two colours in RGB: crude, but enough to tell "alike" from "different". */
const distance = (a: string, b: string): number => {
  const x = toRgb(a);
  const y = toRgb(b);
  return Math.hypot(x[0] - y[0], x[1] - y[1], x[2] - y[2]);
};

/** What is wrong with a set of ball colours: pairs that look alike and colours lost against the cells. */
export function ballWarnings(balls: Record<BallColor, string>, cell: string): { similar: [BallColor, BallColor][]; faint: BallColor[] } {
  const similar: [BallColor, BallColor][] = [];
  for (let i = 0; i < BALL_COLORS.length; i++) {
    for (let j = i + 1; j < BALL_COLORS.length; j++) {
      if (distance(balls[BALL_COLORS[i]], balls[BALL_COLORS[j]]) < 70) similar.push([BALL_COLORS[i], BALL_COLORS[j]]);
    }
  }
  const faint = BALL_COLORS.filter((c) => contrastRatio(balls[c], cell) < 1.5);
  return { similar, faint };
}

function ColorField({ id, label, value, onChange }: { id: string; label: string; value: string; onChange: (hex: string) => void }) {
  return (
    <label className="color-field" htmlFor={id}>
      <span>{label}</span>
      <input id={id} type="color" value={normalizeHex(value, '#000000')} onChange={(e) => onChange(normalizeHex(e.target.value, value))} />
    </label>
  );
}

export function LookBuilder({ lang, theme, custom, overrides, onCustom, onUseCustom, onApplyShare }: BuilderProps) {
  const t = (key: string, params?: Record<string, string>) => translate(lang, key as MessageKey, params);
  const [code, setCode] = useState('');
  const [note, setNote] = useState('');
  const active = theme === 'custom';
  const warnings = ballWarnings(custom.balls, custom.cell);
  const share = encodeLook({ theme, look: overrides ?? {}, custom });

  const fromCurrent = () => {
    const style = getComputedStyle(document.body);
    const read = (name: string, fallback: string) => normalizeHex(style.getPropertyValue(name).trim(), fallback);
    const look = effectiveLook(theme, overrides);
    const tones = paletteTones(theme === 'custom' ? 'custom' : look.palette, custom.balls);
    const balls = { ...custom.balls };
    for (const c of BALL_COLORS) balls[c] = tones[c].base;
    onCustom({
      dark: style.colorScheme.includes('dark') && !style.colorScheme.includes('light'),
      page: read('--page-bg', custom.page),
      board: read('--board-bg', custom.board),
      cell: read('--cell-bg', custom.cell),
      accent: read('--accent', custom.accent),
      balls,
    });
  };

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(share);
      setNote(t('share.copied'));
    } catch {
      setNote('');
    }
  };

  const apply = () => {
    const decoded = decodeLook(code);
    if (!decoded) {
      setNote(t('share.bad'));
      return;
    }
    onApplyShare(decoded);
    setCode('');
    setNote(t('share.applied'));
  };

  return (
    <div className="look-builder">
      <p className="settings-hint">{t('builder.intro')}</p>

      <div className="look-group" role="group" aria-labelledby="builder-start">
        <h4 className="look-label" id="builder-start">{t('builder.start')}</h4>
        <div className="chips">
          <button type="button" className="chip" onClick={() => onCustom(customPreset(false))}>{t('builder.startLight')}</button>
          <button type="button" className="chip" onClick={() => onCustom(customPreset(true))}>{t('builder.startDark')}</button>
          <button type="button" className="chip" onClick={fromCurrent}>{t('builder.fromCurrent')}</button>
          <button type="button" className="chip" onClick={() => onCustom(customPreset(custom.dark))}>{t('builder.reset')}</button>
        </div>
      </div>

      <div className="look-group" role="group" aria-labelledby="builder-surface">
        <h4 className="look-label" id="builder-surface">{t('builder.mode')}</h4>
        <div className="chips">
          <button type="button" className={`chip ${!custom.dark ? 'active' : ''}`} aria-pressed={!custom.dark} onClick={() => onCustom({ ...custom, dark: false })}>{t('builder.startLight')}</button>
          <button type="button" className={`chip ${custom.dark ? 'active' : ''}`} aria-pressed={custom.dark} onClick={() => onCustom({ ...custom, dark: true })}>{t('builder.startDark')}</button>
        </div>
        <div className="color-fields">
          <ColorField id="cf-page" label={t('builder.page')} value={custom.page} onChange={(page) => onCustom({ ...custom, page })} />
          <ColorField id="cf-board" label={t('builder.board')} value={custom.board} onChange={(board) => onCustom({ ...custom, board })} />
          <ColorField id="cf-cell" label={t('builder.cell')} value={custom.cell} onChange={(cell) => onCustom({ ...custom, cell })} />
          <ColorField id="cf-accent" label={t('builder.accent')} value={custom.accent} onChange={(accent) => onCustom({ ...custom, accent })} />
        </div>
      </div>

      <div className="look-group" role="group" aria-labelledby="builder-balls">
        <h4 className="look-label" id="builder-balls">{t('builder.balls')}</h4>
        <div className="color-fields">
          {BALL_COLORS.map((color) => (
            <ColorField key={color} id={`cf-${color}`} label={colorName(lang, color)} value={custom.balls[color]} onChange={(hex) => onCustom({ ...custom, balls: { ...custom.balls, [color]: hex } })} />
          ))}
        </div>
        <ul className="look-warnings">
          {warnings.similar.map(([a, b]) => (
            <li key={`${a}-${b}`} className="warn">{t('builder.warnSimilar', { a: colorName(lang, a), b: colorName(lang, b) })}</li>
          ))}
          {warnings.faint.map((c) => (
            <li key={c} className="warn">{t('builder.warnCell', { color: colorName(lang, c) })}</li>
          ))}
          {warnings.similar.length === 0 && warnings.faint.length === 0 && <li className="ok">{t('builder.good')}</li>}
        </ul>
      </div>

      <button type="button" className="modal-btn primary" onClick={onUseCustom} disabled={active}>
        {active ? t('builder.active') : t('builder.use')}
      </button>

      <div className="look-group" role="group" aria-labelledby="share-title">
        <h4 className="look-label" id="share-title">{t('share.title')}</h4>
        <p className="settings-hint">{t('share.hint')}</p>
        <textarea className="share-code" readOnly rows={2} value={share} aria-label={t('share.title')} onFocus={(e) => e.currentTarget.select()} />
        <div className="chips">
          <button type="button" className="chip" onClick={copy}>{t('share.copy')}</button>
        </div>
        <textarea className="share-code" rows={2} value={code} placeholder={t('share.placeholder')} aria-label={t('share.placeholder')} onChange={(e) => setCode(e.target.value)} />
        <div className="chips">
          <button type="button" className="chip" onClick={apply} disabled={code.trim() === ''}>{t('share.apply')}</button>
        </div>
        <p className="settings-hint" role="status">{note}</p>
      </div>
    </div>
  );
}
