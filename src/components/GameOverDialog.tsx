import { Dialog } from './Dialog';
import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';

interface GameOverDialogProps {
  lang: Language;
  score: number;
  best: number;
  newRecord: boolean;
  xpGained: number;
  /** New level reached with this game, if any. */
  levelUp: number | null;
  /** Ids of the achievements unlocked with this game. */
  unlocked: string[];
  onPlayAgain: () => void;
}

export function GameOverDialog(props: GameOverDialogProps) {
  const { lang } = props;
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  return (
    <Dialog titleId="game-over-title" title={t('gameover.title')} alert>
      {props.newRecord && <p className="record-banner">★ {t('gameover.newRecord')}</p>}
      <dl className="stats-grid stats-grid-2">
        <div className="stats-card">
          <dt>{t('gameover.final')}</dt>
          <dd>{props.score}</dd>
        </div>
        <div className="stats-card">
          <dt>{t('gameover.best')}</dt>
          <dd>{props.best}</dd>
        </div>
      </dl>
      {props.xpGained > 0 && <p className="xp-line">{t('gameover.xp', { xp: props.xpGained })}</p>}
      {props.levelUp !== null && <p className="record-banner">{t('gameover.levelUp', { level: props.levelUp })}</p>}
      {props.unlocked.length > 0 && (
        <div className="unlocked-box">
          <h3 className="stats-subtitle">{t('gameover.unlocked')}</h3>
          <ul className="ach-list">
            {props.unlocked.map((id) => (
              <li key={id} className="ach-item unlocked">
                <span className="ach-mark" aria-hidden="true">★</span>
                <span className="ach-text">
                  <strong>{t(`ach.${id}.name` as MessageKey)}</strong>
                  <span>{t(`ach.${id}.desc` as MessageKey)}</span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="modal-actions">
        <button type="button" className="modal-btn" onClick={props.onPlayAgain} data-autofocus>
          {t('gameover.playAgain')}
        </button>
      </div>
    </Dialog>
  );
}
