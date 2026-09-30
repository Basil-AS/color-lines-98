import { Dialog } from './Dialog';
import { translate, translatePlural } from '../i18n';
import type { Language } from '../i18n';
import { LineDetector } from '../engine/linedetector';

interface HelpDialogProps {
  lang: Language;
  onClose: () => void;
}

const SCORED_LENGTHS = [5, 6, 7, 8, 9];

export function HelpDialog({ lang, onClose }: HelpDialogProps) {
  const t = (key: Parameters<typeof translate>[1], params?: Record<string, string | number>) =>
    translate(lang, key, params);

  return (
    <Dialog titleId="help-title" title={t('help.title')} onClose={onClose}>
      <div className="modal-text modal-text-left">
        <p>{t('help.objective')}</p>
        <p>{t('help.scoring')}</p>
        <ul className="help-scores">
          {SCORED_LENGTHS.map((n) => (
            <li key={n}>
              {t('help.scoringLine', {
                count: translatePlural(lang, 'plural.balls', n),
                points: LineDetector.calculateScore(n, 'gamos'),
              })}
            </li>
          ))}
        </ul>
        <p>{t('help.movement')}</p>
        <p>{t('help.freeTurn')}</p>
        <p>{t('help.keyboard')}</p>
        <h3 className="stats-subtitle">{t('help.modes')}</h3>
        <ul className="help-scores">
          {(['classic', 'easy', 'blitz', 'daily'] as const).map((id) => (
            <li key={id}>{t(`help.modes.${id}` as const)}</li>
          ))}
        </ul>
        <p>{t('help.hint')}</p>
        <p>{t('help.goals')}</p>
        <p>{t('help.switching')}</p>
      </div>
      <div className="modal-actions">
        <button type="button" className="modal-btn" onClick={onClose} data-autofocus>
          {t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
