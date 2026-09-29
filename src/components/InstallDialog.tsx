import { Dialog } from './Dialog';
import { translate } from '../i18n';
import type { Language, MessageKey } from '../i18n';

interface InstallDialogProps {
  lang: Language;
  kind: 'ios' | 'safari-mac';
  onClose: () => void;
}

/** Safari cannot show an install prompt, so explain the manual steps. */
export function InstallDialog({ lang, kind, onClose }: InstallDialogProps) {
  const t = (key: MessageKey) => translate(lang, key);
  const steps: MessageKey[] =
    kind === 'ios' ? ['install.ios.step1', 'install.ios.step2', 'install.ios.step3'] : ['install.mac.step1', 'install.mac.step2'];
  return (
    <Dialog titleId="install-title" title={t('install.title')} onClose={onClose}>
      <ol className="install-steps modal-text-left">
        {steps.map((key) => (
          <li key={key}>{t(key)}</li>
        ))}
      </ol>
      <div className="modal-actions">
        <button type="button" className="modal-btn" onClick={onClose} data-autofocus>
          {t('btn.close')}
        </button>
      </div>
    </Dialog>
  );
}
