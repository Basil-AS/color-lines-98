import { useState } from 'react';
import { BALL_COLORS } from '../appearance';
import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import { ballThemeClass } from '../themes';
import type { Theme } from '../themes';

/** Colour-vision simulations (Viénot matrices) so a look can be checked for balls that merge. */
export const VISIONS = ['normal', 'grey', 'protan', 'deutan', 'tritan'] as const;
export type Vision = (typeof VISIONS)[number];
const MATRICES: Record<Exclude<Vision, 'normal' | 'grey'>, string> = {
  protan: '0.567 0.433 0 0 0  0.558 0.442 0 0 0  0 0.242 0.758 0 0  0 0 0 1 0',
  deutan: '0.625 0.375 0 0 0  0.7 0.3 0 0 0  0 0.3 0.7 0 0  0 0 0 1 0',
  tritan: '0.95 0.05 0 0 0  0 0.433 0.567 0 0  0 0.475 0.525 0 0  0 0 0 1 0',
};

/** A small live board: seven balls, a selected one, a spawn marker and a reachable cell, drawn by the real styles. */
export function LookPreview({ lang, theme }: { lang: Language; theme: Theme }) {
  const cls = ballThemeClass(theme);
  const [vision, setVision] = useState<Vision>('normal');
  const filter = vision === 'normal' ? undefined : vision === 'grey' ? 'grayscale(1)' : `url(#vision-${vision})`;
  return (
    <div className="look-preview">
      <svg className="vision-defs" width="0" height="0" aria-hidden="true" focusable="false">
        {(Object.keys(MATRICES) as (keyof typeof MATRICES)[]).map((id) => (
          <filter key={id} id={`vision-${id}`} colorInterpolationFilters="sRGB">
            <feColorMatrix type="matrix" values={MATRICES[id]} />
          </filter>
        ))}
      </svg>
      <div className="board-grid look-preview-grid" role="img" aria-label={translate(lang, 'look.preview')} style={{ filter }} data-vision={vision}>
        {BALL_COLORS.map((color) => (
          <div key={color} className="board-cell look-cell">
            <div className={`ball ${cls} color-${color}`} />
          </div>
        ))}
        <div className="board-cell look-cell" />
        <div className="board-cell look-cell reachable" />
        <div className="board-cell look-cell">
          <div className={`ball ball-preview ${cls} color-green`} />
        </div>
        <div className="board-cell look-cell selected">
          <div className={`ball selected-ball ${cls} color-yellow`} />
        </div>
        <div className="board-cell look-cell" />
        <div className="board-cell look-cell" />
        <div className="board-cell look-cell" />
      </div>
      <div className="chips vision-chips" role="group" aria-label={translate(lang, 'look.vision')}>
        {VISIONS.map((id) => (
          <button type="button" key={id} className={`chip chip-small ${vision === id ? 'active' : ''}`} aria-pressed={vision === id} onClick={() => setVision(id)}>
            {translate(lang, `look.vision.${id}` as MessageKey)}
          </button>
        ))}
      </div>
    </div>
  );
}
