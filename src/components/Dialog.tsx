import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';

interface DialogProps {
  titleId: string;
  title: string;
  onClose?: () => void;
  onSwipe?: (direction: 'left' | 'right') => void;
  /** An alert dialog demands a choice: Escape and backdrop clicks do nothing. */
  alert?: boolean;
  children: ReactNode;
}

const FOCUSABLE = 'button:not([disabled]), [href], select, input, [tabindex]:not([tabindex="-1"])';

export function Dialog({ titleId, title, onClose, onSwipe, alert = false, children }: DialogProps) {
  const ref = useRef<HTMLDivElement>(null);
  const touchStart = useRef<{ id: number; x: number; y: number } | null>(null);

  const handleTouchStart = (event: React.TouchEvent<HTMLDivElement>) => {
    const touch = event.touches.length === 1 ? event.touches[0] : null;
    const target = event.target;
    if (!onSwipe || !touch || !(target instanceof Element)) {
      touchStart.current = null;
      return;
    }
    if (target.closest('button, a, input, select, textarea, [role="button"], [role="tab"], [role="slider"], [contenteditable="true"]')) {
      touchStart.current = null;
      return;
    }
    touchStart.current = { id: touch.identifier, x: touch.clientX, y: touch.clientY };
  };

  const handleTouchMove = (event: React.TouchEvent<HTMLDivElement>) => {
    const start = touchStart.current;
    if (!start) return;
    if (event.touches.length !== 1 || event.touches[0]?.identifier !== start.id) {
      touchStart.current = null;
    }
  };

  const handleTouchEnd = (event: React.TouchEvent<HTMLDivElement>) => {
    const start = touchStart.current;
    touchStart.current = null;
    const touch = event.changedTouches.length === 1 ? event.changedTouches[0] : null;
    if (!start || !touch || touch.identifier !== start.id || event.touches.length !== 0 || !onSwipe) return;
    const dx = touch.clientX - start.x;
    const dy = touch.clientY - start.y;
    if (Math.abs(dx) < 56 || Math.abs(dx) < Math.abs(dy) * 1.25) return;
    onSwipe(dx < 0 ? 'left' : 'right');
  };

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    const node = ref.current;
    const first = node?.querySelector<HTMLElement>('[data-autofocus]') ?? node;
    first?.focus({ preventScroll: true });

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
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onTouchCancel={() => {
          touchStart.current = null;
        }}
      >
        <h2 className="modal-title" id={titleId}>
          {title}
        </h2>
        {children}
      </div>
    </div>
  );
}
