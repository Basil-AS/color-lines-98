import { useState } from 'react';
import { Dialog } from './Dialog';
import { MODES, MODE_IDS } from '../engine/modes';
import { ColorDots } from './ColorDots';
import type { ModeId } from '../engine/modes';
import { translate, translatePlural } from '../i18n';
import type { Language, MessageKey } from '../i18n';

interface NewGameDialogProps {
  lang: Language;
  current: ModeId;
  /** The game that would be abandoned, when one is in progress. */
  inProgress: { score: number; moves: number } | null;
  /** Games played and best score in each mode. */
  stats: Record<ModeId, { games: number; best: number }>;
  onStart: (mode: ModeId) => void;
  onClose: () => void;
  initialPicking?: boolean;
}

/**
 * Starting over is the one action that throws a game away, so the dialog states plainly what happens to
 * the current game and puts the safe choice ("Keep playing") in focus whenever a game is in progress.
 */
export function NewGameDialog({ lang, current, inProgress, stats, onStart, onClose, initialPicking }: NewGameDialogProps) {
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  const [mode, setMode] = useState<ModeId>(current);
  const [picking, setPicking] = useState(() => initialPicking ?? !inProgress);

  return (
    <Dialog titleId="newgame-title" title={t('newgame.title')} onClose={onClose}>
      {inProgress && (
        <p className="warn-box" role="alert">
          {t('newgame.warning', {
            score: inProgress.score,
            moves: translatePlural(lang, 'plural.moves', inProgress.moves),
          })}
        </p>
      )}

      {inProgress && mode !== current && (
        <p className="modal-text" role="status">
          {t('newgame.switching', { from: t(`mode.${current}` as MessageKey), to: t(`mode.${mode}` as MessageKey) })}
        </p>
      )}

      {!picking ? (
        <div className="mode-folded">
          <div className="mode-folded-info">
            <span className="mode-folded-label">{t('mode.label')}</span>
            <div className="mode-folded-title">
              <strong>{t(`mode.${mode}` as MessageKey)}</strong> <ColorDots mode={mode} />
            </div>
            <p className="mode-folded-desc">{t(`mode.${mode}.desc` as MessageKey)}</p>
          </div>
          <button
            type="button"
            className="mode-fold-btn"
            onClick={() => setPicking(true)}
          >
            {t('newgame.changeMode')}
          </button>
        </div>
      ) : (
        <div className="mode-list" role="radiogroup" aria-label={t('mode.label')}>
          {MODE_IDS.map((id) => (
            <button
              type="button"
              key={id}
              role="radio"
              aria-checked={mode === id}
              className={`mode-card ${mode === id ? 'active' : ''}`}
              onClick={() => setMode(id)}
            >
              <strong>
                {t(`mode.${id}` as MessageKey)} <ColorDots mode={id} />
              </strong>
              <span>{t(`mode.${id}.desc` as MessageKey)}</span>
              <span className="mode-facts">
                {t('mode.facts', {
                  colors: t('mode.colors', { n: MODES[id].colors }),
                  time: MODES[id].timeLimitMs === null ? t('mode.noLimit') : t('mode.minutes', { n: MODES[id].timeLimitMs / 60000 }),
                })}
                {stats[id].games > 0 && ` · ${t('mode.bestIn', { best: stats[id].best, games: stats[id].games })}`}
              </span>
            </button>
          ))}
        </div>
      )}

      <div className="modal-actions">
        <button
          type="button"
          className={`modal-btn ${inProgress ? 'modal-btn-danger' : ''}`}
          onClick={() => onStart(mode)}
          data-autofocus={inProgress ? undefined : true}
        >
          {t('newgame.startMode', { mode: t(`mode.${mode}` as MessageKey) })}
        </button>
        <button
          type="button"
          className="modal-btn modal-btn-secondary"
          onClick={onClose}
          data-autofocus={inProgress ? true : undefined}
        >
          {inProgress ? t('newgame.keep') : t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
