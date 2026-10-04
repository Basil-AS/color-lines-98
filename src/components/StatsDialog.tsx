import { useState } from 'react';
import { Dialog } from './Dialog';
import { formatDuration } from '../format';
import { formatDate, translate, translatePlural } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import {
  ACHIEVEMENTS,
  LEVEL_TITLES,
  bestStreak,
  currentStreak,
  dayKey,
  levelInfo,
  scoreTrend,
  titleIndex,
  xpOf,
} from '../progress';
import type { Progress } from '../progress';
import { InsightsPanel } from './InsightsPanel';
import { CareerPanel, RecordsPanel, SeasonsPanel } from './LongTermPanels';
import { DataPanel } from './DataPanel';
import { MindPanel } from './MindPanel';
import type { Backup } from '../backup';
import type { Ledger } from '../ledger';
import { MODE_IDS } from '../engine/modes';
import { summarize } from '../stats';
import type { GameRecord } from '../stats';

interface StatsDialogProps {
  lang: Language;
  history: readonly GameRecord[];
  progress: Progress;
  ledger: Ledger;
  now: number;
  onExport: (kind: 'json' | 'csv') => void;
  onImport: (backup: Backup, mode: 'merge' | 'replace') => void;
  onClear: () => void;
  onClose: () => void;
}

const RECENT_LIMIT = 10;
const TREND_POINTS = 20;

function Sparkline({ values, label }: { values: number[]; label: string }) {
  if (values.length < 2) return null;
  const w = 240;
  const h = 56;
  const max = Math.max(...values, 1);
  const step = w / (values.length - 1);
  const points = values.map((v, i) => `${(i * step).toFixed(1)},${(h - 4 - (v / max) * (h - 8)).toFixed(1)}`);
  return (
    <svg className="sparkline" viewBox={`0 0 ${w} ${h}`} role="img" aria-label={`${label}: ${values.join(', ')}`}>
      <polyline points={points.join(' ')} fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      {values.map((v, i) => (
        <circle key={i} cx={i * step} cy={h - 4 - (v / max) * (h - 8)} r="2.5" fill="currentColor" />
      ))}
    </svg>
  );
}

const TABS = ['overview', 'career', 'mind', 'seasons', 'records', 'data'] as const;
type Tab = (typeof TABS)[number];

