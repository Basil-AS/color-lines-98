import { useState } from 'react';
import { Dialog } from './Dialog';
import { MODE_IDS } from '../engine/modes';
import type { ModeId } from '../engine/modes';
import { translate, translatePlural } from '../i18n';
import type { Language, MessageKey } from '../i18n';

interface NewGameDialogProps {
  lang: Language;
  current: ModeId;
  /** The game that would be abandoned, when one is in progress. */
  inProgress: { score: number; moves: number } | null;
  onStart: (mode: ModeId) => void;
  onClose: () => void;
}

/**
 * Starting over is the one action that throws a game away, so the dialog states plainly what happens to
 * the current game and puts the safe choice ("Keep playing") in focus whenever a game is in progress.
 */
export function NewGameDialog({ lang, current, inProgress, onStart, onClose }: NewGameDialogProps) {
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  const [mode, setMode] = useState<ModeId>(current);

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
            <strong>{t(`mode.${id}` as MessageKey)}</strong>
            <span>{t(`mode.${id}.desc` as MessageKey)}</span>
          </button>
        ))}
      </div>

      <div className="modal-actions">
        <button
          type="button"
          className={`modal-btn ${inProgress ? 'modal-btn-danger' : ''}`}
          onClick={() => onStart(mode)}
          data-autofocus={inProgress ? undefined : true}
        >
          {t('newgame.start')}
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
