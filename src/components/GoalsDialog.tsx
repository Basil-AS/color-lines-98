import { Dialog } from './Dialog';
import { GoalsPanel } from './GoalsPanel';
import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import type { GoalProgress } from '../goals';

interface GoalsDialogProps {
  lang: Language;
  goals: readonly GoalProgress[];
  goalStreak: number;
  dailyBest: number | null;
  onPlayDaily: () => void;
  onClose: () => void;
}

export function GoalsDialog({ lang, goals, goalStreak, dailyBest, onPlayDaily, onClose }: GoalsDialogProps) {
  const t = (key: MessageKey) => translate(lang, key);
  return (
    <Dialog titleId="goals-title" title={t('goals.title')} onClose={onClose}>
      <GoalsPanel lang={lang} goals={goals} goalStreak={goalStreak} dailyBest={dailyBest} onPlayDaily={onPlayDaily} />
      <div className="modal-actions">
        <button type="button" className="modal-btn" onClick={onClose} data-autofocus>
          {t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
