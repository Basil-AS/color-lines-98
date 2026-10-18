import { LOOK_OPTIONS, PALETTE_COLORS, effectiveLook, isStudioTheme } from '../appearance';
import type { Look, LookKey, LookOverrides } from '../appearance';
import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';
import type { Theme } from '../themes';

interface ControlsProps {
  lang: Language;
  theme: Theme;
  overrides: LookOverrides | undefined;
  onChange: <K extends LookKey>(key: K, value: Look[K] | undefined) => void;
  onReset: () => void;
  /** Which groups to show: the balls tab or the board tab. */
  groups: readonly LookKey[];
}

/** One group of choices: the chips show what is in force; until one is picked the theme decides. */
function Group<K extends LookKey>({ lang, name, theme, overrides, onChange }: { lang: Language; name: K } & Pick<ControlsProps, 'theme' | 'overrides'> & { onChange: ControlsProps['onChange'] }) {
  const t = (key: string) => translate(lang, key as MessageKey);
  const current = overrides?.[name];
  const effective = effectiveLook(theme, overrides)[name];
  const labelId = `look-${name}-label`;
  return (
    <div className="look-group" role="group" aria-labelledby={labelId}>
      <div className="look-head">
        <h4 className="look-label" id={labelId}>
          {t(`look.${name}`)}
        </h4>
        {current === undefined && <span className="look-caption">{t('look.followTheme')}</span>}
      </div>
      <div className="chips">
        {(LOOK_OPTIONS[name] as readonly Look[K][]).map((option) => {
          const id = String(option);
          return (
            <button type="button" key={id} className={`chip ${effective === option ? 'active' : ''}`} aria-pressed={effective === option} onClick={() => onChange(name, option)}>
              {name === 'palette' && id !== 'custom' && (
                <span className="chip-dots" aria-hidden="true">
                  {Object.values(PALETTE_COLORS[id as keyof typeof PALETTE_COLORS]).map((c, i) => (
                    <i key={i} style={{ background: c }} />
                  ))}
                </span>
              )}
              {t(`look.${name}.${id}`)}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function LookControls({ lang, theme, overrides, onChange, onReset, groups }: ControlsProps) {
  if (!isStudioTheme(theme)) {
    return <p className="settings-hint look-note">{translate(lang, 'look.retroNote')}</p>;
  }
  return (
    <div className="look-controls">
      {groups.map((name) => (
        <Group key={name} lang={lang} name={name} theme={theme} overrides={overrides} onChange={onChange} />
      ))}
      <button type="button" className="modal-btn look-reset" disabled={!overrides || Object.keys(overrides).length === 0} onClick={onReset}>
        {translate(lang, 'look.reset')}
      </button>
    </div>
  );
}
