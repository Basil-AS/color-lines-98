import { formatDay, formatDuration, formatMonth } from '../format';
import { translate, translatePlural } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import { heatmap, milestones, monthly, records, season, streaks, totals } from '../career';
import type { Ledger } from '../ledger';
import { dayKey } from '../progress';
import type { GameRecord } from '../stats';

interface PanelProps {
  lang: Language;
  ledger: Ledger;
  history: readonly GameRecord[];
  now: number;
}

const HEAT_WEEKS = 26;

const tr =
  (lang: Language) =>
  (key: MessageKey, params?: Record<string, string | number>) =>
    translate(lang, key, params);

/** Lifetime totals, the activity heatmap, endless milestones and the month by month table. */
export function CareerPanel({ lang, ledger, now }: Omit<PanelProps, 'history'>) {
  const t = tr(lang);
  const n = (v: number) => v.toLocaleString(lang);
  const sum = totals(ledger);
  const run = streaks(ledger, dayKey(now));
  const grid = heatmap(ledger, now, HEAT_WEEKS);
  const months = monthly(ledger).slice(-12).reverse();

  const cards: [string, string][] = [
    [t('stats.gamesPlayed'), n(sum.games)],
    [t('stats.totalScore'), n(sum.score)],
    [t('stats.lines'), n(sum.lines)],
    [t('career.activeDays'), n(sum.activeDays)],
    [t('stats.streak'), translatePlural(lang, 'plural.days', run.current)],
    [t('stats.bestStreak'), translatePlural(lang, 'plural.days', run.longest)],
    [t('stats.playTime'), formatDuration(lang, sum.playMs)],
  ];

  return (
    <div className="long-panel">
      <p className="modal-text">{t('career.intro')}</p>
      <dl className="stats-grid">
        {cards.map(([label, value]) => (
          <div className="stats-card" key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>

      <h3 className="stats-subtitle">{t('career.heatmap')}</h3>
      <div className="heatmap" role="group" aria-label={t('career.heatmap')}>
        {grid.map((col, w) => (
          <div className="heat-col" key={w}>
            {col.map((cell, r) =>
              cell === null ? (
                <span className="heat-cell heat-empty" key={r} aria-hidden="true" />
              ) : (
                <span
                  key={r}
                  className={`heat-cell heat-${cell.level}`}
                  title={t('career.heatDay', { day: formatDay(lang, cell.day), games: cell.games })}
                  role="img"
                  aria-label={t('career.heatDay', { day: formatDay(lang, cell.day), games: cell.games })}
                />
              )
            )}
          </div>
        ))}
      </div>
      <div className="heat-legend" aria-hidden="true">
        <span>{t('career.less')}</span>
        {[0, 1, 2, 3, 4].map((l) => (
          <span key={l} className={`heat-cell heat-${l}`} />
        ))}
        <span>{t('career.more')}</span>
      </div>

      <h3 className="stats-subtitle">{t('career.milestones')}</h3>
      <ul className="milestones">
        {milestones(sum).map((m) => (
          <li key={m.id}>
            <div className="milestone-head">
              <strong>{t(`career.ms.${m.id}` as MessageKey)}</strong>
              <span>{t('career.ms.progress', { value: n(m.value), next: n(m.ladder.next), reached: m.ladder.reached })}</span>
            </div>
            <div
              className="level-bar"
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(m.ladder.fraction * 100)}
              aria-label={t(`career.ms.${m.id}` as MessageKey)}
            >
              <div className="level-bar-fill" style={{ width: `${Math.round(m.ladder.fraction * 100)}%` }} />
            </div>
          </li>
        ))}
      </ul>

      <h3 className="stats-subtitle">{t('career.months')}</h3>
      {months.length === 0 ? (
        <p className="modal-text">{t('stats.empty')}</p>
      ) : (
        <ul className="mode-rows">
          {months.map((m) => (
            <li key={m.month}>
              <strong>{formatMonth(lang, m.month)}</strong>
              <span>{t('career.monthRow', { games: m.games, best: n(m.best), average: n(m.average) })}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** The current monthly season and the ones before it. */
export function SeasonsPanel({ lang, ledger, now }: Omit<PanelProps, 'history'>) {
  const t = tr(lang);
  const n = (v: number) => v.toLocaleString(lang);
  const s = season(ledger, now);
  const tier = (id: string) => t(`season.tier.${id}` as MessageKey);
  return (
    <div className="long-panel">
      <section className="level-card" aria-label={t('season.title', { month: formatMonth(lang, s.month) })}>
        <div className="level-head">
          <span className={`level-badge tier-${s.tier}`}>{tier(s.tier)}</span>
          <span className="level-title">{t('season.title', { month: formatMonth(lang, s.month) })}</span>
        </div>
        <span className="level-xp">{t('season.points', { points: n(s.points), games: s.games })}</span>
        <span className="level-xp">
          {s.nextTier && s.toNext !== null ? t('season.toNext', { n: n(s.toNext), tier: tier(s.nextTier) }) : t('season.top')}
        </span>
        <span className="level-xp">{t('season.daysLeft', { n: s.daysLeft })}</span>
      </section>
      <p className="modal-text">{t('season.baseline', { baseline: n(s.baseline) })}</p>

      <h3 className="stats-subtitle">{t('season.past')}</h3>
      {s.past.length === 0 ? (
        <p className="modal-text">{t('season.none')}</p>
      ) : (
        <ul className="mode-rows">
          {s.past.map((p) => (
            <li key={p.month}>
              <strong>{formatMonth(lang, p.month)}</strong>
              <span>
                <span className={`tier-chip tier-${p.tier}`}>{tier(p.tier)}</span> {n(p.points)}
              </span>
            </li>
          ))}
        </ul>
      )}
      {s.bestMonth && <p className="modal-text">{t('season.best', { month: formatMonth(lang, s.bestMonth.month), points: n(s.bestMonth.score) })}</p>}
    </div>
  );
}

export function RecordsPanel({ lang, ledger, history }: Omit<PanelProps, 'now'>) {
  const t = tr(lang);
  const n = (v: number) => v.toLocaleString(lang);
  const r = records(history, ledger);
  const rows: [string, string][] = [];
  if (r.bestScore) rows.push([t('rec.bestScore'), `${n(r.bestScore.score)} · ${formatDay(lang, dayKey(r.bestScore.endedAt))}`]);
  if (r.bestLine > 0) rows.push([t('rec.bestLine'), String(r.bestLine)]);
  if (r.mostLines) rows.push([t('rec.mostLines'), n(r.mostLines.lines)]);
  if (r.bestEfficiency) rows.push([t('rec.bestEfficiency'), r.bestEfficiency.value.toFixed(1)]);
  if (r.longestGame) rows.push([t('rec.longestGame'), formatDuration(lang, r.longestGame.durationMs)]);
  if (r.bestDay && r.bestDay.score > 0) rows.push([t('rec.bestDay'), `${n(r.bestDay.score)} · ${formatDay(lang, r.bestDay.day)}`]);
  if (r.bestDayGames) rows.push([t('rec.busiestDay'), `${r.bestDayGames.games} · ${formatDay(lang, r.bestDayGames.day)}`]);
  return (
    <div className="long-panel">
      <h3 className="stats-subtitle">{t('rec.title')}</h3>
      {rows.length === 0 ? (
        <p className="modal-text">{t('rec.none')}</p>
      ) : (
        <ul className="mode-rows">
          {rows.map(([label, value]) => (
            <li key={label}>
              <strong>{label}</strong>
              <span>{value}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
