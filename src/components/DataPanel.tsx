import { useRef, useState } from 'react';
import { BACKUP_MAX_BYTES, describeBackup, parseBackup } from '../backup';
import type { Backup } from '../backup';
import { formatBytes } from '../format';
import { formatDate, translate } from '../i18n';
import { historyStorage } from '../storage';
import type { Language, MessageKey } from '../i18n';

interface DataPanelProps {
  lang: Language;
  games: number;
  onExport: (kind: 'json' | 'csv') => void;
  onImport: (backup: Backup, mode: 'merge' | 'replace') => void;
}

/** Saving results to a file and loading them back. Replacing data asks twice; adding data loses nothing. */
export function DataPanel({ lang, games, onExport, onImport }: DataPanelProps) {
  const t = (key: MessageKey, params?: Record<string, string | number>) => translate(lang, key, params);
  const input = useRef<HTMLInputElement>(null);
  const [backup, setBackup] = useState<Backup | null>(null);
  const [error, setError] = useState<MessageKey | null>(null);
  const [done, setDone] = useState<MessageKey | null>(null);
  const [replacing, setReplacing] = useState(false);
  const [understood, setUnderstood] = useState(false);

  const reset = () => {
    setBackup(null);
    setReplacing(false);
    setUnderstood(false);
  };

  const onFile = async (file: File | undefined) => {
    setError(null);
    setDone(null);
    reset();
    if (!file) return;
    // Checked before reading: a huge file must not be loaded into memory just to be refused.
    if (file.size > BACKUP_MAX_BYTES) {
      setError('data.error.tooBig');
      return;
    }
    let text: string;
    try {
      text = await file.text();
    } catch {
      setError('data.error.notJson');
      return;
    }
    const parsed = parseBackup(text);
    if (!parsed.ok) {
      setError(`data.error.${parsed.error}` as MessageKey);
      return;
    }
    setBackup(parsed.backup);
  };

  const storage = historyStorage();
  const info = backup ? describeBackup(backup) : null;
  const day = (ms: number) => formatDate(lang, ms).split(',')[0];

  return (
    <div className="long-panel">
      <h3 className="stats-subtitle">{t('data.title')}</h3>
      <p className="modal-text">{t('data.intro')}</p>
      <p className={storage.failed ? 'storage-warning' : 'storage-note'} role={storage.failed ? 'alert' : undefined}>
        {storage.failed ? t('data.storageFull') : t('data.storage', { games: games.toLocaleString(lang), size: formatBytes(lang, storage.bytes) })}
      </p>
      <div className="modal-actions data-actions">
        <button type="button" className="modal-btn" onClick={() => onExport('json')}>
          {t('data.exportJson')}
        </button>
        <button type="button" className="modal-btn modal-btn-secondary" onClick={() => onExport('csv')}>
          {t('data.exportCsv')}
        </button>
        <button type="button" className="modal-btn modal-btn-secondary" onClick={() => input.current?.click()}>
          {t('data.import')}
        </button>
        <input
          ref={input}
          type="file"
          accept=".json,application/json"
          className="sr-only"
          aria-label={t('data.import')}
          data-testid="backup-file"
          onChange={(e) => {
            void onFile(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
      </div>

      {error && (
        <p className="warn-box" role="alert">
          {t(error)}
        </p>
      )}
      {done && (
        <p className="modal-text" role="status">
          {t(done)}
        </p>
      )}

      {backup && info && (
        <div className="confirm-box" role="group" aria-label={t('data.import')}>
          <p className="modal-text">
            <strong>{t('data.preview', { date: info.exportedAt ? day(info.exportedAt) : '?', games: info.games })}</strong>
            {info.first !== null && info.last !== null && (
              <>
                <br />
                {t('data.range', { from: day(info.first), to: day(info.last) })}
              </>
            )}
          </p>
          {replacing ? (
            <div role="alertdialog" aria-labelledby="replace-warning">
              <p id="replace-warning" className="warn-box">
                {t('data.replaceWarning', { games })}
              </p>
              <label className="confirm-check">
                <input type="checkbox" checked={understood} onChange={(e) => setUnderstood(e.target.checked)} />
                <span>{t('data.replaceCheck')}</span>
              </label>
              <div className="modal-actions">
                <button
                  type="button"
                  className="modal-btn modal-btn-danger"
                  disabled={!understood}
                  onClick={() => {
                    onImport(backup, 'replace');
                    setDone('data.done.replace');
                    reset();
                  }}
                >
                  {t('data.replaceConfirm')}
                </button>
                <button type="button" className="modal-btn modal-btn-secondary" onClick={reset} autoFocus>
                  {t('data.cancel')}
                </button>
              </div>
            </div>
          ) : (
            <>
              <p className="modal-text">{t('data.mergeHint')}</p>
              <div className="modal-actions">
                <button
                  type="button"
                  className="modal-btn"
                  onClick={() => {
                    onImport(backup, 'merge');
                    setDone('data.done.merge');
                    reset();
                  }}
                  data-autofocus
                >
                  {t('data.merge')}
                </button>
                <button type="button" className="modal-btn modal-btn-danger" onClick={() => setReplacing(true)}>
                  {t('data.replace')}
                </button>
                <button type="button" className="modal-btn modal-btn-secondary" onClick={reset}>
                  {t('data.cancel')}
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
