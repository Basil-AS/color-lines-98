import { useEffect, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';

export interface MenuItem {
  label: string;
  onSelect: () => void;
  disabled?: boolean;
  /** Shows a check mark (a switch or the selected radio item). */
  checked?: boolean;
  separatorBefore?: boolean;
}

export interface MenuGroup {
  label: string;
  items: MenuItem[];
}

interface WinMenuProps {
  label: string;
  groups: MenuGroup[];
}

/** The menu bar of the Windows look, with real drop-down menus that work with mouse and keyboard. */
export function WinMenu({ label, groups }: WinMenuProps) {
  const [open, setOpen] = useState<number | null>(null);
  const barRef = useRef<HTMLDivElement>(null);
  const triggers = useRef<(HTMLButtonElement | null)[]>([]);

  useEffect(() => {
    if (open === null) return;
    const onDown = (e: MouseEvent) => {
      if (!barRef.current?.contains(e.target as Node)) setOpen(null);
    };
    const onEscape = (e: globalThis.KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      e.stopPropagation();
      setOpen(null);
      triggers.current[open]?.focus();
    };
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onEscape);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onEscape);
    };
  }, [open]);

  const focusItem = (menu: number, index: number) => {
    const items = barRef.current?.querySelectorAll<HTMLElement>(`[data-menu="${menu}"] [role^="menuitem"]:not([disabled])`);
    if (items && items.length > 0) items[(index + items.length) % items.length].focus();
  };

  const onTriggerKey = (e: KeyboardEvent, menu: number) => {
    if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setOpen(menu);
      window.setTimeout(() => focusItem(menu, 0), 0);
    } else if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const next = (menu + (e.key === 'ArrowRight' ? 1 : -1) + groups.length) % groups.length;
      triggers.current[next]?.focus();
      if (open !== null) setOpen(next);
    }
  };

  const onItemKey = (e: KeyboardEvent, menu: number) => {
    const list = Array.from(barRef.current?.querySelectorAll<HTMLElement>(`[data-menu="${menu}"] [role^="menuitem"]:not([disabled])`) ?? []);
    const at = list.indexOf(e.currentTarget as HTMLElement);
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      focusItem(menu, at + 1);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      focusItem(menu, at - 1);
    } else if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const next = (menu + (e.key === 'ArrowRight' ? 1 : -1) + groups.length) % groups.length;
      setOpen(next);
      triggers.current[next]?.focus();
    }
  };

  return (
    <div className="win98-menubar" role="menubar" aria-label={label} ref={barRef}>
      {groups.map((group, menu) => (
        <div className="win-menu" key={group.label} data-menu={menu}>
          <button
            type="button"
            role="menuitem"
            aria-haspopup="menu"
            aria-expanded={open === menu}
            className={open === menu ? 'open' : ''}
            ref={(el) => {
              triggers.current[menu] = el;
            }}
            onClick={() => setOpen(open === menu ? null : menu)}
            onMouseEnter={() => open !== null && setOpen(menu)}
            onKeyDown={(e) => onTriggerKey(e, menu)}
          >
            {group.label}
          </button>
          {open === menu && (
            <ul className="win-menu-list" role="menu" aria-label={group.label}>
              {group.items.map((item) => (
                <li key={item.label} role="none" className={item.separatorBefore ? 'separator' : undefined}>
                  <button
                    type="button"
                    role={item.checked === undefined ? 'menuitem' : 'menuitemcheckbox'}
                    aria-checked={item.checked}
                    disabled={item.disabled}
                    onClick={() => {
                      setOpen(null);
                      item.onSelect();
                    }}
                    onKeyDown={(e) => onItemKey(e, menu)}
                  >
                    <span className="win-check" aria-hidden="true">
                      {item.checked ? '✓' : ''}
                    </span>
                    {item.label}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  );
}