export function StatsDialog({ lang, history, progress, ledger, now, onExport, onImport, onClear, onClose }: StatsDialogProps) {
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  const [confirming, setConfirming] = useState(false);
  const [understood, setUnderstood] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const [tab, setTab] = useState<Tab>('overview');

  const summary = summarize(history);
  const level = levelInfo(xpOf(progress));
  const title = LEVEL_TITLES[titleIndex(level.level)];
  const streak = currentStreak(progress.days, dayKey(now));
  const unlockedCount = ACHIEVEMENTS.filter((a) => progress.achievements[a.id] !== undefined).length;
  const shown = showAll ? history : history.slice(0, RECENT_LIMIT);
  const bestIndex = summary.bestRecord ? history.indexOf(summary.bestRecord) : -1;

  const cards: [string, string | number][] = [
    [t('stats.gamesPlayed'), progress.totalGames],
    [t('stats.best'), progress.bestScore],
    [t('stats.average'), summary.averageScore],
    [t('stats.totalScore'), progress.totalScore],
    [t('stats.lines'), progress.totalLines],
    [t('stats.bestLine'), progress.bestLine],
    [t('stats.streak'), translatePlural(lang, 'plural.days', streak)],
    [t('stats.bestStreak'), translatePlural(lang, 'plural.days', bestStreak(progress.days))],
    [t('stats.playTime'), formatDuration(lang, progress.totalPlayMs)],
    [t('stats.longestGame'), translatePlural(lang, 'plural.moves', progress.longestGameMoves)],
  ];

  return (
    <Dialog titleId="stats-title" title={t('stats.title')} onClose={onClose}>
      <div className="tabs" role="tablist" aria-label={t('stats.title')}>
        {TABS.map((id) => (
          <button
            key={id}
            type="button"
            role="tab"
            id={`stats-tab-${id}`}
            aria-selected={tab === id}
            aria-controls="stats-panel"
            tabIndex={tab === id ? 0 : -1}
            className={`tab-btn ${tab === id ? 'active' : ''}`}
            onClick={() => setTab(id)}
            onKeyDown={(e) => {
              const at = TABS.indexOf(tab);
              const next = e.key === 'ArrowRight' ? at + 1 : e.key === 'ArrowLeft' ? at - 1 : null;
              if (next === null) return;
              e.preventDefault();
              const target = TABS[(next + TABS.length) % TABS.length];
              setTab(target);
              document.getElementById(`stats-tab-${target}`)?.focus();
            }}
          >
            {t(`stats.tab.${id}` as MessageKey)}
          </button>
        ))}
      </div>
      <div id="stats-panel" role="tabpanel" aria-labelledby={`stats-tab-${tab}`}>
      {tab === 'career' && <CareerPanel lang={lang} ledger={ledger} now={now} />}
      {tab === 'mind' && <MindPanel lang={lang} history={history} />}
      {tab === 'seasons' && <SeasonsPanel lang={lang} ledger={ledger} now={now} />}
      {tab === 'records' && <RecordsPanel lang={lang} ledger={ledger} history={history} />}
      {tab === 'data' && <DataPanel lang={lang} games={progress.totalGames} onExport={onExport} onImport={onImport} />}
      {tab === 'overview' && (
      <>
      <section className="level-card" aria-label={t('level.label', { level: level.level })}>
        <div className="level-head">
          <span className="level-badge">{t('level.label', { level: level.level })}</span>
          <span className="level-title">{t(`level.title.${title}` as MessageKey)}</span>
        </div>
        <div
          className="level-bar"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={level.needed}
          aria-valuenow={level.into}
          aria-label={t('level.xp', { into: level.into, needed: level.needed })}
        >
          <div className="level-bar-fill" style={{ width: `${Math.round(level.fraction * 100)}%` }} />
        </div>
        <span className="level-xp">{t('level.xp', { into: level.into, needed: level.needed })}</span>
      </section>

      <dl className="stats-grid">
        {cards.map(([label, value]) => (
          <div className="stats-card" key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>

      <InsightsPanel lang={lang} history={history} progress={progress} now={now} />

      <h3 className="stats-subtitle">{t('stats.byMode')}</h3>
      <ul className="mode-rows">
        {MODE_IDS.map((id) => (
          <li key={id}>
            <strong>{t(`mode.${id}` as MessageKey)}</strong>
            <span>{t('stats.modeRow', { games: progress.gamesByMode[id], best: progress.bestByMode[id] })}</span>
          </li>
        ))}
      </ul>

      <h3 className="stats-subtitle">{t('stats.trend')}</h3>
      {history.length < 2 ? (
        <p className="modal-text">{t('stats.empty')}</p>
      ) : (
        <Sparkline values={scoreTrend(history, TREND_POINTS)} label={t('stats.trend')} />
      )}

      <h3 className="stats-subtitle">
        {t('stats.achievements')} · {t('stats.achievementsCount', { unlocked: unlockedCount, total: ACHIEVEMENTS.length })}
      </h3>
      <ul className="ach-list">
        {ACHIEVEMENTS.map((a) => {
          const when = progress.achievements[a.id];
          return (
            <li key={a.id} className={`ach-item ${when === undefined ? 'locked' : 'unlocked'}`}>
              <span className="ach-mark" aria-hidden="true">
                {when === undefined ? '🔒' : '★'}
              </span>
              <span className="ach-text">
                <strong>{t(`ach.${a.id}.name` as MessageKey)}</strong>
                <span>{t(`ach.${a.id}.desc` as MessageKey)}</span>
              </span>
              <span className="ach-when">
                {when === undefined ? t('stats.locked') : formatDate(lang, when).split(',')[0]}
              </span>
            </li>
          );
        })}
      </ul>

      <h3 className="stats-subtitle">{t('stats.recent')}</h3>
      {history.length === 0 ? (
        <p className="modal-text">{t('stats.empty')}</p>
      ) : (
        <ol className="stats-list">
          {shown.map((game, i) => (
            <li className="stats-row" key={game.endedAt + ':' + i}>
              <span className="stats-row-score">
                {game.score}
                {i === bestIndex && <span className="stats-badge">{t('stats.bestMark')}</span>}
                {!game.completed && <span className="stats-badge stats-badge-muted">{t('stats.unfinished')}</span>}
              </span>
              <span className="stats-row-meta">
                {formatDate(lang, game.endedAt)} · {translatePlural(lang, 'plural.moves', game.moves)} ·{' '}
                {formatDuration(lang, game.durationMs)}
              </span>
            </li>
          ))}
        </ol>
      )}
      {history.length > RECENT_LIMIT && (
        <button type="button" className="link-btn" onClick={() => setShowAll(!showAll)}>
          {showAll ? t('stats.showLess') : t('stats.showAll', { n: history.length })}
        </button>
      )}
      </>
      )}
      </div>

      <div className="modal-actions">
        {tab === 'overview' && (history.length > 0 || progress.totalGames > 0) &&
          (confirming ? (
            <div className="confirm-box" role="alertdialog" aria-labelledby="clear-warning" aria-describedby="clear-warning">
              <p id="clear-warning" className="warn-box">
                {t('stats.confirmClear', { games: history.length, level: level.level })}
              </p>
              <label className="confirm-check">
                <input type="checkbox" checked={understood} onChange={(e) => setUnderstood(e.target.checked)} />
                <span>{t('stats.confirmCheck')}</span>
              </label>
              <div className="modal-actions">
                <button
                  type="button"
                  className="modal-btn modal-btn-danger"
                  disabled={!understood}
                  onClick={() => {
                    onClear();
                    setConfirming(false);
                    setUnderstood(false);
                  }}
                >
                  {t('stats.confirmYes')}
                </button>
                <button
                  type="button"
                  className="modal-btn modal-btn-secondary"
                  onClick={() => {
                    setConfirming(false);
                    setUnderstood(false);
                  }}
                  autoFocus
                >
                  {t('stats.confirmNo')}
                </button>
              </div>
            </div>
          ) : (
            <button type="button" className="modal-btn modal-btn-secondary" onClick={() => setConfirming(true)}>
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
