import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import { GOAL_BONUS_XP } from '../goals';
import type { GoalProgress } from '../goals';

interface GoalsPanelProps {
  lang: Language;
  goals: readonly GoalProgress[];
  goalStreak: number;
  /** Best score of today's daily challenge, or null when it has not been played. */
  dailyBest: number | null;
  onPlayDaily?: () => void;
}

const fmt = (value: number, type: string) => (type === 'efficiency' ? value.toFixed(1) : String(Math.round(value)));

export function GoalsPanel({ lang, goals, goalStreak, dailyBest, onPlayDaily }: GoalsPanelProps) {
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  const allDone = goals.length > 0 && goals.every((g) => g.done);

  return (
    <section className="goals" aria-label={t('goals.title')}>
      <h3 className="stats-subtitle">{t('goals.title')}</h3>
      <ul className="goal-list">
        {goals.map(({ goal, value, done }) => (
          <li key={goal.id} className={`goal-item ${done ? 'done' : ''}`}>
            <div className="goal-head">
              <span className="goal-mark" aria-hidden="true">
                {done ? '✔' : '○'}
              </span>
              <span className="goal-text">{t(`goals.${goal.type}` as MessageKey, { target: fmt(goal.target, goal.type) })}</span>
            </div>
            <div
              className="level-bar"
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={goal.target}
              aria-valuenow={Math.min(value, goal.target)}
              aria-label={t('goals.progress', { value: fmt(value, goal.type), target: fmt(goal.target, goal.type) })}
            >
              <div className="level-bar-fill" style={{ width: `${Math.min(100, (value / goal.target) * 100)}%` }} />
            </div>
            <span className="goal-value">{done ? t('goals.done') : t('goals.progress', { value: fmt(value, goal.type), target: fmt(goal.target, goal.type) })}</span>
          </li>
        ))}
      </ul>
      <p className="goal-note">{allDone ? t('goals.allDone') : t('goals.hint')}</p>
      <p className="goal-note">{t('goals.bonus', { xp: GOAL_BONUS_XP })} · {t('goals.streak')}: {goalStreak}</p>
      <p className="goal-note">
        {dailyBest === null ? t('goals.dailyNone') : t('goals.dailyBest', { score: dailyBest })}
      </p>
      {onPlayDaily && (
        <button type="button" className="modal-btn modal-btn-secondary" onClick={onPlayDaily}>
          {t('goals.playDaily')}
        </button>
      )}
    </section>
  );
}
