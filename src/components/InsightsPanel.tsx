import { formatDate, translate, translatePlural } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import {
  dailyActivity,
  efficiency,
  levelEta,
  recordProgression,
  scoreHistogram,
  trendOf,
  weekdayActivity,
} from '../insights';
import { levelInfo, levelThreshold, xpOf } from '../progress';
import type { Progress } from '../progress';
import type { GameRecord } from '../stats';

interface InsightsPanelProps {
  lang: Language;
  history: readonly GameRecord[];
  progress: Progress;
  now: number;
}

const TREND_WINDOW = 10;

function Bars({ values, labels, label }: { values: number[]; labels: string[]; label: string }) {
  const max = Math.max(...values, 1);
  return (
    <div className="bars" role="img" aria-label={`${label}: ${labels.map((l, i) => `${l} ${values[i]}`).join(', ')}`}>
      {values.map((v, i) => (
        <div className="bar-col" key={i}>
          <div className="bar" style={{ height: `${Math.max(v > 0 ? 8 : 2, (v / max) * 100)}%` }} />
          <span className="bar-label" aria-hidden="true">
            {labels[i]}
          </span>
        </div>
      ))}
    </div>
  );
}

export function InsightsPanel({ lang, history, progress, now }: InsightsPanelProps) {
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  if (history.length === 0) return null;

  const trend = trendOf(history, TREND_WINDOW);
  const eff = efficiency(history);
  const level = levelInfo(xpOf(progress));
  const eta = levelEta(history, levelThreshold(level.level + 1) - level.xp);
  const days = dailyActivity(history, 14, now);
  const hist = scoreHistogram(history, 50);
  const weekdays = weekdayActivity(history);
  const progression = recordProgression(history).slice(-5);

  const weekdayName = (weekday: number) =>
    new Intl.DateTimeFormat(lang, { weekday: 'short' }).format(new Date(2024, 0, 1 + weekday)); // 2024-01-01 is a Monday
  const dayLabel = (day: string) => day.slice(8);

  return (
    <section className="insights" aria-label={t('insights.title')}>
      <h3 className="stats-subtitle">{t('insights.title')}</h3>

      <p className={`insight-trend trend-${trend?.direction ?? 'none'}`}>
        {trend
          ? t(`insights.trend.${trend.direction}` as MessageKey, {
              n: TREND_WINDOW,
              recent: trend.recentAverage,
              percent: Math.abs(trend.changePercent),
            })
          : t('insights.trend.needMore', { n: Math.max(1, TREND_WINDOW * 2 - history.length) })}
      </p>

      <p className="insight-line">
        {t('insights.efficiency', { average: eff.average.toFixed(1), best: eff.best.toFixed(1) })}
      </p>
      {eta !== null && (
        <p className="insight-line">
          {t('insights.eta', { games: translatePlural(lang, 'plural.games', eta), level: level.level + 1 })}
        </p>
      )}

      <h4 className="insight-heading">{t('insights.daily')}</h4>
      <Bars values={days.map((d) => d.games)} labels={days.map((d) => dayLabel(d.day))} label={t('insights.daily')} />

      <h4 className="insight-heading">{t('insights.histogram')}</h4>
      <Bars
        values={hist.map((b) => b.count)}
        labels={hist.map((b) => t('insights.bucketLabel', { from: b.from }))}
        label={t('insights.histogram')}
      />

      <h4 className="insight-heading">{t('insights.weekdays')}</h4>
      <Bars
        values={weekdays.map((w) => w.average)}
        labels={weekdays.map((w) => weekdayName(w.weekday))}
        label={t('insights.weekdays')}
      />

      {progression.length > 0 && (
        <>
          <h4 className="insight-heading">{t('insights.records')}</h4>
          <ol className="record-steps">
            {progression.map((r) => (
              <li key={r.at + ':' + r.score}>
                <strong>{r.score}</strong> <span>{formatDate(lang, r.at)}</span>
              </li>
            ))}
          </ol>
        </>
      )}
    </section>
  );
}
