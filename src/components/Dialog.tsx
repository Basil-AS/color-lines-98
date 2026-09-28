import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';

interface DialogProps {
  titleId: string;
  title: string;
  onClose?: () => void;
  /** An alert dialog demands a choice: Escape and backdrop clicks do nothing. */
  alert?: boolean;
  children: ReactNode;
}

const FOCUSABLE = 'button:not([disabled]), [href], select, input, [tabindex]:not([tabindex="-1"])';

export function Dialog({ titleId, title, onClose, alert = false, children }: DialogProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    const node = ref.current;
    const first = node?.querySelector<HTMLElement>('[data-autofocus]') ?? node;
    first?.focus();

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !alert && onClose) {
        e.stopPropagation();
        onClose();
        return;
      }
      if (e.key !== 'Tab' || !node) return;
      const items = [...node.querySelectorAll<HTMLElement>(FOCUSABLE)];
      if (items.length === 0) {
        e.preventDefault();
        return;
      }
      const firstItem = items[0];
      const lastItem = items[items.length - 1];
      if (e.shiftKey && (document.activeElement === firstItem || document.activeElement === node)) {
        e.preventDefault();
        lastItem.focus();
      } else if (!e.shiftKey && document.activeElement === lastItem) {
        e.preventDefault();
        firstItem.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      opener?.focus();
    };
  }, [alert, onClose]);

  return (
    <div className="modal-overlay" onClick={alert ? undefined : onClose}>
      <div
        ref={ref}
        className="modal-content"
        role={alert ? 'alertdialog' : 'dialog'}
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal-title" id={titleId}>
          {title}
        </h2>
        {children}
      </div>
    </div>
  );
}
