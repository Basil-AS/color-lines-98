import { useState } from 'react';
import { Dialog } from './Dialog';
import { formatDate, translate, translatePlural } from '../i18n';
import type { Language } from '../i18n';
import { summarize } from '../stats';
import type { GameRecord } from '../stats';

interface StatsDialogProps {
  lang: Language;
  history: readonly GameRecord[];
  onClear: () => void;
  onClose: () => void;
}

const RECENT_LIMIT = 10;

export function StatsDialog({ lang, history, onClear, onClose }: StatsDialogProps) {
  const t = (key: Parameters<typeof translate>[1]) => translate(lang, key);
  const [confirming, setConfirming] = useState(false);
  const summary = summarize(history);
  const recent = history.slice(0, RECENT_LIMIT);

  const cards: [string, string | number][] = [
    [t('stats.gamesPlayed'), summary.gamesPlayed],
    [t('stats.best'), summary.bestScore],
    [t('stats.average'), summary.averageScore],
    [t('stats.lines'), summary.totalLines],
  ];

  return (
    <Dialog titleId="stats-title" title={t('stats.title')} onClose={onClose}>
      <dl className="stats-grid">
        {cards.map(([label, value]) => (
          <div className="stats-card" key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>

      <h3 className="stats-subtitle">{t('stats.recent')}</h3>
      {recent.length === 0 ? (
        <p className="modal-text">{t('stats.empty')}</p>
      ) : (
        <ol className="stats-list">
          {recent.map((game) => (
            <li className="stats-row" key={game.endedAt + ':' + game.score}>
              <span className="stats-row-score">
                {game.score}
                {game === summary.bestRecord && (
                  <span className="stats-badge">{t('stats.bestMark')}</span>
                )}
                {!game.completed && (
                  <span className="stats-badge stats-badge-muted">{t('stats.unfinished')}</span>
                )}
              </span>
              <span className="stats-row-meta">
                {formatDate(lang, game.endedAt)} · {translatePlural(lang, 'plural.moves', game.moves)}
              </span>
            </li>
          ))}
        </ol>
      )}

      <div className="modal-actions">
        {history.length > 0 &&
          (confirming ? (
            <div className="confirm-row" role="group" aria-label={t('stats.confirmClear')}>
              <span>{t('stats.confirmClear')}</span>
              <button
                type="button"
                className="modal-btn modal-btn-danger"
                onClick={() => {
                  onClear();
                  setConfirming(false);
                }}
              >
                {t('stats.confirmYes')}
              </button>
              <button
                type="button"
                className="modal-btn modal-btn-secondary"
                onClick={() => setConfirming(false)}
                autoFocus
              >
                {t('stats.confirmNo')}
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="modal-btn modal-btn-secondary"
              onClick={() => setConfirming(true)}
            >
              {t('stats.clear')}
            </button>
          ))}
        <button type="button" className="modal-btn" onClick={onClose} data-autofocus>
          {t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
