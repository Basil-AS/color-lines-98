import { Dialog } from './Dialog';
import { LANGUAGES, translate } from '../i18n';
import type { MessageKey } from '../i18n';
import type { Language } from '../i18n';
import { THEME_FAMILIES } from '../appearance';
import type { CustomTheme, Look, LookKey, LookOverrides, LookShare } from '../appearance';
import { COLOR_MODES } from '../themes';
import type { ColorMode, Theme } from '../themes';
import { useState } from 'react';
import { LookBuilder } from './LookBuilder';
import { LookControls } from './LookControls';
import { LookPreview } from './LookPreview';
import type { LanguagePref } from '../storage';
import { EFFECT_LEVELS } from '../effects';
import type { EffectsLevel } from '../effects';
import { hapticsSupported } from '../haptics';

interface SettingsDialogProps {
  lang: Language;
  theme: Theme;
  onTheme: (theme: Theme) => void;
  colorMode: ColorMode;
  onColorMode: (mode: ColorMode) => void;
  langPref: LanguagePref;
  onLangPref: (pref: LanguagePref) => void;
  soundEnabled: boolean;
  onToggleSound: () => void;
  spawnPreview: boolean;
  onTogglePreview: () => void;
  effects: EffectsLevel;
  onEffects: (level: EffectsLevel) => void;
  vibration: boolean;
  onVibration: (on: boolean) => void;
  looks: LookOverrides | undefined;
  onLook: <K extends LookKey>(key: K, value: Look[K] | undefined) => void;
  onResetLook: () => void;
  custom: CustomTheme;
  onCustom: (custom: CustomTheme) => void;
  onApplyShare: (share: LookShare) => void;
  onClose: () => void;
}

type Tab = 'themes' | 'balls' | 'board' | 'builder' | 'game';
const TABS: readonly Tab[] = ['themes', 'balls', 'board', 'builder', 'game'];
const BALL_GROUPS: readonly LookKey[] = ['palette', 'volume', 'contrast', 'shapes', 'ballSize'];
const BOARD_GROUPS: readonly LookKey[] = ['grid', 'cells'];

export function SettingsDialog(props: SettingsDialogProps) {
  const { lang, theme, langPref } = props;
  const t = (key: MessageKey) => translate(lang, key);

  const [tab, setTab] = useState<Tab>('themes');
  const showPreview = tab !== 'game';

  const themeCard = (id: Theme) => (
    <button
      type="button"
      key={id}
      className={`theme-card ${theme === id ? 'active' : ''}`}
      aria-pressed={theme === id}
      onClick={() => props.onTheme(id)}
      data-autofocus={theme === id && tab === 'themes' ? true : undefined}
    >
      <span className={`theme-swatch swatch-${id}`} aria-hidden="true" />
      <span>{t(`theme.${id}` as MessageKey)}</span>
    </button>
  );

  return (
    <Dialog titleId="settings-title" title={t('settings.title')} onClose={props.onClose}>
      <div className="settings-head">
        <div className="settings-tabs" role="group" aria-label={t('settings.tabs')}>
          {TABS.map((id) => (
            <button type="button" key={id} className={`settings-tab ${tab === id ? 'active' : ''}`} aria-pressed={tab === id} onClick={() => setTab(id)}>
              {t(`settings.tab.${id}` as MessageKey)}
            </button>
          ))}
        </div>
        {showPreview && <LookPreview lang={lang} theme={theme} />}
      </div>

      {tab === 'themes' && (
        <div className="settings-section">
          <div className="settings-row settings-effects" role="group" aria-labelledby="colormode-label">
            <span id="colormode-label">{t('settings.colorMode')}</span>
            <div className="segmented">
              {COLOR_MODES.map((mode) => (
                <button type="button" key={mode} className={`segment ${props.colorMode === mode ? 'active' : ''}`} aria-pressed={props.colorMode === mode} onClick={() => props.onColorMode(mode)}>
                  {t(`colorMode.${mode}` as MessageKey)}
                </button>
              ))}
            </div>
          </div>
          {THEME_FAMILIES.map((family) => (
            <section key={family.id} className="theme-family" aria-labelledby={`family-${family.id}`}>
              <h3 className="settings-label" id={`family-${family.id}`}>
                {t(`family.${family.id}` as MessageKey)}
              </h3>
              <div className="theme-grid">{family.themes.map(themeCard)}</div>
            </section>
          ))}
        </div>
      )}

      {tab === 'balls' && (
        <div className="settings-section">
          <LookControls lang={lang} theme={theme} overrides={props.looks} onChange={props.onLook} onReset={props.onResetLook} groups={BALL_GROUPS} />
        </div>
      )}

      {tab === 'board' && (
        <div className="settings-section">
          <LookControls lang={lang} theme={theme} overrides={props.looks} onChange={props.onLook} onReset={props.onResetLook} groups={BOARD_GROUPS} />
        </div>
      )}

      {tab === 'builder' && (
        <div className="settings-section">
          <LookBuilder
            lang={lang}
            theme={theme}
            custom={props.custom}
            overrides={props.looks}
            onCustom={props.onCustom}
            onUseCustom={() => props.onTheme('custom')}
            onApplyShare={props.onApplyShare}
          />
        </div>
      )}

      {tab === 'game' && (
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
              </div>
      )}

      <div className="modal-actions settings-actions">
        <button type="button" className="modal-btn" onClick={props.onClose}>
          {t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
