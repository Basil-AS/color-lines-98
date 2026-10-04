import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import { findings, mindReport } from '../cognition';
import type { Bucket, Finding } from '../cognition';
import type { GameRecord } from '../stats';

const NEEDED = 8;

const tr =
  (lang: Language) =>
  (key: MessageKey, params?: Record<string, string | number>) =>
    translate(lang, key, params);

/** Bars that stand against a baseline of 100 (a typical game): taller is better, empty groups stay empty. */
function IndexBars({ buckets, labels, label }: { buckets: Bucket[]; labels: string[]; label: string }) {
  const max = Math.max(100, ...buckets.map((b) => b.index));
  return (
    <div className="bars" role="img" aria-label={`${label}: ${labels.map((l, i) => `${l} ${buckets[i].games > 0 ? buckets[i].index : '–'}`).join(', ')}`}>
      {buckets.map((b, i) => (
        <div className="bar-col" key={i}>
          <div className={`bar ${b.games === 0 ? 'bar-none' : b.index >= 100 ? '' : 'bar-low'}`} style={{ height: `${Math.max(b.games > 0 ? 8 : 2, (b.index / max) * 100)}%` }} title={`${labels[i]}: ${b.games > 0 ? b.index : '–'}`} />
          <span className="bar-label" aria-hidden="true">
            {labels[i]}
          </span>
        </div>
      ))}
    </div>
  );
}

function findingText(lang: Language, f: Finding): string {
  const t = tr(lang);
  const p = { ...f.params };
  if (f.id === 'chronotype') return t(`mind.chrono.${f.params.type}` as MessageKey);
  if (f.id === 'weekday') p.day = new Intl.DateTimeFormat(lang, { weekday: 'long' }).format(new Date(2024, 0, 1 + Number(f.params.day)));
  if (f.id === 'window' || f.id === 'tempoFast' || f.id === 'tempoSlow' || f.id === 'tempoMid' || f.id === 'weekday') p.index = `+${p.index}`;
  return t(`mind.f.${f.id}` as MessageKey, p);
}

/** How the player thinks and when they play well, from the play data of every game. */
export function MindPanel({ lang, history }: { lang: Language; history: readonly GameRecord[] }) {
  const t = tr(lang);
  const report = mindReport(history);
  const list = findings(report);
  const sec = (ms: number) => t('mind.seconds', { n: (ms / 1000).toFixed(1) });
  const pct = (v: number) => `${Math.round(v * 100)}%`;
  const weekday = (d: number) => new Intl.DateTimeFormat(lang, { weekday: 'short' }).format(new Date(2024, 0, 1 + d));

  if (report.games < NEEDED) {
    return (
      <div className="long-panel">
        <p className="modal-text">{t('mind.intro')}</p>
        <p className="modal-text">{t('mind.need', { n: NEEDED - report.games })}</p>
      </div>
    );
  }

  const cards: [string, string][] = [
    [t('mind.avgDecision'), sec(report.avgDecisionMs)],
    [t('mind.fast'), pct(report.fastShare)],
    [t('mind.slow'), pct(report.slowShare)],
    [t('mind.planning'), pct(report.planning)],
    [t('mind.variation'), report.variation.toFixed(2)],
    [t('mind.tightest'), String(report.tightest)],
    [t('mind.danger'), String(report.dangerPer100)],
    [t('mind.undos'), String(report.undosPer100)],
    [t('mind.hints'), String(report.hintsPer100)],
    [t('mind.misses'), String(report.missesPer100)],
    [t('mind.clearing'), pct(report.clearingShare)],
  ];
  const phases = [report.phases.early, report.phases.mid, report.phases.late];
  const maxPhase = Math.max(...phases, 1);
  const hourBuckets: Bucket[] = report.byHour.map((h) => ({ games: h.games, index: h.index }));

  return (
    <div className="long-panel">
      <p className="modal-text">{t('mind.intro')}</p>
      <p className="modal-text">{t('mind.sample', { games: report.games, moves: report.decisions })}</p>

      {list.length > 0 && (
        <>
          <h3 className="stats-subtitle">{t('mind.findings')}</h3>
          <ul className="findings">
            {list.map((f) => (
              <li key={f.id}>{findingText(lang, f)}</li>
            ))}
          </ul>
        </>
      )}

      <h3 className="stats-subtitle">{t('mind.cards')}</h3>
      <dl className="stats-grid">
        {cards.map(([label, value]) => (
          <div className="stats-card" key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>

      <h3 className="stats-subtitle">{t('mind.hours')}</h3>
      <p className="modal-text">{t('mind.hoursHint')}</p>
      <IndexBars buckets={hourBuckets} labels={report.byHour.map((h) => (h.hour % 3 === 0 ? String(h.hour) : ''))} label={t('mind.hours')} />

      <h3 className="stats-subtitle">{t('mind.weekdays')}</h3>
      <IndexBars buckets={report.byWeekday} labels={report.byWeekday.map((_, d) => weekday(d))} label={t('mind.weekdays')} />

      <h3 className="stats-subtitle">{t('mind.sittings')}</h3>
      <p className="modal-text">{t('mind.sittingsHint')}</p>
      <IndexBars buckets={report.sittings} labels={[t('mind.sitting1'), t('mind.sitting2'), t('mind.sitting3'), t('mind.sitting4')]} label={t('mind.sittings')} />

      {report.tempo && (
        <>
          <h3 className="stats-subtitle">{t('mind.tempo')}</h3>
          <p className="modal-text">{t('mind.tempoHint')}</p>
          <IndexBars buckets={[report.tempo.fast, report.tempo.mid, report.tempo.slow]} labels={[t('mind.tempoFast'), t('mind.tempoMid'), t('mind.tempoSlow')]} label={t('mind.tempo')} />
        </>
      )}

      <h3 className="stats-subtitle">{t('mind.phases')}</h3>
      <div className="bars" role="img" aria-label={`${t('mind.phases')}: ${phases.map((p, i) => `${t(`mind.phase${i + 1}` as MessageKey)} ${sec(p)}`).join(', ')}`}>
        {phases.map((p, i) => (
          <div className="bar-col" key={i}>
            <div className="bar" style={{ height: `${Math.max(p > 0 ? 8 : 2, (p / maxPhase) * 100)}%` }} title={sec(p)} />
            <span className="bar-label" aria-hidden="true">
              {['1–20', '21–60', '61+'][i]}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
