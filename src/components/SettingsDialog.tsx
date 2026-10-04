import { Dialog } from './Dialog';
import { LANGUAGES, translate } from '../i18n';
import type { MessageKey } from '../i18n';
import type { Language } from '../i18n';
import { THEMES } from '../themes';
import type { Theme } from '../themes';
import type { LanguagePref } from '../storage';
import { EFFECT_LEVELS } from '../effects';
import type { EffectsLevel } from '../effects';
import { hapticsSupported } from '../haptics';

interface SettingsDialogProps {
  lang: Language;
  theme: Theme;
  onTheme: (theme: Theme) => void;
  langPref: LanguagePref;
  onLangPref: (pref: LanguagePref) => void;
  soundEnabled: boolean;
  onToggleSound: () => void;
  spawnPreview: boolean;
  onTogglePreview: () => void;
  playerName: string;
  onPlayerName: (name: string) => void;
  effects: EffectsLevel;
  onEffects: (level: EffectsLevel) => void;
  vibration: boolean;
  onVibration: (on: boolean) => void;
  onClose: () => void;
}

export function SettingsDialog(props: SettingsDialogProps) {
  const { lang, theme, langPref } = props;
  const t = (key: MessageKey) => translate(lang, key);

  return (
    <Dialog titleId="settings-title" title={t('settings.title')} onClose={props.onClose}>
      <div className="settings-section">
        <h3 className="settings-label" id="theme-label">
          {t('settings.theme')}
        </h3>
        <div className="theme-grid" role="group" aria-labelledby="theme-label">
          {THEMES.map((id) => (
            <button
              type="button"
              key={id}
              className={`theme-card ${theme === id ? 'active' : ''}`}
              aria-pressed={theme === id}
              onClick={() => props.onTheme(id)}
              data-autofocus={theme === id ? true : undefined}
            >
              <span className={`theme-swatch swatch-${id}`} aria-hidden="true" />
              <span>{t(`theme.${id}` as MessageKey)}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="settings-section">
        <label className="settings-row">
          <span>{t('settings.language')}</span>
          <select
            value={langPref}
            onChange={(e) => props.onLangPref(e.target.value as LanguagePref)}
          >
            <option value="auto">{t('lang.auto')}</option>
            {LANGUAGES.map((l) => (
              <option key={l} value={l}>
                {t(`lang.${l}` as MessageKey)}
              </option>
            ))}
          </select>
        </label>

        <label className="settings-row">
          <span>{t('settings.sound')}</span>
          <input
            type="checkbox"
            role="switch"
            checked={props.soundEnabled}
            onChange={props.onToggleSound}
          />
        </label>

        <label className="settings-row">
          <span>{t('settings.preview')}</span>
          <input
            type="checkbox"
            role="switch"
            checked={props.spawnPreview}
            onChange={props.onTogglePreview}
          />
        </label>

        <div className="settings-row settings-effects" role="group" aria-labelledby="effects-label">
          <span id="effects-label">{t('settings.effects')}</span>
          <div className="segmented">
            {EFFECT_LEVELS.map((level) => (
              <button
                type="button"
                key={level}
                className={`segment ${props.effects === level ? 'active' : ''}`}
                aria-pressed={props.effects === level}
                onClick={() => props.onEffects(level)}
              >
                {t(`effects.${level}` as MessageKey)}
              </button>
            ))}
          </div>
        </div>
        <p className="settings-hint">{t('settings.effectsHint')}</p>

        {hapticsSupported() && (
          <label className="settings-row">
            <span>{t('settings.vibration')}</span>
            <input type="checkbox" role="switch" checked={props.vibration} onChange={(e) => props.onVibration(e.target.checked)} />
          </label>
        )}

        <label className="settings-row">
          <span>{t('settings.playerName')}</span>
          <input
            type="text"
            className="settings-text"
            value={props.playerName}
            maxLength={12}
            onChange={(e) => props.onPlayerName(e.target.value)}
          />
        </label>
      </div>

      <div className="modal-actions">
        <button type="button" className="modal-btn" onClick={props.onClose}>
          {t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
