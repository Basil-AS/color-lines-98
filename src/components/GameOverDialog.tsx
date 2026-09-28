import { Dialog } from './Dialog';
import { translate } from '../i18n';
import type { Language } from '../i18n';

interface GameOverDialogProps {
  lang: Language;
  score: number;
  best: number;
  newRecord: boolean;
  onPlayAgain: () => void;
}

export function GameOverDialog({ lang, score, best, newRecord, onPlayAgain }: GameOverDialogProps) {
  const t = (key: Parameters<typeof translate>[1]) => translate(lang, key);
  return (
    <Dialog titleId="game-over-title" title={t('gameover.title')} alert>
      {newRecord && <p className="record-banner">★ {t('gameover.newRecord')}</p>}
      <dl className="stats-grid stats-grid-2">
        <div className="stats-card">
          <dt>{t('gameover.final')}</dt>
          <dd>{score}</dd>
        </div>
        <div className="stats-card">
          <dt>{t('gameover.best')}</dt>
          <dd>{best}</dd>
        </div>
      </dl>
      <div className="modal-actions">
        <button type="button" className="modal-btn" onClick={onPlayAgain} data-autofocus>
          {t('gameover.playAgain')}
        </button>
      </div>
    </Dialog>
  );
}
